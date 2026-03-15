// frontend/src/app/pages/appointments/appointments.ts
import { Component, inject, OnInit, signal } from '@angular/core';
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

  consultas = signal<any[]>([]);
  isLoading = signal(true);

  statusLabel: Record<string, string> = {
    AGENDADA: 'Agendada', CONFIRMADA_PACIENTE: 'Aguardando profissional',
    CONFIRMADA: 'Confirmada', CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada', FALTA: 'Faltou'
  };

  ngOnInit() { this.carregarConsultas(); }

  carregarConsultas() {
    this.isLoading.set(true);
    this.appointmentService.getConsultas().subscribe({
      next: (data: any) => { this.consultas.set(data.content ?? data); this.isLoading.set(false); },
      error: (err: any) => { console.error(err); this.isLoading.set(false); }
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