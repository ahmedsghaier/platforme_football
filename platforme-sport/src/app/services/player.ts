import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

export interface Player {
  id: number;
  name: string;
  age: number;
  position: string;
  nationality: string;
  marketValue: string;
  clubName: string;
  marketValueNumeric: number;
  confidence: number | string;
  image: string;
  club: {
    id: number;
    name: string;
    league?: string;
    level?: string;
    createdAt?: string;
  };
  createdAt?: string;
  stats: { [key: string]: any };
  valueHistory: ValueHistory[];
}

export interface ValueHistory {
  month: string;
  value: number;
}

export interface SearchFilters {
  query?: string;
  position?: string;
  club?: string;
  league?: string;
  ageMin?: number;
  ageMax?: number;
  valueMin?: number;
  valueMax?: number;
  nationality?: string;
}

export interface ComparisonResult {
  id: number;
  comparisonName: string;
  playerIds: number[];
  createdAt: string;
  players?: ComparisonPlayer[];
}

export interface ComparisonPlayer extends Player {
  comparisonStats?: {
    goals: number;
    assists: number;
    speed: number;
    finishing: number;
    technique: number;
    passing: number;
    defending: number;
    physical: number;
    shots?: number;
    keyPasses?: number;
    conversionRate?: string;
    passesReussies?: number;
    recuperations?: number;
    distanceParcourue?: number;
    tackles?: number;
    taclesReussis?: number;
    interceptions?: number;
    duelsAeriens?: number;
    duelsGagnes?: number;
    tackleSuccessRate?: string;
    saves?: number;
    cleanSheets?: number;
    savePercentage?: string;
    penaltiesSaved?: number;
  };
}

export interface ComparisonRequest {
  name: string;
  playerIds: number[];
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlayerService {
  private apiUrl = 'http://localhost:8080/api/players';
  private topPlayersCache: Player[] | null = null;
  private playerCache: { [id: number]: Player } = {};

  constructor(private http: HttpClient) {}

  getPlayerProfile(id: number, forceRefresh: boolean = false): Observable<Player> {
    if (!forceRefresh && this.playerCache[id]) {
      return of(this.playerCache[id]);
    }
    return this.http.get<Player>(`${this.apiUrl}/${id}`).pipe(
      tap(data => this.playerCache[id] = data),
      catchError((error: HttpErrorResponse) => {
        console.error('Erreur lors de la récupération du joueur:', error.status, error.statusText, error.message);
        return throwError(() => new Error(`Échec de la récupération du joueur ${id}. Vérifiez le serveur.`));
      })
    );
  }

  getTopPlayers(forceRefresh: boolean = false): Observable<Player[]> {
    if (!forceRefresh && this.topPlayersCache) {
      return of(this.topPlayersCache);
    }
    return this.http.get<Player[]>(`${this.apiUrl}/top`).pipe(
      tap(data => this.topPlayersCache = data),
      catchError((error: HttpErrorResponse) => {
        console.error('Erreur lors de la récupération des top joueurs:', error.status, error.statusText, error.message);
        return throwError(() => new Error('Échec de la récupération des top joueurs.'));
      })
    );
  }

  getAllPlayers(): Observable<Player[]> {
    return this.http.get<Player[]>(this.apiUrl).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des joueurs:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Recherche simple par texte
   */
  searchPlayers(query: string): Observable<Player[]> {
    const params = new HttpParams().set('query', query || '');
    return this.http.get<Player[]>(`${this.apiUrl}/search`, { params }).pipe(
      catchError(error => {
        console.error('Erreur lors de la recherche de joueurs:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Recherche avancée avec tous les filtres
   */
  advancedSearch(filters: SearchFilters): Observable<Player[]> {
    let params = new HttpParams();

    if (filters.query?.trim()) params = params.set('query', filters.query.trim());
    if (filters.position?.trim()) params = params.set('position', filters.position.trim());
    if (filters.club?.trim()) params = params.set('club', filters.club.trim());
    if (filters.league?.trim()) params = params.set('league', filters.league.trim());
    if (filters.ageMin != null && filters.ageMin > 0) params = params.set('ageMin', filters.ageMin.toString());
    if (filters.ageMax != null && filters.ageMax > 0) params = params.set('ageMax', filters.ageMax.toString());
    if (filters.valueMin != null && filters.valueMin > 0) params = params.set('valueMin', (filters.valueMin * 1000000).toString());
    if (filters.valueMax != null && filters.valueMax > 0) params = params.set('valueMax', (filters.valueMax * 1000000).toString());
    if (filters.nationality?.trim()) params = params.set('nationality', filters.nationality.trim());

    return this.http.get<Player[]>(`${this.apiUrl}/search/advanced`, { params }).pipe(
      catchError(error => {
        console.error('Erreur lors de la recherche avancée:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Recherche par position spécifique
   */
  getPlayersByPosition(position: string): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.apiUrl}/position/${position || ''}`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des joueurs par position:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Recherche par club
   */
  getPlayersByClub(clubName: string): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.apiUrl}/club/${clubName || ''}`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des joueurs par club:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Recherche par nationalité
   */
  getPlayersByNationality(nationality: string): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.apiUrl}/nationality/${nationality || ''}`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des joueurs par nationalité:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Recherche par fourchette d'âge
   */
  getPlayersByAgeRange(minAge: number, maxAge: number): Observable<Player[]> {
    const params = new HttpParams()
      .set('minAge', (minAge || 0).toString())
      .set('maxAge', (maxAge || 0).toString());
    return this.http.get<Player[]>(`${this.apiUrl}/age-range`, { params }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des joueurs par âge:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Recherche par fourchette de valeur
   */
  getPlayersByValueRange(minValue: number, maxValue: number): Observable<Player[]> {
    const params = new HttpParams()
      .set('minValue', ((minValue || 0) * 1000000).toString())
      .set('maxValue', ((maxValue || 0) * 1000000).toString());
    return this.http.get<Player[]>(`${this.apiUrl}/value-range`, { params }).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des joueurs par valeur:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Récupère les options de filtres disponibles
   */
  getAvailablePositions(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/filters/positions`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des positions:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  getAvailableClubs(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/filters/clubs`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des clubs:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  getAvailableLeagues(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/filters/leagues`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des ligues:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  getAvailableNationalities(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/filters/nationalities`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des nationalités:', error);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }

  /**
   * Récupère les statistiques générales
   */
  getPlayersStatsOverview(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats/overview`).pipe(
      catchError(error => {
        console.error('Erreur lors de la récupération des statistiques:', error);
        return of({}); // Retourner un objet vide en cas d'erreur
      })
    );
  }

  /**
   * Normalise la position d'un joueur
   */
  getPositionLabel(position: string): string {
    const positionMap: { [key: string]: string } = {
      'FW': 'Attaquant',
      'ATTAQUANT': 'Attaquant',
      'MF': 'Milieu',
      'MILIEU': 'Milieu',
      'DF': 'Défenseur',
      'DEFENSEUR': 'Défenseur',
      'DÉFENSEUR': 'Défenseur',
      'GK': 'Gardien',
      'GARDIEN': 'Gardien'
    };

    const primaryPosition = position?.split(',')[0]?.trim().toUpperCase();
    return positionMap[primaryPosition] || primaryPosition || 'Inconnu';
  }

  clearCache(): void {
    this.topPlayersCache = null;
    this.playerCache = {};
  }

  createComparison(request: ComparisonRequest): Observable<ComparisonResult> {
    return this.http.post<ComparisonResult>(`${this.apiUrl}/comparisons`, request).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Erreur lors de la création de la comparaison:', error.status, error.statusText, error.message);
        return throwError(() => new Error('Échec de la création de la comparaison.'));
      })
    );
  }

  /**
   * Récupère une comparaison par son ID
   */
  getComparison(id: number): Observable<ComparisonResult> {
    return this.http.get<ComparisonResult>(`${this.apiUrl}/comparisons/${id}`).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Erreur lors de la récupération de la comparaison:', error.status, error.statusText, error.message);
        return throwError(() => new Error(`Échec de la récupération de la comparaison ${id}.`));
      })
    );
  }

  /**
   * Récupère toutes les comparaisons
   */
  getAllComparisons(): Observable<ComparisonResult[]> {
    return this.http.get<ComparisonResult[]>(`${this.apiUrl}/comparisons`).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Erreur lors de la récupération des comparaisons:', error.status, error.statusText, error.message);
        return of([]); // Retourner un tableau vide en cas d'erreur
      })
    );
  }
}