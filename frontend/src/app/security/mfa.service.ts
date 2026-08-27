// frontend/src/app/security/mfa.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MfaService {
  private http = inject(HttpClient);

  // Login em duas etapas: o cookie mfa_pending_token (HttpOnly) já foi setado por /auth/login —
  // withCredentials é global via auth.interceptor.ts.
  verify(code: string) {
    return this.http.post<any>(`${environment.apiUrl}/auth/mfa/verify`, { code }).pipe(
      catchError((error: HttpErrorResponse) => {
        const mensagem = error.error?.message || 'Código inválido.';
        return throwError(() => mensagem);
      })
    );
  }

  setup() {
    return this.http.post<{ secretBase32: string; otpAuthUri: string }>(`${environment.apiUrl}/auth/mfa/setup`, {}).pipe(
      catchError((error: HttpErrorResponse) => {
        const mensagem = error.error?.message || 'Erro ao iniciar configuração de MFA.';
        return throwError(() => mensagem);
      })
    );
  }

  enable(code: string) {
    return this.http.post<{ backupCodes: string[] }>(`${environment.apiUrl}/auth/mfa/enable`, { code }).pipe(
      catchError((error: HttpErrorResponse) => {
        const mensagem = error.error?.message || 'Código inválido.';
        return throwError(() => mensagem);
      })
    );
  }

  disable(password: string, code: string) {
    return this.http.post(`${environment.apiUrl}/auth/mfa/disable`, { password, code }).pipe(
      catchError((error: HttpErrorResponse) => {
        const mensagem = error.error?.message || 'Não foi possível desativar o MFA.';
        return throwError(() => mensagem);
      })
    );
  }
}
