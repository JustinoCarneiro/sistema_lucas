// frontend/src/app/pages/my-availability/my-availability.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvailabilityService } from './availability.service';

interface DayConfig {
  key: string;
  label: string;
  enabled: boolean;
  selectedSlots: string[]; // Agora rastreamos os horários de início selecionados
  saved: boolean;
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

  // Faixa definida pelo usuário: 08:00 às 19:00 (último slot começa às 18:00)
  possibleSlots = [
    '08:00', '09:00', '10:00', '11:00', '12:00', 
    '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'
  ];

  days: DayConfig[] = [
    { key: 'MONDAY',    label: 'Segunda-feira', enabled: false, selectedSlots: [], saved: false },
    { key: 'TUESDAY',   label: 'Terça-feira',   enabled: false, selectedSlots: [], saved: false },
    { key: 'WEDNESDAY', label: 'Quarta-feira',  enabled: false, selectedSlots: [], saved: false },
    { key: 'THURSDAY',  label: 'Quinta-feira',  enabled: false, selectedSlots: [], saved: false },
    { key: 'FRIDAY',    label: 'Sexta-feira',   enabled: false, selectedSlots: [], saved: false },
    { key: 'SATURDAY',  label: 'Sábado',        enabled: false, selectedSlots: [], saved: false },
    { key: 'SUNDAY',    label: 'Domingo',       enabled: false, selectedSlots: [], saved: false },
  ];

  ngOnInit() {
    this.loadAvailability();
  }

  loadAvailability() {
    this.isLoading.set(true);
    this.service.getMinhaDisponibilidade().subscribe({
      next: (data) => {
        // Limpar estados
        this.days.forEach(d => { d.enabled = false; d.selectedSlots = []; d.saved = false; });

        // Mapear dados do backend
        data.forEach((item: any) => {
          const day = this.days.find(d => d.key === item.dayOfWeek);
          if (day) {
            day.enabled = true;
            day.saved = true;
            const time = item.startTime?.substring(0, 5);
            if (time && !day.selectedSlots.includes(time)) {
              day.selectedSlots.push(time);
            }
          }
        });
        
        // Ordenar slots para consistência visual
        this.days.forEach(d => d.selectedSlots.sort());
        
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  toggleSlot(day: DayConfig, time: string) {
    const index = day.selectedSlots.indexOf(time);
    if (index > -1) {
      day.selectedSlots.splice(index, 1);
    } else {
      day.selectedSlots.push(time);
      day.selectedSlots.sort();
    }
  }

  isSlotSelected(day: DayConfig, time: string): boolean {
    return day.selectedSlots.includes(time);
  }

  salvarDia(day: DayConfig) {
    if (day.selectedSlots.length === 0) {
        this.removerDia(day);
        return;
    }

    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const payload = {
      dayOfWeek: day.key,
      startTimes: day.selectedSlots.map(t => t + ':00')
    };

    this.service.salvarDia(payload).subscribe({
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
    if (!confirm(`Remover toda a disponibilidade de ${day.label}?`)) return;

    this.service.removerDia(day.key).subscribe({
      next: () => {
        day.enabled = false;
        day.saved = false;
        day.selectedSlots = [];
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
      if (day.saved) {
        this.removerDia(day);
      } else {
        day.enabled = false;
        day.selectedSlots = [];
      }
    } else {
      day.enabled = true;
    }
  }

  get enabledDaysCount(): number {
    return this.days.filter(d => d.enabled).length;
  }
}
