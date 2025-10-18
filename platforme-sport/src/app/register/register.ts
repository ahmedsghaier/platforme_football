import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Auth } from '../auth';
import { GoogleAuthService } from '../services/google-auth';
import { finalize, timeout, catchError, of ,filter,take} from 'rxjs';
interface FormData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  accountType: string;
  organization: string;
  acceptTerms: boolean;
  newsletter: boolean;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  formData: FormData = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    accountType: 'recruiter',
    organization: '',
    acceptTerms: false,
    newsletter: true
  };

  isLoading = false;
  showPassword = false;
  showConfirmPassword = false;
  errors: { [key: string]: string } = {};

  constructor(private auth: Auth, private router: Router,private googleAuth: GoogleAuthService) {}

  clearError(field: string): void {
    if (this.errors[field]) {
      delete this.errors[field];
    }
  }

  toggleShowPassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleShowConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  validateForm(): boolean {
    const newErrors: { [key: string]: string } = {};

    if (!this.formData.firstName.trim()) {
      newErrors['firstName'] = 'Le prénom est requis';
    }
    if (!this.formData.lastName.trim()) {
      newErrors['lastName'] = 'Le nom est requis';
    }
    if (!this.formData.email.trim()) {
      newErrors['email'] = 'L\'email est requis';
    } else if (!/\S+@\S+\.\S+/.test(this.formData.email)) {
      newErrors['email'] = 'Format d\'email invalide';
    }
    if (!this.formData.password) {
      newErrors['password'] = 'Le mot de passe est requis';
    } else if (this.formData.password.length < 8) {
      newErrors['password'] = 'Le mot de passe doit contenir au moins 8 caractères';
    }
    if (this.formData.password !== this.formData.confirmPassword) {
      newErrors['confirmPassword'] = 'Les mots de passe ne correspondent pas';
    }
    if (!this.formData.acceptTerms) {
      newErrors['acceptTerms'] = 'Vous devez accepter les conditions d\'utilisation';
    }

    this.errors = newErrors;
    return Object.keys(newErrors).length === 0;
  }

  handleSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    this.isLoading = true;

    const registerData = {
      name: `${this.formData.firstName} ${this.formData.lastName}`,
      email: this.formData.email,
      password: this.formData.password,
      accountType: this.formData.accountType,
      organization: this.formData.organization,
      newsletter: this.formData.newsletter,
      acceptTerms: this.formData.acceptTerms
    };

    this.auth.register(registerData).subscribe(
      (response) => {
        this.isLoading = false;
        if (response.success) {
          this.router.navigate(['/login']);
        } else {
          this.errors['general'] = response.message || 'Échec de l\'inscription. Veuillez vérifier vos données.';
        }
      },
      (error) => {
        this.isLoading = false;
        this.errors['general'] = 'Une erreur est survenue. Veuillez réessayer plus tard.';
        console.error('Erreur d\'inscription:', error); // Log pour débogage
      }
    );
  }

  registerWithGoogle(): void {
    if (this.isLoading) return;
    
    this.isLoading = true;
    this.errors = {};

    console.log('[Register] Début de l\'inscription Google');

    // Check if Google Auth is ready with timeout
    this.googleAuth.isReady().pipe(
      filter(isReady => isReady),
      take(1),
      timeout(10000), // 10 second timeout
      catchError(() => {
        console.error('[Register] Google Auth initialization timeout');
        return of(false);
      })
    ).subscribe({
      next: (isReady) => {
        if (!isReady) {
          this.isLoading = false;
          this.errors['general'] = 'Service Google non disponible. Veuillez réessayer.';
          return;
        }

        this.googleAuth.signInWithPopup()
          .then((googleResponse) => {
            console.log('[Register] Réponse Google reçue:', googleResponse);
            
            let token = null;
            let userInfo = null;
            
            // Handle different response types
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
            
            // Validate required fields for registration
            if (userInfo && (!userInfo.email || !userInfo.name)) {
              throw new Error('Informations utilisateur incomplètes reçues de Google');
            }
            
            // Prepare registration data with Google info
            const additionalData = {
              accountType: this.formData.accountType,
              organization: this.formData.organization || '',
              newsletter: this.formData.newsletter || false,
              acceptTerms: true // Required for registration
            };
            
            console.log('[Register] Données supplémentaires:', additionalData);
            
            return this.auth.registerWithGoogle(token, additionalData).toPromise();
          })
          .then((authResponse) => {
            console.log('[Register] Réponse serveur inscription:', authResponse);
            
            if (authResponse?.success) {
              console.log('[Register] Inscription Google réussie');
              
              // Show success message and redirect
              this.router.navigate(['/login'], { 
                queryParams: { 
                  message: 'Inscription réussie! Vous pouvez maintenant vous connecter.',
                  type: 'success'
                }
              });
            } else {
              // Handle server-side validation errors
              if (authResponse?.errors && typeof authResponse.errors === 'object') {
                this.errors['general'] = authResponse?.message || 'Échec de l\'inscription Google.';
              } else {
                this.errors['general'] = authResponse?.message || 'Échec de l\'inscription Google.';
              }
            }
          })
          .catch((error) => {
            console.error('[Register] Erreur inscription Google:', error);
            
            let errorMsg = 'Erreur lors de l\'inscription avec Google. Veuillez réessayer.';
            
            if (error === 'popup_closed_by_user') {
              errorMsg = 'Inscription annulée par l\'utilisateur.';
            } else if (error === 'popup_blocked_by_browser') {
              errorMsg = 'La popup a été bloquée par votre navigateur. Veuillez autoriser les popups pour ce site.';
            } else if (error?.message?.includes('blocked')) {
              errorMsg = 'Inscription bloquée. Vérifiez que les popups sont autorisées.';
            } else if (error?.message?.includes('network')) {
              errorMsg = 'Erreur de réseau. Vérifiez votre connexion internet.';
            } else if (error?.message?.includes('already exists')) {
              errorMsg = 'Un compte existe déjà avec cette adresse email Google.';
            } else if (error?.message?.includes('invalid')) {
              errorMsg = 'Token Google invalide. Veuillez réessayer.';
            }
            
            this.errors['general'] = errorMsg;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      error: () => {
        this.isLoading = false;
        this.errors['general'] = 'Service Google non disponible. Veuillez réessayer.';
      }
    });
  }
  registerWithLinkedIn(): void {
    console.log('Inscription avec LinkedIn');
    // Implémenter la logique OAuth avec LinkedIn
  }
}