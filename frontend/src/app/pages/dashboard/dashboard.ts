// frontend/src/app/pages/dashboard/dashboard.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService } from './dashboard.service';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);

  userRole: string | null = '';
  dados: any = null;
  isLoading = true;

  tiposLabel: Record<string, string> = {
    LAUDO_PSICOLOGICO:     'Laudo Psicológico',
    RELATORIO_EVOLUCAO:    'Relatório de Evolução',
    ENCAMINHAMENTO:        'Encaminhamento',
    ATESTADO:              'Atestado',
    AVALIACAO_PSICOLOGICA: 'Avaliação Psicológica',
    RECEITA_PRESCRICAO:    'Receita / Prescrição'
  };

  statusLabel: Record<string, string> = {
    AGENDADA:  'Agendada',
    CONCLUIDA: 'Concluída',
    CANCELADA: 'Cancelada',
    FALTA:     'Faltou'
  };

  statusClass: Record<string, string> = {
    AGENDADA:  'bg-blue-100 text-blue-700',
    CONCLUIDA: 'bg-green-100 text-green-700',
    CANCELADA: 'bg-red-100 text-red-700',
    FALTA:     'bg-yellow-100 text-yellow-700'
  };

  ngOnInit() {
    this.userRole = this.authService.getUserRole();
    this.carregarDados();
  }

  carregarDados() {
    this.isLoading = true;

    const req = this.userRole === 'ADMIN'
      ? this.dashboardService.getAdminDashboard()
      : this.userRole === 'PROFESSIONAL'
        ? this.dashboardService.getProfissionalDashboard()
        : this.dashboardService.getPacienteDashboard();

    req.subscribe({
      next: (data) => { this.dados = data; this.isLoading = false; },
      error: () => this.isLoading = false
    });
  }

  // Helpers para o gráfico de barras do admin
  get maxStatus(): number {
    if (!this.dados?.consultasPorStatus) return 1;
    return Math.max(...Object.values(this.dados.consultasPorStatus) as number[]) || 1;
  }

  barWidth(valor: number): string {
    return Math.round((valor / this.maxStatus) * 100) + '%';
  }

  statusEntries(): [string, number][] {
    if (!this.dados?.consultasPorStatus) return [];
    return Object.entries(this.dados.consultasPorStatus) as [string, number][];
  }

  barClass(status: string): string {
    const map: Record<string, string> = {
      AGENDADA:  'bg-blue-500',
      CONCLUIDA: 'bg-green-500',
      CANCELADA: 'bg-red-400',
      FALTA:     'bg-yellow-400'
    };
    return map[status] ?? 'bg-gray-400';
  }

  proximaConsulta(): any {
    const lista = this.dados?.proximaConsulta;
    return lista && lista.length > 0 ? lista[0] : null;
  }
}