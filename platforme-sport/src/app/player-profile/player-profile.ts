import { Component, OnInit, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PlayerService, Player } from '../services/player';
import { Subject, takeUntil } from 'rxjs';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-player-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './player-profile.html',
  styleUrls: ['./player-profile.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlayerProfile implements OnInit, OnDestroy {
  player: Player | null = null;
  activeTab: string = 'stats';
  loading: boolean = true;
  error: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private playerService: PlayerService,
    private cdr: ChangeDetectorRef
  ) {
    this.router.routeReuseStrategy.shouldReuseRoute = () => false;
    console.log('Constructeur - cdr injecté');
  }

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = +params['id'];
      if (id && !isNaN(id)) {
        this.loadPlayerProfile(id);
      } else {
        this.error = 'ID de joueur invalide';
        this.loading = false;
        this.markForCheck();
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.player = null;
    this.loading = true;
    this.error = null;
    console.log('Composant détruit');
  }

  loadPlayerProfile(playerId: number, forceRefresh: boolean = false): void {
    this.loading = true;
    this.error = null;
    console.log('Chargement joueur id:', playerId, 'Force refresh:', forceRefresh);

    this.playerService.getPlayerProfile(playerId, forceRefresh)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (player) => {
          console.log('Joueur reçu - Avant mise à jour:', this.player);
          this.player = this.normalizePlayerData(player); // Normalisation des données
          this.loading = false;
          console.log('Joueur reçu - Après mise à jour:', this.player, 'Loading:', this.loading);
          this.markForCheck();
        },
        error: (error) => {
          console.error('Erreur chargement joueur:', error);
          this.error = error.status === 404 ? 'Joueur non trouvé' : 'Erreur lors du chargement du profil';
          this.loading = false;
          this.markForCheck();
        }
      });
  }

  // Normalise les données pour éviter des valeurs aberrantes
  private normalizePlayerData(player: Player): Player {
    const normalizedStats = { ...player.stats };
    return { ...player, stats: normalizedStats };
  }

  refreshPlayer(): void {
    if (this.player?.id) {
      this.loadPlayerProfile(this.player.id, true);
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    console.log('Onglet actif:', this.activeTab);
    this.markForCheck();
  }

  private markForCheck(): void {
    this.cdr.markForCheck();
  }

  getStatsByPosition(): any[] {
    if (!this.player?.stats) return [];

    const rawPosition = this.player.position?.toUpperCase() || '';
    const position = this.getPositionLabel(this.player.position)?.toUpperCase() || rawPosition;

    switch (position) {
      case 'ATTAQUANT':
        return [
          { label: 'Buts', value: this.player.stats['goals'] || 0, color: 'green', icon: '⚽' },
          { label: 'Passes décisives', value: this.player.stats['assists'] || 0, color: 'blue', icon: '🎯' },
          { label: 'Tirs', value: this.player.stats['shots'] || 0, color: 'purple', icon: '🔫' },
          { label: 'Taux de conversion', value: this.player.stats['conversionRate'] || '0%', color: 'red', icon: '📊' },
          { label: 'Minutes jouées', value: this.player.stats['minutesPlayed'] || 0, color: 'gray', icon: '⏱️' }
        ];

      case 'MILIEU':
        return [
          { label: 'Buts', value: this.player.stats['goals'] || 0, color: 'green', icon: '⚽' },
          { label: 'Passes décisives', value: this.player.stats['assists'] || 0, color: 'blue', icon: '🎯' },
          { label: 'Passes réussies', value: this.player.stats['passesReussies'] || 0, color: 'purple', icon: '✅' },
          { label: 'Récupérations', value: this.player.stats['recuperations'] || 0, color: 'orange', icon: '🔄' },
          { label: 'Distance parcourue', value: `${this.player.stats['distanceParcourue'] || 0} km`, color: 'red', icon: '🏃' },
          { label: 'Minutes jouées', value: this.player.stats['minutesPlayed'] || 0, color: 'gray', icon: '⏱️' }
        ];

      case 'DÉFENSEUR':
        return [
          { label: 'Tacles', value: this.player.stats['tackles'] || 0, color: 'blue', icon: '🛡️' },
          { label: 'Tacles réussis', value: this.player.stats['taclesReussis'] || 0, color: 'green', icon: '✅' },
          { label: 'Interceptions', value: this.player.stats['interceptions'] || 0, color: 'purple', icon: '🚫' },
          { label: 'Duels aériens', value: this.player.stats['duelsAeriens'] || 0, color: 'orange', icon: '🎈' },
          { label: 'Duels gagnés', value: this.player.stats['duelsGagnes'] || 0, color: 'cyan', icon: '🏆' },
          { label: '% Tacles réussis', value: this.player.stats['tackleSuccessRate'] || '0%', color: 'indigo', icon: '📊' },
          { label: 'Cartons jaunes', value: this.player.stats['cartonsJaunes'] || 0, color: 'yellow', icon: '🟨' },
          { label: 'Minutes jouées', value: this.player.stats['minutesPlayed'] || 0, color: 'gray', icon: '⏱️' }
        ];

      case 'GARDIEN':
        return [
          { label: 'Arrêts', value: this.player.stats['saves'] || 0, color: 'green', icon: '🥅' },
          { label: 'Clean sheets', value: this.player.stats['cleanSheets'] || 0, color: 'blue', icon: '🛡️' },
          { label: '% d\'arrêts', value: this.player.stats['savePercentage'] || '0%', color: 'purple', icon: '📊' },
          { label: 'Pénaltys arrêtés', value: this.player.stats['penaltiesSaved'] || 0, color: 'orange', icon: '🚫' },
          { label: 'Matchs joués', value: this.player.stats['matchsPlayed'] || 0, color: 'red', icon: '📊' }
        ];

      default:
        return [
          { label: 'Matchs joués', value: this.player.stats['matchsPlayed'] || 0, color: 'blue', icon: '📊' },
          { label: 'Minutes jouées', value: this.player.stats['minutesPlayed'] || 0, color: 'gray', icon: '⏱️' }
        ];
    }
  }

  getPerformanceMetrics(): any[] {
    if (!this.player) return [];
    
    const position = this.getPositionLabel(this.player.position)?.toUpperCase() || '';

    switch (position) {
      case 'ATTAQUANT':
        return [
          { label: 'Finition', value: 92, max: 100 },
          { label: 'Vitesse', value: 95, max: 100 },
          { label: 'Technique', value: 88, max: 100 },
          { label: 'Vision de jeu', value: 85, max: 100 }
        ];
      
      case 'MILIEU':
        return [
          { label: 'Passes', value: 89, max: 100 },
          { label: 'Vision de jeu', value: 91, max: 100 },
          { label: 'Technique', value: 87, max: 100 },
          { label: 'Récupération', value: 84, max: 100 }
        ];
      
      case 'DÉFENSEUR':
        return [
          { label: 'Défense', value: 92, max: 100 },
          { label: 'Placement', value: 88, max: 100 },
          { label: 'Jeu aérien', value: 90, max: 100 },
          { label: 'Physique', value: 89, max: 100 },
          { label: 'Anticipation', value: 87, max: 100 },
          { label: 'Mental', value: 85, max: 100 }
        ];
      
      case 'GARDIEN':
        return [
          { label: 'Réflexes', value: 94, max: 100 },
          { label: 'Placement', value: 88, max: 100 },
          { label: 'Relance', value: 82, max: 100 },
          { label: 'Communication', value: 90, max: 100 }
        ];
      
      default:
        return [
          { label: 'Technique', value: 78, max: 100 },
          { label: 'Physique', value: 85, max: 100 },
          { label: 'Mental', value: 82, max: 100 },
          { label: 'Tactique', value: 80, max: 100 }
        ];
    }
  }

  getAIRecommendation(): string {
    if (!this.player) return '';
    
    const position = this.getPositionLabel(this.player.position)?.toUpperCase() || '';

    switch (position) {
      case 'ATTAQUANT':
        return 'Joueur exceptionnel avec un potentiel de croissance élevé. Recommandé pour les clubs cherchant un attaquant polyvalent capable de marquer et créer des occasions. Valeur attendue en hausse de 10-15% sur les 12 prochains mois.';
      
      case 'MILIEU':
        return 'Milieu de terrain complet avec d\'excellentes capacités de distribution et de récupération. Idéal pour les équipes recherchant la stabilité au centre du jeu. Potentiel de progression constant.';
      
      case 'DÉFENSEUR':
        return 'Défenseur solide avec d\'excellentes capacités de récupération et de jeu aérien. Recommandé pour les clubs cherchant la stabilité défensive et le leadership. Excellent rapport qualité-prix avec une valeur stable.';
      
      case 'GARDIEN':
        return 'Gardien fiable avec d\'excellents réflexes et un bon placement. Recommandé pour les clubs cherchant la stabilité défensive. Valeur stable avec légère tendance à la hausse.';
      
      default:
        return 'Joueur solide dans son poste avec des performances régulières. Recommandé pour les clubs cherchant la stabilité et la fiabilité. Bon rapport qualité-prix.';
    }
  }

  navigateToHome(): void {
    this.router.navigate(['/']);
  }

  goBack(): void {
    window.history.back();
  }

  generateRandomStat(): number {
    return 1.2 + Math.random() * 1.5;
  }

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

  getDefaultPlayerImage(): string {
    return 'assets/images/default-player.jpg';
  }

  onImageError(event: any): void {
    event.target.src = this.getDefaultPlayerImage();
  }

  convertToMillions(value: number): string {
    if (!value) return '0 M€';
    const millions = value / 1_000_000;
    return millions.toFixed(1) + ' M€';
  }
}