// frontend/src/app/pages/medical-record/medical-record.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { NotificationService } from '../../notification.service';

@Component({
  selector: 'app-medical-record',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './medical-record.html'
})
export class MedicalRecordComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private notify = inject(NotificationService);

  appointmentId: string | null = '';
  nomePaciente = signal('Carregando...');
  novasNotas = '';
  historico = signal<any[]>([]);
  isLoading = signal(true);

  ngOnInit() {
    this.appointmentId = this.route.snapshot.paramMap.get('id');
    this.carregarDados();
  }

  carregarDados() {
    this.http.get<any>(`${environment.apiUrl}/consultas/${this.appointmentId}`).subscribe({
      next: (consulta) => {
        this.nomePaciente.set(consulta.patientName);
        this.carregarHistorico(consulta.patientId);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  carregarHistorico(patientId: number) {
    this.http.get<any[]>(`${environment.apiUrl}/prontuarios/paciente/${patientId}`).subscribe({
      next: (data) => this.historico.set(data),
      error: (err) => console.error('Erro ao carregar histórico:', err)
    });
  }

  salvarProntuario() {
    if (!this.novasNotas.trim()) {
      this.notify.error('Por favor, preencha as anotações antes de salvar.');
      return;
    }
    const payload = { appointmentId: this.appointmentId, notas: this.novasNotas };
    this.http.post(`${environment.apiUrl}/prontuarios`, payload).subscribe({
      next: () => {
        this.notify.success('Prontuário salvo com sucesso! Atendimento finalizado.');
        this.router.navigate(['/panel/professional-appointments']);
      },
      error: (err: any) => this.notify.error('Erro ao salvar prontuário: ' + (err.error?.message || 'Tente novamente.'))
    });
  }
}