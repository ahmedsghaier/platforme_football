import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil, finalize, timer, combineLatest ,switchMap} from 'rxjs';

import { 
  DashboardService, 
  CompleteDashboard, 
  DashboardStats,
  RecentSearch,
  FavoritePlayer,
  Alert,
  MarketTrends
} from '../dashboard-service';

import { Auth, User } from '../auth';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class Dashboard implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private retryAttempts = 0;
  private maxRetryAttempts = 3;
  private initializationComplete = false;
  
  // États de chargement
  isLoading = true;
  isExporting = false;
  isRefreshing = false;
  serverConnectionStatus: 'connected' | 'disconnected' | 'checking' = 'checking';
  
  // Modal d'export
  showExportModal = false;
  exportFormat: 'pdf' | 'excel' = 'pdf';
  
  // Données du dashboard
  dashboardData: CompleteDashboard | null = null;
  stats: DashboardStats | null = null;
  recentSearches: RecentSearch[] = [];
  favoritePlayers: FavoritePlayer[] = [];
  alerts: Alert[] = [];
  marketTrends: MarketTrends | null = null;
  
  // Messages d'erreur et statut
  errorMessage: string | null = null;
  warningMessage: string | null = null;
  isOfflineMode = false;
  
  // Utilisateur connecté
  currentUser: User | null = null;
  isAuthenticated = false;

  constructor(
    private router: Router,
    private dashboardService: DashboardService,
    private authService: Auth,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Retarder l'initialisation pour s'assurer que l'auth service est prêt
    setTimeout(() => {
      this.initializeComponent();
    }, 100);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialise le composant
   */
  private initializeComponent(): void {
    console.log('Initialisation du dashboard...');
    
    // Vérifier l'état initial de l'authentification
    this.checkInitialAuthState();
    
    // S'abonner aux changements d'authentification
    this.authService.isAuthenticated$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isAuth => {
        console.log('Changement d\'authentification:', isAuth);
        this.isAuthenticated = isAuth;
        
        if (isAuth && this.initializationComplete) {
          this.handleAuthenticated();
        } else if (!isAuth) {
          this.handleUnauthenticated();
        }
        
        this.cdr.detectChanges();
      });

    // S'abonner aux changements d'utilisateur
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        console.log('Changement d\'utilisateur:', user);
        this.currentUser = user;
        this.cdr.detectChanges();
      });

    this.initializationComplete = true;
  }

  /**
   * Vérifie l'état initial de l'authentification
   */
  private checkInitialAuthState(): void {
    this.isAuthenticated = this.authService.isAuthenticated();
    this.currentUser = this.authService.getCurrentUser();
    
    console.log('État initial - Authentifié:', this.isAuthenticated, 'Utilisateur:', this.currentUser);
    
    if (this.isAuthenticated && this.currentUser) {
      this.handleAuthenticated();
    } else if (this.isAuthenticated && !this.currentUser) {
      // Token présent mais pas d'utilisateur, vérifier le token
      console.log('Token présent mais pas d\'utilisateur, vérification...');
      this.verifyAuthenticationState();
    } else {
      this.handleUnauthenticated();
    }
  }

  /**
   * Vérifie l'état d'authentification en cas de doute
   */
  private verifyAuthenticationState(): void {
    this.authService.verifyToken()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          console.log('Vérification du token réussie:', response);
          this.handleAuthenticated();
        },
        error: (error) => {
          console.error('Échec de la vérification du token:', error);
          this.handleUnauthenticated();
        }
      });
  }

  /**
   * Gère le cas où l'utilisateur est authentifié
   */
  private handleAuthenticated(): void {
    console.log('Utilisateur authentifié, chargement des données...');
    this.errorMessage = null;
    this.checkServerConnection();
    this.loadDashboardData();
    this.subscribeToDataChanges();
    this.setupPeriodicRefresh();
  }

  /**
   * Gère le cas où l'utilisateur n'est pas authentifié
   */
  private handleUnauthenticated(): void {
    console.log('Utilisateur non authentifié');
    this.isLoading = false;
    this.isAuthenticated = false;
    this.currentUser = null;
    this.errorMessage = 'Vous devez être connecté pour accéder au tableau de bord.';
    
    // Redirection vers la page de connexion après un délai
    timer(2000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      console.log('Redirection vers la page de connexion...');
      this.router.navigate(['/login']);
    });
  }

  /**
   * Vérifie la connexion au serveur
   */
  private checkServerConnection(): void {
    if (!this.isAuthenticated) return;

    this.serverConnectionStatus = 'checking';
    console.log('Vérification de la connexion au serveur...');
    
    this.dashboardService.checkServerConnection()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (isConnected) => {
          console.log('État de connexion au serveur:', isConnected);
          this.serverConnectionStatus = isConnected ? 'connected' : 'disconnected';
          
          if (!isConnected) {
            this.isOfflineMode = true;
            this.warningMessage = 'Connexion au serveur impossible. Mode hors ligne activé.';
          } else {
            this.isOfflineMode = false;
            this.warningMessage = null;
          }
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Erreur de connexion au serveur:', error);
          this.serverConnectionStatus = 'disconnected';
          this.isOfflineMode = true;
          this.warningMessage = 'Serveur indisponible. Fonctionnalités limitées.';
          this.cdr.detectChanges();
        }
      });
  }

  /**
   * Configure le rafraîchissement périodique
   */
  private setupPeriodicRefresh(): void {
    if (!this.isAuthenticated) return;

    timer(300000, 300000) // 5 minutes
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.serverConnectionStatus === 'connected' && 
            !this.isLoading && 
            this.isAuthenticated) {
          console.log('Rafraîchissement automatique des données...');
          this.refreshDataSilently();
        }
      });
  }

  /**
   * Charge toutes les données du dashboard avec retry logic
   */
  private loadDashboardData(): void {
    if (!this.isAuthenticated) {
      console.warn('Tentative de chargement sans authentification');
      this.handleUnauthenticated();
      return;
    }

    if (this.isOfflineMode) {
      console.log('Mode hors ligne, chargement des données par défaut');
      this.loadOfflineData();
      return;
    }

    console.log('Chargement des données du dashboard...');
    this.isLoading = true;
    this.errorMessage = null;
    this.dashboardService.clearErrors();
    
    this.dashboardService.getCompleteDashboard()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (data) => {
          console.log('Données du dashboard chargées avec succès:', data);
          this.handleSuccessfulDataLoad(data);
          this.retryAttempts = 0;
        },
        error: (error) => {
          console.error('Erreur lors du chargement du dashboard:', error);
          this.handleDataLoadError(error);
        }
      });
  }

  /**
   * Gère le chargement réussi des données
   */
 private handleSuccessfulDataLoad(data: CompleteDashboard): void {
  console.log('Données reçues du serveur:', data);
  
  this.dashboardData = data;
  this.updateLocalData(data);
  this.errorMessage = null;
  this.warningMessage = null;
  this.isOfflineMode = false;
  
  // Vérification améliorée pour les recherches récentes
  const hasValidSearches = this.recentSearches && this.recentSearches.length > 0 && 
    !this.recentSearches.every(search => search.playerName === 'Données de test');
  
  const hasActualStats = this.stats && this.stats.searchesThisMonth > 0;
  
  if (!hasValidSearches && hasActualStats) {
    console.warn('Stats indiquent des recherches mais aucune recherche récente détaillée trouvée');
    this.warningMessage = 'Historique des recherches en cours de synchronisation...';
  } else if (!hasValidSearches && !hasActualStats) {
    console.info('Aucune recherche effectuée par cet utilisateur');
    // Pas de message d'avertissement pour un nouvel utilisateur
  } else {
    console.log(`${this.recentSearches.length} recherche(s) récente(s) trouvée(s)`);
  }
  
  console.log('Données du dashboard mises à jour avec succès');
}

  /**
   * Gère les erreurs de chargement avec retry
   */
  private handleDataLoadError(error: any): void {
    console.error('Erreur lors du chargement du dashboard:', error);
    
    // Vérifier si c'est une erreur d'authentification
    if (error.message?.includes('Session expirée') || 
        error.message?.includes('Non authentifié') ||
        error.message?.includes('401')) {
      console.log('Erreur d\'authentification détectée');
      this.handleUnauthenticated();
      return;
    }
    
    if (this.retryAttempts < this.maxRetryAttempts) {
      this.retryAttempts++;
      this.warningMessage = `Tentative de reconnexion (${this.retryAttempts}/${this.maxRetryAttempts})...`;
      const retryDelay = Math.pow(2, this.retryAttempts) * 1000;
      
      console.log(`Nouvelle tentative dans ${retryDelay}ms...`);
      timer(retryDelay)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => this.loadDashboardData());
    } else {
      console.error('Nombre maximum de tentatives atteint');
      this.errorMessage = 'Impossible de charger les données. Veuillez vérifier votre connexion.';
      this.isOfflineMode = true;
      this.loadOfflineData();
    }
  }

  /**
   * Charge des données par défaut en mode hors ligne
   */
  private loadOfflineData(): void {
    console.log('Chargement des données en mode hors ligne');
    
    this.stats = {
      searchesThisMonth: 0,
      searchGrowthPercentage: 0,
      favoritePlayers: 0,
      favoritePlayersAddedThisWeek: 0,
      comparisons: 0,
      comparisonGrowthPercentage: 0,
      reportsExported: 0,
      reportsExportedThisWeek: 0
    };
    this.recentSearches = [];
    this.favoritePlayers = [];
    this.alerts = [];
    this.marketTrends = {
      attackersGrowth: 0,
      midfieldersGrowth: 0,
      youngTalentsGrowth: 0,
      defendersGrowth: 0
    };
    this.isLoading = false;
    this.warningMessage = 'Mode hors ligne : données limitées disponibles';
    this.cdr.detectChanges();
  }

  /**
   * S'abonne aux changements de données en temps réel
   */
  private subscribeToDataChanges(): void {
  if (!this.isAuthenticated) return;

  console.log('Abonnement aux changements de données...');
  
  combineLatest([
    this.dashboardService.dashboardData$,
    this.dashboardService.loading$,
    this.dashboardService.error$
  ]).pipe(
    takeUntil(this.destroy$)
  ).subscribe({
    next: ([data, loading, error]) => {
      console.log('Changement de données détecté:', {
        hasData: !!data,
        isLoading: loading,
        hasError: !!error
      });
      
      if (data && !loading) {
        console.log('Traitement des nouvelles données...');
        this.handleSuccessfulDataLoad(data);
      }
      
      this.isLoading = loading;
      
      if (error) {
        console.error('Erreur détectée dans le service:', error);
        this.errorMessage = error;
      }
      
      this.cdr.detectChanges();
    },
    error: (err) => {
      console.error('Erreur dans l\'abonnement aux données:', err);
      this.errorMessage = 'Erreur de synchronisation des données';
      this.cdr.detectChanges();
    }
  });
}

  /**
   * Met à jour les données locales
   */
  private updateLocalData(data: CompleteDashboard): void {
  if (!data) {
    console.warn('Aucune donnée à mettre à jour');
    return;
  }
  
  this.stats = data.stats || null;
  
  // Filtrer les données de test du backend
  this.recentSearches = (data.recentSearches || []).filter(search => 
    search.playerName !== 'Données de test' && 
    search.club !== 'Mode hors ligne'
  );
  
  this.favoritePlayers = data.favoritePlayers || [];
  this.alerts = data.alerts || [];
  this.marketTrends = data.marketTrends || {
    attackersGrowth: 0,
    midfieldersGrowth: 0,
    youngTalentsGrowth: 0,
    defendersGrowth: 0
  };
  
  console.log('Données locales mises à jour:', {
    stats: !!this.stats,
    recentSearchesCount: this.recentSearches.length,
    favoritesCount: this.favoritePlayers.length,
    alertsCount: this.alerts.length,
    hasTrends: !!this.marketTrends
  });
}
  /**
   * Rafraîchit les données (visible à l'utilisateur)
   */
  refreshData(): void {
    if (!this.isAuthenticated) {
      console.log('Tentative de rafraîchissement sans authentification');
      this.handleUnauthenticated();
      return;
    }

    if (this.isOfflineMode) {
      console.log('Mode hors ligne, vérification de la connexion...');
      this.checkServerConnection();
      return;
    }
    
    console.log('Rafraîchissement manuel des données...');
    this.isRefreshing = true;
    this.retryAttempts = 0;
    this.dashboardService.refreshDashboard();
    
    timer(1500).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.isRefreshing = false;
      this.cdr.detectChanges();
    });
  }

  /**
   * Rafraîchit les données silencieusement (background)
   */
  private refreshDataSilently(): void {
    if (!this.isAuthenticated) return;

    console.log('Rafraîchissement silencieux...');
    this.dashboardService.getCompleteDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          console.log('Rafraîchissement silencieux réussi');
          this.handleSuccessfulDataLoad(data);
        },
        error: (error) => {
          console.warn('Échec du rafraîchissement silencieux:', error);
          // En cas d'erreur d'auth, gérer la déconnexion
          if (error.message?.includes('401') || error.message?.includes('Non authentifié')) {
            this.handleUnauthenticated();
          }
        }
      });
  }

  /**
   * Navigation vers la recherche avec logging sécurisé
   */
navigateToSearch(): void {
  if (!this.isAuthenticated) {
    console.log('Navigation vers login depuis search');
    this.router.navigate(['/login']);
    return;
  }
  
  this.router.navigate(['/search']);
}


  /**
   * Navigation vers le comparateur
   */
  navigateToCompare(): void {
    if (!this.isAuthenticated) {
      console.log('Navigation vers login depuis compare');
      this.router.navigate(['/login']);
      return;
    }
    this.router.navigate(['/compare']);
  }

  /**
   * Navigation vers l'accueil
   */
  navigateToHome(): void {
    this.router.navigate(['/']);
  }

  /**
   * Déconnexion de l'utilisateur
   */
  logout(): void {
    console.log('Déconnexion de l\'utilisateur...');
    this.authService.logout().subscribe({
      next: () => {
        console.log('Déconnexion réussie');
        this.router.navigate(['/login']);
      },
      error: (error) => {
        console.error('Erreur lors de la déconnexion:', error);
        // Rediriger quand même vers la page de connexion
        this.router.navigate(['/login']);
      }
    });
  }

  /**
   * Ajoute/retire un joueur des favoris avec validation
   */
  toggleFavoritePlayer(playerName: string, playerId?: number): void {
    if (!this.isAuthenticated) {
      this.showOfflineMessage('Vous devez être connecté pour gérer vos favoris.');
      return;
    }

    if (this.isOfflineMode) {
      this.showOfflineMessage('Cette fonctionnalité nécessite une connexion.');
      return;
    }
    
    if (!playerId || playerId <= 0) {
      console.warn('ID du joueur manquant ou invalide pour:', playerName);
      return;
    }

    console.log('Toggle favori pour le joueur:', playerName, playerId);
    this.dashboardService.toggleFavoritePlayer(playerId)
      .pipe(takeUntil(this.destroy$), finalize(() => this.cdr.detectChanges()))
      .subscribe({
        next: (message) => {
          console.log('Toggle favori réussi:', message);
          this.loadFavoritePlayers();
        },
        error: (error) => {
          console.error('Erreur lors de la modification des favoris:', error);
          if (error.message?.includes('401') || error.message?.includes('Non authentifié')) {
            this.handleUnauthenticated();
          } else {
            this.errorMessage = 'Impossible de modifier les favoris. Veuillez réessayer.';
          }
        }
      });
  }

  /**
   * Charge uniquement les joueurs favoris
   */
  private loadFavoritePlayers(): void {
    if (!this.isAuthenticated || this.isOfflineMode) return;
    
    this.dashboardService.getFavoritePlayers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (players) => {
          console.log('Joueurs favoris rechargés:', players);
          this.favoritePlayers = players;
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Erreur lors du chargement des favoris:', error);
          if (error.message?.includes('401')) {
            this.handleUnauthenticated();
          }
        }
      });
  }

  /**
   * Ouvre le modal d'export
   */
  openExportModal(): void {
    if (!this.isAuthenticated) {
      this.showOfflineMessage('Vous devez être connecté pour exporter des rapports.');
      return;
    }

    if (this.isOfflineMode) {
      this.showOfflineMessage('L\'export nécessite une connexion au serveur.');
      return;
    }

    if (!this.dashboardService.canPerformAction('export')) {
      this.showOfflineMessage('Votre compte ne permet pas l\'export de rapports.');
      return;
    }

    this.showExportModal = true;
  }

  /**
   * Ferme le modal d'export
   */
  closeExportModal(): void {
    this.showExportModal = false;
    this.exportFormat = 'pdf';
  }

  /**
   * Gère l'export de rapport avec gestion d'erreur améliorée
   */
handleExportReport(): void {
  if (!this.isAuthenticated || this.isOfflineMode) {
    this.closeExportModal();
    return;
  }
  
  console.log('Export de rapport en format:', this.exportFormat);
  this.isExporting = true;

  // CORRECTION: Logger l'export AVANT l'export réel
  this.dashboardService.logExport({
    format: this.exportFormat,
    dataType: 'dashboard',
    playerIds: this.favoritePlayers.map(p => p.playerId).filter(id => id) as number[]
  }).subscribe({
    next: () => {
      console.log('Export loggé avec succès');
      // Faire l'export réel après le logging
      this.performActualExport();
    },
    error: (error) => {
      console.error('Erreur lors du logging:', error);
      // Continuer avec l'export même si le logging échoue
      this.performActualExport();
    }
  });
}

private performActualExport(): void {
  this.dashboardService.exportReport(this.exportFormat).subscribe({
    next: (blob) => {
      if (blob && blob.size > 0) {
        const filename = this.dashboardService.generateFilename(this.exportFormat);
        this.dashboardService.downloadBlob(blob, filename);
        console.log('Export réussi:', filename);
        
        // CORRECTION: Rafraîchir les stats APRÈS un export réussi
        setTimeout(() => {
          this.dashboardService.refreshDashboard();
        }, 1000);
      } else {
        this.errorMessage = 'Le fichier exporté est vide.';
      }
    },
    error: (error) => {
      console.error('Erreur lors de l\'export:', error);
      if (error.message?.includes('401')) {
        this.handleUnauthenticated();
      } else {
        this.errorMessage = 'Erreur lors de l\'export du rapport. Veuillez réessayer.';
      }
    },
    complete: () => {
      this.isExporting = false;
      this.showExportModal = false;
      this.cdr.detectChanges();
    }
  });
}
  /**
   * Affiche un message d'information pour le mode hors ligne
   */
  private showOfflineMessage(message: string): void {
    this.warningMessage = message;
    timer(3000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.warningMessage === message) {
        this.warningMessage = null;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Formate les pourcentages avec couleur
   */
  formatGrowthWithColor(percentage: number): { text: string; color: string; icon: string } {
    return {
      text: this.dashboardService.formatGrowthPercentage(percentage),
      color: this.dashboardService.getTrendColor(percentage),
      icon: this.dashboardService.getTrendIcon(percentage)
    };
  }

  /**
   * Retourne les classes CSS pour les icônes d'alerte
   */
  getAlertIconClass(type: string): string {
    return this.dashboardService.getAlertIconClass(type);
  }

  /**
   * Retourne les classes CSS pour l'arrière-plan des alertes
   */
  getAlertBackgroundClass(type: string): string {
    return this.dashboardService.getAlertBackgroundClass(type);
  }

  /**
   * Marque une alerte comme vue
   */
  markAlertAsViewed(alert: Alert): void {
    if (!this.isAuthenticated || this.isOfflineMode) return;

    if (alert.id) {
      this.dashboardService.markAlertAsRead(alert.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            alert.isRead = true;
            this.cdr.detectChanges();
          },
          error: (error) => {
            console.error('Erreur lors du marquage de l\'alerte:', error);
            if (error.message?.includes('401')) {
              this.handleUnauthenticated();
            }
          }
        });
    }
  }

  /**
   * Navigue vers les détails d'un joueur
   */
viewPlayerDetails(playerName: string, playerId?: number): void {
  if (!this.isAuthenticated) {
    this.router.navigate(['/login']);
    return;
  }

  // SUPPRESSION du logging automatique pour éviter d'augmenter le compteur de recherches
  // Cette méthode est utilisée pour accéder aux détails depuis l'historique des recherches
  // donc elle ne doit PAS être comptée comme une nouvelle recherche
  
  console.log('Navigation vers les détails du joueur (sans logging):', playerName);

  // Navigation directe vers les détails du joueur
  if (playerId) {
    this.router.navigate(['/player', playerId]);
  } else {
    // Fallback au cas où on n'a que le nom
    this.router.navigate(['/player', playerName]);
  }
}

/**
 * NOUVELLE MÉTHODE : Pour logger une vraie nouvelle recherche
 * Cette méthode devrait être utilisée uniquement lors d'une recherche réelle
 * depuis la barre de recherche ou d'autres actions de recherche intentionnelles
 */
searchAndViewPlayer(playerName: string, playerId?: number): void {
  if (!this.isAuthenticated) {
    this.router.navigate(['/login']);
    return;
  }

  // Logger seulement les vraies nouvelles recherches
  if (!this.isOfflineMode && playerId) {
    console.log('Logging nouvelle recherche pour:', playerName);
    this.dashboardService.logUserSearch({
      searchQuery: playerName,
      playerId: playerId,
      type: 'SEARCH'
    })
    .pipe(takeUntil(this.destroy$)).subscribe({
      next: (result) => console.log('Nouvelle recherche loggée:', result),
      error: (error) => console.warn('Erreur lors du logging:', error)
    });
  }

  // Navigation vers les détails
  if (playerId) {
    this.router.navigate(['/player', playerId]);
  } else {
    this.router.navigate(['/player', playerName]);
  }
}

  /**
   * Gestion des erreurs de chargement d'images
   */
  onImageError(event: any): void {
    event.target.src = '/assets/images/default-player.png';
  }

  /**
   * Réessaye de charger les données en cas d'erreur
   */
  retryLoadData(): void {
    if (!this.isAuthenticated) {
      this.handleUnauthenticated();
      return;
    }

    console.log('Nouvelle tentative de chargement des données...');
    this.errorMessage = null;
    this.warningMessage = null;
    this.retryAttempts = 0;
    this.checkServerConnection();
    this.loadDashboardData();
  }

  /**
   * Efface les messages d'erreur/avertissement
   */
  clearMessages(): void {
    this.errorMessage = null;
    this.warningMessage = null;
    this.dashboardService.clearErrors();
  }

  /**
   * Force la reconnexion au serveur
   */
  reconnectToServer(): void {
    if (!this.isAuthenticated) {
      this.handleUnauthenticated();
      return;
    }

    console.log('Reconnexion au serveur...');
    this.isOfflineMode = false;
    this.serverConnectionStatus = 'checking';
    this.checkServerConnection();
    
    timer(1000).pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.serverConnectionStatus === 'connected') {
        this.loadDashboardData();
      }
    });
  }

  /**
   * Navigation vers la page de connexion
   */
  navigateToLogin(): void {
    console.log('Navigation vers la page de connexion');
    this.router.navigate(['/login']);
  }

  
  // Getters pour les templates

  /**
   * Vérifie si les données sont disponibles
   */
  get hasData(): boolean {
    return !!(this.stats && (this.recentSearches.length > 0 || this.favoritePlayers.length > 0));
  }

  /**
   * Vérifie si on a des données minimales à afficher
   */
  get hasMinimalData(): boolean {
    return !!this.stats;
  }

  /**
   * Retourne le nombre d'alertes non lues
   */
  get unreadAlertsCount(): number {
    return this.alerts?.filter(alert => !alert.isRead).length || 0;
  }

  /**
   * Vérifie si il y a des tendances positives
   */
  get hasPositiveTrends(): boolean {
    if (!this.marketTrends) return false;
    return this.marketTrends.attackersGrowth > 0 ||
           this.marketTrends.midfieldersGrowth > 0 ||
           this.marketTrends.youngTalentsGrowth > 0 ||
           this.marketTrends.defendersGrowth > 0;
  }

  /**
   * Retourne le statut de connexion pour l'affichage
   */
  get connectionStatusText(): string {
    switch (this.serverConnectionStatus) {
      case 'connected': return 'Connecté';
      case 'disconnected': return 'Déconnecté';
      case 'checking': return 'Vérification...';
      default: return 'Inconnu';
    }
  }

  /**
   * Retourne la classe CSS pour le statut de connexion
   */
  get connectionStatusClass(): string {
    switch (this.serverConnectionStatus) {
      case 'connected': return 'text-green-600';
      case 'disconnected': return 'text-red-600';
      case 'checking': return 'text-yellow-600';
      default: return 'text-gray-600';
    }
  }

  /**
   * Vérifie si une action nécessitant le serveur peut être effectuée
   */
  canPerformServerAction(): boolean {
    return this.isAuthenticated && !this.isOfflineMode && this.serverConnectionStatus === 'connected';
  }

  /**
   * Vérifie si l'utilisateur peut effectuer une action spécifique
   */
  canPerformAction(action: string): boolean {
    return this.isAuthenticated && this.dashboardService.canPerformAction(action);
  }

  /**
   * Retourne le nom d'affichage de l'utilisateur
   */
  get userDisplayName(): string {
    return this.currentUser?.name || 'Utilisateur';
  }

  /**
   * Trackby functions pour optimiser le rendu des listes
   */
  trackByPlayerName(index: number, player: FavoritePlayer): string {
    return player.id ? player.id.toString() : player.name;
  }

  trackBySearchDate(index: number, search: RecentSearch): string {
    return search.id ? search.id.toString() : search.date + search.playerName;
  }

  trackByAlertTime(index: number, alert: Alert): string {
    return alert.id ? alert.id.toString() : alert.time + alert.player;
  }
}