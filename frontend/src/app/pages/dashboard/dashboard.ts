// frontend/src/app/pages/dashboard/dashboard.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService } from './dashboard.service';
import { AuthService } from '../../security/auth.service';
import { AppointmentService } from '../appointments/appointment.service';
import { ExportService } from '../export/export.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private appointmentService = inject(AppointmentService);
  private exportService = inject(ExportService);

  exportar(tipo?: string) {
    const role = this.userRole();
    if (role === 'ADMIN') {
      if (tipo === 'patients') this.exportService.exportPatients();
      else if (tipo === 'professionals') this.exportService.exportProfessionals();
      else this.exportService.exportAdmin();
    }
    if (role === 'PROFESSIONAL') this.exportService.exportProfessional();
  }

  userRole = signal<string | null>(null);
  dados = signal<any>(null);
  isLoading = signal(true);

  tiposLabel: Record<string, string> = {
    LAUDO_PSICOLOGICO: 'Laudo Psicológico', RELATORIO_EVOLUCAO: 'Relatório de Evolução',
    ENCAMINHAMENTO: 'Encaminhamento', ATESTADO: 'Atestado',
    AVALIACAO_PSICOLOGICA: 'Avaliação Psicológica', RECEITA_PRESCRICAO: 'Receita / Prescrição'
  };

  statusLabel: Record<string, string> = {
    AGENDADA: 'Agendada', CONFIRMADA_PACIENTE: 'Aguard. profissional',
    CONFIRMADA: 'Confirmada', CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada', FALTA: 'Faltou'
  };

  statusClass: Record<string, string> = {
    AGENDADA: 'bg-blue-100 text-blue-700', CONFIRMADA_PACIENTE: 'bg-yellow-100 text-yellow-700',
    CONFIRMADA: 'bg-green-100 text-green-700', CONCLUIDA: 'bg-gray-100 text-gray-600',
    CANCELADA: 'bg-red-100 text-red-700', FALTA: 'bg-orange-100 text-orange-700'
  };

  ngOnInit() {
    this.userRole.set(this.authService.getUserRole());
    this.carregarDados();
  }

  carregarDados() {
    this.isLoading.set(true);
    const role = this.userRole();
    const req = role === 'ADMIN'
      ? this.dashboardService.getAdminDashboard()
      : role === 'PROFESSIONAL'
        ? this.dashboardService.getProfissionalDashboard()
        : this.dashboardService.getPacienteDashboard();

    req.subscribe({
      next: (data) => { this.dados.set(data); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  get maxStatus(): number {
    const d = this.dados();
    if (!d?.consultasPorStatus) return 1;
    return Math.max(...Object.values(d.consultasPorStatus) as number[]) || 1;
  }

  barWidth(valor: number): string {
    return Math.round((valor / this.maxStatus) * 100) + '%';
  }

  statusEntries(): [string, number][] {
    const d = this.dados();
    if (!d?.consultasPorStatus) return [];
    return Object.entries(d.consultasPorStatus) as [string, number][];
  }

  barClass(status: string): string {
    const map: Record<string, string> = {
      AGENDADA: 'bg-blue-500', CONCLUIDA: 'bg-green-500',
      CANCELADA: 'bg-red-400', FALTA: 'bg-yellow-400'
    };
    return map[status] ?? 'bg-gray-400';
  }

  proximaConsulta(): any {
    const lista = this.dados()?.proximaConsulta;
    return lista?.length > 0 ? lista[0] : null;
  }

  confirmarNoDashboard(id: number) {
    this.appointmentService.confirmarPaciente(id).subscribe({
      next: () => { alert('Presença confirmada!'); this.carregarDados(); },
      error: (err: any) => alert('Erro: ' + (err.error?.message || 'Não foi possível confirmar.'))
    });
  }
}