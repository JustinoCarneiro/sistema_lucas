import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);

  getAdminDashboard() {
    return this.http.get<any>(`${environment.apiUrl}/dashboard/admin`);
  }

  getProfissionalDashboard() {
    return this.http.get<any>(`${environment.apiUrl}/dashboard/profissional`);
  }

  getPacienteDashboard() {
    return this.http.get<any>(`${environment.apiUrl}/dashboard/paciente`);
  }
}