import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class ProfessionalService {
  
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/professionals';

  // Busca todos os médicos
  getProfessionals() {
    return this.http.get(this.apiUrl);
  }

  // Cadastra um novo médico
  createProfessional(professionalData: any) {
    return this.http.post(this.apiUrl, professionalData);
  }

  // No seu professional.service.ts
  updateProfessional(id: number, data: any) {
    return this.http.put(`${this.apiUrl}/${id}`, data);
  }

  deleteProfessional(id: number) {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}