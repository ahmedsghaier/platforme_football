// compare.ts
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PlayerService, Player, ComparisonResult, ComparisonRequest, ComparisonPlayer } from '../services/player';
import { Observable, of,map } from 'rxjs';
import { catchError, debounceTime, switchMap } from 'rxjs/operators';
import { FormControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';   
import { DashboardService } from '../dashboard-service'; 
@Component({
  selector: 'app-compare',
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  standalone: true,
  templateUrl: './compare.html',
  styleUrls: ['./compare.css']
})
export class Compare implements OnInit {
  selectedPlayers: ComparisonPlayer[] = [];
  availablePlayers: Player[] = [];
  searchResults: Player[] = [];
  isLoading = false;
  isExportingPDF = false;
  showExportSuccess = false;
  errorMessage = '';
  comparisons: ComparisonResult[] = [];
  selectedComparisonId: number | null = null;
  searchText = '';

  // Search control pour la recherche en temps réel
  searchControl = new FormControl('');

  technicalSkills = [
    { key: 'speed', label: 'Vitesse' },
    { key: 'finishing', label: 'Finition' },
    { key: 'technique', label: 'Technique' },
    { key: 'passing', label: 'Passes' },
    { key: 'defending', label: 'Défense' },
    { key: 'physical', label: 'Physique' }
  ];

  constructor(
    private router: Router,
    private playerService: PlayerService,
    private dashboardService: DashboardService // AJOUTER
  ) {}

  ngOnInit(): void {
  this.loadInitialData(); // Charge d'abord les joueurs
  setTimeout(() => this.loadComparisons(), 1000); // Attendre 1 seconde avant de charger les comparaisons
  this.setupSearch(); // La recherche peut rester asynchrone
}

  /**
   * Charge les données initiales
   */
  private loadInitialData(): void {
  this.isLoading = true;
  const timeoutId = setTimeout(() => {
    if (this.isLoading) {
      console.warn('Chargement initial bloqué après 10 secondes, forçant la fin du loading.');
      this.isLoading = false;
      this.errorMessage = 'Délai dépassé, veuillez réessayer.';
    }
  }, 10000); // 10 secondes de timeout

  this.playerService.getTopPlayers().pipe(
    catchError(error => {
      console.error('Erreur lors du chargement des top joueurs:', error);
      this.errorMessage = 'Erreur lors du chargement des joueurs';
      return of([]);
    })
  ).subscribe(players => {
    clearTimeout(timeoutId); // Annuler le timeout si la requête réussit
    this.availablePlayers = players.slice(0, 10);
    this.isLoading = false;
  });
}

  /**
   * Configure la recherche en temps réel
   */
  private setupSearch(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      switchMap(query => {
        if (!query || query.trim().length < 2) {
          return of([]);
        }
        return this.playerService.searchPlayers(query.trim()).pipe(
          catchError(error => {
            console.error('Erreur lors de la recherche:', error);
            return of([]);
          })
        );
      })
    ).subscribe(results => {
      this.searchResults = results;
    });
  }

  /**
   * Ajoute un joueur à la comparaison
   */
  addPlayer(player: Player): void {
    if (this.selectedPlayers.length >= 3) {
      alert('Vous ne pouvez comparer que 3 joueurs maximum');
      return;
    }

    if (this.selectedPlayers.find(p => p.id === player.id)) {
      alert('Ce joueur est déjà sélectionné');
      return;
    }

    this.playerService.getPlayerProfile(player.id).subscribe({
      next: (fullPlayer) => {
        const comparisonPlayer: ComparisonPlayer = {
          ...fullPlayer,
          comparisonStats: this.extractComparisonStats(fullPlayer)
        };
        this.selectedPlayers = [...this.selectedPlayers, comparisonPlayer];
        this.searchControl.setValue('');
        this.searchResults = [];
      },
      error: (error) => {
        console.error('Erreur lors du chargement du joueur:', error);
        alert('Erreur lors du chargement du joueur');
      }
    });
  }

  /**
   * Extrait les statistiques pour la comparaison basées sur le profil
   */
  private extractComparisonStats(player: Player): any {
    const stats = player.stats || {};
    const position = this.playerService.getPositionLabel(player.position)?.toUpperCase() || '';

    const baseStats = {
      goals: Number(stats['goals'] || stats['buts'] || 0),
      assists: Number(stats['assists'] || stats['passes_decisives'] || 0),
    };

    // Ajuster selon la position pour inclure des statistiques spécifiques
    switch (position) {
      case 'ATTAQUANT':
        return {
          ...baseStats,
          shots: Number(stats['shots'] || 0),
          conversionRate: stats['conversionRate'] || '0%'
        };
      case 'MILIEU':
        return {
          ...baseStats,
          passesReussies: Number(stats['passesReussies'] || 0),
          recuperations: Number(stats['recuperations'] || 0),
          distanceParcourue: Number(stats['distanceParcourue'] || 0)
        };
      case 'DÉFENSEUR':
        return {
          ...baseStats,
          tackles: Number(stats['tackles'] || 0),
          taclesReussis: Number(stats['taclesReussis'] || 0),
          interceptions: Number(stats['interceptions'] || 0),
          duelsAeriens: Number(stats['duelsAeriens'] || 0),
          duelsGagnes: Number(stats['duelsGagnes'] || 0),
          tackleSuccessRate: stats['tackleSuccessRate'] || '0%'
        };
      case 'GARDIEN':
        return {
          ...baseStats,
          saves: Number(stats['saves'] || 0),
          cleanSheets: Number(stats['cleanSheets'] || 0),
          savePercentage: stats['savePercentage'] || '0%',
          penaltiesSaved: Number(stats['penaltiesSaved'] || 0)
        };
      default:
        return baseStats;
    }
  }

  /**
   * Supprime un joueur de la comparaison
   */
  removePlayer(playerId: number): void {
    this.selectedPlayers = this.selectedPlayers.filter(p => p.id !== playerId);
  }

  /**
   * Crée une nouvelle comparaison
   */
createComparison(): void {
  if (this.selectedPlayers.length < 2) {
    alert('Vous devez sélectionner au moins 2 joueurs pour créer une comparaison');
    return;
  }

  const request: ComparisonRequest = {
    name: `Comparaison ${new Date().toLocaleDateString()}`,
    playerIds: this.selectedPlayers.map(p => p.id),
    description: 'Comparaison générée automatiquement'
  };

  this.playerService.createComparison(request).subscribe({
    next: (result) => {
      // CORRECTION: Logger la comparaison APRÈS la création réussie
      this.dashboardService.logComparison({
        playerIds: request.playerIds,
        comparisonName: request.name,
        comparisonType: 'manual'
      }).subscribe({
        next: (logResult) => {
          console.log('Comparaison loggée:', logResult);
          // Rafraîchir les stats APRÈS le logging
          setTimeout(() => {
            this.dashboardService.refreshDashboard();
          }, 500);
        },
        error: (logError) => {
          console.warn('Erreur lors du logging:', logError);
        }
      });

      alert('Comparaison créée avec succès ! ID: ' + result.id);
      this.loadComparisons();
      this.selectedPlayers = [];
    },
    error: (error) => {
      console.error('Erreur lors de la création de la comparaison:', error);
      alert('Échec de la création de la comparaison');
    }
  });
}
  /**
   * Charge toutes les comparaisons
   */
  private loadComparisons(): void {
  this.isLoading = true;
  const timeoutId = setTimeout(() => {
    if (this.isLoading) {
      console.warn('Chargement des comparaisons bloqué après 10 secondes, forçant la fin du loading.');
      this.isLoading = false;
      this.errorMessage = 'Délai dépassé, veuillez réessayer.';
    }
  }, 10000);

  this.playerService.getAllComparisons().subscribe({
    next: (comparisons) => {
      clearTimeout(timeoutId);
      this.comparisons = comparisons;
      this.isLoading = false;
    },
    error: (error) => {
      console.error('Erreur lors du chargement des comparaisons:', error);
      this.errorMessage = 'Erreur lors du chargement des comparaisons';
      this.isLoading = false;
    }
  });
}

loadComparison(id: number): void {
  this.isLoading = true;
  const timeoutId = setTimeout(() => {
    if (this.isLoading) {
      console.warn('Chargement de la comparaison bloqué après 10 secondes, forçant la fin du loading.');
      this.isLoading = false;
      this.errorMessage = 'Délai dépassé, veuillez réessayer.';
    }
  }, 10000);

  this.playerService.getComparison(id).subscribe({
    next: (comparison) => {
      clearTimeout(timeoutId);
      if (comparison.players) {
        this.selectedPlayers = comparison.players.map(p => ({
          ...p,
          comparisonStats: this.extractComparisonStats(p)
        }) as ComparisonPlayer);
      } else {
        this.selectedPlayers = [];
        comparison.playerIds.forEach(playerId => {
          this.playerService.getPlayerProfile(playerId).subscribe({
            next: (player) => {
              const comparisonPlayer: ComparisonPlayer = {
                ...player,
                comparisonStats: this.extractComparisonStats(player)
              };
              this.selectedPlayers.push(comparisonPlayer);
            },
            error: (error) => console.error('Erreur chargement joueur:', error)
          });
        });
      }
      this.selectedComparisonId = id;
      this.isLoading = false;
    },
    error: (error) => {
      clearTimeout(timeoutId);
      console.error('Erreur lors du chargement de la comparaison:', error);
      this.errorMessage = 'Erreur lors du chargement de la comparaison';
      this.isLoading = false;
    }
  });
}
  /**
   * Exporte en PDF
   */
  handleExportPDF(): void {
  if (this.selectedPlayers.length < 2) {
    alert('Vous devez sélectionner au moins 2 joueurs pour exporter');
    return;
  }

  this.isExportingPDF = true;
  this.dashboardService.logExport({
    format: 'pdf',
    dataType: 'comparison',
    playerIds: this.selectedPlayers.map(p => p.id)
  }).subscribe({
    next: () => console.log('Export PDF loggé'),
    error: (error) => console.warn('Erreur logging export PDF:', error)
  });
  setTimeout(() => {
    this.isExportingPDF = false;
    this.showExportSuccess = true;

    const playerData = this.selectedPlayers.map((player, index) => {
      const stats = this.extractComparisonStats(player); // Extraire les stats
      const position = this.playerService.getPositionLabel(player.position)?.toUpperCase() || '';

      let specificStats = '';
      switch (position) {
        case 'ATTAQUANT':
          specificStats = (stats.shots ? `\nTirs: ${stats.shots}` : '') +
                          (stats.conversionRate ? `\nTaux conversion: ${stats.conversionRate}` : '');
          break;
        case 'MILIEU':
          specificStats = (stats.passesReussies ? `\nPasses réussies: ${stats.passesReussies}` : '') +
                          (stats.recuperations ? `\nRécupérations: ${stats.recuperations}` : '') +
                          (stats.distanceParcourue ? `\nDistance parcourue: ${stats.distanceParcourue} km` : '');
          break;
        case 'DÉFENSEUR':
          specificStats = (stats.tackles ? `\nTacles: ${stats.tackles}` : '') +
                          (stats.taclesReussis ? `\nTacles réussis: ${stats.taclesReussis}` : '') +
                          (stats.interceptions ? `\nInterceptions: ${stats.interceptions}` : '') +
                          (stats.duelsAeriens ? `\nDuels aériens: ${stats.duelsAeriens}` : '') +
                          (stats.duelsGagnes ? `\nDuels gagnés: ${stats.duelsGagnes}` : '') +
                          (stats.tackleSuccessRate ? `\n% Tacles réussis: ${stats.tackleSuccessRate}` : '');
          break;
        case 'GARDIEN':
          specificStats = (stats.saves ? `\nArrêts: ${stats.saves}` : '') +
                          (stats.cleanSheets ? `\nClean sheets: ${stats.cleanSheets}` : '') +
                          (stats.savePercentage ? `\n% Arrêts: ${stats.savePercentage}` : '') +
                          (stats.penaltiesSaved ? `\nPénaltys arrêtés: ${stats.penaltiesSaved}` : '');
          break;
        default:
          specificStats = '';
      }

      return `\nJoueur ${index + 1}: ${player.name}\n` +
             `Club: ${player.clubName || player.club?.name || ''}\n` +
             `Position: ${player.position}\n` +
             `Âge: ${player.age || ''} ans\n` +
             `Nationalité: ${player.nationality || ''}\n` +
             `Valeur marchande: ${player.marketValue || ''}\n` +
             `Buts: ${stats.goals || 0}\n` +
             `Passes décisives: ${stats.assists || 0}` +
             specificStats;
    }).join('');

    const pdfContent = `Rapport de Comparaison - FootballAI\n` +
      `Date: ${new Date().toLocaleDateString()}\n\n${playerData}\n\n` +
      `Rapport généré par FootballAI`;

    const blob = new Blob([pdfContent], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `comparaison_joueurs_${new Date().toISOString().split('T')[0]}.pdf`;
    link.click();
    window.URL.revokeObjectURL(url);

    setTimeout(() => {
      this.showExportSuccess = false;
    }, 3000);
  }, 2000);
}

  /**
   * Exporte en Excel/CSV
   */
  handleExportExcel(): void {
  if (this.selectedPlayers.length < 2) {
    alert('Vous devez sélectionner au moins 2 joueurs pour exporter');
    return;
  }

  // Définir les en-têtes de base communs à toutes les positions
  const baseHeaders = ['Nom', 'Club', 'Position', 'Âge', 'Nationalité', 'Valeur', 'Buts', 'Passes'];
  const positionSpecificHeaders: { [key: string]: string[] } = {
    'ATTAQUANT': ['Tirs', 'Taux conversion'],
    'MILIEU': ['Passes réussies', 'Récupérations', 'Distance parcourue'],
    'DÉFENSEUR': ['Tacles', 'Tacles réussis', 'Interceptions', 'Duels aériens', 'Duels gagnés', '% Tacles réussis'],
    'GARDIEN': ['Arrêts', 'Clean sheets', '% Arrêts', 'Pénaltys arrêtés']
  };

  // Construire l'en-tête CSV en fonction des positions des joueurs sélectionnés
  const allHeaders = new Set<string>(baseHeaders);
  this.selectedPlayers.forEach(player => {
    const position = this.playerService.getPositionLabel(player.position)?.toUpperCase() || '';
    const specificHeaders = positionSpecificHeaders[position] || [];
    specificHeaders.forEach(header => allHeaders.add(header));
  });
  const csvHeaders = Array.from(allHeaders).join(',') + '\n';

  // Générer le contenu CSV
  const csvContent = csvHeaders + this.selectedPlayers.map(player => {
    const position = this.playerService.getPositionLabel(player.position)?.toUpperCase() || '';
    const stats = this.extractComparisonStats(player); // Extraire les stats

    // Construire la ligne CSV avec les valeurs correspondantes
    const baseValues = [
      player.name,
      player.clubName || player.club?.name || '',
      player.position,
      player.age?.toString() || '',
      player.nationality || '',
      player.marketValue || '',
      stats.goals?.toString() || '0',
      stats.assists?.toString() || '0'
    ];

    // Ajouter les statistiques spécifiques selon la position
    let specificValues = '';
    switch (position) {
      case 'ATTAQUANT':
        specificValues = `,${stats.shots || 0},${stats.conversionRate || '0%'}`;
        break;
      case 'MILIEU':
        specificValues = `,${stats.passesReussies || 0},${stats.recuperations || 0},${stats.distanceParcourue || 0}`;
        break;
      case 'DÉFENSEUR':
        specificValues = `,${stats.tackles || 0},${stats.taclesReussis || 0},${stats.interceptions || 0},${stats.duelsAeriens || 0},${stats.duelsGagnes || 0},${stats.tackleSuccessRate || '0%'}`;
        break;
      case 'GARDIEN':
        specificValues = `,${stats.saves || 0},${stats.cleanSheets || 0},${stats.savePercentage || '0%'},${stats.penaltiesSaved || 0}`;
        break;
      default:
        specificValues = '';
    }

    return baseValues.join(',') + specificValues;
  }).join('\n');

  // Créer et télécharger le fichier
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `comparaison_joueurs_${new Date().toISOString().split('T')[0]}.csv`;
  link.click();
  window.URL.revokeObjectURL(url);
}
  /**
   * Partage l'analyse
   */
  handleShareAnalysis(): void {
    if (this.selectedPlayers.length < 2) {
      alert('Vous devez sélectionner au moins 2 joueurs pour partager');
      return;
    }

    const shareText = `Comparaison FootballAI: ${this.selectedPlayers.map(p => p.name).join(' vs ')}`;

    if (navigator.share) {
      navigator.share({
        title: 'Comparaison de joueurs - FootballAI',
        text: shareText,
        url: window.location.href
      });
    } else {
      navigator.clipboard.writeText(`${shareText} - ${window.location.href}`);
      alert('Lien de comparaison copié dans le presse-papiers!');
    }
  }

  /**
   * Navigation
   */
  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  /**
   * Obtient la couleur pour un joueur
   */
  getPlayerColor(index: number): string {
    const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500'];
    return colors[index] || 'bg-gray-500';
  }

  /**
   * Obtient une valeur de statistique
   */
 getStatValue(player: ComparisonPlayer, stat: string): number {
  const value = player.comparisonStats?.[stat as keyof typeof player.comparisonStats];
  return typeof value === 'number' ? value : Number(value) || 0;
}


  /**
   * Calcule le total buts + passes décisives
   */
  getTotalGoalsAssists(player: ComparisonPlayer): number {
    const goals = player.comparisonStats?.goals || 0;
    const assists = player.comparisonStats?.assists || 0;
    return goals + assists;
  }

  /**
   * Obtient la liste des joueurs à afficher (recherche ou disponibles)
   */
  getDisplayPlayers(): Player[] {
    return this.searchResults.length > 0 ? this.searchResults : this.availablePlayers;
  }

  getStatWidth(value: number | undefined, max: number): number {
    if (!value || !max) return 0;
    const ratio = (value / max) * 100;
    return Math.min(100, Math.max(0, Math.round(ratio))); // Clamp entre 0 et 100
  }

  /**
   * Rafraîchit les données
   */
  refreshData(): void {
    this.loadInitialData();
    this.loadComparisons();
  }


}