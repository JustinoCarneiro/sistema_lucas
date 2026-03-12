import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../environments/environment';
import { catchError, throwError } from 'rxjs'; // Importes necessários

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  // 1. Registo com tratamento de erro
  registerPatient(patientData: any) {
    return this.http.post(`${environment.apiUrl}/auth/register`, patientData).pipe(
      catchError(this.handleError) // Captura o erro vindo do GlobalExceptionHandler
    );
  }

  // Novo: Login com tratamento de erro
  login(loginData: any) {
    return this.http.post<any>(`${environment.apiUrl}/auth/login`, loginData).pipe(
      catchError(this.handleError)
    );
  }

  // Função privada para processar a mensagem do ExceptionDTO do Java
  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Ocorreu um erro inesperado.';

    if (error.error && error.error.message) {
      // Aqui capturamos a String "message" que definimos no ExceptionDTO do Backend
      errorMessage = error.error.message;
    }

    return throwError(() => errorMessage);
  }

  getUserRole(): string | null {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try {
      const decodedToken: any = jwtDecode(token);
      return decodedToken.role || null; 
    } catch (error) {
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