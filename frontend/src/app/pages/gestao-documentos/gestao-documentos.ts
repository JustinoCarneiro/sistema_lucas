// frontend/src/app/pages/gestao-documentos/gestao-documentos.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentoService } from '../documentos/documento.service';
import { PatientService } from '../patients/patients.service';

@Component({
  selector: 'app-gestao-documentos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestao-documentos.html'
})
export class GestaoDocumentosComponent implements OnInit {
  private documentoService = inject(DocumentoService);
  private patientService = inject(PatientService);

  documentos: any[] = [];
  pacientes: any[] = [];
  isLoading = true;
  mostrarFormulario = false;

  // Formulário
  form = {
    pacienteId: '',
    tipo: '',
    titulo: '',
    conteudoTexto: '',
    nomeArquivo: '',
    arquivoBase64: '',
    disponivel: true
  };

  tiposDocumento = [
    { value: 'LAUDO_PSICOLOGICO',     label: 'Laudo Psicológico' },
    { value: 'RELATORIO_EVOLUCAO',    label: 'Relatório de Evolução' },
    { value: 'ENCAMINHAMENTO',        label: 'Encaminhamento' },
    { value: 'ATESTADO',              label: 'Atestado' },
    { value: 'AVALIACAO_PSICOLOGICA', label: 'Avaliação Psicológica' },
    { value: 'RECEITA_PRESCRICAO',    label: 'Receita / Prescrição' }
  ];

  tiposLabel: Record<string, string> = {
    LAUDO_PSICOLOGICO:    'Laudo Psicológico',
    RELATORIO_EVOLUCAO:   'Relatório de Evolução',
    ENCAMINHAMENTO:       'Encaminhamento',
    ATESTADO:             'Atestado',
    AVALIACAO_PSICOLOGICA:'Avaliação Psicológica',
    RECEITA_PRESCRICAO:   'Receita / Prescrição'
  };

  ngOnInit() {
    this.carregarDocumentos();
    this.carregarPacientes();
  }

  carregarDocumentos() {
    this.isLoading = true;
    this.documentoService.dosProfissional().subscribe({
      next: (data) => { this.documentos = data; this.isLoading = false; },
      error: () => this.isLoading = false
    });
  }

  carregarPacientes() {
    this.patientService.getPatients().subscribe({
      next: (data: any) => this.pacientes = data.content ?? data
    });
  }

  onArquivoSelecionado(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (file.type !== 'application/pdf') {
      alert('Apenas arquivos PDF são aceitos.');
      return;
    }

    this.form.nomeArquivo = file.name;
    const reader = new FileReader();
    reader.onload = () => {
      // Remove o prefixo "data:application/pdf;base64,"
      const base64 = (reader.result as string).split(',')[1];
      this.form.arquivoBase64 = base64;
    };
    reader.readAsDataURL(file);
  }

  salvar() {
    if (!this.form.pacienteId || !this.form.tipo || !this.form.titulo) {
      alert('Preencha paciente, tipo e título.');
      return;
    }
    if (!this.form.conteudoTexto && !this.form.arquivoBase64) {
      alert('Adicione um texto ou faça upload de um PDF.');
      return;
    }

    this.documentoService.criar(this.form).subscribe({
      next: () => {
        alert('Documento criado com sucesso!');
        this.mostrarFormulario = false;
        this.resetForm();
        this.carregarDocumentos();
      },
      error: (err: any) => alert('Erro: ' + (err.error?.message || 'Tente novamente.'))
    });
  }

  alterarDisponibilidade(doc: any) {
    this.documentoService.alterarDisponibilidade(doc.id, !doc.disponivel).subscribe({
      next: () => {
        doc.disponivel = !doc.disponivel;
      },
      error: () => alert('Erro ao alterar disponibilidade.')
    });
  }

  excluir(id: number) {
    if (confirm('Excluir este documento? Esta ação não pode ser desfeita.')) {
      this.documentoService.excluir(id).subscribe({
        next: () => this.carregarDocumentos(),
        error: () => alert('Erro ao excluir.')
      });
    }
  }

  resetForm() {
    this.form = {
      pacienteId: '', tipo: '', titulo: '',
      conteudoTexto: '', nomeArquivo: '',
      arquivoBase64: '', disponivel: true
    };
  }
}