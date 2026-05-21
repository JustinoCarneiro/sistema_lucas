import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * SEC-06 (PCI-DSS Req. 8.2.8): Encerra a sessão automaticamente após um
 * período de inatividade. Necessário porque o access token de 15 min é
 * renovado silenciosamente pelo interceptor — sem este serviço a sessão
 * nunca expiraria por inatividade.
 */
@Injectable({ providedIn: 'root' })
export class InactivityService {

  private readonly TIMEOUT_MS = 15 * 60 * 1000; // 15 minutos
  private readonly CHECK_INTERVAL_MS = 30 * 1000;
  private readonly events = ['click', 'keydown', 'mousemove', 'scroll', 'touchstart'];

  private authService = inject(AuthService);

  private lastActivity = Date.now();
  private intervalId: any = null;
  // Apenas registra o instante da última atividade — barato mesmo em mousemove.
  private activityHandler = () => { this.lastActivity = Date.now(); };

  /** Inicia o monitoramento de inatividade (chamar ao entrar na área autenticada). */
  start(): void {
    if (this.intervalId) return; // já ativo
    this.lastActivity = Date.now();
    this.events.forEach(e => document.addEventListener(e, this.activityHandler, { passive: true }));
    this.intervalId = setInterval(() => {
      if (Date.now() - this.lastActivity > this.TIMEOUT_MS) {
        this.stop();
        this.authService.logout(); // revoga a sessão no backend e redireciona
      }
    }, this.CHECK_INTERVAL_MS);
  }

  /** Encerra o monitoramento (chamar ao sair da área autenticada). */
  stop(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.events.forEach(e => document.removeEventListener(e, this.activityHandler));
  }
}
