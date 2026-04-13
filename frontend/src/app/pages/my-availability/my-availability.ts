// frontend/src/app/pages/my-availability/my-availability.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvailabilityService } from './availability.service';

interface DayConfig {
  key: string;
  label: string;
  enabled: boolean;
  startTime: string;
  endTime: string;
  saved: boolean;
  slots: string[];
}

@Component({
  selector: 'app-my-availability',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-availability.html',
  styleUrl: './my-availability.css'
})
export class MyAvailabilityComponent implements OnInit {
  private service = inject(AvailabilityService);

  isLoading = signal(true);
  isSaving = signal(false);
  successMessage = signal('');
  errorMessage = signal('');

  days: DayConfig[] = [
    { key: 'MONDAY',    label: 'Segunda-feira', enabled: false, startTime: '08:00', endTime: '12:00', saved: false, slots: [] },
    { key: 'TUESDAY',   label: 'Terça-feira',   enabled: false, startTime: '08:00', endTime: '12:00', saved: false, slots: [] },
    { key: 'WEDNESDAY', label: 'Quarta-feira',   enabled: false, startTime: '08:00', endTime: '12:00', saved: false, slots: [] },
    { key: 'THURSDAY',  label: 'Quinta-feira',   enabled: false, startTime: '08:00', endTime: '12:00', saved: false, slots: [] },
    { key: 'FRIDAY',    label: 'Sexta-feira',    enabled: false, startTime: '08:00', endTime: '12:00', saved: false, slots: [] },
    { key: 'SATURDAY',  label: 'Sábado',         enabled: false, startTime: '08:00', endTime: '12:00', saved: false, slots: [] },
    { key: 'SUNDAY',    label: 'Domingo',        enabled: false, startTime: '08:00', endTime: '12:00', saved: false, slots: [] },
  ];

  ngOnInit() {
    this.loadAvailability();
  }

  loadAvailability() {
    this.isLoading.set(true);
    this.service.getMinhaDisponibilidade().subscribe({
      next: (data) => {
        // Marcar os dias que já estão salvos
        data.forEach((item: any) => {
          const day = this.days.find(d => d.key === item.dayOfWeek);
          if (day) {
            day.enabled = true;
            day.startTime = item.startTime?.substring(0, 5) || '08:00';
            day.endTime = item.endTime?.substring(0, 5) || '12:00';
            day.saved = true;
            day.slots = this.calculateSlots(day.startTime, day.endTime);
          }
        });
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  calculateSlots(start: string, end: string): string[] {
    const slots: string[] = [];
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    let cursor = sh * 60 + sm;
    const endMin = eh * 60 + em;

    while (cursor + 60 <= endMin) {
      const h = Math.floor(cursor / 60);
      const m = cursor % 60;
      const hEnd = Math.floor((cursor + 60) / 60);
      const mEnd = (cursor + 60) % 60;
      slots.push(
        `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')} – ${String(hEnd).padStart(2, '0')}:${String(mEnd).padStart(2, '0')}`
      );
      cursor += 60;
    }
    return slots;
  }

  onTimeChange(day: DayConfig) {
    day.slots = this.calculateSlots(day.startTime, day.endTime);
  }

  salvarDia(day: DayConfig) {
    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    this.service.salvarDia({
      dayOfWeek: day.key,
      startTime: day.startTime + ':00',
      endTime: day.endTime + ':00'
    }).subscribe({
      next: () => {
        day.saved = true;
        this.isSaving.set(false);
        this.successMessage.set(`${day.label} salvo com sucesso!`);
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err: string) => {
        this.isSaving.set(false);
        this.errorMessage.set(err);
        setTimeout(() => this.errorMessage.set(''), 5000);
      }
    });
  }

  removerDia(day: DayConfig) {
    if (!confirm(`Remover disponibilidade de ${day.label}?`)) return;

    this.service.removerDia(day.key).subscribe({
      next: () => {
        day.enabled = false;
        day.saved = false;
        day.startTime = '08:00';
        day.endTime = '12:00';
        day.slots = [];
        this.successMessage.set(`${day.label} removido.`);
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: () => {
        this.errorMessage.set('Erro ao remover disponibilidade.');
        setTimeout(() => this.errorMessage.set(''), 5000);
      }
    });
  }

  toggleDay(day: DayConfig) {
    if (day.enabled) {
      // Desabilitando → remove do backend se salvo
      if (day.saved) {
        this.removerDia(day);
      } else {
        day.enabled = false;
      }
    } else {
      day.enabled = true;
      day.slots = this.calculateSlots(day.startTime, day.endTime);
    }
  }

  get enabledDaysCount(): number {
    return this.days.filter(d => d.enabled).length;
  }
}
