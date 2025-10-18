// auth.guard.ts - Guard pour protéger les routes nécessitant une authentification
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { 
  CanActivate, 
  ActivatedRouteSnapshot, 
  RouterStateSnapshot, 
  Router,
  CanActivateChild,
  UrlTree 
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError, take } from 'rxjs/operators';
import { Auth } from '../auth';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild {
  private isBrowser: boolean;

  constructor(
    private authService: Auth,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.checkAuth(state.url);
  }

  canActivateChild(
    childRoute: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.checkAuth(state.url);
  }

  private checkAuth(url: string): Observable<boolean | UrlTree> {
    // Côté serveur, autoriser l'accès (sera géré côté client)
    if (!this.isBrowser) {
      return of(true);
    }

    console.log('AuthGuard: Vérification d\'accès pour:', url);
    
    // Vérifier d'abord l'état local
    const isAuthenticated = this.authService.isAuthenticated();
    const currentUser = this.authService.getCurrentUser();
    
    console.log('AuthGuard: État local - Authentifié:', isAuthenticated, 'Utilisateur:', !!currentUser);

    if (isAuthenticated && currentUser) {
      // Utilisateur authentifié avec des données complètes
      return of(true);
    }

    if (isAuthenticated && !currentUser) {
      // Token présent mais pas d'utilisateur, vérifier avec le serveur
      console.log('AuthGuard: Vérification du token avec le serveur...');
      return this.authService.verifyToken().pipe(
        take(1),
        map(() => {
          console.log('AuthGuard: Token valide');
          return true;
        }),
        catchError((error) => {
          console.error('AuthGuard: Token invalide:', error);
          return of(this.router.createUrlTree(['/login'], { queryParams: { returnUrl: url } }));
        })
      );
    }

    // Pas de token ou utilisateur non authentifié
    console.log('AuthGuard: Redirection vers login');
    return of(this.router.createUrlTree(['/login'], { queryParams: { returnUrl: url } }));
  }
}

// public-auth.guard.ts - Guard pour les pages publiques (login, register)
@Injectable({
  providedIn: 'root'
})
export class PublicAuthGuard implements CanActivate {
  private isBrowser: boolean;

  constructor(
    private authService: Auth,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    // Côté serveur, autoriser l'accès
    if (!this.isBrowser) {
      return of(true);
    }

    console.log('PublicAuthGuard: Vérification pour:', state.url);
    
    const isAuthenticated = this.authService.isAuthenticated();
    const currentUser = this.authService.getCurrentUser();

    if (isAuthenticated && currentUser) {
      // Utilisateur déjà connecté, rediriger vers le dashboard
      console.log('PublicAuthGuard: Utilisateur connecté, redirection vers dashboard');
      return of(this.router.createUrlTree(['/dashboard']));
    }

    // Autoriser l'accès aux pages publiques
    return of(true);
  }
}

// role.guard.ts - Guard basé sur les rôles
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  private isBrowser: boolean;

  constructor(
    private authService: Auth,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    if (!this.isBrowser) {
      return of(true);
    }

    const requiredRoles = route.data['roles'] as string[];
    if (!requiredRoles || requiredRoles.length === 0) {
      return of(true);
    }

    const isAuthenticated = this.authService.isAuthenticated();
    const currentUser = this.authService.getCurrentUser();

    if (!isAuthenticated || !currentUser) {
      console.log('RoleGuard: Utilisateur non authentifié');
      return of(this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }));
    }

    const hasRequiredRole = requiredRoles.some(role => this.authService.hasRole(role));
    
    if (hasRequiredRole) {
      return of(true);
    }

    console.log('RoleGuard: Rôle insuffisant pour:', currentUser.accountType, 'requis:', requiredRoles);
    return of(this.router.createUrlTree(['/unauthorized']));
  }
}