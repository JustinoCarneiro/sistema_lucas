import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { jwtDecode } from 'jwt-decode';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);

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
      // O Spring Boot guarda o perfil aqui. Pode ser 'role', 'roles' ou 'authorities'.
      return decodedToken.role; 
    } catch (error) {
      console.error('Erro ao descodificar o token:', error);
      return null;
    }
  }
}