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
    const link = document.createElement('a');
    link.href = 'data:application/pdf;base64,' + doc.arquivoBase64;
    link.download = doc.nomeArquivo || 'documento.pdf';
    link.click();
  }
}