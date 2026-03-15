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
    const token = localStorage.getItem('token');
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role || null;
    } catch {
      return null;
    }
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }
}