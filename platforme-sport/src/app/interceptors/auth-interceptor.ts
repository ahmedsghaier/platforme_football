// auth.interceptor.ts - Intercepteur pour gérer automatiquement l'authentification
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, switchMap, filter, take, finalize } from 'rxjs/operators';
import { Router } from '@angular/router';
import { Auth } from '../auth';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isBrowser: boolean;
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private authService: Auth,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Ne pas traiter côté serveur
    if (!this.isBrowser) {
      return next.handle(request);
    }

    // Ajouter le token d'authentification si disponible
    const authRequest = this.addAuthHeader(request);

    return next.handle(authRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        // Gérer les erreurs d'authentification
        if (error.status === 401) {
          return this.handle401Error(authRequest, next);
        }
        
        // Gérer les erreurs de serveur
        if (error.status === 0) {
          console.warn('Erreur de connexion réseau:', error.url);
        } else if (error.status >= 500) {
          console.error('Erreur serveur:', error.status, error.message);
        }

        return throwError(() => error);
      })
    );
  }

  /**
   * Ajoute le header d'authentification à la requête
   */
  private addAuthHeader(request: HttpRequest<any>): HttpRequest<any> {
    const token = this.authService.getToken();
    
    if (token) {
      return request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return request;
  }

  /**
   * Gère les erreurs 401 (non autorisé)
   */
  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Éviter les boucles infinies sur les endpoints d'auth
    if (this.isAuthEndpoint(request.url)) {
      this.authService.logout().subscribe();
      return throwError(() => new Error('Échec de l\'authentification'));
    }

    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const token = this.authService.getToken();
      
      if (token) {
        console.log('Tentative de rafraîchissement du token...');
        return this.authService.refreshToken().pipe(
          switchMap((response) => {
            this.isRefreshing = false;
            this.refreshTokenSubject.next(response.token);
            
            // Relancer la requête originale avec le nouveau token
            return next.handle(this.addAuthHeader(request));
          }),
          catchError((refreshError) => {
            console.error('Échec du rafraîchissement du token:', refreshError);
            this.isRefreshing = false;
            this.refreshTokenSubject.next(null);
            
            // Déconnecter l'utilisateur et rediriger
            this.authService.logout().subscribe(() => {
              this.router.navigate(['/login']);
            });
            
            return throwError(() => new Error('Session expirée'));
          })
        );
      } else {
        // Pas de token, déconnecter immédiatement
        this.isRefreshing = false;
        this.authService.logout().subscribe(() => {
          this.router.navigate(['/login']);
        });
        return throwError(() => new Error('Non authentifié'));
      }
    } else {
      // Attendre que le rafraîchissement soit terminé
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(() => next.handle(this.addAuthHeader(request)))
      );
    }
  }

  /**
   * Vérifie si l'URL correspond à un endpoint d'authentification
   */
  private isAuthEndpoint(url: string): boolean {
    return url.includes('/api/auth/login') || 
           url.includes('/api/auth/register') || 
           url.includes('/api/auth/refresh') ||
           url.includes('/api/auth/logout');
  }
}

// error.interceptor.ts - Intercepteur pour gérer les erreurs globales
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  private isBrowser: boolean;

  constructor(
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!this.isBrowser) {
      return next.handle(request);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'Une erreur est survenue';

        if (error.error instanceof ErrorEvent) {
          // Erreur côté client
          errorMessage = `Erreur client: ${error.error.message}`;
        } else {
          // Erreur côté serveur
          switch (error.status) {
            case 400:
              errorMessage = 'Requête invalide';
              break;
            case 403:
              errorMessage = 'Accès refusé';
              break;
            case 404:
              errorMessage = 'Ressource non trouvée';
              break;
            case 429:
              errorMessage = 'Trop de requêtes. Veuillez patienter.';
              break;
            case 500:
              errorMessage = 'Erreur interne du serveur';
              break;
            case 503:
              errorMessage = 'Service temporairement indisponible';
              break;
            default:
              if (error.error?.message) {
                errorMessage = error.error.message;
              } else {
                errorMessage = `Erreur HTTP ${error.status}: ${error.statusText}`;
              }
          }
        }

        console.error('Erreur HTTP interceptée:', {
          status: error.status,
          message: errorMessage,
          url: request.url
        });

        // Créer une nouvelle erreur avec le message formaté
        const formattedError = new Error(errorMessage);
        (formattedError as any).status = error.status;
        (formattedError as any).originalError = error;

        return throwError(() => formattedError);
      })
    );
  }
}

// loading.interceptor.ts - Intercepteur pour gérer le state de chargement global
@Injectable()
export class LoadingInterceptor implements HttpInterceptor {
  private isBrowser: boolean;
  private loadingRequests: Set<string> = new Set();
  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!this.isBrowser) {
      return next.handle(request);
    }

    // Générer un ID unique pour cette requête
    const requestId = this.generateRequestId(request);
    
    // Ignorer certaines requêtes (comme les health checks)
    if (this.shouldIgnoreRequest(request)) {
      return next.handle(request);
    }

    // Ajouter la requête au set et mettre à jour l'état
    this.loadingRequests.add(requestId);
    this.updateLoadingState();

    return next.handle(request).pipe(
      finalize(() => {
        // Retirer la requête du set et mettre à jour l'état
        this.loadingRequests.delete(requestId);
        this.updateLoadingState();
      })
    );
  }

  private generateRequestId(request: HttpRequest<any>): string {
    return `${request.method}-${request.url}-${Date.now()}`;
  }

  private shouldIgnoreRequest(request: HttpRequest<any>): boolean {
    return request.url.includes('/health') || 
           request.url.includes('/ping') ||
           request.headers.has('X-Skip-Loading');
  }

  private updateLoadingState(): void {
    this.loadingSubject.next(this.loadingRequests.size > 0);
  }

  /**
   * Méthode publique pour vérifier si des requêtes sont en cours
   */
  isLoading(): boolean {
    return this.loadingRequests.size > 0;
  }

  /**
   * Méthode publique pour forcer l'arrêt du chargement (utile en cas d'erreur)
   */
  clearLoading(): void {
    this.loadingRequests.clear();
    this.updateLoadingState();
  }
}