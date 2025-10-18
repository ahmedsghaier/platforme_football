import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlayerService, Player, SearchFilters } from '../services/player';
import { DashboardService } from '../dashboard-service'; // Import du DashboardService
import { debounceTime, distinctUntilChanged, switchMap, takeUntil, catchError } from 'rxjs/operators';
import { Subject, Observable, of } from 'rxjs';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './search.html',
  styleUrl: './search.css'
})
export class Search implements OnInit, OnDestroy {
  searchQuery: string = '';
  filters = {
    position: '',
    club: '',
    league: '',
    ageMin: '',
    ageMax: '',
    valueMin: '',
    valueMax: ''
  };
  viewMode: 'grid' | 'list' = 'grid';
  
  // Données dynamiques du backend
  filteredPlayers: Player[] = [];
  isLoading: boolean = false;
  error: string | null = null;
  
  // Options pour les filtres (extraites des données)
  availablePositions: string[] = [];
  availableClubs: string[] = [];
  availableLeagues: string[] = [];
  
  // Subject pour la recherche avec debounce
  private searchTerms = new Subject<SearchFilters>();
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private playerService: PlayerService,
    private dashboardService: DashboardService // Injection du DashboardService
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
    this.setupSearch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Charge les données initiales et les options de filtres
   */
  private loadInitialData(): void {
    this.isLoading = true;
    this.error = null;
    
    // Charger tous les joueurs au démarrage
    this.playerService.getAllPlayers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (players) => {
          this.filteredPlayers = players;
          this.isLoading = false;
        },
        error: (error) => {
          this.error = 'Erreur lors du chargement des joueurs';
          this.isLoading = false;
          console.error('Erreur:', error);
        }
      });

    // Charger les options de filtres en parallèle
    this.loadFilterOptions();
  }

  /**
   * Charge les options disponibles pour les filtres depuis l'API
   */
  private loadFilterOptions(): void {
    // Charger les positions
    this.playerService.getAvailablePositions().subscribe({
      next: positions => this.availablePositions = positions,
      error: err => console.error("Erreur lors du chargement des positions", err)
    });

    // Charger les clubs
    this.playerService.getAvailableClubs().subscribe({
      next: clubs => this.availableClubs = clubs,
      error: err => console.error("Erreur lors du chargement des clubs", err)
    });

    // Charger les ligues
    this.playerService.getAvailableLeagues().subscribe({
      next: leagues => this.availableLeagues = leagues,
      error: err => console.error("Erreur lors du chargement des ligues", err)
    });
  }

  /**
   * Configure la recherche avec debounce pour éviter trop de requêtes
   */
  private setupSearch(): void {
    this.searchTerms.pipe(
      debounceTime(300),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
      switchMap((filters: SearchFilters) => this.performSearch(filters)),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (players) => {
        this.filteredPlayers = players;
        this.isLoading = false;
        this.error = null;
        
        // NOUVEAU: Logger la recherche après réception des résultats
        this.logCurrentSearch();
      },
      error: (error) => {
        this.error = 'Erreur lors de la recherche';
        this.isLoading = false;
        this.filteredPlayers = [];
        console.error('Erreur de recherche:', error);
      }
    });
  }

  /**
   * NOUVEAU: Log la recherche actuelle avec les résultats obtenus
   */
  private logCurrentSearch(): void {
    const searchFilters = this.buildSearchFilters();
    
    // Ne logger que si il y a une vraie recherche (pas juste un chargement initial)
    if (this.hasActiveFilters(searchFilters)) {
      const searchLog = {
        searchQuery: this.buildSearchString(searchFilters),
        type: 'SEARCH' as const,
        playerId: null, // Aucun joueur spécifique sélectionné pour une recherche générale
        searchFilters: searchFilters
      };

      this.dashboardService.logUserSearch(searchLog)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (result) => console.log('Recherche loggée:', result),
          error: (error) => console.warn('Erreur de logging:', error)
        });
    }
  }

  /**
   * NOUVEAU: Construit une chaîne de recherche lisible
   */
  private buildSearchString(filters: SearchFilters): string {
    const parts: string[] = [];
    
    if (filters.query) parts.push(`"${filters.query}"`);
    if (filters.position) parts.push(`Position: ${filters.position}`);
    if (filters.club) parts.push(`Club: ${filters.club}`);
    if (filters.league) parts.push(`Ligue: ${filters.league}`);
    if (filters.ageMin || filters.ageMax) {
      const ageRange = `Âge: ${filters.ageMin || 'min'}-${filters.ageMax || 'max'}`;
      parts.push(ageRange);
    }
    if (filters.valueMin || filters.valueMax) {
      const valueRange = `Valeur: ${filters.valueMin || 'min'}M€-${filters.valueMax || 'max'}M€`;
      parts.push(valueRange);
    }
    
    return parts.length > 0 ? parts.join(' | ') : 'Recherche générale';
  }

  /**
   * Effectue la recherche via l'API
   */
  private performSearch(searchFilters: SearchFilters): Observable<Player[]> {
    // Si aucun filtre n'est appliqué, retourner tous les joueurs
    if (!this.hasActiveFilters(searchFilters)) {
      return this.playerService.getAllPlayers().pipe(
        catchError(error => {
          console.error('Erreur lors du chargement de tous les joueurs:', error);
          return of([]);
        })
      );
    }

    // Utiliser la recherche avancée avec tous les filtres
    return this.playerService.advancedSearch(searchFilters).pipe(
      catchError(error => {
        console.error('Erreur lors de la recherche avancée:', error);
        return of([]);
      })
    );
  }

  /**
   * Vérifie si des filtres sont actifs
   */
  private hasActiveFilters(filters: SearchFilters): boolean {
    const hasFilters = !!(
      (filters.query && filters.query.trim()) ||
      (filters.position && filters.position.trim()) ||
      (filters.club && filters.club.trim()) ||
      (filters.league && filters.league.trim()) ||
      (filters.ageMin && filters.ageMin > 0) ||
      (filters.ageMax && filters.ageMax > 0) ||
      (filters.valueMin && filters.valueMin > 0) ||
      (filters.valueMax && filters.valueMax > 0)
    );
    
    console.log('Filtres actifs:', filters, 'Résultat:', hasFilters);
    return hasFilters;
  }

  /**
   * Convertit les filtres du composant en SearchFilters
   */
  private buildSearchFilters(): SearchFilters {
    return {
      query: this.searchQuery.trim() || undefined,
      position: this.filters.position.trim() || undefined,
      club: this.filters.club.trim() || undefined,
      league: this.filters.league.trim() || undefined,
      ageMin: this.parseNumber(this.filters.ageMin),
      ageMax: this.parseNumber(this.filters.ageMax),
      valueMin: this.parseNumber(this.filters.valueMin),
      valueMax: this.parseNumber(this.filters.valueMax)
    };
  }

  /**
   * Parse un string en number avec validation
   */
  private parseNumber(value: string): number | undefined {
    if (!value || value.trim() === '') return undefined;
    const num = Number(value.trim());
    return isNaN(num) || num <= 0 ? undefined : num;
  }

  /**
   * Gère le changement de la recherche textuelle
   */
  onSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchQuery = target.value;
    this.triggerSearch();
  }

  /**
   * Gère le changement des filtres
   */
  onFilterChange(filterKey: keyof typeof this.filters, event: Event): void {
    const target = event.target as HTMLInputElement | HTMLSelectElement;
    this.filters = {
      ...this.filters,
      [filterKey]: target.value
    };
    
    this.triggerSearch();
  }

  /**
   * Recherche par position spécifique
   */
  searchByPosition(position: string): void {
    this.filters.position = position;
    this.triggerSearch();
  }

  /**
   * Déclenche une nouvelle recherche
   */
  private triggerSearch(): void {
    this.isLoading = true;
    this.error = null;
    
    setTimeout(() => {
      const searchFilters = this.buildSearchFilters();
      this.searchTerms.next(searchFilters);
    }, 50);
  }

  /**
   * Réinitialise tous les filtres
   */
  resetFilters(): void {
    this.filters = {
      position: '',
      club: '',
      league: '',
      ageMin: '',
      ageMax: '',
      valueMin: '',
      valueMax: ''
    };
    this.searchQuery = '';
    
    this.isLoading = true;
    this.error = null;
    
    this.playerService.getAllPlayers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (players) => {
          this.filteredPlayers = players;
          this.isLoading = false;
        },
        error: (error) => {
          this.error = 'Erreur lors du chargement des joueurs';
          this.isLoading = false;
          console.error('Erreur:', error);
        }
      });
  }

  /**
   * Change le mode d'affichage
   */
  setViewMode(mode: 'grid' | 'list'): void {
    this.viewMode = mode;
  }

  /**
   * MODIFIÉ: Navigation vers le profil d'un joueur avec logging
   */
  navigateToPlayer(playerId: number): void {
    // Trouver le joueur dans la liste actuelle pour obtenir son nom
    const selectedPlayer = this.filteredPlayers.find(p => p.id === playerId);
    const playerName = selectedPlayer ? selectedPlayer.name : `Joueur ID: ${playerId}`;
    
    // Logger la sélection du joueur spécifique
    const searchLog = {
      searchQuery: `Navigation vers: ${playerName}`,
      type: 'SEARCH' as const,
      playerId: playerId, // CORRECTION: Maintenant le playerId est enregistré
      searchFilters: this.buildSearchFilters() // Conserver les filtres actuels
    };

    this.dashboardService.logUserSearch(searchLog)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => console.log('Navigation vers joueur loggée:', result),
        error: (error) => console.warn('Erreur de logging navigation:', error)
      });

    // Naviguer vers le profil du joueur
    this.router.navigate(['/player', playerId]);
  }

  /**
   * NOUVEAU: Logger explicitement une recherche de joueur spécifique
   */
  logPlayerSearch(player: Player): void {
    const searchLog = {
      searchQuery: `Recherche: ${player.name} (${player.club})`,
      type: 'SEARCH' as const,
      playerId: player.id,
      searchFilters: this.buildSearchFilters()
    };

    this.dashboardService.logUserSearch(searchLog)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => console.log('Recherche joueur spécifique loggée:', result),
        error: (error) => console.warn('Erreur de logging recherche spécifique:', error)
      });
  }

  /**
   * NOUVEAU: Méthode pour logger quand un utilisateur clique sur un joueur (pas forcément navigation)
   */
  onPlayerClick(player: Player, action: 'view' | 'select' = 'view'): void {
    this.logPlayerSearch(player);
    
    if (action === 'view') {
      this.navigateToPlayer(player.id);
    }
  }

  /**
   * Rafraîchit les données
   */
  refreshData(): void {
    this.loadInitialData();
  }

  /**
   * Formate la valeur marchande pour l'affichage
   */
  formatMarketValue(value: string | number): string {
    if (typeof value === 'number') {
      return this.convertToMillions(value);
    }
    return value;
  }

  /**
   * Calcule le pourcentage de confiance
   */
  getConfidencePercentage(confidence: string | number): string {
    if (typeof confidence === 'string') {
      return confidence;
    }
    return `${confidence}%`;
  }

  /**
   * Fonction de tracking pour ngFor
   */
  trackByPlayerId(index: number, player: Player): number {
    return player.id;
  }

  /**
   * Convertit la valeur en millions d'euros
   */
  convertToMillions(value: number): string {
    if (!value) return '0 M€';
    const millions = value / 1_000_000;
    return millions.toFixed(1) + ' M€';
  }

  /**
   * Obtient la position simplifiée d'un joueur pour l'affichage
   */
  getSimplifiedPosition(position: string): string {
    return position || 'N/A';
  }

  /**
   * Obtient toutes les positions simplifiées disponibles
   */
  getSimplifiedPositions(): string[] {
    return ['Attaquant', 'Milieu', 'Défenseur', 'Gardien'];
  }
}