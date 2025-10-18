import { Component, OnInit, OnDestroy,ChangeDetectorRef} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PlayerService, Player } from '../services/player';
import { Subscription,Subject,takeUntil } from 'rxjs';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class Home implements OnInit, OnDestroy {
  searchQuery: string = '';
  topPlayers: Player[] = [];
  private routerSubscription?: Subscription;
  private destroy$ = new Subject<void>();

  features = [
    {
      icon: 'ri-search-eye-line',
      title: 'Recherche Avancée',
      description: 'Trouvez des joueurs avec des filtres précis : âge, poste, club, championnat et valeur estimée.'
    },
    {
      icon: 'ri-line-chart-line',
      title: 'Analyse IA',
      description: 'Estimation de valeur marchande basée sur l\'intelligence artificielle avec niveau de confiance.'
    },
    {
      icon: 'ri-bar-chart-grouped-line',
      title: 'Comparateur',
      description: 'Comparez jusqu\'à 3 joueurs simultanément avec graphiques détaillés et statistiques.'
    },
    {
      icon: 'ri-trophy-line',
      title: 'Statistiques Détaillées',
      description: 'Stats complètes adaptées par poste : buts, passes, tacles, arrêts selon le rôle.'
    },
    {
      icon: 'ri-pulse-line',
      title: 'Tendances Marché',
      description: 'Suivez l\'évolution des valeurs et identifiez les opportunités d\'investissement.'
    },
    {
      icon: 'ri-file-download-line',
      title: 'Rapports Export',
      description: 'Exportez vos analyses en PDF ou Excel pour vos présentations professionnelles.'
    }
  ];

constructor(
    private router: Router,
    private playerService: PlayerService,
    private cdr: ChangeDetectorRef
  ) {
    // Désactiver la réutilisation de route pour ce composant
    this.router.routeReuseStrategy.shouldReuseRoute = () => false;
  }

  ngOnInit() {
    this.loadTopPlayers();

    this.routerSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.loadTopPlayers(true); // Recharger avec forceRefresh
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.routerSubscription?.unsubscribe();
  }

  loadTopPlayers(forceRefresh: boolean = false) {
    this.playerService.getTopPlayers(forceRefresh).subscribe({
      next: (data) => {
        this.topPlayers = data || [];
        this.cdr.detectChanges(); // Force la mise à jour du DOM
        console.log('Top joueurs chargés:', this.topPlayers);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des joueurs:', err);
        this.topPlayers = []; // Réinitialiser en cas d'erreur
        this.cdr.detectChanges();
      }
    });
  }

  handleSearch(): void {
    const query = this.searchQuery.trim();
    if (query) {
      this.router.navigate(['/search'], { queryParams: { q: query } });
    } else {
      this.router.navigate(['/search']);
    }
    this.searchQuery = '';
  }

  convertToMillions(value: number): string {
    if (!value) return '0 M€';
    const millions = value / 1_000_000;
    return millions.toFixed(1) + ' M€';
  }
 getConfidenceDisplay(confidence: number | string | null | undefined): string {
  if (confidence === null || confidence === undefined || confidence === '') {
    return 'N/A';
  }

  const val = Number(confidence);
  if (isNaN(val)) {
    return 'N/A';
  }

  // Si la valeur est entre 0 et 1, considère que c'est un float (genre 0.83)
  const formatted = val <= 1 ? val * 100 : val;

  return formatted.toFixed(2) + ' %';
}


}
