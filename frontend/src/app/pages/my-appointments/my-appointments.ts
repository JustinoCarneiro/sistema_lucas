// frontend/src/app/pages/my-appointments/my-appointments.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppointmentService } from '../appointments/appointment.service';
import { ProfessionalService } from '../professionals/professionals.service';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.css'
})
export class MyAppointmentsComponent implements OnInit {
  private appointmentService = inject(AppointmentService);
  private professionalService = inject(ProfessionalService);
  private fb = inject(FormBuilder);

  myAppointments = signal<any[]>([]);
  professionals = signal<any[]>([]);
  isLoading = signal(true);
  isScheduling = signal(false);
  scheduleForm: FormGroup;

  statusLabel: Record<string, string> = {
    AGENDADA: 'Agendada', CONFIRMADA_PACIENTE: 'Aguardando profissional',
    CONFIRMADA: 'Confirmada', CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada', FALTA: 'Faltou'
  };

  statusClass: Record<string, string> = {
    AGENDADA: 'bg-blue-100 text-blue-700', CONFIRMADA_PACIENTE: 'bg-yellow-100 text-yellow-700',
    CONFIRMADA: 'bg-green-100 text-green-700', CONCLUIDA: 'bg-gray-100 text-gray-600',
    CANCELADA: 'bg-red-100 text-red-700', FALTA: 'bg-orange-100 text-orange-700'
  };

  constructor() {
    this.scheduleForm = this.fb.group({
      professionalId: ['', Validators.required],
      startTime: ['', Validators.required],
      reason: ['']
    });
  }

  ngOnInit() { this.loadAppointments(); this.loadProfessionals(); }

  loadAppointments() {
    this.isLoading.set(true);
    this.appointmentService.minhasConsultas().subscribe({
      next: (r: any) => { this.myAppointments.set(r.content ?? r ?? []); this.isLoading.set(false); },
      error: () => { this.myAppointments.set([]); this.isLoading.set(false); }
    });
  }

  loadProfessionals() {
    this.professionalService.getProfessionals().subscribe({
      next: (r: any) => this.professionals.set(r.content ?? r ?? [])
    });
  }

  confirmar(id: number) {
    this.appointmentService.confirmarPaciente(id).subscribe({
      next: () => { alert('Presença confirmada!'); this.loadAppointments(); },
      error: (err: any) => alert('Erro: ' + (err.error?.message || 'Não foi possível confirmar.'))
    });
  }

  cancelar(id: number) {
    if (confirm('Confirmar cancelamento desta consulta?')) {
      this.appointmentService.cancelarMinhaConsulta(id).subscribe({
        next: () => this.loadAppointments(),
        error: (err: any) => alert('Erro: ' + (err.error?.message || 'Não foi possível cancelar.'))
      });
    }
  }

  onSubmitSchedule() {
    if (this.scheduleForm.valid) {
      const payload = {
        professionalId: this.scheduleForm.value.professionalId,
        dateTime: this.scheduleForm.value.startTime + ':00',
        reason: this.scheduleForm.value.reason
      };
      this.appointmentService.agendarConsulta(payload).subscribe({
        next: () => { alert('Consulta agendada com sucesso!'); this.isScheduling.set(false); this.scheduleForm.reset(); this.loadAppointments(); },
        error: (err: any) => alert('Erro: ' + (err.error?.message || 'Verifique os dados.'))
      });
    }
  }
}