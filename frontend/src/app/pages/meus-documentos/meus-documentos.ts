// frontend/src/app/pages/meus-documentos/meus-documentos.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentoService } from '../documentos/documento.service';

@Component({
  selector: 'app-meus-documentos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './meus-documentos.html'
})
export class MeusDocumentosComponent implements OnInit {
  private documentoService = inject(DocumentoService);

  documentos = signal<any[]>([]);
  isLoading = signal(true);

  tiposLabel: Record<string, string> = {
    LAUDO_PSICOLOGICO: 'Laudo Psicológico', RELATORIO_EVOLUCAO: 'Relatório de Evolução',
    ENCAMINHAMENTO: 'Encaminhamento', ATESTADO: 'Atestado',
    AVALIACAO_PSICOLOGICA: 'Avaliação Psicológica', RECEITA_PRESCRICAO: 'Receita / Prescrição'
  };

  ngOnInit() {
    this.documentoService.meusDocs().subscribe({
      next: (data) => { this.documentos.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  baixarPdf(doc: any) {
    if (!doc.arquivoBase64) return;
    try {
      const cleanBase64 = doc.arquivoBase64.replace(/\s/g, '');
      const byteCharacters = atob(cleanBase64);
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);
      const blob = new Blob([byteArray], { type: 'application/pdf' });
      const fileURL = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = fileURL;
      link.download = doc.nomeArquivo || 'documento.pdf';
      link.click();
      URL.revokeObjectURL(fileURL);
    } catch (e) {
      alert('Erro ao tentar baixar arquivo PDF.');
    }
  }
}