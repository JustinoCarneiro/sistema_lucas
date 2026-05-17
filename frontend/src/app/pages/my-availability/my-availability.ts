import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AvailabilityService } from './availability.service';

interface DayConfig {
  dateStr: string;
  dayNum: number;
  isCurrentMonth: boolean;
  selectedSlots: string[];
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
  
  diasRestantes = signal<number | null>(null);
  bloqueado = signal(false);
  
  viewedMonthOffset = signal<number>(1); // 0 = Atual, 1 = Próximo
  currentMonthStr = signal('');
  currentMonthLabel = signal('');
  
  calendarDays = signal<DayConfig[]>([]);
  weekDays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
  
  possibleSlots = [
    '08:00', '09:00', '10:00', '11:00', '12:00', 
    '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'
  ];

  selectedDate = signal<DayConfig | null>(null);

  hasSubmitted = computed(() => {
    return this.calendarDays().some(d => d.selectedSlots.length > 0);
  });
  
  showAlert = computed(() => {
    const dr = this.diasRestantes();
    if (dr === null) return false;
    if (this.bloqueado()) return true;
    if (dr <= 10 && dr >= 5) return true;
    if (!this.hasSubmitted() && dr < 15) return true;
    return false;
  });

  ngOnInit() {
    this.initCalendar(1); // Inicia no próximo mês (padrão de submissão)
    this.loadStatus();
  }

  initCalendar(offset: number) {
    this.viewedMonthOffset.set(offset);
    this.isLoading.set(true);
    this.selectedDate.set(null);

    const today = new Date();
    let y = today.getFullYear();
    let m = today.getMonth() + offset;

    // Ajuste de virada de ano
    const date = new Date(y, m, 1);
    y = date.getFullYear();
    m = date.getMonth();
    
    const mm = (m + 1).toString().padStart(2, '0');
    this.currentMonthStr.set(`${y}-${mm}`);
    
    const monthNames = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
    this.currentMonthLabel.set(`${monthNames[m]} de ${y}`);
    
    const firstDay = new Date(y, m, 1);
    const lastDay = new Date(y, m + 1, 0);
    
    const days: DayConfig[] = [];
    const startOffset = firstDay.getDay();
    
    for (let i = 0; i < startOffset; i++) {
      days.push({ dateStr: '', dayNum: 0, isCurrentMonth: false, selectedSlots: [] });
    }
    
    for (let i = 1; i <= lastDay.getDate(); i++) {
      const dd = i.toString().padStart(2, '0');
      days.push({
        dateStr: `${y}-${mm}-${dd}`,
        dayNum: i,
        isCurrentMonth: true,
        selectedSlots: []
      });
    }
    
    this.calendarDays.set(days);
    this.loadAvailability();
  }

  changeMonth(offset: number) {
    if (this.viewedMonthOffset() === offset) return;
    this.initCalendar(offset);
  }

  loadStatus() {
    this.service.getStatusMes().subscribe({
      next: (status) => {
        this.diasRestantes.set(status.diasRestantes);
        this.bloqueado.set(status.bloqueado);
        this.loadAvailability();
      },
      error: () => this.isLoading.set(false)
    });
  }

  loadAvailability() {
    this.service.getMinhaDisponibilidade(this.currentMonthStr()).subscribe({
      next: (data) => {
        const days = [...this.calendarDays()];
        
        data.forEach((item: any) => {
          const day = days.find(d => d.dateStr === item.date);
          if (day) {
            const time = item.startTime?.substring(0, 5);
            if (time && !day.selectedSlots.includes(time)) {
              day.selectedSlots.push(time);
            }
          }
        });
        
        days.forEach(d => d.selectedSlots.sort());
        this.calendarDays.set(days);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  selectDate(day: DayConfig) {
    if (!day.isCurrentMonth) return;
    this.selectedDate.set(day);
  }

  toggleSlot(time: string) {
    if (this.bloqueado()) {
      return;
    }
    const day = this.selectedDate();
    if (!day) {
      return;
    }
    
    const index = day.selectedSlots.indexOf(time);
    const newSlots = [...day.selectedSlots];
    if (index > -1) {
      newSlots.splice(index, 1);
    } else {
      newSlots.push(time);
      newSlots.sort();
    }
    
    const updatedDay = {
      ...day,
      selectedSlots: newSlots
    };
    
    this.selectedDate.set(updatedDay);
    
    const updatedDays = this.calendarDays().map(d => 
      d.dateStr === day.dateStr ? updatedDay : d
    );
    this.calendarDays.set(updatedDays);
  }

  isSlotSelected(time: string): boolean {
    const day = this.selectedDate();
    return day ? day.selectedSlots.includes(time) : false;
  }

  salvarMes() {
    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const dtos = this.calendarDays()
      .filter(d => d.isCurrentMonth && d.selectedSlots.length > 0)
      .map(d => ({
        date: d.dateStr,
        startTimes: d.selectedSlots.map(t => t + ':00')
      }));

    this.service.salvarMes(this.currentMonthStr(), dtos).subscribe({
      next: (res) => {
        this.isSaving.set(false);
        this.successMessage.set('Agenda do mês salva com sucesso!');
        setTimeout(() => this.successMessage.set(''), 30000);
      },
      error: (err: any) => {
        console.error('Availability UI error received:', err);
        this.isSaving.set(false);
        
        let msg = 'Erro ao salvar agenda.';
        if (typeof err === 'string') {
          msg = err;
        } else if (err?.error) {
          try {
            const parsed = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
            msg = parsed?.message || err.message || msg;
          } catch {
            msg = err.error || err.message || msg;
          }
        } else if (err?.message) {
          msg = err.message;
        } else {
          msg = JSON.stringify(err) || msg;
        }
        
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 30000);
      }
    });
  }

  get enabledDaysCount() {
    return this.calendarDays().filter(d => d.selectedSlots.length > 0).length;
  }
}
