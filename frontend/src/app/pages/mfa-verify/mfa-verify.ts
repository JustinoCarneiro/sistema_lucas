// frontend/src/app/pages/mfa-verify/mfa-verify.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MfaService } from '../../security/mfa.service';

@Component({
  selector: 'app-mfa-verify',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './mfa-verify.html'
})
export class MfaVerifyComponent {
  private mfaService = inject(MfaService);
  private router = inject(Router);

  code = '';
  usarBackupCode = signal(false);
  errorMessage = signal('');
  isLoading = signal(false);

  toggleBackupCode() {
    this.usarBackupCode.update(v => !v);
    this.code = '';
    this.errorMessage.set('');
  }

  onSubmit() {
    this.errorMessage.set('');
    if (!this.code.trim()) {
      this.errorMessage.set('Informe o código.');
      return;
    }

    this.isLoading.set(true);
    this.mfaService.verify(this.code.trim()).subscribe({
      next: (response: any) => {
        // Mesma cauda do login.ts sem MFA — a sessão real já foi emitida pelo backend.
        localStorage.setItem('role', response.role);
        localStorage.setItem('verified', String(response.verified));
        this.isLoading.set(false);
        this.router.navigate(['/panel']);
      },
      error: (mensagem: any) => {
        this.errorMessage.set(typeof mensagem === 'string' ? mensagem : 'Código inválido.');
        this.isLoading.set(false);
      }
    });
  }
}
