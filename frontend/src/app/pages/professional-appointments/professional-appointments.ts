// frontend/src/app/pages/professional-appointments/professional-appointments.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AppointmentService } from '../appointments/appointment.service';

@Component({
  selector: 'app-professional-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './professional-appointments.html'
})
export class ProfessionalAppointmentsComponent implements OnInit {
  private http = inject(HttpClient);
  private router = inject(Router);
  private appointmentService = inject(AppointmentService);

  abaAtiva = signal<'hoje' | 'proximas'>('hoje');
  consultasHoje = signal<any[]>([]);
  proximasConsultas = signal<any[]>([]);
  isLoadingHoje = signal(true);
  isLoadingProximas = signal(true);
  today = new Date();

  statusLabel: Record<string, string> = {
    AGUARDANDO_CONFIRMACAO:  'Aguardando Confirmação',
    AGENDADA:                'Agendada',
    CONFIRMADA_PROFISSIONAL: 'Aguardando paciente',
    CONFIRMADA:              'Confirmada',
    CONCLUIDA:               'Concluída',
    CANCELADA:               'Cancelada',
    FALTA:                   'Faltou'
  };

  statusClass: Record<string, string> = {
    AGUARDANDO_CONFIRMACAO:  'bg-purple-100 text-purple-700 border border-purple-200',
    AGENDADA:                'bg-blue-100 text-blue-700',
    CONFIRMADA_PROFISSIONAL: 'bg-yellow-100 text-yellow-700',
    CONFIRMADA:              'bg-green-100 text-green-700',
    CONCLUIDA:               'bg-gray-100 text-gray-600',
    CANCELADA:               'bg-red-100 text-red-700',
    FALTA:                   'bg-orange-100 text-orange-700'
  };

  ngOnInit() {
    this.carregarHoje();
    this.carregarProximas();
  }

  carregarHoje() {
    this.isLoadingHoje.set(true);
    this.http.get<any[]>(`${environment.apiUrl}/consultas/profissional/hoje`).subscribe({
      next: (data) => { this.consultasHoje.set(data); this.isLoadingHoje.set(false); },
      error: () => this.isLoadingHoje.set(false)
    });
  }

  carregarProximas() {
    this.isLoadingProximas.set(true);
    this.http.get<any[]>(`${environment.apiUrl}/consultas/profissional/todas`).subscribe({
      next: (data) => {
        // Filtra apenas futuras ativas (incluindo as confirmadas e canceladas)
        const futuras = data.filter((c: any) =>
          new Date(c.startTime) > new Date() &&
          (c.status === 'AGUARDANDO_CONFIRMACAO' || c.status === 'AGENDADA' ||
           c.status === 'CONFIRMADA_PROFISSIONAL' || c.status === 'CONFIRMADA' ||
           c.status === 'CANCELADA')
        );
        this.proximasConsultas.set(futuras);
        this.isLoadingProximas.set(false);
      },
      error: () => this.isLoadingProximas.set(false)
    });
  }

  iniciarAtendimento(app: any) {
    this.router.navigate(['/panel/medical-record', app.id]);
  }

  confirmarConsulta(app: any) {
    if (confirm(`Confirmar a consulta de ${app.patientName}?`)) {
      this.http.patch(
        `${environment.apiUrl}/consultas/${app.id}/confirmar-profissional`, {},
        { responseType: 'text' }
      ).subscribe({
        next: () => {
          alert('Consulta confirmada!');
          this.carregarHoje();
          this.carregarProximas();
        },
        error: (msg: string) => alert('Erro: ' + msg)
      });
    }
  }

  aprovar(app: any) {
    if (confirm(`Aprovar a consulta solicitada por ${app.patientName}?`)) {
      this.appointmentService.aprovarAgendamento(app.id).subscribe({
        next: () => {
          alert('Agendamento confirmado!');
          this.carregarHoje();
          this.carregarProximas();
        },
        error: (msg: string) => alert('Erro ao aprovar: ' + msg)
      });
    }
  }

  recusar(app: any) {
    const motivo = prompt(`Justificativa para recusar a consulta de ${app.patientName}:`);
    if (motivo !== null) {
      const justificativa = motivo.trim() || 'Indisponibilidade de agenda do profissional.';
      this.appointmentService.recusarAgendamento(app.id, justificativa).subscribe({
        next: () => {
          alert('Agendamento recusado com sucesso.');
          // Optimistic update: mark as CANCELADA immediately in both lists
          this.proximasConsultas.update(list =>
            list.map(c => c.id === app.id ? { ...c, status: 'CANCELADA' } : c)
          );
          this.consultasHoje.update(list =>
            list.map(c => c.id === app.id ? { ...c, status: 'CANCELADA' } : c)
          );
          // Reload only today's list — reloading proximasConsultas would overwrite
          // the optimistic update if the server filter excludes CANCELADA status
          this.carregarHoje();
        },
        error: (msg: string) => alert('Erro ao recusar: ' + msg)
      });
    }
  }

  marcarFalta(app: any) {
    if (confirm(`Confirmar que ${app.patientName} não compareceu?`)) {
      this.http.patch(
        `${environment.apiUrl}/consultas/${app.id}/falta`, {},
        { responseType: 'text' }
      ).subscribe({
        next: () => {
          alert('Paciente marcado como faltante.');
          this.carregarHoje();
          this.carregarProximas();
        },
        error: () => alert('Erro ao registrar falta.')
      });
    }
  }
}