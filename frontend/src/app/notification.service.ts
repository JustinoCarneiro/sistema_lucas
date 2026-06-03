// frontend/src/app/notification.service.ts
import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

/**
 * Notificações em toast (substitui o alert() nativo do navegador).
 * Uso: inject(NotificationService) e chamar success()/error()/info().
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly toasts = signal<Toast[]>([]);
  private seq = 0;

  success(message: string, duration = 4000): void {
    this.push('success', message, duration);
  }

  error(message: string, duration = 6000): void {
    this.push('error', this.normalize(message), duration);
  }

  info(message: string, duration = 4000): void {
    this.push('info', message, duration);
  }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  private push(type: ToastType, message: string, duration: number): void {
    const id = ++this.seq;
    this.toasts.update(list => [...list, { id, type, message }]);
    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }
  }

  /**
   * Remove prefixos "Erro:" redundantes. O visual do toast já comunica que é
   * um erro, então a mensagem do backend (que costuma vir como "Erro: ...")
   * não precisa do prefixo — e isso elimina o antigo "Erro: Erro: ...".
   */
  private normalize(message: string): string {
    const clean = (message ?? '').replace(/^\s*(erro:\s*)+/i, '').trim();
    return clean || 'Ocorreu um erro. Tente novamente.';
  }
}
