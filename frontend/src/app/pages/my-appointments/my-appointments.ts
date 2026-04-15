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

  scheduleForm: FormGroup;

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
    this.workingDays.set([]);
    this.dateErrorMessage.set('');
    this.scheduleForm.patchValue({ date: '', slot: '' });

    if (profId) {
      this.availabilityService.getWorkingDays(Number(profId)).subscribe({
        next: (days) => {
          this.workingDays.set(days);
          this.generateAvailableDates(days);
        }
      });
    }
  }

  generateAvailableDates(days: string[]) {
    console.log('Gerando datas únicas por dia da semana...', days);
    const dates: {value: string, label: string}[] = [];
    const today = new Date();
    
    // Gerar apenas o próximo dia disponível para cada dia da semana configurado
    // (ou o dia atual, ou o da próxima semana se já passou)
    days.forEach(dayName => {
      const today = new Date();
      // Encontrar o enum index correspondente (0-6)
      const targetJsDay = Object.keys(this.jsToEnum).find(k => this.jsToEnum[Number(k)] === dayName);
      if (targetJsDay === undefined) return;
      
      const targetDay = Number(targetJsDay);
      const currentDay = today.getDay();
      
      // Calcula quantos dias faltam para o próximo 'targetDay'
      let daysDiff = (targetDay - currentDay + 7) % 7;
      
      const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + daysDiff);
      
      const value = d.toISOString().split('T')[0];
      const label = d.toLocaleDateString('pt-BR', { 
        weekday: 'long', 
        day: '2-digit', 
        month: '2-digit' 
      });
      dates.push({ value, label });
    });

    // Ordenar as datas cronologicamente
    dates.sort((a, b) => a.value.localeCompare(b.value));
    this.availableDates.set(dates);
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

  // Calcula a data máxima para agendamento (1 mês a partir de hoje)
  get maxDate(): string {
    const max = new Date();
    max.setMonth(max.getMonth() + 1);
    return max.toISOString().split('T')[0];
  }
}