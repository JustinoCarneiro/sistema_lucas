// frontend/src/app/pages/my-appointments/my-appointments.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppointmentService } from '../appointments/appointment.service';
import { AvailabilityService } from '../my-availability/availability.service';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.css'
})
export class MyAppointmentsComponent implements OnInit {
  private appointmentService = inject(AppointmentService);
  private availabilityService = inject(AvailabilityService);
  private fb = inject(FormBuilder);

  myAppointments = signal<any[]>([]);
  professionals = signal<any[]>([]);
  availableSlots = signal<any[]>([]);
  isLoading = signal(true);
  isScheduling = signal(false);
  isLoadingSlots = signal(false);

  selectedProfessional = signal<any>(null);
  selectedDate = signal('');
  selectedSlot = signal<any>(null);

  scheduleForm: FormGroup;

  statusLabel: Record<string, string> = {
    AGENDADA: 'Agendada', CONFIRMADA_PROFISSIONAL: 'Aguardando paciente',
    CONFIRMADA: 'Confirmada', CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada', FALTA: 'Faltou'
  };

  statusClass: Record<string, string> = {
    AGENDADA: 'bg-blue-100 text-blue-700', CONFIRMADA_PROFISSIONAL: 'bg-yellow-100 text-yellow-700',
    CONFIRMADA: 'bg-green-100 text-green-700', CONCLUIDA: 'bg-gray-100 text-gray-600',
    CANCELADA: 'bg-red-100 text-red-700', FALTA: 'bg-orange-100 text-orange-700'
  };

  constructor() {
    this.scheduleForm = this.fb.group({
      professionalId: ['', Validators.required],
      date: ['', Validators.required],
      slot: ['', Validators.required],
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
    // Carrega apenas profissionais com disponibilidade configurada
    this.availabilityService.getProfissionaisDisponiveis().subscribe({
      next: (r: any) => this.professionals.set(r ?? [])
    });
  }

  onProfessionalChange() {
    const profId = this.scheduleForm.value.professionalId;
    const prof = this.professionals().find((p: any) => p.id == profId);
    this.selectedProfessional.set(prof || null);
    this.selectedDate.set('');
    this.selectedSlot.set(null);
    this.availableSlots.set([]);
    this.scheduleForm.patchValue({ date: '', slot: '' });
  }

  onDateChange() {
    const date = this.scheduleForm.value.date;
    const profId = this.scheduleForm.value.professionalId;

    if (!date || !profId) return;

    this.selectedDate.set(date);
    this.selectedSlot.set(null);
    this.scheduleForm.patchValue({ slot: '' });
    this.isLoadingSlots.set(true);

    this.availabilityService.getSlots(Number(profId), date).subscribe({
      next: (slots: any[]) => {
        this.availableSlots.set(slots);
        this.isLoadingSlots.set(false);
      },
      error: () => {
        this.availableSlots.set([]);
        this.isLoadingSlots.set(false);
      }
    });
  }

  selectSlot(slot: any) {
    this.selectedSlot.set(slot);
    this.scheduleForm.patchValue({ slot: slot.startTime });
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
    if (this.scheduleForm.valid && this.selectedSlot()) {
      const slot = this.selectedSlot();
      const date = this.scheduleForm.value.date;

      const payload = {
        professionalId: Number(this.scheduleForm.value.professionalId),
        dateTime: `${date}T${slot.startTime}`,
        reason: this.scheduleForm.value.reason
      };

      this.appointmentService.agendarConsulta(payload).subscribe({
        next: () => {
          alert('Consulta agendada com sucesso!');
          this.isScheduling.set(false);
          this.scheduleForm.reset();
          this.selectedProfessional.set(null);
          this.selectedDate.set('');
          this.selectedSlot.set(null);
          this.availableSlots.set([]);
          this.loadAppointments();
        },
        error: (msg: string) => alert('Erro: ' + msg)
      });
    }
  }

  // Calcula a data mínima para agendamento (amanhã)
  get minDate(): string {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  }
}