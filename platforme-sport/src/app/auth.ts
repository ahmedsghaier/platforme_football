import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { throwError } from 'rxjs';

export interface User {
  id: number;
  name: string;
  email: string;
  accountType: string;
  organization?: string;
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  token?: string;
  user?: User;
   errors?: string[];
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterData {
  name: string;
  email: string;
  password: string;
  accountType?: string;
  organization?: string;
  newsletter?: boolean;
  acceptTerms?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class Auth {
  private apiUrl = 'http://localhost:8080/api/auth';
  private tokenKey = 'authToken';
  private userKey = 'currentUser';
  private isBrowser: boolean;

  // BehaviorSubjects pour la réactivité
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private currentUserSubject = new BehaviorSubject<User | null>(null);

  // Observables publics
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    
    // Initialiser seulement côté client après que le composant soit prêt
    if (this.isBrowser) {
      // Utiliser setTimeout pour s'assurer que le DOM est prêt
      setTimeout(() => {
        this.initializeAuth();
      }, 0);
    }
  }

  /**
   * Initialise l'authentification au démarrage de l'application
   */
  private initializeAuth(): void {
    if (!this.isBrowser) return;

    try {
      // Vérifier si localStorage est disponible
      if (typeof Storage === 'undefined') {
        console.warn('localStorage n\'est pas disponible');
        this.setDefaultAuthState();
        return;
      }

      const hasToken = this.hasValidToken();
      const user = this.getCurrentUserFromStorage();
      
      // Initialiser les sujets avec les valeurs du storage
      this.isAuthenticatedSubject.next(hasToken);
      this.currentUserSubject.next(user);
      
      if (hasToken && user) {
        this.verifyTokenOnInit().subscribe({
          next: (response) => {
            if (response.valid && response.user) {
              this.setCurrentUser(response.user);
            } else {
              this.clearAuthData();
            }
          },
          error: () => {
            console.warn('Échec de la vérification du token au démarrage');
            this.clearAuthData();
          }
        });
      } else if (hasToken && !user) {
        // Token présent mais pas d'utilisateur, nettoyer
        this.clearAuthData();
      }
    } catch (error) {
      console.error('Erreur lors de l\'initialisation de l\'authentification:', error);
      this.setDefaultAuthState();
    }
  }

  /**
   * Définit l'état d'authentification par défaut
   */
  private setDefaultAuthState(): void {
    this.isAuthenticatedSubject.next(false);
    this.currentUserSubject.next(null);
  }

  /**
   * Vérifie le token au démarrage (version silencieuse)
   */
  private verifyTokenOnInit(): Observable<any> {
    const token = this.getToken();
    if (!token) {
      return of({ valid: false });
    }

    try {
      const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
      return this.http.get(`${this.apiUrl}/verify`, { headers }).pipe(
        catchError(() => of({ valid: false }))
      );
    } catch (error) {
      console.error('Erreur lors de la vérification du token:', error);
      return of({ valid: false });
    }
  }

  /**
   * Méthode de connexion
   */
  login(credentials: LoginCredentials): Observable<AuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials, { headers }).pipe(
      tap((response: AuthResponse) => {
        if (response.success && response.token && response.user) {
          this.setAuthData(response.token, response.user);
        } else {
          throw new Error(response.message || 'Échec de la connexion');
        }
      }),
      catchError(this.handleAuthError.bind(this))
    );
  }

  /**
   * Méthode d'inscription
   */
  register(data: RegisterData): Observable<AuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    
    const registrationData = {
      name: data.name,
      email: data.email,
      password: data.password,
      accountType: data.accountType || 'recruiter',
      organization: data.organization || '',
      newsletter: data.newsletter || false,
      acceptTerms: data.acceptTerms || false
    };

    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, registrationData, { headers }).pipe(
      tap((response: AuthResponse) => {
        if (!response.success) {
          throw new Error(response.message || 'Échec de l\'inscription');
        }
      }),
      catchError(this.handleAuthError.bind(this))
    );
  }

  /**
   * Méthode de vérification du token (publique)
   */
  verifyToken(): Observable<any> {
    const token = this.getToken();
    if (!token) {
      return throwError(() => new Error('Aucun token trouvé'));
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    return this.http.get(`${this.apiUrl}/verify`, { headers }).pipe(
      tap((response: any) => {
        if (!response.valid) {
          this.clearAuthData();
          throw new Error('Token invalide');
        }
        if (response.user) {
          this.setCurrentUser(response.user);
        }
      }),
      catchError((error) => {
        console.error('[AuthService] Verify token error:', error);
        this.clearAuthData();
        return throwError(() => new Error(error.error?.message || 'Échec de la vérification du token'));
      })
    );
  }

  /**
   * Méthode de déconnexion
   */
  logout(): Observable<any> {
    const token = this.getToken();
    
    // Nettoyer les données locales immédiatement
    this.clearAuthData();

    // Informer le serveur de la déconnexion si possible
    if (token) {
      const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
      return this.http.post(`${this.apiUrl}/logout`, {}, { headers }).pipe(
        catchError(() => of({ success: true })) // Ignorer les erreurs de logout côté serveur
      );
    }

    return of({ success: true });
  }

  /**
   * Rafraîchit le token
   */
  refreshToken(): Observable<AuthResponse> {
    const token = this.getToken();
    if (!token) {
      return throwError(() => new Error('Aucun token à rafraîchir'));
    }

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}, { headers }).pipe(
      tap((response: AuthResponse) => {
        if (response.success && response.token) {
          this.setToken(response.token);
          if (response.user) {
            this.setCurrentUser(response.user);
          }
        } else {
          this.clearAuthData();
          throw new Error(response.message || 'Échec du rafraîchissement du token');
        }
      }),
      catchError(this.handleAuthError.bind(this))
    );
  }

  /**
   * Réinitialisation de mot de passe
   */
  requestPasswordReset(email: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(`${this.apiUrl}/password-reset-request`, { email }, { headers }).pipe(
      catchError(this.handleAuthError.bind(this))
    );
  }

  /**
   * Confirmer la réinitialisation de mot de passe
   */
  resetPassword(token: string, newPassword: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(`${this.apiUrl}/password-reset`, { token, newPassword }, { headers }).pipe(
      catchError(this.handleAuthError.bind(this))
    );
  }

  /**
   * Gestion centralisée des erreurs d'authentification
   */
  private handleAuthError(error: HttpErrorResponse): Observable<never> {
    let errorMessage: string;

    console.error('[AuthService] Error details:', {
      status: error.status,
      statusText: error.statusText,
      url: error.url,
      message: error.message,
      error: error.error
    });

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur de connexion: ${error.error.message}`;
    } else {
      // Erreur côté serveur
      switch (error.status) {
        case 0:
          errorMessage = 'Impossible de joindre le serveur d\'authentification. Vérifiez votre connexion.';
          break;
        case 401:
          errorMessage = 'Identifiants incorrects ou session expirée.';
          this.clearAuthData();
          break;
        case 403:
          errorMessage = 'Accès non autorisé.';
          break;
        case 404:
          errorMessage = 'Service d\'authentification non trouvé.';
          break;
        case 422:
          errorMessage = error.error?.message || 'Données d\'authentification invalides.';
          break;
        case 429:
          errorMessage = 'Trop de tentatives de connexion. Veuillez réessayer plus tard.';
          break;
        case 500:
          errorMessage = 'Erreur interne du serveur d\'authentification.';
          break;
        case 503:
          errorMessage = 'Service d\'authentification temporairement indisponible.';
          break;
        default:
          errorMessage = error.error?.message || `Erreur d'authentification (${error.status})`;
      }
    }

    return throwError(() => new Error(errorMessage));
  }

  /**
   * Définit les données d'authentification
   */
  private setAuthData(token: string, user: User): void {
    this.setToken(token);
    this.setCurrentUser(user);
    this.isAuthenticatedSubject.next(true);
  }

  /**
   * Nettoie toutes les données d'authentification
   */
  private clearAuthData(): void {
    if (this.isBrowser && this.isStorageAvailable()) {
      try {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.userKey);
      } catch (error) {
        console.warn('Erreur lors du nettoyage du localStorage:', error);
      }
    }
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
  }

  /**
   * Vérifie si le stockage local est disponible
   */
  private isStorageAvailable(): boolean {
    try {
      return typeof Storage !== 'undefined' && localStorage !== null;
    } catch {
      return false;
    }
  }

  /**
   * Définit le token
   */
  private setToken(token: string): void {
    if (this.isBrowser && this.isStorageAvailable()) {
      try {
        localStorage.setItem(this.tokenKey, token);
      } catch (error) {
        console.warn('Erreur lors de la sauvegarde du token:', error);
      }
    }
  }

  /**
   * Récupère le token
   */
  getToken(): string | null {
    if (this.isBrowser && this.isStorageAvailable()) {
      try {
        return localStorage.getItem(this.tokenKey);
      } catch (error) {
        console.warn('Erreur lors de la récupération du token:', error);
        return null;
      }
    }
    return null;
  }

  /**
   * Définit l'utilisateur actuel
   */
  private setCurrentUser(user: User): void {
    if (this.isBrowser && this.isStorageAvailable()) {
      try {
        localStorage.setItem(this.userKey, JSON.stringify(user));
      } catch (error) {
        console.warn('Erreur lors de la sauvegarde de l\'utilisateur:', error);
      }
    }
    this.currentUserSubject.next(user);
  }

  /**
   * Récupère l'utilisateur actuel depuis le stockage
   */
  private getCurrentUserFromStorage(): User | null {
    if (!this.isBrowser || !this.isStorageAvailable()) {
      return null;
    }

    try {
      const userJson = localStorage.getItem(this.userKey);
      if (userJson) {
        return JSON.parse(userJson);
      }
    } catch (error) {
      console.error('Erreur lors de la désérialisation de l\'utilisateur:', error);
      if (this.isStorageAvailable()) {
        try {
          localStorage.removeItem(this.userKey);
        } catch (removeError) {
          console.warn('Erreur lors de la suppression de l\'utilisateur corrompu:', removeError);
        }
      }
    }
    return null;
  }

  /**
   * Récupère l'utilisateur actuel
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Récupère l'ID de l'utilisateur actuel
   */
  getCurrentUserId(): number | null {
    const user = this.getCurrentUser();
    return user ? user.id : null;
  }

  /**
   * Vérifie si l'utilisateur est authentifié
   */
  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  /**
   * Vérifie si un token valide existe
   */
  private hasValidToken(): boolean {
    if (!this.isBrowser || !this.isStorageAvailable()) {
      return false;
    }

    const token = this.getToken();
    if (!token) return false;

    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        console.warn('Format de token invalide');
        return false;
      }

      // Décodage du payload JWT
      const payload = JSON.parse(
        decodeURIComponent(
          atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
            .split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join('')
        )
      );

      // Vérifier si le token a un champ exp et qu'il n'est pas expiré
      if (!payload.exp) {
        console.warn('Token sans champ exp');
        return false;
      }

      const now = Math.floor(Date.now() / 1000);
      const isValid = payload.exp > now;
      
      if (!isValid) {
        console.warn('Token expiré');
        this.clearAuthData();
      }
      
      return isValid;
    } catch (error) {
      console.error('Erreur lors de la validation du token:', error);
      this.clearAuthData();
      return false;
    }
  }

  /**
   * Obtient les headers d'authentification
   */
  getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    if (token) {
      return new HttpHeaders().set('Authorization', `Bearer ${token}`);
    }
    return new HttpHeaders();
  }

  /**
   * Vérifie si l'utilisateur a un rôle spécifique
   */
  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user ? user.accountType === role : false;
  }

  /**
   * Vérifie si l'utilisateur est un administrateur
   */
  isAdmin(): boolean {
    return this.hasRole('admin');
  }

  /**
   * Vérifie si l'utilisateur est un recruteur
   */
  isRecruiter(): boolean {
    return this.hasRole('recruiter');
  }
loginWithGoogle(googleToken: string): Observable<AuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    
    console.log('[Auth] Envoi du token Google au backend:', googleToken.substring(0, 20) + '...');
    
    return this.http.post<AuthResponse>(`${this.apiUrl}/google`, {
      token: googleToken
    }, { headers }).pipe(
      tap((response: AuthResponse) => {
        console.log('[Auth] Réponse du backend:', response);
        if (response.success && response.token && response.user) {
          this.setAuthData(response.token, response.user);
        } else {
          throw new Error(response.message || 'Échec de la connexion avec Google');
        }
      }),
      catchError((error) => {
        console.error('[Auth] Erreur lors de la connexion Google:', error);
        return this.handleAuthError(error);
      })
    );
  }

  registerWithGoogle(googleToken: string, additionalData?: any): Observable<AuthResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    
    const registrationData = {
      token: googleToken,
      accountType: additionalData?.accountType || 'recruiter',
      organization: additionalData?.organization || '',
      newsletter: additionalData?.newsletter || false,
      acceptTerms: additionalData?.acceptTerms || false
    };

    console.log('[Auth] Données d\'inscription Google:', registrationData);

    return this.http.post<AuthResponse>(`${this.apiUrl}/google/register`, 
      registrationData, 
      { headers }
    ).pipe(
      tap((response: AuthResponse) => {
        console.log('[Auth] Réponse inscription Google:', response);
        if (response.success) {
          // Pour l'inscription, on ne connecte pas automatiquement l'utilisateur
          // On laisse l'utilisateur se connecter manuellement après
          console.log('[Auth] Inscription Google réussie');
        } else {
          throw new Error(response.message || 'Échec de l\'inscription avec Google');
        }
      }),
      catchError((error) => {
        console.error('[Auth] Erreur lors de l\'inscription Google:', error);
        return this.handleAuthError(error);
      })
    );
  }
}