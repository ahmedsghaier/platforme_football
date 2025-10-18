import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, Subject, of, throwError, timer } from 'rxjs';
import { map, catchError, takeUntil, timeout, tap, finalize, retry, delay, switchMap } from 'rxjs/operators';
import { Auth } from './auth';

export interface DashboardStats {
  searchesThisMonth: number;
  searchGrowthPercentage: number;
  favoritePlayers: number;
  favoritePlayersAddedThisWeek: number;
  comparisons: number;
  comparisonGrowthPercentage: number;
  reportsExported: number;
  reportsExportedThisWeek: number;
}

export interface RecentSearch {
  id?: number;
  playerName: string;
  club: string;
  date: string;
  playerId?: number;
}

export interface FavoritePlayer {
  id?: number;
  name: string;
  club: string;
  value: string;
  trend: string;
  playerId?: number;
}

export interface Alert {
  id?: number;
  type: 'increase' | 'opportunity' | 'market';
  player: string;
  message: string;
  time: string;
  isRead?: boolean;
}

export interface MarketTrends {
  attackersGrowth: number;
  midfieldersGrowth: number;
  youngTalentsGrowth: number;
  defendersGrowth: number;
}

export interface CompleteDashboard {
    stats: DashboardStats;
    recentSearches: RecentSearch[];
    recentComparisons: RecentSearch[];
    favoritePlayers: FavoritePlayer[];
    alerts: Alert[];
    marketTrends: MarketTrends;
}

export interface UserSearchLog {
    userId?: number;
    playerId?: number | null; // Permettre null explicitement
    searchQuery: string;
    type?: 'SEARCH' | 'COMPARISON';
    searchFilters?: any;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService implements OnDestroy {
  private readonly apiUrl = 'http://localhost:8080/api/dashboard';
  private readonly REQUEST_TIMEOUT = 10000; // 10 secondes
  private readonly MAX_RETRY_ATTEMPTS = 2;

  // Subject pour nettoyer les subscriptions
  private destroy$ = new Subject<void>();
  
  // BehaviorSubjects pour la réactivité
  private dashboardDataSubject = new BehaviorSubject<CompleteDashboard | null>(null);
  public dashboardData$ = this.dashboardDataSubject.asObservable();
  
  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();
  
  private errorSubject = new BehaviorSubject<string | null>(null);
  public error$ = this.errorSubject.asObservable();

  // Contrôle des requêtes en cours
  private currentRequest: Observable<CompleteDashboard> | null = null;
  private requestInProgress = false;

  constructor(
    private http: HttpClient,
    private authService: Auth
  ) {
    console.log('DashboardService initialized');
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.dashboardDataSubject.complete();
    this.loadingSubject.complete();
    this.errorSubject.complete();
  }

  /**
   * Gestion centralisée des erreurs HTTP avec logging détaillé
   */
  private handleError = (error: HttpErrorResponse, context: string = 'Unknown'): Observable<never> => {
    console.error(`[DashboardService] ${context} Error:`, {
      status: error.status,
      statusText: error.statusText,
      url: error.url,
      message: error.message,
      error: error.error,
      timestamp: new Date().toISOString()
    });

    let errorMessage: string;
    
    switch (error.status) {
      case 0:
        errorMessage = 'Impossible de joindre le serveur. Vérifiez votre connexion réseau.';
        break;
      case 400:
        errorMessage = error.error?.message || 'Données de requête invalides.';
        break;
      case 401:
        errorMessage = 'Session expirée. Veuillez vous reconnecter.';
        // Déclencher une vérification d'auth après un délai
        timer(100).subscribe(() => {
          if (this.authService.isAuthenticated()) {
            console.log('Tentative de vérification du token après erreur 401');
            this.authService.verifyToken().subscribe({
              error: () => console.log('Token invalide confirmé')
            });
          }
        });
        break;
      case 403:
        errorMessage = 'Accès refusé à cette ressource.';
        break;
      case 404:
        errorMessage = 'Ressource non trouvée sur le serveur.';
        break;
      case 500:
        errorMessage = 'Erreur interne du serveur. Veuillez réessayer plus tard.';
        break;
      case 503:
        errorMessage = 'Service temporairement indisponible.';
        break;
      default:
        errorMessage = error.error?.message || `Erreur serveur (${error.status}): ${error.statusText}`;
    }

    // Mettre à jour l'état d'erreur de manière asynchrone
    setTimeout(() => {
      this.errorSubject.next(errorMessage);
    }, 0);

    this.loadingSubject.next(false);
    this.requestInProgress = false;
    
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Crée les paramètres HTTP avec authentification et validation
   */
  private createHttpParams(additionalParams?: { [key: string]: string }): HttpParams {
    let params = new HttpParams();
    
    const userId = this.authService.getCurrentUserId();
    if (!userId || isNaN(userId) || userId <= 0) {
      throw new Error('ID utilisateur invalide ou manquant');
    }
    
    params = params.set('userId', userId.toString());
    console.log(`Using userId: ${userId}`);
    
    if (additionalParams) {
      Object.keys(additionalParams).forEach(key => {
        if (additionalParams[key] !== null && additionalParams[key] !== undefined) {
          params = params.set(key, additionalParams[key]);
        }
      });
    }
    
    return params;
  }

  /**
   * Vérifie si l'utilisateur est authentifié avec logging
   */
  private checkAuthentication(): boolean {
    const isAuth = this.authService.isAuthenticated();
    const userId = this.authService.getCurrentUserId();
    const user = this.authService.getCurrentUser();
    
    console.log('Auth check:', { isAuth, userId, hasUser: !!user });
    
    if (!isAuth) {
      setTimeout(() => {
        this.errorSubject.next('Vous devez être connecté pour accéder à ces données.');
      }, 0);
      return false;
    }
    
    if (!userId || userId <= 0) {
      setTimeout(() => {
        this.errorSubject.next('ID utilisateur invalide. Veuillez vous reconnecter.');
      }, 0);
      return false;
    }
    
    return true;
  }

  /**
   * Teste la connectivité avec le serveur
   */
  testConnection(): Observable<boolean> {
    console.log('Testing server connection...');
    
    return this.http.get(`${this.apiUrl}/test`, { 
      responseType: 'json'
    }).pipe(
      timeout(5000),
      map((response: any) => {
        console.log('Connection test successful:', response);
        return true;
      }),
      catchError(error => {
        console.warn('Connection test failed:', error);
        return of(false);
      })
    );
  }

  /**
   * Récupère toutes les données du dashboard avec gestion d'erreur robuste
   */
  getCompleteDashboard(): Observable<CompleteDashboard> {
    console.log('getCompleteDashboard called');
    
    // Éviter les requêtes simultanées
    if (this.requestInProgress && this.currentRequest) {
      console.log('Request already in progress, returning existing request');
      return this.currentRequest;
    }

    if (!this.checkAuthentication()) {
      return throwError(() => new Error('Non authentifié'));
    }

    this.requestInProgress = true;
    this.loadingSubject.next(true);
    this.errorSubject.next(null);

    console.log('Starting dashboard request...');

    try {
      const params = this.createHttpParams();
      const headers = this.authService.getAuthHeaders();

      console.log('Request params:', params.toString());
      console.log('Request headers:', headers.keys());

      this.currentRequest = this.http.get<CompleteDashboard>(`${this.apiUrl}/complete`, { 
        params, 
        headers 
      }).pipe(
        timeout(this.REQUEST_TIMEOUT),
        retry({
          count: this.MAX_RETRY_ATTEMPTS,
          delay: (error, retryCount) => {
            console.log(`Retry attempt ${retryCount} after error:`, error.message);
            return timer(Math.min(1000 * retryCount, 3000));
          }
        }),
        takeUntil(this.destroy$),
        tap(data => {
          console.log('Dashboard data received:', {
            hasStats: !!data?.stats,
            searchesCount: data?.recentSearches?.length || 0,
            favoritesCount: data?.favoritePlayers?.length || 0,
            alertsCount: data?.alerts?.length || 0,
            hasTrends: !!data?.marketTrends
          });
          
          const validateDashboardData = (data: any) => {
    if (!data) return false;
    if (!data.recentComparisons) return false;
    if (!Array.isArray(data.recentComparisons)) return false;
    return true;
};
        }),
        map(data => {
          // Mettre à jour les sujets de manière asynchrone
          setTimeout(() => {
            this.dashboardDataSubject.next(data);
            this.loadingSubject.next(false);
            this.errorSubject.next(null);
          }, 0);
          return data;
        }),
        finalize(() => {
          console.log('Dashboard request finalized');
          this.requestInProgress = false;
          this.currentRequest = null;
          this.loadingSubject.next(false);
        }),
        catchError(error => {
          console.error('Dashboard request failed:', error);
          this.requestInProgress = false;
          this.currentRequest = null;
          
          // En cas d'erreur, essayer de charger des données de fallback
          return this.loadFallbackData().pipe(
            catchError(() => this.handleError(error, 'getCompleteDashboard'))
          );
        })
      );

      return this.currentRequest;
    } catch (error) {
      console.error('Error creating dashboard request:', error);
      this.requestInProgress = false;
      this.loadingSubject.next(false);
      return this.loadFallbackData().pipe(
        catchError(() => throwError(() => error))
      );
    }
  }

  /**
   * Charge les données de secours en cas d'échec
   */
  private loadFallbackData(): Observable<CompleteDashboard> {
    console.log('Loading fallback data...');
    
    const fallbackData: CompleteDashboard = {
      stats: {
        searchesThisMonth: 0,
        searchGrowthPercentage: 0,
        favoritePlayers: 0,
        favoritePlayersAddedThisWeek: 0,
        comparisons: 0,
        comparisonGrowthPercentage: 0,
        reportsExported: 0,
        reportsExportedThisWeek: 0
      },
      recentSearches: [],
      recentComparisons: [],
      favoritePlayers: [],
      alerts: [],
      marketTrends: {
        attackersGrowth: 0,
        midfieldersGrowth: 0,
        youngTalentsGrowth: 0,
        defendersGrowth: 0
      }
    };

    // Mettre à jour de manière asynchrone
    setTimeout(() => {
      this.dashboardDataSubject.next(fallbackData);
      this.errorSubject.next('Données limitées disponibles. Certaines fonctionnalités peuvent être indisponibles.');
    }, 0);

    return of(fallbackData);
  }

  /**
   * Valide la structure des données reçues avec logging détaillé
   */
  private isValidDashboardData(data: any): data is CompleteDashboard {
    if (!data || typeof data !== 'object') {
      console.error('Dashboard data is null or not an object');
      return false;
    }

    const validations = [
      { condition: data.stats && typeof data.stats === 'object', name: 'stats' },
      { condition: Array.isArray(data.recentSearches), name: 'recentSearches' },
      { condition: Array.isArray(data.recentComparisons), name: 'recentComparisons' },
      { condition: Array.isArray(data.favoritePlayers), name: 'favoritePlayers' },
      { condition: Array.isArray(data.alerts), name: 'alerts' },
      { condition: data.marketTrends && typeof data.marketTrends === 'object', name: 'marketTrends' }
    ];

    for (const validation of validations) {
      if (!validation.condition) {
        console.error(`Invalid dashboard data: missing or invalid ${validation.name}`);
        return false;
      }
    }

    return true;
  }

  /**
   * Récupère uniquement les statistiques avec retry logic
   */
  getDashboardStats(): Observable<DashboardStats> {
    if (!this.checkAuthentication()) {
      return throwError(() => new Error('Non authentifié'));
    }

    const params = this.createHttpParams();
    const headers = this.authService.getAuthHeaders();
    
    return this.http.get<DashboardStats>(`${this.apiUrl}/stats`, { params, headers }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      retry(this.MAX_RETRY_ATTEMPTS),
      takeUntil(this.destroy$),
      catchError(error => this.handleError(error, 'getDashboardStats'))
    );
  }

  /**
   * Récupère les recherches récentes avec gestion d'erreur améliorée
   */
 getRecentSearches(): Observable<RecentSearch[]> {
  if (!this.checkAuthentication()) {
    return of([]);
  }

  const params = this.createHttpParams();
  const headers = this.authService.getAuthHeaders();

  return this.http.get<RecentSearch[]>(`${this.apiUrl}/recent-searches`, { params, headers }).pipe(
    timeout(this.REQUEST_TIMEOUT),
    takeUntil(this.destroy$),
    map(searches => {
      // CORRECTION : Filtrer les recherches valides côté frontend aussi
      return searches.filter(search => 
        search && 
        search.playerName && 
        search.playerName.trim() !== '' &&
        search.playerName !== 'Données de test'
      );
    }),
    catchError(error => {
      console.warn('Error fetching recent searches:', error);
      return of([]);
    })
  );
}

  /**
   * Récupère les comparaisons récentes
   */
  getRecentComparisons(): Observable<RecentSearch[]> {
    if (!this.checkAuthentication()) {
      return of([]);
    }

    const params = this.createHttpParams();
    const headers = this.authService.getAuthHeaders();
    
    return this.http.get<RecentSearch[]>(`${this.apiUrl}/recent-comparisons`, { params, headers }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      takeUntil(this.destroy$),
      catchError(error => {
        console.warn('Error fetching recent comparisons:', error);
        return of([]);
      })
    );
  }

  /**
   * Récupère les joueurs favoris
   */
  getFavoritePlayers(): Observable<FavoritePlayer[]> {
    if (!this.checkAuthentication()) {
      return of([]);
    }

    const params = this.createHttpParams();
    const headers = this.authService.getAuthHeaders();
    
    return this.http.get<FavoritePlayer[]>(`${this.apiUrl}/favorite-players`, { params, headers }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      takeUntil(this.destroy$),
      catchError(error => {
        console.warn('Error fetching favorite players:', error);
        return of([]);
      })
    );
  }

  /**
   * Ajoute ou retire un joueur des favoris avec validation
   */
  toggleFavoritePlayer(playerId: number): Observable<string> {
    if (!this.checkAuthentication()) {
      return throwError(() => new Error('Non authentifié'));
    }

    if (!playerId || playerId <= 0) {
      return throwError(() => new Error('ID de joueur invalide'));
    }

    console.log('Toggling favorite for player:', playerId);

    const params = this.createHttpParams();
    const headers = this.authService.getAuthHeaders();
    
    return this.http.post(`${this.apiUrl}/favorite-players/${playerId}`, {}, { 
      params, 
      headers,
      responseType: 'json'
    }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      takeUntil(this.destroy$),
      map((response: any) => {
        console.log('Toggle favorite response:', response);
        return response.message || 'Favori mis à jour';
      }),
      catchError(error => this.handleError(error, 'toggleFavoritePlayer'))
    );
  }

  /**
   * Récupère les alertes
   */
  getAlerts(): Observable<Alert[]> {
    if (!this.checkAuthentication()) {
      return of([]);
    }

    const params = this.createHttpParams();
    const headers = this.authService.getAuthHeaders();
    
    return this.http.get<Alert[]>(`${this.apiUrl}/alerts`, { params, headers }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      takeUntil(this.destroy$),
      catchError(error => {
        console.warn('Error fetching alerts:', error);
        return of([]);
      })
    );
  }

  /**
   * Marque une alerte comme lue
   */
  markAlertAsRead(alertId: number): Observable<any> {
    if (!this.checkAuthentication()) {
      return throwError(() => new Error('Non authentifié'));
    }

    const params = this.createHttpParams();
    const headers = this.authService.getAuthHeaders();
    
    return this.http.put(`${this.apiUrl}/alerts/${alertId}/read`, {}, { params, headers }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      takeUntil(this.destroy$),
      catchError(error => this.handleError(error, 'markAlertAsRead'))
    );
  }

  /**
   * Récupère les tendances du marché
   */
  getMarketTrends(): Observable<MarketTrends> {
    const headers = this.authService.getAuthHeaders();
    
    return this.http.get<MarketTrends>(`${this.apiUrl}/market-trends`, { headers }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      takeUntil(this.destroy$),
      catchError(error => {
        console.warn('Error fetching market trends:', error);
        return of({
          attackersGrowth: 0,
          midfieldersGrowth: 0,
          youngTalentsGrowth: 0,
          defendersGrowth: 0
        });
      })
    );
  }

  /**
   * Enregistre une recherche utilisateur avec gestion d'erreur silencieuse
   */
  logUserSearch(searchLog: UserSearchLog): Observable<string> {
    if (!this.checkAuthentication()) {
      return of('Non authentifié - log ignoré');
    }
    if (!searchLog.searchQuery || searchLog.searchQuery.trim().length === 0) {
      return of('Recherche vide ignorée');
    }
    
    const headers = this.authService.getAuthHeaders();
    const endpoint = searchLog.type?.toLowerCase() === 'comparison' ? 'log-comparison' : 'log-search';
    
    // Préparer les données avec validation du playerId
    const logData = {
      userId: this.authService.getCurrentUserId(),
      playerId: searchLog.playerId || null, // S'assurer que playerId est présent
      searchQuery: searchLog.searchQuery.trim(),
      type: searchLog.type || 'SEARCH',
      searchFilters: searchLog.searchFilters || null
    };
    
    // Debug log pour vérifier les données envoyées
    console.log('Données envoyées pour logging:', logData);
    
    return this.http.post(`${this.apiUrl}/${endpoint}`, logData, {
      headers,
      responseType: 'json'
    }).pipe(
      timeout(this.REQUEST_TIMEOUT),
      takeUntil(this.destroy$),
      map((response: any) => {
        console.log(`${searchLog.type || 'Search'} logged successfully:`, response);
        return response.message || 'Log enregistré';
      }),
      catchError(error => {
        console.warn(`Error logging ${searchLog.type || 'search'}:`, error);
        return of(`Erreur d'enregistrement ${searchLog.type || 'search'}`);
      })
    );
}


  /**
   * Exporte un rapport
   */
  exportReport(format: 'pdf' | 'excel'): Observable<Blob> {
    if (!this.checkAuthentication()) {
      return throwError(() => new Error('Non authentifié'));
    }

    console.log('Exporting report in format:', format);

    const params = this.createHttpParams({ format });
    const headers = this.authService.getAuthHeaders();
      
    return this.http.post(`${this.apiUrl}/export-report`, {}, {
      params,
      headers,
      responseType: 'blob'
    }).pipe(
      timeout(30000), // Plus de temps pour l'export
      takeUntil(this.destroy$),
      tap(blob => {
        console.log('Report exported successfully, size:', blob.size);
      }),
      catchError(error => this.handleError(error, 'exportReport'))
    );
  }

  /**
   * Rafraîchit les données du dashboard sans boucle infinie
   */
  refreshDashboard(): void {
    if (this.requestInProgress) {
      console.log('Refresh ignored: request already in progress');
      return;
    }

    console.log('Refreshing dashboard...');
    this.getCompleteDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => console.log('Dashboard refreshed successfully'),
        error: error => console.warn('Error refreshing dashboard:', error)
      });
  }

  /**
   * Vérifie la connexion au serveur avec test de connectivité
   */
  checkServerConnection(): Observable<boolean> {
    return this.testConnection().pipe(
      switchMap(isConnected => {
        if (isConnected) {
          // Test plus approfondi avec un endpoint spécifique
          return this.http.get(`${this.apiUrl}/health`, { 
            responseType: 'text'
          }).pipe(
            timeout(3000),
            map(() => true),
            catchError(() => of(false))
          );
        }
        return of(false);
      }),
      catchError(error => {
        console.warn('Server connection check failed:', error);
        return of(false);
      })
    );
  }

  /**
   * Nettoie les erreurs de manière asynchrone
   */
  clearErrors(): void {
    setTimeout(() => {
      this.errorSubject.next(null);
    }, 0);
  }

  /**
   * Vérifie si une action peut être effectuée
   */
  canPerformAction(action: string): boolean {
    if (!this.authService.isAuthenticated()) {
      return false;
    }

    const user = this.authService.getCurrentUser();
    if (!user) return false;

    switch (action) {
      case 'export':
      case 'compare':
      case 'favorites':
        return true;
      case 'alerts':
        return user.accountType !== 'basic';
      default:
        return true;
    }
  }

  // Méthodes utilitaires pour le formatage

  formatValue(value: number): string {
    if (!value || isNaN(value)) return 'N/A';
    if (value >= 1000000) {
      return (value / 1000000).toFixed(0) + 'M€';
    } else if (value >= 1000) {
      return (value / 1000).toFixed(0) + 'K€';
    }
    return value + '€';
  }

  formatGrowthPercentage(percentage: number): string {
    if (!percentage || isNaN(percentage)) return '0%';
    const sign = percentage >= 0 ? '+' : '';
    return sign + percentage.toFixed(0) + '%';
  }

  getTrendColor(percentage: number): string {
    if (!percentage || isNaN(percentage)) return 'text-gray-600';
    return percentage >= 0 ? 'text-green-600' : 'text-red-600';
  }

  getTrendIcon(percentage: number): string {
    if (!percentage || isNaN(percentage)) return 'ri-minus-line';
    return percentage >= 0 ? 'ri-arrow-up-line' : 'ri-arrow-down-line';
  }

  getAlertIconClass(type: string): string {
    switch (type) {
      case 'increase':
        return 'ri-arrow-up-line text-green-600';
      case 'opportunity':
        return 'ri-lightbulb-line text-blue-600';
      case 'market':
        return 'ri-trending-up-line text-purple-600';
      default:
        return 'ri-information-line text-gray-600';
    }
  }

  getAlertBackgroundClass(type: string): string {
    switch (type) {
      case 'increase':
        return 'bg-green-100';
      case 'opportunity':
        return 'bg-blue-100';
      case 'market':
        return 'bg-purple-100';
      default:
        return 'bg-gray-100';
    }
  }

  downloadBlob(blob: Blob, filename: string): void {
    try {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      console.log('File downloaded successfully:', filename);
    } catch (error) {
      console.error('Error downloading file:', error);
    }
  }

  generateFilename(format: string): string {
    const date = new Date().toISOString().split('T')[0];
    const extension = format === 'pdf' ? 'pdf' : 'xlsx';
    return `rapport_dashboard_${date}.${extension}`;
  }

  getCurrentUser() {
    return this.authService.getCurrentUser();
  }

  /**
   * Obtient les données actuelles du dashboard (sync)
   */
  getCurrentDashboardData(): CompleteDashboard | null {
    return this.dashboardDataSubject.value;
  }

  /**
   * Vérifie si le service est en cours de chargement
   */
  isLoading(): boolean {
    return this.loadingSubject.value;
  }

  /**
   * Obtient l'erreur actuelle
   */
  getCurrentError(): string | null {
    return this.errorSubject.value;
  }

  /**
   * Force le rechargement des données avec nettoyage
   */
  forceReload(): void {
    console.log('Force reloading dashboard data...');
    this.requestInProgress = false;
    this.currentRequest = null;
    this.clearErrors();
    this.dashboardDataSubject.next(null);
    this.getCompleteDashboard().subscribe({
      next: () => console.log('Force reload completed'),
      error: error => console.error('Force reload failed:', error)
    });
  }

  /**
   * Réinitialise complètement le service
   */
  reset(): void {
    console.log('Resetting dashboard service...');
    this.requestInProgress = false;
    this.currentRequest = null;
    this.dashboardDataSubject.next(null);
    this.loadingSubject.next(false);
    this.errorSubject.next(null);
  }

  /**
   * Obtient des statistiques sur l'état du service
   */
  getServiceStats(): { 
    isLoading: boolean; 
    hasData: boolean; 
    hasError: boolean; 
    requestInProgress: boolean 
  } {
    return {
      isLoading: this.loadingSubject.value,
      hasData: !!this.dashboardDataSubject.value,
      hasError: !!this.errorSubject.value,
      requestInProgress: this.requestInProgress
    };
  }
 logComparison(comparisonData: {
  playerIds: number[];
  comparisonName?: string;
  comparisonType?: string;
}): Observable<string> {
  if (!this.checkAuthentication()) {
    return of('Non authentifié - log ignoré');
  }

  const headers = this.authService.getAuthHeaders();
  const logData = {
    userId: this.authService.getCurrentUserId(),
    searchQuery: comparisonData.comparisonName || `Comparaison ${new Date().toLocaleDateString()}`,
    type: 'COMPARISON',
    playerId: null,
    searchFilters: {
      comparisonType: comparisonData.comparisonType || 'manual',
      playerCount: comparisonData.playerIds.length,
      playerIds: comparisonData.playerIds
    }
  };

  console.log('Logging comparison:', logData);

  return this.http.post(`${this.apiUrl}/log-comparison`, logData, {
    headers,
    responseType: 'json'
  }).pipe(
    timeout(this.REQUEST_TIMEOUT),
    takeUntil(this.destroy$),
    map((response: any) => {
      console.log('Comparaison loggée avec succès:', response);
      // NOUVEAU: Déclencher un rafraîchissement après un délai
      timer(1000).subscribe(() => {
        this.refreshDataSilently();
      });
      return response.message || 'Comparaison loggée';
    }),
    catchError(error => {
      console.warn('Erreur lors du logging de la comparaison:', error);
      return of('Erreur d\'enregistrement de la comparaison');
    })
  );
}

logExport(exportData: {
  format: 'pdf' | 'excel';
  dataType: 'dashboard' | 'comparison' | 'player';
  playerIds?: number[];
}): Observable<string> {
  if (!this.checkAuthentication()) {
    return of('Non authentifié - log ignoré');
  }

  const headers = this.authService.getAuthHeaders();
  const logData = {
    userId: this.authService.getCurrentUserId(),
    exportFormat: exportData.format,
    dataType: exportData.dataType,
    playerIds: exportData.playerIds || [],
    timestamp: new Date().toISOString()
  };

  console.log('Logging export:', logData);

  return this.http.post(`${this.apiUrl}/log-export`, logData, {
    headers,
    responseType: 'json'
  }).pipe(
    timeout(this.REQUEST_TIMEOUT),
    takeUntil(this.destroy$),
    map((response: any) => {
      console.log('Export loggé avec succès:', response);
      // NOUVEAU: Déclencher un rafraîchissement après un délai
      timer(1000).subscribe(() => {
        this.refreshDataSilently();
      });
      return response.message || 'Export loggé';
    }),
    catchError(error => {
      console.warn('Erreur lors du logging de l\'export:', error);
      return of('Erreur d\'enregistrement de l\'export');
    })
  );
}
private refreshDataSilently(): void {
 
  if (!this.authService.isAuthenticated()) {
    console.log('User not authenticated, skipping silent refresh');
    return;
  }

  if (this.requestInProgress) {
    console.log('Request already in progress, skipping silent refresh');
    return;
  }

  console.log('Rafraîchissement silencieux des stats...');
  
  this.getCompleteDashboard()
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (data) => {
        console.log('Stats rafraîchies silencieusement');
      
      },
      error: (error) => {
        console.warn('Échec du rafraîchissement silencieux:', error);
      }
    });
}
}