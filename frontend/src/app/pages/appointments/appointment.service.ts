import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/appointments';

  getAppointments() {
    return this.http.get(this.apiUrl);
  }

  createAppointment(appointmentData: any) {
    return this.http.post(this.apiUrl, appointmentData);
  }

  // Busca apenas as consultas do paciente logado
  getMyAppointments() {
    // Adicionamos ?size=100 para trazer até 100 consultas de uma vez, para não termos de lidar com paginação logo no primeiro teste
    return this.http.get<any>('http://localhost:8081/appointments/me?size=100');
  }

  schedulePatientAppointment(appointmentData: any) {
    // Removido o '/me' pois o Controller usa apenas @PostMapping na raiz
    return this.http.post('http://localhost:8081/appointments', appointmentData);
  }

  // Cancela a consulta (o Backend já sabe quem é o paciente pelo Token)
  cancelPatientAppointment(appointmentId: number) {
    return this.http.delete(`http://localhost:8081/appointments/me/${appointmentId}`);
  }
}