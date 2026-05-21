// frontend/src/app/security/auth.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  login(loginData: any) {
    return this.http.post<any>(`${environment.apiUrl}/auth/login`, loginData).pipe(
      catchError((error: HttpErrorResponse) => {
        const mensagem = error.error?.message || 'E-mail ou senha inválidos.';
        return throwError(() => mensagem);
      })
    );
  }

  registerPatient(patientData: any) {
    return this.http.post(`${environment.apiUrl}/auth/register`, patientData, {
      responseType: 'text' // ✅ informa que a resposta é texto, não JSON
    }).pipe(
      catchError((error: HttpErrorResponse) => {
        const mensagem = error.error?.message || error.error || 'Erro ao criar conta.';
        return throwError(() => mensagem);
      })
    );
  }

  getUserRole(): string | null {
    return localStorage.getItem('role');
  }

  isVerified(): boolean {
    const verified = localStorage.getItem('verified');
    return verified === 'true';
  }

  verifyEmail(token: string) {
    return this.http.get(`${environment.apiUrl}/auth/verify?token=${token}`, {
      responseType: 'text'
    });
  }

  refreshToken() {
    // SEC-03: Rota para rotacionar o refresh token e renovar a sessão
    return this.http.post(`${environment.apiUrl}/auth/refresh`, {}, { withCredentials: true });
  }

  logout() {
    // SEC-01: Chama o backend para invalidar o Cookie HttpOnly
    this.http.post(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => {
        this.clearLocalSession();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.clearLocalSession();
        this.router.navigate(['/login']);
      }
    });
  }

  clearLocalSession() {
    localStorage.removeItem('role');
    localStorage.removeItem('verified');
  }

  isAuthenticated(): boolean {
    // O backend validará a autenticação de fato pelo Cookie, mas o frontend
    // utiliza essa flag para esconder as rotas nas Guards
    return !!localStorage.getItem('role');
  }
}