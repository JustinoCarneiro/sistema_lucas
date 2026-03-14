// frontend/src/app/pages/appointments/appointments.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppointmentService } from './appointment.service';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './appointments.html',
  styleUrl: './appointments.css'
})
export class Appointments implements OnInit {
  private appointmentService = inject(AppointmentService);

  consultas: any[] = [];
  isLoading = true;

  statusLabel: Record<string, string> = {
    AGENDADA:            'Agendada',
    CONFIRMADA_PACIENTE: 'Aguardando profissional',
    CONFIRMADA:          'Confirmada',
    CONCLUIDA:           'Concluída',
    CANCELADA:           'Cancelada',
    FALTA:               'Faltou'
  };

  ngOnInit() {
    this.carregarConsultas();
  }

  carregarConsultas() {
    this.isLoading = true;
    this.appointmentService.getConsultas().subscribe({
      next: (data: any) => {
        this.consultas = data.content ?? data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Erro ao buscar consultas', err);
        this.isLoading = false;
      }
    });
  }

  cancelar(id: number) {
    if (confirm('Confirmar cancelamento desta consulta?')) {
      this.appointmentService.cancelarConsulta(id).subscribe({
        next: () => this.carregarConsultas(),
        error: () => alert('Erro ao cancelar a consulta.')
      });
    }
  }

  labelStatus(status: string): string {
    return this.statusLabel[status] ?? status;
  }
}