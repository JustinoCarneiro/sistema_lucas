// frontend/src/app/pages/professional-appointments/professional-appointments.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AppointmentService } from '../appointments/appointment.service';
import { NotificationService } from '../../notification.service';

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
  private notify = inject(NotificationService);

  abaAtiva = signal<'hoje' | 'proximas' | 'atrasadas'>('hoje');
  consultasHoje = signal<any[]>([]);
  proximasConsultas = signal<any[]>([]);
  consultasAtrasadas = signal<any[]>([]);
  isLoadingHoje = signal(true);
  isLoadingProximas = signal(true);
  isLoadingAtrasadas = signal(false);
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
    AGUARDANDO_CONFIRMACAO:  'bg-purple-100 text-purple-700 border border-purple-200 dark:bg-purple-900/40 dark:text-purple-300 dark:border-purple-700',
    AGENDADA:                'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',
    CONFIRMADA_PROFISSIONAL: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300',
    CONFIRMADA:              'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',
    CONCLUIDA:               'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
    CANCELADA:               'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
    FALTA:                   'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-300'
  };

  ngOnInit() {
    this.carregarHoje();
    this.carregarProximas();
    this.carregarAtrasadas();
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

  carregarAtrasadas() {
    this.isLoadingAtrasadas.set(true);
    this.http.get<any[]>(`${environment.apiUrl}/consultas/profissional/atrasadas`).subscribe({
      next: (data) => { this.consultasAtrasadas.set(data); this.isLoadingAtrasadas.set(false); },
      error: () => this.isLoadingAtrasadas.set(false)
    });
  }

  iniciarAtendimento(app: any) {
    this.router.navigate(['/panel/medical-record', app.id]);
    this.carregarAtrasadas();
  }

  confirmarConsulta(app: any) {
    if (confirm(`Confirmar a consulta de ${app.patientName}?`)) {
      this.http.patch(
        `${environment.apiUrl}/consultas/${app.id}/confirmar-profissional`, {},
        { responseType: 'text' }
      ).subscribe({
        next: () => {
          this.notify.success('Consulta confirmada!');
          this.carregarHoje();
          this.carregarProximas();
        },
        error: (msg: string) => this.notify.error(msg)
      });
    }
  }

  aprovar(app: any) {
    if (confirm(`Aprovar a consulta solicitada por ${app.patientName}?`)) {
      this.appointmentService.aprovarAgendamento(app.id).subscribe({
        next: () => {
          this.notify.success('Agendamento confirmado!');
          this.carregarHoje();
          this.carregarProximas();
        },
        error: (msg: string) => this.notify.error('Erro ao aprovar: ' + msg)
      });
    }
  }

  recusar(app: any) {
    const motivo = prompt(`Justificativa para recusar a consulta de ${app.patientName}:`);
    if (motivo !== null) {
      const justificativa = motivo.trim();
      if (!justificativa) {
        this.notify.error('A justificativa é obrigatória para recusar a consulta.');
        return;
      }
      this.appointmentService.recusarAgendamento(app.id, justificativa).subscribe({
        next: () => {
          this.notify.success('Agendamento recusado com sucesso.');
          this.proximasConsultas.update(list =>
            list.map(c => c.id === app.id ? { ...c, status: 'CANCELADA' } : c)
          );
          this.consultasHoje.update(list =>
            list.map(c => c.id === app.id ? { ...c, status: 'CANCELADA' } : c)
          );
          this.carregarHoje();
        },
        error: (msg: string) => this.notify.error('Erro ao recusar: ' + msg)
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
          this.notify.success('Paciente marcado como faltante.');
          this.carregarHoje();
          this.carregarProximas();
          this.carregarAtrasadas();
        },
        error: () => this.notify.error('Erro ao registrar falta.')
      });
    }
  }

  cancelarAtrasada(app: any) {
    const motivo = prompt(`Justificativa para cancelar a consulta de ${app.patientName}:`);
    if (motivo !== null) {
      const justificativa = motivo.trim();
      if (!justificativa) {
        this.notify.error('A justificativa é obrigatória para cancelar a consulta.');
        return;
      }
      this.http.post(
        `${environment.apiUrl}/consultas/${app.id}/cancelar`,
        { justification: justificativa },
        { responseType: 'text' }
      ).subscribe({
        next: () => {
          this.notify.success('Consulta cancelada.');
          this.carregarAtrasadas();
        },
        error: () => this.notify.error('Erro ao cancelar a consulta.')
      });
    }
  }
}
