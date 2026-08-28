// frontend/src/app/pages/system-logs/system-logs.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface SystemLogDTO {
  id: number;
  level: string;
  loggerName: string;
  message: string;
  stackTrace: string | null;
  criadoEm: string;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number; // página atual (0-based)
}

// /panel/logs (ADMIN/TECNICO) — captura automática de WARN/ERROR do sistema inteiro (ver
// InMemoryLogAppender/SystemLogPersistenceService no backend). Achado que motivou isso: SMTP
// quebrado silenciosamente por quem sabe quanto tempo, sem log nenhum visível (27/08/2026).
@Component({
  selector: 'app-system-logs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './system-logs.html'
})
export class SystemLogsComponent implements OnInit {
  private http = inject(HttpClient);

  logs = signal<SystemLogDTO[]>([]);
  isLoading = signal(true);
  filtroLevel = signal<'TODOS' | 'WARN' | 'ERROR'>('TODOS');
  paginaAtual = signal(0);
  totalPaginas = signal(0);
  expandido = signal<Set<number>>(new Set());

  ngOnInit() {
    this.carregar();
  }

  setFiltro(level: 'TODOS' | 'WARN' | 'ERROR') {
    this.filtroLevel.set(level);
    this.paginaAtual.set(0);
    this.carregar();
  }

  carregar() {
    this.isLoading.set(true);
    const level = this.filtroLevel();
    const params: any = { page: this.paginaAtual(), size: 25, sort: 'criadoEm,desc' };
    if (level !== 'TODOS') params.level = level;

    this.http.get<PageResponse<SystemLogDTO>>(`${environment.apiUrl}/system-logs`, { params }).subscribe({
      next: (res) => {
        this.logs.set(res.content);
        this.totalPaginas.set(res.totalPages);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  proximaPagina() {
    if (this.paginaAtual() < this.totalPaginas() - 1) {
      this.paginaAtual.update(p => p + 1);
      this.carregar();
    }
  }

  paginaAnterior() {
    if (this.paginaAtual() > 0) {
      this.paginaAtual.update(p => p - 1);
      this.carregar();
    }
  }

  toggleStackTrace(id: number) {
    const set = new Set(this.expandido());
    if (set.has(id)) set.delete(id); else set.add(id);
    this.expandido.set(set);
  }

  isExpandido(id: number): boolean {
    return this.expandido().has(id);
  }

  badgeClass(level: string): string {
    switch (level) {
      case 'ERROR': return 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300';
      case 'WARN': return 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300';
      default: return 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300';
    }
  }
}
