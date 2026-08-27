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

  filtroStatus = signal<'TODOS' | 'BLOQUEADOS' | 'COM_INFRACOES' | 'ANONIMIZADOS'>('TODOS');

  filteredPatients = computed(() => {
    const list = this.patientsList();
    const status = this.filtroStatus();
    if (status === 'BLOQUEADOS') {
      return list.filter(p => p.blockedUntil && new Date(p.blockedUntil) > new Date());
    }
    if (status === 'COM_INFRACOES') {
      return list.filter(p => p.infractionCount > 0);
    }
    if (status === 'ANONIMIZADOS') {
      return list.filter(p => !p.active);
    }
    // "Todos" continua mostrando os anonimizados também (é o admin quem precisa enxergar que
    // existem, com o selo "Anonimizado" na linha) — só não é o filtro padrão pra não poluir
    // a visão do dia a dia.
    return list;
  });

  openDetails(item: any) {
    this.selectedItem = item;
  }

  // Mesmo padrão de mascaramento do export (ExportService.maskCpf) — a listagem mostrava o
  // CPF em texto pleno na tela pra qualquer ADMIN, sem motivo funcional pra exibir o dado
  // completo aqui (é só identificação visual, não é usado pra nenhuma ação na tela).
  maskCpf(cpf: string | null | undefined): string {
    if (!cpf) return '—';
    const clean = cpf.replace(/\D/g, '');
    if (clean.length !== 11) return '***.***.***-**';
    return `${clean.substring(0, 3)}.***.***-${clean.substring(9, 11)}`;
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
