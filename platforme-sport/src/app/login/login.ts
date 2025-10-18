import { Component, ChangeDetectorRef, ChangeDetectionStrategy, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Auth } from '../auth';
import { finalize, timeout, catchError, of, filter, take } from 'rxjs'; // Fixed: Added take import
import { GoogleAuthService } from '../services/google-auth';
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
  // Changement important : utilisation de OnPush pour forcer la détection manuelle
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Login {
  formData = {
    email: '',
    password: '',
    rememberMe: false
  };

  isLoading = false;
  showPassword = false;
  errorMessage: string = '';

  constructor(
    private auth: Auth, 
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone, // Ajout de NgZone pour gérer les changements
    private googleAuth : GoogleAuthService
  ) {}

  toggleShowPassword(): void {
    this.showPassword = !this.showPassword;
    // Forcer la détection immédiatement
    this.cdr.markForCheck();
  }

  handleSubmit(): void {
    // Validation des données avant envoi
    if (!this.formData.email?.trim() || !this.formData.password?.trim()) {
      this.errorMessage = 'Veuillez renseigner tous les champs obligatoires.';
      this.cdr.markForCheck();
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.formData.email.trim())) {
      this.errorMessage = 'Veuillez saisir une adresse email valide.';
      this.cdr.markForCheck();
      return;
    }

    // Empêcher les soumissions multiples
    if (this.isLoading) {
      return;
    }

    // Utiliser NgZone pour s'assurer que les changements sont détectés
    this.ngZone.run(() => {
      this.isLoading = true;
      this.errorMessage = '';
      this.cdr.markForCheck();
    });

    console.log('[Login] Début de la connexion avec:', { email: this.formData.email.trim() });

    this.auth.login({
      email: this.formData.email.trim(),
      password: this.formData.password
    })
      .pipe(
        timeout(30000),
        catchError((error) => {
          console.error('[Login] Erreur interceptée:', error);
          return of({ success: false, httpError: error });
        }),
        finalize(() => {
          // Utiliser NgZone pour s'assurer que le loading se termine
          this.ngZone.run(() => {
            console.log('[Login] Finalisation - Arrêt du loading');
            this.isLoading = false;
            this.cdr.markForCheck();
          });
        })
      )
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            console.log('[Login] Réponse reçue:', response);
            
            // Vérifier si c'est une erreur HTTP interceptée par catchError
            if (response && 'httpError' in response) {
              this.handleHttpError(response.httpError);
              return;
            }
            
            // Vérifier si la réponse est null ou undefined
            if (!response) {
              console.error('[Login] Réponse null ou undefined');
              this.errorMessage = 'Aucune réponse du serveur. Veuillez réessayer.';
              this.cdr.markForCheck();
              return;
            }
            
            // Traiter la réponse de succès
            if (response.success === true) {
              console.log('[Login] Connexion réussie');
              this.formData.password = '';
              this.router.navigate(['/dashboard']);
            } else {
              // Réponse du serveur mais pas de succès
              this.errorMessage = response.message || 'Échec de la connexion.';
              console.warn('[Login] Connexion échouée:', this.errorMessage);
              this.cdr.markForCheck();
            }
          });
        },
        error: (error) => {
          this.ngZone.run(() => {
            console.error('[Login] Erreur non interceptée:', error);
            this.handleHttpError(error);
          });
        }
      });
  }

  private handleHttpError(error: any): void {
    console.error('[Login] Traitement de l\'erreur HTTP:', error);
    
    // Log plus détaillé pour le debugging
    if (error.status === 401) {
      console.error('[Login] Erreur 401 - Détails complètes:', {
        url: error.url,
        status: error.status,
        statusText: error.statusText,
        errorBody: error.error,
        errorBodyStringified: JSON.stringify(error.error, null, 2),
        message: error.message,
        fullError: error
      });
    }
    
    // Gestion spéciale pour le timeout
    if (error.name === 'TimeoutError') {
      this.errorMessage = 'La connexion a pris trop de temps. Veuillez réessayer.';
      this.cdr.markForCheck();
      return;
    }

    // Gestion des erreurs HTTP
    switch (error.status) {
      case 0:
        this.errorMessage = "Impossible de contacter le serveur. Vérifiez votre connexion internet.";
        break;
      case 400:
        this.errorMessage = 'Données de connexion invalides.';
        break;
      case 401:
        // Message plus spécifique basé sur la réponse du serveur
        const serverMessage = error.error?.message || error.error?.error;
        this.errorMessage = serverMessage || 'Email ou mot de passe incorrect.';
        console.warn('[Login] Authentification échouée:', serverMessage);
        break;
      case 403:
        this.errorMessage = 'Accès refusé. Votre compte pourrait être suspendu.';
        break;
      case 404:
        this.errorMessage = 'Service de connexion non disponible.';
        break;
      case 422:
        this.errorMessage = 'Données de connexion invalides.';
        break;
      case 429:
        this.errorMessage = 'Trop de tentatives. Veuillez patienter avant de réessayer.';
        break;
      case 500:
      case 502:
      case 503:
        this.errorMessage = 'Erreur serveur. Veuillez réessayer plus tard.';
        break;
      default:
        this.errorMessage = error.error?.message || error.message || 'Une erreur est survenue. Veuillez réessayer.';
    }
    
    // Force la détection des changements pour afficher le message d'erreur immédiatement
    this.cdr.markForCheck();
  }

  // Méthode pour nettoyer les messages d'erreur quand l'utilisateur commence à taper
  onInputChange(): void {
    if (this.errorMessage) {
      this.errorMessage = '';
      this.cdr.markForCheck();
    }
  }

  // Méthode pour gérer la touche Entrée
  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !this.isLoading) {
      this.handleSubmit();
    }
  }
loginWithGoogle(): void {
    if (this.isLoading) return;
    
    this.ngZone.run(() => {
      this.isLoading = true;
      this.errorMessage = '';
      this.cdr.markForCheck();
    });

    console.log('[Login] Début de la connexion Google');

    // Check if Google Auth is ready with timeout
    this.googleAuth.isReady().pipe(
      filter(isReady => isReady),
      take(1), // Utilisation correcte de take depuis rxjs
      timeout(10000),
      catchError(() => {
        console.error('[Login] Google Auth initialization timeout');
        return of(false);
      })
    ).subscribe({
      next: (isReady) => {
        if (!isReady) {
          this.ngZone.run(() => {
            this.errorMessage = 'Service Google non disponible. Veuillez réessayer.';
            this.isLoading = false;
            this.cdr.markForCheck();
          });
          return;
        }

        this.googleAuth.signInWithPopup()
          .then((googleResponse) => {
            console.log('[Login] Réponse Google reçue:', googleResponse);
            
            let token = null;
            let userInfo = null;
            
            if (googleResponse.credential) {
              token = googleResponse.credential;
              userInfo = this.googleAuth.parseJwt(googleResponse.credential);
            } else if (googleResponse.access_token) {
              token = googleResponse.access_token;
              userInfo = googleResponse.profile;
            } else if (googleResponse.id_token) {
              token = googleResponse.id_token;
              userInfo = this.googleAuth.parseJwt(googleResponse.id_token);
            }
            
            if (!token) {
              throw new Error('Aucun token valide reçu de Google');
            }

            console.log('[Login] Informations utilisateur Google:', userInfo);
            
            return this.auth.loginWithGoogle(token).toPromise();
          })
          .then((authResponse) => {
            this.ngZone.run(() => {
              console.log('[Login] Réponse serveur authentification:', authResponse);
              
              if (authResponse?.success) {
                console.log('[Login] Connexion Google réussie');
                this.router.navigate(['/dashboard']);
              } else {
                this.errorMessage = authResponse?.message || 'Échec de la connexion Google.';
                this.cdr.markForCheck();
              }
            });
          })
          .catch((error) => {
            this.ngZone.run(() => {
              console.error('[Login] Erreur connexion Google:', error);
              
              let errorMsg = 'Erreur lors de la connexion avec Google. Veuillez réessayer.';
              
              if (error === 'popup_closed_by_user') {
                errorMsg = 'Connexion annulée par l\'utilisateur.';
              } else if (error === 'popup_blocked_by_browser') {
                errorMsg = 'La popup a été bloquée par votre navigateur. Veuillez autoriser les popups pour ce site.';
              } else if (error?.message?.includes('blocked')) {
                errorMsg = 'Connexion bloquée. Vérifiez que les popups sont autorisées.';
              } else if (error?.message?.includes('network')) {
                errorMsg = 'Erreur de réseau. Vérifiez votre connexion internet.';
              }
              
              this.errorMessage = errorMsg;
              this.cdr.markForCheck();
            });
          })
          .finally(() => {
            this.ngZone.run(() => {
              this.isLoading = false;
              this.cdr.markForCheck();
            });
          });
      },
      error: () => {
        this.ngZone.run(() => {
          this.errorMessage = 'Service Google non disponible. Veuillez réessayer.';
          this.isLoading = false;
          this.cdr.markForCheck();
        });
      }
    });
  }


  loginWithFacebook(): void {
    if (this.isLoading) return;
    
    this.ngZone.run(() => {
      console.log('[Login] Tentative de connexion Facebook');
      this.errorMessage = 'Connexion Facebook non encore implémentée.';
      this.cdr.markForCheck();
    });
  }

  // Méthode pour forcer manuellement la détection des changements (utile pour le debugging)
  forceUpdate(): void {
    this.cdr.detectChanges();
  }
 
}

