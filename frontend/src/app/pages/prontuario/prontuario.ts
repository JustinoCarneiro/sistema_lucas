// frontend/src/app/pages/prontuario/prontuario.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-prontuario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prontuario.html'
})
export class ProntuarioComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  appointmentId: string | null = '';
  nomePaciente = 'Carregando...';
  novasNotas = '';
  historico: any[] = [];
  isLoading = true;

  ngOnInit() {
    this.appointmentId = this.route.snapshot.paramMap.get('id');
    this.carregarDados();
  }

  carregarDados() {
    this.http.get<any>(`${environment.apiUrl}/appointments/${this.appointmentId}`).subscribe({
      next: (consulta) => {
        this.nomePaciente = consulta.patientName;
        this.carregarHistorico(consulta.patientId);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao carregar consulta:', err);
        this.isLoading = false;
      }
    });
  }

  carregarHistorico(patientId: number) {
    this.http.get<any[]>(`${environment.apiUrl}/prontuarios/paciente/${patientId}`).subscribe({
      next: (data) => this.historico = data,
      error: (err) => console.error('Erro ao carregar histórico:', err)
    });
  }

  salvarProntuario() {
    if (!this.novasNotas.trim()) {
      alert('Por favor, preencha as anotações antes de salvar.');
      return;
    }

    const payload = {
      appointmentId: this.appointmentId,
      notas: this.novasNotas
    };

    this.http.post(`${environment.apiUrl}/prontuarios`, payload).subscribe({
      next: () => {
        alert('Prontuário salvo com sucesso! Atendimento finalizado.');
        this.router.navigate(['/panel/professional-appointments']);
      },
      error: (err) => {
        alert('Erro ao salvar prontuário: ' + (err.error?.message || 'Tente novamente.'));
      }
    });
  }
}