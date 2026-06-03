// frontend/src/app/components/toast/toast.ts
import { Component, inject } from '@angular/core';
import { NotificationService } from '../../notification.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="fixed top-6 right-6 z-9999 flex w-96 max-w-[90vw] flex-col gap-4">
      @for (t of notify.toasts(); track t.id) {
        <div role="alert"
             class="toast-in group relative overflow-hidden rounded-2xl border shadow-2xl backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-3xl"
             [class]="card[t.type]">
             
          <!-- Efeito de brilho/glassmorphism de fundo -->
          <div class="absolute inset-0 bg-linear-to-r opacity-10" [class]="gradient[t.type]"></div>
          
          <div class="relative flex items-start gap-4 p-4">
            <!-- Ícone com animação de pulse -->
            <div class="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl shadow-inner transition-transform duration-500 group-hover:scale-110 group-hover:rotate-3"
                 [class]="badge[t.type]">
              @switch (t.type) {
                @case ('success') {
                  <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/></svg>
                }
                @case ('error') {
                  <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
                }
                @case ('info') {
                  <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                }
              }
            </div>
            
            <!-- Conteúdo -->
            <div class="flex-1 pt-1">
              <h4 class="mb-1 font-bold tracking-tight text-sm uppercase" [class]="titleColor[t.type]">
                {{ titleText[t.type] }}
              </h4>
              <p class="text-sm font-medium leading-relaxed opacity-90">{{ t.message }}</p>
            </div>
            
            <!-- Botão fechar -->
            <button type="button" (click)="notify.dismiss(t.id)"
                    class="shrink-0 rounded-lg p-1.5 opacity-50 transition-all hover:bg-black/10 hover:opacity-100 dark:hover:bg-white/10"
                    aria-label="Fechar">
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
          
          <!-- Barra de progresso visual (animação decorativa) -->
          <div class="h-1 w-full bg-black/5 dark:bg-white/5">
            <div class="h-full animate-progress" [class]="progressColor[t.type]"></div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-in { 
      animation: slideIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards; 
    }
    @keyframes slideIn {
      0% { opacity: 0; transform: translateX(100%) scale(0.9); }
      100% { opacity: 1; transform: translateX(0) scale(1); }
    }
    .animate-progress {
      animation: progress linear forwards;
      animation-duration: 6s; /* Fallback geral para o visual, já que o tempo real varia */
      transform-origin: left;
    }
    @keyframes progress {
      0% { transform: scaleX(1); }
      100% { transform: scaleX(0); }
    }
  `]
})
export class ToastComponent {
  readonly notify = inject(NotificationService);

  readonly card: Record<string, string> = {
    success: 'border-green-200 bg-white/85 text-green-900 dark:border-green-900/50 dark:bg-gray-900/90 dark:text-green-100',
    error:   'border-red-200 bg-white/85 text-red-900 dark:border-red-900/50 dark:bg-gray-900/90 dark:text-red-100',
    info:    'border-blue-200 bg-white/85 text-blue-900 dark:border-blue-900/50 dark:bg-gray-900/90 dark:text-blue-100',
  };

  readonly gradient: Record<string, string> = {
    success: 'from-green-500 to-emerald-500',
    error:   'from-red-500 to-rose-500',
    info:    'from-blue-500 to-cyan-500',
  };

  readonly badge: Record<string, string> = {
    success: 'bg-linear-to-br from-green-400 to-green-600 text-white shadow-green-500/30',
    error:   'bg-linear-to-br from-red-400 to-red-600 text-white shadow-red-500/30',
    info:    'bg-linear-to-br from-blue-400 to-blue-600 text-white shadow-blue-500/30',
  };

  readonly titleColor: Record<string, string> = {
    success: 'text-green-700 dark:text-green-400',
    error:   'text-red-700 dark:text-red-400',
    info:    'text-blue-700 dark:text-blue-400',
  };

  readonly titleText: Record<string, string> = {
    success: 'Sucesso',
    error:   'Erro Detectado',
    info:    'Informação',
  };

  readonly progressColor: Record<string, string> = {
    success: 'bg-green-500',
    error:   'bg-red-500',
    info:    'bg-blue-500',
  };
}
