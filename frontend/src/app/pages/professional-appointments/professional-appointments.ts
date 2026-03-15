// frontend/src/app/pages/professional-appointments/professional-appointments.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-professional-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './professional-appointments.html'
})
export class ProfessionalAppointmentsComponent implements OnInit {
  private http = inject(HttpClient);
  private router = inject(Router);

  appointments = signal<any[]>([]);
  isLoading = signal(true);
  today = new Date();

  statusLabel: Record<string, string> = {
    AGENDADA: 'Agendada', CONFIRMADA_PACIENTE: 'Confirmada pelo paciente',
    CONFIRMADA: 'Confirmada', CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada', FALTA: 'Faltou'
  };

  statusClass: Record<string, string> = {
    AGENDADA: 'bg-blue-100 text-blue-700', CONFIRMADA_PACIENTE: 'bg-yellow-100 text-yellow-700',
    CONFIRMADA: 'bg-green-100 text-green-700', CONCLUIDA: 'bg-gray-100 text-gray-600',
    CANCELADA: 'bg-red-100 text-red-700', FALTA: 'bg-orange-100 text-orange-700'
  };

  ngOnInit() { this.fetchTodayAppointments(); }

  fetchTodayAppointments() {
    this.isLoading.set(true);
    this.http.get<any[]>(`${environment.apiUrl}/consultas/profissional/hoje`).subscribe({
      next: (data) => { this.appointments.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  iniciarAtendimento(app: any) { this.router.navigate(['/panel/prontuario', app.id]); }

  confirmarConsulta(app: any) {
    if (confirm(`Confirmar a consulta de ${app.patientName}?`)) {
      this.http.patch(`${environment.apiUrl}/consultas/${app.id}/confirmar-profissional`, {}).subscribe({
        next: () => { alert('Consulta confirmada!'); this.fetchTodayAppointments(); },
        error: (err: any) => alert('Erro: ' + (err.error?.message || 'Não foi possível confirmar.'))
      });
    }
  }

  marcarFalta(app: any) {
    if (confirm(`Confirmar que ${app.patientName} não compareceu?`)) {
      this.http.patch(`${environment.apiUrl}/consultas/${app.id}/falta`, {}).subscribe({
        next: () => { alert('Paciente marcado como faltante.'); this.fetchTodayAppointments(); },
        error: () => alert('Erro ao registrar falta.')
      });
    }
  }
}