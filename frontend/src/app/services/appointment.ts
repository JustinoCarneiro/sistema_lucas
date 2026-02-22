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
}