import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class DoctorService {
  
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/doctors';

  // Busca todos os médicos
  getDoctors() {
    return this.http.get(this.apiUrl);
  }

  // Cadastra um novo médico
  createDoctor(doctorData: any) {
    return this.http.post(this.apiUrl, doctorData);
  }

  // No seu doctor.service.ts
  updateDoctor(id: number, data: any) {
    return this.http.put(`${this.apiUrl}/${id}`, data);
  }

  deleteDoctor(id: number) {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}