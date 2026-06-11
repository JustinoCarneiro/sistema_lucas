import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppointmentService } from './appointment.service';
import { NotificationService } from '../../notification.service';

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './appointments.html',
  styleUrl: './appointments.css'
})
export class Appointments implements OnInit {
  private appointmentService = inject(AppointmentService);
  private notify = inject(NotificationService);

  consultas = signal<any[]>([]);
  isLoading = signal(true);
  selectedItem: any = null;

  searchTerm = signal('');
  statusFilter = signal('');
  dateFilter = signal('');

  consultasFiltradas = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const status = this.statusFilter();
    const date = this.dateFilter();
    return this.consultas().filter(c => {
      const matchTerm = !term || 
        (c.patientName && c.patientName.toLowerCase().includes(term)) ||
        (c.professionalName && c.professionalName.toLowerCase().includes(term));
      const matchStatus = !status || c.status === status;
      const matchDate = !date || (c.startTime && c.startTime.startsWith(date));
      return matchTerm && matchStatus && matchDate;
    });
  });

  openDetails(item: any) {
    this.selectedItem = item;
  }

  closeDetails() {
    this.selectedItem = null;
  }

  statusLabel: Record<string, string> = {
    'AGENDADA': 'Agendada',
    'CONFIRMADA_PROFISSIONAL': 'Aguardando paciente',
    'CONFIRMADA': 'Confirmada', CONCLUIDA: 'Concluída',
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
        error: () => this.notify.error('Erro ao cancelar a consulta.')
      });
    }
  }

  labelStatus(status: string): string {
    return this.statusLabel[status] ?? status;
  }
}