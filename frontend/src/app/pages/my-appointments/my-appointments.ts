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
  workingDays = signal<string[]>([]);
  availableDates = signal<{value: string, label: string}[]>([]);
  dateErrorMessage = signal('');

  // 🔄 Modais e Signals de Controle
  isCancelling = signal(false);
  isRescheduling = signal(false);
  selectedAppointment = signal<any>(null);

  scheduleForm: FormGroup;
  cancelForm: FormGroup;
  rescheduleForm: FormGroup;

  dayNames: Record<string, string> = {
    'MONDAY': 'Segunda-feira',
    'TUESDAY': 'Terça-feira',
    'WEDNESDAY': 'Quarta-feira',
    'THURSDAY': 'Quinta-feira',
    'FRIDAY': 'Sexta-feira',
    'SATURDAY': 'Sábado',
    'SUNDAY': 'Domingo'
  };

  // Mapeamento JS (0-6) para DayOfWeek (MONDAY-SUNDAY)
  // JS: 0=Sunday, 1=Monday... 6=Saturday
  jsToEnum: Record<number, string> = {
    0: 'SUNDAY', 1: 'MONDAY', 2: 'TUESDAY', 3: 'WEDNESDAY', 4: 'THURSDAY', 5: 'FRIDAY', 6: 'SATURDAY'
  };

  statusLabel: Record<string, string> = {
    AGUARDANDO_CONFIRMACAO: 'Aguardando Confirmação',
    AGENDADA: 'Agendada', CONFIRMADA_PROFISSIONAL: 'Aguardando paciente',
    CONFIRMADA: 'Confirmada', CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada', FALTA: 'Faltou'
  };

  statusClass: Record<string, string> = {
    AGUARDANDO_CONFIRMACAO: 'bg-purple-100 text-purple-700 border border-purple-200 dark:bg-purple-900/40 dark:text-purple-300 dark:border-purple-700',
    AGENDADA: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',
    CONFIRMADA_PROFISSIONAL: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300',
    CONFIRMADA: 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300',
    CONCLUIDA: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
    CANCELADA: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300',
    FALTA: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-300'
  };

  constructor() {
    this.scheduleForm = this.fb.group({
      professionalId: ['', Validators.required],
      date: ['', Validators.required],
      slot: ['', Validators.required],
      reason: ['']
    });

    this.cancelForm = this.fb.group({
      justification: ['', [Validators.required, Validators.minLength(10)]]
    });

    this.rescheduleForm = this.fb.group({
      professionalId: ['', Validators.required],
      date: ['', Validators.required],
      slot: ['', Validators.required],
      justification: ['', [Validators.required, Validators.minLength(10)]]
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
    this.workingDays.set([]);
    this.dateErrorMessage.set('');
    this.scheduleForm.patchValue({ date: '', slot: '' });

    if (profId) {
      this.loadAvailableDates(Number(profId));
    }
  }

  private loadAvailableDates(profId: number) {
    this.availabilityService.getAvailableDates(profId).subscribe({
      next: (dates) => {
        const formatted = dates.map(d => {
          const [year, month, day] = d.split('-').map(Number);
          const date = new Date(year, month - 1, day);
          return {
            value: d,
            label: date.toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: '2-digit' })
          };
        });
        this.availableDates.set(formatted);
      }
    });
  }

  onDateChange() {
    const dateStr = this.scheduleForm.value.date;
    const profId = this.scheduleForm.value.professionalId;

    if (!dateStr || !profId) return;

    // A validação agora é implícita pois o dropdown só tem datas válidas,
    // mas mantemos o reset de slots e o carregamento.
    this.dateErrorMessage.set('');
    this.selectedDate.set(dateStr);
    this.selectedSlot.set(null);
    this.scheduleForm.patchValue({ slot: '' });
    this.isLoadingSlots.set(true);

    this.availabilityService.getSlots(Number(profId), dateStr).subscribe({
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
      error: (msg: string) => alert('Erro: ' + msg)
    });
  }

  cancelar(id: number) {
    const appointment = this.myAppointments().find(a => a.id === id);
    if (!appointment) return;
    
    this.selectedAppointment.set(appointment);
    this.cancelForm.reset();
    this.isCancelling.set(true);
  }

  openRescheduleModal(appointment: any) {
    this.selectedAppointment.set(appointment);
    this.rescheduleForm.reset({
      professionalId: appointment.professionalId, // Assuming we have this or need to load it
      justification: ''
    });
    
    // Configura o formulário com o profissional da consulta original
    // Precisamos achar o profissional no signal de professionals
    const prof = this.professionals().find(p => p.name === appointment.professionalName);
    if (prof) {
      this.rescheduleForm.patchValue({ professionalId: prof.id });
      this.onRescheduleProfessionalChange();
    }
    
    this.isRescheduling.set(true);
  }

  onRescheduleProfessionalChange() {
    const profId = this.rescheduleForm.value.professionalId;
    if (profId) {
      this.loadAvailableDates(Number(profId));
    }
  }

  onRescheduleDateChange() {
    const dateStr = this.rescheduleForm.value.date;
    const profId = this.rescheduleForm.value.professionalId;
    if (!dateStr || !profId) return;

    this.isLoadingSlots.set(true);
    this.availabilityService.getSlots(Number(profId), dateStr).subscribe({
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

  onCancelSubmit() {
    if (this.cancelForm.valid && this.selectedAppointment()) {
      const id = this.selectedAppointment().id;
      const justification = this.cancelForm.value.justification;

      this.appointmentService.cancelarMinhaConsulta(id, justification).subscribe({
        next: () => {
          this.isCancelling.set(false);
          this.loadAppointments();
        },
        error: (msg: string) => alert('Erro ao cancelar: ' + msg)
      });
    }
  }

  onRescheduleSubmit() {
    if (this.rescheduleForm.valid && this.selectedAppointment()) {
      const id = this.selectedAppointment().id;
      const date = this.rescheduleForm.value.date;
      const slot = this.rescheduleForm.value.slot;
      const justification = this.rescheduleForm.value.justification;

      const newDateTime = `${date}T${slot}`;

      this.appointmentService.reagendarConsulta(id, newDateTime, justification).subscribe({
        next: () => {
          this.isRescheduling.set(false);
          this.loadAppointments();
        },
        error: (msg: string) => alert('Erro ao reagendar: ' + msg)
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

  // Calcula a data máxima para agendamento (1 mês a partir de hoje)
  get maxDate(): string {
    const max = new Date();
    max.setMonth(max.getMonth() + 1);
    return max.toISOString().split('T')[0];
  }
}