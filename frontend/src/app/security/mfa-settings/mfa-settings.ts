// frontend/src/app/security/mfa-settings/mfa-settings.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MfaService } from '../mfa.service';
import { AuthService } from '../auth.service';
import { NotificationService } from '../../notification.service';
import { environment } from '../../../environments/environment';
import * as QRCode from 'qrcode';

// Componente autocontido — busca o próprio estado (mfaEnabled) via GET /auth/me em vez de
// receber via @Input, porque esse endpoint funciona pra QUALQUER role (inclusive ADMIN, que
// não tem "meu perfil" nesse sistema — Patient/Professional têm, ADMIN não). Isso permite
// embutir esse componente tanto em my-profile.html (PROFESSIONAL/PATIENT) quanto numa tela de
// Segurança dedicada pro ADMIN, sem duplicar a lógica de setup/enable/disable/backup codes.
@Component({
  selector: 'app-mfa-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mfa-settings.html'
})
export class MfaSettingsComponent implements OnInit {
  private http = inject(HttpClient);
  private mfaService = inject(MfaService);
  private authService = inject(AuthService);
  private notify = inject(NotificationService);

  isLoading = signal(true);
  mfaEnabled = signal(false);

  mfaSetupData = signal<{ secretBase32: string; otpAuthUri: string } | null>(null);
  mfaQrCodeDataUrl = signal<string>('');
  mfaEnableCode = '';
  mfaBackupCodes = signal<string[] | null>(null);
  mfaIsBusy = signal(false);
  showDisableMfaModal = signal(false);
  disableMfaPassword = '';
  disableMfaCode = '';

  ngOnInit() {
    this.http.get<{ role: string; mfaEnabled: boolean }>(`${environment.apiUrl}/auth/me`).subscribe({
      next: (data) => { this.mfaEnabled.set(data.mfaEnabled); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  iniciarSetupMfa() {
    this.mfaIsBusy.set(true);
    this.mfaService.setup().subscribe({
      next: async (data) => {
        this.mfaSetupData.set(data);
        try {
          const dataUrl = await QRCode.toDataURL(data.otpAuthUri, { width: 220, margin: 1 });
          this.mfaQrCodeDataUrl.set(dataUrl);
        } catch {
          this.mfaQrCodeDataUrl.set('');
        }
        this.mfaIsBusy.set(false);
      },
      error: (mensagem: any) => {
        this.notify.error(typeof mensagem === 'string' ? mensagem : 'Erro ao iniciar configuração de MFA.');
        this.mfaIsBusy.set(false);
      }
    });
  }

  cancelarSetupMfa() {
    this.mfaSetupData.set(null);
    this.mfaQrCodeDataUrl.set('');
    this.mfaEnableCode = '';
  }

  confirmarAtivacaoMfa() {
    if (!this.mfaEnableCode.trim()) {
      this.notify.error('Informe o código do aplicativo autenticador.');
      return;
    }
    this.mfaIsBusy.set(true);
    this.mfaService.enable(this.mfaEnableCode.trim()).subscribe({
      next: (data) => {
        this.mfaBackupCodes.set(data.backupCodes);
        this.mfaSetupData.set(null);
        this.mfaQrCodeDataUrl.set('');
        this.mfaEnableCode = '';
        this.mfaEnabled.set(true);
        this.notify.success('MFA ativado com sucesso! Salve seus códigos de backup agora.');
        this.mfaIsBusy.set(false);
      },
      error: (mensagem: any) => {
        this.notify.error(typeof mensagem === 'string' ? mensagem : 'Código inválido.');
        this.mfaIsBusy.set(false);
      }
    });
  }

  fecharBackupCodes() {
    this.mfaBackupCodes.set(null);
  }

  openDisableMfaModal() {
    this.disableMfaPassword = '';
    this.disableMfaCode = '';
    this.showDisableMfaModal.set(true);
  }

  closeDisableMfaModal() {
    this.showDisableMfaModal.set(false);
  }

  confirmarDesativacaoMfa() {
    if (!this.disableMfaPassword || !this.disableMfaCode.trim()) {
      this.notify.error('Preencha a senha e o código.');
      return;
    }
    this.mfaIsBusy.set(true);
    this.mfaService.disable(this.disableMfaPassword, this.disableMfaCode.trim()).subscribe({
      next: () => {
        this.mfaEnabled.set(false);
        this.showDisableMfaModal.set(false);
        this.notify.success('MFA desativado. Todas as sessões ativas foram encerradas — faça login novamente.');
        this.mfaIsBusy.set(false);
        this.authService.logout();
      },
      error: (mensagem: any) => {
        this.notify.error(typeof mensagem === 'string' ? mensagem : 'Não foi possível desativar o MFA.');
        this.mfaIsBusy.set(false);
      }
    });
  }
}
