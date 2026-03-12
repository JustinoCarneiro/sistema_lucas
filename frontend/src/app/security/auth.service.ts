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

  // Adicione ou substitua o método getUserRole no auth.service.ts
  getUserRole(): string | null {
    const token = localStorage.getItem('token'); // ou a forma como você guarda
    if (!token) return null;

    try {
      // O JWT tem 3 partes separadas por ponto. A payload é a segunda parte [1].
      const payloadBase64 = token.split('.')[1]; 
      
      // Descodifica de Base64 para String (JSON) e faz o parse
      const decodedJson = JSON.parse(atob(payloadBase64));
      
      // Retorna a role que acabámos de injetar no backend
      return decodedJson.role || null;
    } catch (e) {
      console.error('Erro ao ler a Role do token', e);
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