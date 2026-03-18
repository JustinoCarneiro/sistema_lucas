// frontend/src/app/pages/documentos/documento.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DocumentoService {
  private http = inject(HttpClient);

  meusDocs() {
    return this.http.get<any[]>(`${environment.apiUrl}/documentos/meus`);
  }

  dosProfissional() {
    return this.http.get<any[]>(`${environment.apiUrl}/documentos/profissional`);
  }

  porPaciente(pacienteId: number) {
    return this.http.get<any[]>(`${environment.apiUrl}/documentos/paciente/${pacienteId}`);
  }

  criar(payload: any) {
    return this.http.post<any>(`${environment.apiUrl}/documentos`, payload);
  }

  alterarDisponibilidade(id: number, disponivel: boolean) {
    return this.http.patch(
      `${environment.apiUrl}/documentos/${id}/disponibilidade`,
      { disponivel },
      { responseType: 'text' } // ✅ backend retorna String
    ).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  excluir(id: number) {
    return this.http.delete(`${environment.apiUrl}/documentos/${id}`).pipe(
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