// frontend/src/app/pages/waitlist-oferta/waitlist-oferta.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface WaitlistOfertaStatus {
  valido: boolean;
  jaConfirmada: boolean;
  expirada: boolean;
  profissionalNome: string | null;
  dataHora: string | null;
}

@Component({
  selector: 'app-waitlist-oferta',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './waitlist-oferta.html'
})
export class WaitlistOfertaComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  token: string | null = null;
  status = signal<WaitlistOfertaStatus | null>(null);
  isLoadingStatus = signal(true);

  isConfirming = signal(false);
  sucesso = signal(false);
  erro = signal('');

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.isLoadingStatus.set(false);
      return;
    }

    this.http.get<WaitlistOfertaStatus>(`${environment.apiUrl}/waitlist/oferta/${this.token}`).subscribe({
      next: (res) => {
        this.status.set(res);
        this.isLoadingStatus.set(false);
      },
      error: () => {
        this.status.set({ valido: false, jaConfirmada: false, expirada: false, profissionalNome: null, dataHora: null });
        this.isLoadingStatus.set(false);
      }
    });
  }

  confirmar() {
    if (!this.token) return;

    this.isConfirming.set(true);
    this.erro.set('');

    this.http.post(`${environment.apiUrl}/waitlist/oferta/confirmar`, { token: this.token }, { responseType: 'text' }).subscribe({
      next: () => {
        this.sucesso.set(true);
        this.isConfirming.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.erro.set(this.parseError(err));
        this.isConfirming.set(false);
      }
    });
  }

  // responseType: 'text' faz o Angular nunca tentar JSON.parse no corpo do erro — mas o backend
  // responde erro como JSON (ExceptionDTO via GlobalExceptionHandler). Sem isso, err.error vem
  // como a string JSON crua (ex.: '{"message":"...","code":"400"}') direto na tela.
  private parseError(err: HttpErrorResponse): string {
    try {
      const body = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
      return body?.message || 'Não foi possível confirmar a vaga. Tente novamente.';
    } catch {
      return err.error || 'Não foi possível confirmar a vaga. Tente novamente.';
    }
  }
}
