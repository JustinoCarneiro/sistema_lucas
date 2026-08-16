// frontend/src/app/pages/nps-avaliacao/nps-avaliacao.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface NpsStatus {
  valido: boolean;
  jaRespondido: boolean;
  expirado: boolean;
  profissionalNome: string | null;
  dataConsulta: string | null;
}

@Component({
  selector: 'app-nps-avaliacao',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './nps-avaliacao.html'
})
export class NpsAvaliacaoComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  token: string | null = null;
  status = signal<NpsStatus | null>(null);
  isLoadingStatus = signal(true);

  notas = Array.from({ length: 11 }, (_, i) => i); // 0..10
  scoreEscolhida = signal<number | null>(null);
  comentario = '';

  isSubmitting = signal(false);
  sucesso = signal(false);
  erro = signal('');

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.isLoadingStatus.set(false);
      return;
    }

    this.http.get<NpsStatus>(`${environment.apiUrl}/nps/${this.token}`).subscribe({
      next: (res) => {
        this.status.set(res);
        this.isLoadingStatus.set(false);
      },
      error: () => {
        this.status.set({ valido: false, jaRespondido: false, expirado: false, profissionalNome: null, dataConsulta: null });
        this.isLoadingStatus.set(false);
      }
    });
  }

  escolherNota(nota: number) {
    this.scoreEscolhida.set(nota);
  }

  onSubmit() {
    if (this.scoreEscolhida() === null || !this.token) return;

    this.isSubmitting.set(true);
    this.erro.set('');

    const payload = {
      token: this.token,
      score: this.scoreEscolhida(),
      comentario: this.comentario || null
    };

    this.http.post(`${environment.apiUrl}/nps/responder`, payload, { responseType: 'text' }).subscribe({
      next: () => {
        this.sucesso.set(true);
        this.isSubmitting.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.erro.set(this.parseError(err));
        this.isSubmitting.set(false);
      }
    });
  }

  // responseType: 'text' faz o Angular nunca tentar JSON.parse no corpo do erro — mas o backend
  // responde erro como JSON (ExceptionDTO via GlobalExceptionHandler). Sem isso, err.error vem
  // como a string JSON crua (ex.: '{"message":"...","code":"400"}') direto na tela.
  private parseError(err: HttpErrorResponse): string {
    try {
      const body = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
      return body?.message || 'Não foi possível registrar sua avaliação. Tente novamente.';
    } catch {
      return err.error || 'Não foi possível registrar sua avaliação. Tente novamente.';
    }
  }
}
