// frontend/src/app/pages/patients/patients.ts
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientService } from './patients.service';
import { NotificationService } from '../../notification.service';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './patients.html',
  styleUrl: './patients.css'
})
export class Patients implements OnInit {
  private patientService = inject(PatientService);
  private notify = inject(NotificationService);

  patientsList = signal<any[]>([]);
  selectedItem: any = null;

  filtroStatus = signal<'TODOS' | 'BLOQUEADOS' | 'COM_INFRACOES'>('TODOS');

  filteredPatients = computed(() => {
    const list = this.patientsList();
    const status = this.filtroStatus();
    if (status === 'BLOQUEADOS') {
      return list.filter(p => p.blockedUntil && new Date(p.blockedUntil) > new Date());
    }
    if (status === 'COM_INFRACOES') {
      return list.filter(p => p.infractionCount > 0);
    }
    return list;
  });

  openDetails(item: any) {
    this.selectedItem = item;
  }

  closeDetails() {
    this.selectedItem = null;
  }

  ngOnInit() { this.loadPatients(); }

  loadPatients() {
    this.patientService.getPatients().subscribe({
      next: (data: any) => this.patientsList.set(data.content ?? data),
      error: (err: any) => console.error('Erro ao buscar pacientes', err)
    });
  }

  deletePaciente(id: number, nome: string) {
    if (confirm(`Tem certeza que deseja remover o paciente ${nome}? Esta ação não pode ser desfeita.`)) {
      this.patientService.deletePatient(id).subscribe({
        next: () => { this.notify.success('Paciente removido com sucesso!'); this.loadPatients(); },
        error: (msg: string) => this.notify.error(msg)
      });
    }
  }

  desbloquearPaciente(id: number) {
    if (confirm('Deseja liberar este paciente para novos agendamentos agora?')) {
      this.patientService.desbloquear(id).subscribe({
        next: () => { 
          this.notify.success('Paciente desbloqueado com sucesso!');
          this.loadPatients(); 
          if (this.selectedItem) this.selectedItem = null;
        },
        error: (msg: string) => this.notify.error(msg)
      });
    }
  }
}
