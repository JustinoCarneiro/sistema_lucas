import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; // Adicionado para o logout
import { jwtDecode } from 'jwt-decode';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  // 1. Método para Registar um novo Paciente (Card 13)
  registerPatient(patientData: any) {
    return this.http.post('http://localhost:8081/auth/register', patientData);
  }

  // 2. Método para descobrir quem está logado (Card 14)
  getUserRole(): string | null {
    const token = localStorage.getItem('token');
    if (!token) return null;

    try {
      const decodedToken: any = jwtDecode(token);
      // Nota: No Spring Boot com JWT padrão, a role costuma vir em 'role' ou 'sub'.
      // Verifica no teu console do navegador se 'decodedToken.role' existe.
      return decodedToken.role || null; 
    } catch (error) {
      console.error('Erro ao descodificar o token:', error);
      return null;
    }
  }

  // 3. Método para fazer logout (Resolve o erro do PanelComponent)
  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  // 4. Verificação extra para segurança das rotas
  isAuthenticated(): boolean {
    const token = localStorage.getItem('token');
    return !!token; // Retorna true se houver token
  }
}