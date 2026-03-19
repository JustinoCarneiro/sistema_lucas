// frontend/src/app/pages/export/export.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ExportService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/export`;

  exportAdmin() {
    this.downloadFile(`${this.apiUrl}/admin`, 'relatorio_admin.csv');
  }

  exportProfessional() {
    this.downloadFile(`${this.apiUrl}/professional`, 'meus_atendimentos.csv');
  }

  exportPatient() {
    this.downloadFile(`${this.apiUrl}/patient`, 'meus_dados_clinicos.csv');
  }

  exportPatients() {
    this.downloadFile(`${this.apiUrl}/patients`, 'lista_pacientes.csv');
  }

  exportProfessionals() {
    this.downloadFile(`${this.apiUrl}/professionals`, 'lista_profissionais.csv');
  }

  private downloadFile(url: string, filename: string) {
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = filename;
        link.click();
        window.URL.revokeObjectURL(link.href);
      },
      error: (err) => {
        console.error('Erro ao exportar:', err);
        alert('Não foi possível gerar a exportação no momento.');
      }
    });
  }
}
