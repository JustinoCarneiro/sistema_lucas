// frontend/src/app/pages/appointments/appointment.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private http = inject(HttpClient);

  // Admin — leitura e cancelamento
  getConsultas() {
    return this.http.get(`${environment.apiUrl}/consultas`);
  }

  cancelarConsulta(id: number) {
    return this.http.delete(`${environment.apiUrl}/consultas/${id}`);
  }

  // Paciente — agendamento e consultas próprias
  agendarConsulta(data: any) {
    return this.http.post(`${environment.apiUrl}/consultas`, data);
  }

  minhasConsultas() {
    return this.http.get(`${environment.apiUrl}/consultas/minhas`);
  }

  cancelarMinhaConsulta(id: number) {
    return this.http.delete(`${environment.apiUrl}/consultas/${id}`);
  }

  // Confirmações
  confirmarPaciente(id: number) {
    return this.http.patch(`${environment.apiUrl}/consultas/${id}/confirmar-paciente`, {});
  }

  confirmarProfissional(id: number) {
    return this.http.patch(`${environment.apiUrl}/consultas/${id}/confirmar-profissional`, {});
  }
}