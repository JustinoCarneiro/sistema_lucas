// frontend/src/app/pages/professionals/professionals.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProfessionalService {
  private http = inject(HttpClient);

  getProfessionals() {
    return this.http.get(`${environment.apiUrl}/professionals`);
  }

  createProfessional(data: any) {
    return this.http.post(`${environment.apiUrl}/professionals`, data, {
      responseType: 'text'
    }).pipe(
      catchError((err: HttpErrorResponse) => {
        // ✅ erro vem como texto JSON — precisa fazer parse manual
        const body = this.parseError(err);
        return throwError(() => body);
      })
    );
  }

  updateProfessional(id: number, data: any) {
    return this.http.put(`${environment.apiUrl}/professionals/${id}`, data, {
      responseType: 'text'
    }).pipe(
      catchError((err: HttpErrorResponse) => {
        const body = this.parseError(err);
        return throwError(() => body);
      })
    );
  }

  deleteProfessional(id: number) {
    return this.http.delete(`${environment.apiUrl}/professionals/force/${id}`).pipe(
      catchError((err: HttpErrorResponse) => {
        const body = this.parseError(err);
        return throwError(() => body);
      })
    );
  }

  // ✅ tenta parsear o erro como JSON, fallback para texto simples
  private parseError(err: HttpErrorResponse): string {
    try {
      const body = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
      return body?.message || 'Erro desconhecido.';
    } catch {
      return err.error || 'Erro desconhecido.';
    }
  }
}