// frontend/src/app/pages/my-availability/availability.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  private http = inject(HttpClient);

  // Profissional — minha grade do mês
  getMinhaDisponibilidade(mes: string) {
    return this.http.get<any[]>(`${environment.apiUrl}/disponibilidade/minha?mes=${mes}`);
  }

  salvarMes(mes: string, dtos: { date: string; startTimes: string[] }[]) {
    return this.http.post(`${environment.apiUrl}/disponibilidade/mensal?mes=${mes}`, dtos, {
      responseType: 'text'
    }).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  getStatusMes() {
    return this.http.get<{ diasRestantes: number; bloqueado: boolean }>(`${environment.apiUrl}/disponibilidade/status-mes`);
  }

  // Paciente — profissionais com disponibilidade
  getProfissionaisDisponiveis() {
    return this.http.get<any[]>(`${environment.apiUrl}/disponibilidade/profissionais-disponiveis`);
  }

  // Paciente — slots de um profissional em uma data
  getSlots(professionalId: number, data: string) {
    return this.http.get<any[]>(
      `${environment.apiUrl}/disponibilidade/${professionalId}/slots?data=${data}`
    );
  }

  // Paciente — dias da semana que o profissional atende
  getWorkingDays(professionalId: number) {
    return this.http.get<string[]>(
      `${environment.apiUrl}/disponibilidade/${professionalId}/working-days`
    );
  }

  private parseError(err: HttpErrorResponse): string {
    console.error('AvailabilityService parseError input:', err);
    try {
      if (err.error) {
        const body = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
        return body?.message || err.message || 'Erro desconhecido.';
      }
      return err.message || 'Erro desconhecido.';
    } catch {
      return err.error || err.message || 'Erro desconhecido.';
    }
  }
}
