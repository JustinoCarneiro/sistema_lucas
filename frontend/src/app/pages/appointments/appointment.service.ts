// frontend/src/app/pages/appointments/appointment.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private http = inject(HttpClient);

  // Admin — leitura e cancelamento
  getConsultas() {
    return this.http.get(`${environment.apiUrl}/consultas`);
  }

  cancelarConsulta(id: number) {
    return this.http.delete(`${environment.apiUrl}/consultas/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  // Paciente — agendamento e consultas próprias
  agendarConsulta(data: any) {
    return this.http.post(`${environment.apiUrl}/consultas`, data, {
      responseType: 'text' // ✅ backend retorna String
    }).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  minhasConsultas() {
    return this.http.get(`${environment.apiUrl}/consultas/minhas`);
  }

  cancelarMinhaConsulta(id: number) {
    return this.http.delete(`${environment.apiUrl}/consultas/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  // Confirmações
  confirmarPaciente(id: number) {
    return this.http.patch(
      `${environment.apiUrl}/consultas/${id}/confirmar-paciente`, {},
      { responseType: 'text' }
    ).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  confirmarProfissional(id: number) {
    return this.http.patch(
      `${environment.apiUrl}/consultas/${id}/confirmar-profissional`, {},
      { responseType: 'text' }
    ).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  private parseError(err: HttpErrorResponse): string {
    try {
      const body = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
      return body?.message || 'Erro desconhecido.';
    } catch {
      return err.error || 'Erro desconhecido.';
    }
  }
}