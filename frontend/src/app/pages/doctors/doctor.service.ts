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
}