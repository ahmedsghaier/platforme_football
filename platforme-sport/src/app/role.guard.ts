import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { Auth } from './auth';
@Injectable({
  providedIn: 'root'
}) 
export class RoleGuard implements CanActivate {

  constructor(
    private authService: Auth,
    private router: Router
  ) {}

  canActivate(route: any): Observable<boolean | UrlTree> {
    const requiredRole = route.data?.role;
    
    if (!requiredRole) {
      return of(true); // Pas de rôle requis
    }

    if (!this.authService.isAuthenticated()) {
      return of(this.router.createUrlTree(['/login']));
    }

    const user = this.authService.getCurrentUser();
    if (!user) {
      return of(this.router.createUrlTree(['/login']));
    }

    if (user.accountType === requiredRole || user.accountType === 'admin') {
      return of(true);
    }

    console.warn(`[RoleGuard] Accès refusé - rôle requis: ${requiredRole}, rôle utilisateur: ${user.accountType}`);
    return of(this.router.createUrlTree(['/dashboard']));
  }
}