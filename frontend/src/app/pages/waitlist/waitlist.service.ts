// frontend/src/app/pages/waitlist/waitlist.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class WaitlistService {
  private http = inject(HttpClient);

  entrarNaFila(professionalId: number, dateTime: string) {
    return this.http.post(`${environment.apiUrl}/waitlist`, { professionalId, dateTime }, {
      responseType: 'text'
    }).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  minhasEntradas() {
    return this.http.get(`${environment.apiUrl}/waitlist/minhas`);
  }

  sairDaFila(id: number) {
    return this.http.delete(`${environment.apiUrl}/waitlist/${id}`).pipe(
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
