// error.interceptor.ts
import { Injectable } from '@angular/core';
import { 
  HttpInterceptor, 
  HttpRequest, 
  HttpHandler, 
  HttpEvent, 
  HttpErrorResponse,
  HttpResponse
} from '@angular/common/http';
import { Observable, throwError, timer, of } from 'rxjs';
import { catchError, retry, retryWhen, concatMap, delay } from 'rxjs/operators';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      // Retry logic pour les erreurs temporaires
      retryWhen(errors => 
        errors.pipe(
          concatMap((error, count) => {
            // Retry seulement pour certaines erreurs et un nombre limité de fois
            if (count < 2 && this.shouldRetry(error)) {
              console.log(`Tentative de retry ${count + 1} pour ${req.url}`);
              return timer(this.getRetryDelay(count));
            }
            return throwError(error);
          })
        )
      ),
      catchError((error: HttpErrorResponse) => {
        console.error('Erreur HTTP interceptée:', {
          url: req.url,
          status: error.status,
          statusText: error.statusText,
          message: error.message
        });

        // Transformation de l'erreur pour un message plus user-friendly
        const transformedError = this.transformError(error);
        return throwError(transformedError);
      })
    );
  }

  /**
   * Détermine si une erreur mérite un retry
   */
  private shouldRetry(error: HttpErrorResponse): boolean {
    // Retry pour les erreurs de réseau et les erreurs serveur temporaires
    return error.status === 0 || // Erreur de réseau
           error.status === 408 || // Request Timeout
           error.status === 429 || // Too Many Requests
           error.status === 502 || // Bad Gateway
           error.status === 503 || // Service Unavailable
           error.status === 504;   // Gateway Timeout
  }

  /**
   * Calcule le délai de retry avec backoff exponentiel
   */
  private getRetryDelay(retryCount: number): number {
    return Math.min(1000 * Math.pow(2, retryCount), 5000);
  }

  /**
   * Transforme l'erreur HTTP en erreur plus explicite
   */
  private transformError(error: HttpErrorResponse): HttpErrorResponse {
    let message = error.message;

    switch (error.status) {
      case 0:
        message = 'Impossible de joindre le serveur. Vérifiez votre connexion réseau.';
        break;
      case 400:
        message = 'Requête invalide. Vérifiez les données envoyées.';
        break;
      case 401:
        message = 'Accès non autorisé. Authentification requise.';
        break;
      case 403:
        message = 'Accès interdit. Permissions insuffisantes.';
        break;
      case 404:
        message = 'Ressource non trouvée.';
        break;
      case 408:
        message = 'Délai d\'attente dépassé. Veuillez réessayer.';
        break;
      case 429:
        message = 'Trop de requêtes. Veuillez patienter avant de réessayer.';
        break;
      case 500:
        message = 'Erreur interne du serveur. Veuillez réessayer plus tard.';
        break;
      case 502:
        message = 'Passerelle défaillante. Le serveur est temporairement indisponible.';
        break;
      case 503:
        message = 'Service temporairement indisponible. Maintenance en cours.';
        break;
      case 504:
        message = 'Délai d\'attente de la passerelle dépassé.';
        break;
      default:
        message = `Erreur serveur (${error.status}): ${error.statusText}`;
    }

    // Créer une nouvelle erreur avec le message transformé
const httpError = new HttpErrorResponse({
  error: error.error,
  headers: error.headers,
  status: error.status,
  statusText: error.statusText ?? undefined,
  url: error.url ?? undefined
});
(httpError as any).message = message; // forcé si besoin
return httpError;
  }

}