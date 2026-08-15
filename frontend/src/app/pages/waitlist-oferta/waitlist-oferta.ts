// frontend/src/app/pages/waitlist-oferta/waitlist-oferta.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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
      error: (err: any) => {
        this.erro.set(err.error || 'Não foi possível confirmar a vaga. Tente novamente.');
        this.isConfirming.set(false);
      }
    });
  }
}
