// frontend/src/app/pages/documentos/documento.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DocumentoService {
  private http = inject(HttpClient);

  // Paciente
  meusDocs() {
    return this.http.get<any[]>(`${environment.apiUrl}/documentos/meus`);
  }

  // Profissional
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
    return this.http.patch(`${environment.apiUrl}/documentos/${id}/disponibilidade`, { disponivel });
  }

  excluir(id: number) {
    return this.http.delete(`${environment.apiUrl}/documentos/${id}`);
  }
}