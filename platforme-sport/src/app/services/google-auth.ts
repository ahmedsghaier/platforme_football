import { Injectable, NgZone } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';

declare global {
  interface Window {
    google: any;
    gapi: any;
    onGoogleLibraryLoad: () => void;
  }
}

@Injectable({
  providedIn: 'root'
})
export class GoogleAuthService {
  private googleAuth: any;
  private tokenClient: any;
  private isInitialized = new BehaviorSubject<boolean>(false);
  private readonly CLIENT_ID = '750375178874-j8l8kmoi61kqqo1p4a5ui8b5sha97unf.apps.googleusercontent.com';
  private readonly SCOPES = 'email profile openid';
  private initializationAttempted = false;

  constructor(private ngZone: NgZone) {
    this.initGoogleAuth();
  }

  private async initGoogleAuth(): Promise<void> {
    if (typeof window === 'undefined' || this.initializationAttempted) return;
    this.initializationAttempted = true;

    try {
      console.log('[GoogleAuth] Starting initialization...');
      
      // Load GSI library first
      await this.loadGoogleScript();
      console.log('[GoogleAuth] GSI library loaded successfully');
      
      // Initialize Google Identity Services with better error handling
      await this.initializeGoogleIdentityServices();
      console.log('[GoogleAuth] Google Identity Services initialized');
      
      // Load and initialize GAPI as fallback
      try {
        await this.loadGapiScript();
        await this.initializeGoogleAuth2();
        console.log('[GoogleAuth] GAPI fallback initialized');
      } catch (gapiError) {
        console.warn('[GoogleAuth] GAPI fallback failed (non-critical):', gapiError);
      }
      
      this.ngZone.run(() => {
        this.isInitialized.next(true);
        console.log('[GoogleAuth] Initialization completed successfully');
      });
    } catch (error) {
      console.error('[GoogleAuth] Initialization failed:', error);
      this.ngZone.run(() => {
        this.isInitialized.next(false);
      });
    }
  }

  private loadGoogleScript(): Promise<void> {
    return new Promise((resolve, reject) => {
      // Check if already loaded
      if (window.google?.accounts) {
        resolve();
        return;
      }

      // Remove existing script if present
      const existingScript = document.querySelector('script[src*="accounts.google.com/gsi/client"]');
      if (existingScript) {
        existingScript.remove();
      }
      
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      
      let resolved = false;
      
      script.onload = () => {
        if (resolved) return;
        // Multiple checks with timeout for library readiness
        const checkLibrary = (attempts: number = 0) => {
          if (window.google?.accounts?.id && window.google?.accounts?.oauth2) {
            resolved = true;
            resolve();
          } else if (attempts < 20) { // Try for 2 seconds
            setTimeout(() => checkLibrary(attempts + 1), 100);
          } else {
            resolved = true;
            reject(new Error('Google GSI library failed to initialize properly'));
          }
        };
        checkLibrary();
      };
      
      script.onerror = () => {
        if (!resolved) {
          resolved = true;
          reject(new Error('Failed to load Google GSI script'));
        }
      };
      
      // Timeout fallback
      setTimeout(() => {
        if (!resolved) {
          resolved = true;
          reject(new Error('Google GSI script loading timeout'));
        }
      }, 10000);
      
      document.head.appendChild(script);
    });
  }

  private loadGapiScript(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (window.gapi) {
        resolve();
        return;
      }
      
      const script = document.createElement('script');
      script.src = 'https://apis.google.com/js/api.js';
      script.async = true;
      script.defer = true;
      
      script.onload = () => {
        if (window.gapi) {
          window.gapi.load('auth2', {
            callback: resolve,
            onerror: () => reject(new Error('Failed to load GAPI auth2'))
          });
        } else {
          reject(new Error('GAPI not available after script load'));
        }
      };
      
      script.onerror = () => reject(new Error('Failed to load GAPI script'));
      document.head.appendChild(script);
    });
  }

  private async initializeGoogleIdentityServices(): Promise<void> {
    if (!window.google?.accounts?.id) {
      throw new Error('Google Identity Services not available');
    }

    // Initialize with more robust configuration
    try {
      window.google.accounts.id.initialize({
        client_id: this.CLIENT_ID,
        callback: (response: any) => this.handleCredentialResponse(response),
        auto_select: false,
        cancel_on_tap_outside: true,
        ux_mode: 'popup', // Force popup mode
        context: 'signin', // Specify context
        itp_support: true, // Enable Intelligent Tracking Prevention support
      });

      // Initialize OAuth2 token client with better error handling
      if (window.google.accounts.oauth2) {
        this.tokenClient = window.google.accounts.oauth2.initTokenClient({
          client_id: this.CLIENT_ID,
          scope: this.SCOPES,
          ux_mode: 'popup',
          callback: (response: any) => {
            // This will be overridden in signInWithPopup
            console.log('[GoogleAuth] Token client callback (default):', response);
          },
          error_callback: (error: any) => {
            console.error('[GoogleAuth] Token client error:', error);
          }
        });
      }
    } catch (error) {
      console.error('[GoogleAuth] Error initializing Google services:', error);
      throw error;
    }
  }

  private async initializeGoogleAuth2(): Promise<void> {
    if (!window.gapi?.auth2) {
      throw new Error('GAPI auth2 not available');
    }

    try {
      await window.gapi.auth2.init({
        client_id: this.CLIENT_ID,
        scope: this.SCOPES,
        ux_mode: 'popup',
        plugin_name: 'platformesport' // Replace with your app name
      });

      this.googleAuth = window.gapi.auth2.getAuthInstance();
    } catch (error) {
      console.error('[GoogleAuth] GAPI auth2 initialization error:', error);
      throw error;
    }
  }

  private handleCredentialResponse(response: any): void {
    console.log('[GoogleAuth] Credential response:', response);
    // This method can be used for One Tap sign-in in the future
  }

  // Enhanced sign-in method with better error handling
  signInWithPopup(): Promise<any> {
    return new Promise(async (resolve, reject) => {
      if (!this.isInitialized.value) {
        reject(new Error('Google Auth not initialized'));
        return;
      }

      console.log('[GoogleAuth] Starting sign-in process...');

      // Method 1: Try OAuth2 token client first (most reliable for new setup)
      if (this.tokenClient) {
        try {
          const tokenResult = await this.signInWithTokenClient();
          if (tokenResult) {
            console.log('[GoogleAuth] Token client sign-in successful');
            resolve(tokenResult);
            return;
          }
        } catch (error) {
          console.warn('[GoogleAuth] Token client failed:', error);
        }
      }

      // Method 2: Try GAPI auth2 popup as fallback
      if (this.googleAuth) {
        try {
          const authResult = await this.signInWithGapiPopup();
          if (authResult) {
            console.log('[GoogleAuth] GAPI popup sign-in successful');
            resolve(authResult);
            return;
          }
        } catch (error) {
          console.warn('[GoogleAuth] GAPI popup failed:', error);
        }
      }

      // If all methods fail
      reject(new Error('All Google sign-in methods failed'));
    });
  }

  private signInWithTokenClient(): Promise<any> {
    return new Promise((resolve, reject) => {
      if (!this.tokenClient) {
        reject(new Error('Token client not available'));
        return;
      }

      console.log('[GoogleAuth] Attempting token client sign-in...');

      let resolved = false;
      const timeout = setTimeout(() => {
        if (!resolved) {
          resolved = true;
          reject(new Error('Token client timeout'));
        }
      }, 30000); // 30 second timeout

      // Override callback for this specific request
      this.tokenClient.callback = (response: any) => {
        if (resolved) return;
        clearTimeout(timeout);
        resolved = true;

        console.log('[GoogleAuth] Token client response:', response);

        if (response.error) {
          if (response.error === 'popup_closed_by_user') {
            reject('popup_closed_by_user');
          } else {
            reject(new Error(`Token client error: ${response.error}`));
          }
        } else if (response.access_token) {
          // Get user info using the access token
          this.getUserInfo(response.access_token)
            .then(userInfo => {
              resolve({
                access_token: response.access_token,
                profile: userInfo
              });
            })
            .catch(error => {
              console.error('[GoogleAuth] Failed to get user info:', error);
              reject(error);
            });
        } else {
          reject(new Error('No access token received'));
        }
      };

      try {
        this.tokenClient.requestAccessToken({
          prompt: 'select_account'
        });
      } catch (error) {
        clearTimeout(timeout);
        if (!resolved) {
          resolved = true;
          reject(error);
        }
      }
    });
  }

  private signInWithGapiPopup(): Promise<any> {
    return new Promise((resolve, reject) => {
      if (!this.googleAuth) {
        reject(new Error('GAPI auth not available'));
        return;
      }

      console.log('[GoogleAuth] Attempting GAPI popup sign-in...');

      this.googleAuth.signIn({
        scope: this.SCOPES,
        ux_mode: 'popup'
      }).then((googleUser: any) => {
        const authResponse = googleUser.getAuthResponse(true);
        const profile = googleUser.getBasicProfile();
        
        resolve({
          access_token: authResponse.access_token,
          id_token: authResponse.id_token,
          profile: {
            id: profile.getId(),
            name: profile.getName(),
            email: profile.getEmail(),
            picture: profile.getImageUrl()
          }
        });
      }).catch((error: any) => {
        console.error('[GoogleAuth] GAPI sign-in error:', error);
        if (error.error === 'popup_closed_by_user') {
          reject('popup_closed_by_user');
        } else {
          reject(error);
        }
      });
    });
  }

  private async getUserInfo(accessToken: string): Promise<any> {
    try {
      const response = await fetch(`https://www.googleapis.com/oauth2/v2/userinfo?access_token=${accessToken}`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const userInfo = await response.json();
      console.log('[GoogleAuth] User info retrieved:', userInfo);
      return userInfo;
    } catch (error) {
      console.error('[GoogleAuth] Error fetching user info:', error);
      throw error;
    }
  }

  // Method to decode JWT token
  parseJwt(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      if (!base64Url) {
        throw new Error('Invalid JWT format');
      }
      
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      
      const parsed = JSON.parse(jsonPayload);
      console.log('[GoogleAuth] JWT parsed successfully:', parsed);
      return parsed;
    } catch (error) {
      console.error('[GoogleAuth] Error parsing JWT:', error);
      return null;
    }
  }

  // Sign out method
  signOut(): Promise<void> {
    return new Promise((resolve) => {
      const promises: Promise<any>[] = [];

      if (this.googleAuth?.isSignedIn?.get()) {
        promises.push(this.googleAuth.signOut());
      }

      if (window.google?.accounts?.id) {
        try {
          window.google.accounts.id.disableAutoSelect();
        } catch (error) {
          console.warn('[GoogleAuth] Error disabling auto-select:', error);
        }
      }

      Promise.all(promises)
        .then(() => {
          console.log('[GoogleAuth] Sign out completed');
          resolve();
        })
        .catch((error) => {
          console.warn('[GoogleAuth] Sign out error (non-critical):', error);
          resolve(); // Resolve anyway since local cleanup is more important
        });
    });
  }

  // Check if user is signed in
  isSignedIn(): boolean {
    return this.googleAuth ? this.googleAuth.isSignedIn.get() : false;
  }

  // Get current user
  getCurrentUser(): any {
    return this.googleAuth ? this.googleAuth.currentUser.get() : null;
  }

  isReady(): Observable<boolean> {
    return this.isInitialized.asObservable();
  }

  // Method to manually retry initialization
  retryInitialization(): void {
    console.log('[GoogleAuth] Retrying initialization...');
    this.initializationAttempted = false;
    this.isInitialized.next(false);
    this.initGoogleAuth();
  }
}