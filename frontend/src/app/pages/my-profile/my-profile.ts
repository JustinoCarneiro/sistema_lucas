// frontend/src/app/pages/my-profile/my-profile.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; // ✅ importado mas NÃO vai no imports do @Component
import { PatientService } from '../patients/patients.service';
import { AuthService } from '../../security/auth.service';
import { MfaService } from '../../security/mfa.service';
import { environment } from '../../../environments/environment';
import { NotificationService } from '../../notification.service';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, FormsModule], // ✅ Router removido daqui
  templateUrl: './my-profile.html'
})
export class MyProfileComponent implements OnInit {
  private patientService = inject(PatientService);
  private authService = inject(AuthService);
  private mfaService = inject(MfaService);
  private http = inject(HttpClient);
  private notify = inject(NotificationService);
  private router = inject(Router); // ✅ injetado corretamente via inject()

  // --- MFA (SEC-02) ---
  mfaSetupData = signal<{ secretBase32: string; otpAuthUri: string } | null>(null);
  mfaQrCodeDataUrl = signal<string>('');
  mfaEnableCode = '';
  mfaBackupCodes = signal<string[] | null>(null);
  mfaIsBusy = signal(false);
  showDisableMfaModal = signal(false);
  disableMfaPassword = '';
  disableMfaCode = '';

  profile = signal<any>({});
  userRole = signal<string | null>(null);
  isLoading = signal(true);
  isSaving = signal(false);
  newPassword = '';
  initialEmail = '';

  // Controle do Modal de Senha
  showPasswordModal = signal(false);
  modalNewPassword = '';
  modalConfirmPassword = '';

  passwordMismatch(): boolean {
    return this.modalNewPassword !== this.modalConfirmPassword && this.modalConfirmPassword.length > 0;
  }

  isPasswordValid(): boolean {
    return this.modalNewPassword.length >= 6 && this.modalNewPassword === this.modalConfirmPassword;
  }

  openPasswordModal() {
    this.modalNewPassword = '';
    this.modalConfirmPassword = '';
    this.showPasswordModal.set(true);
  }

  closePasswordModal() {
    this.showPasswordModal.set(false);
  }

  confirmPasswordChange() {
    if (!this.isPasswordValid()) return;
    this.newPassword = this.modalNewPassword;
    this.closePasswordModal();
    this.saveProfile(); // Salva IMEDIATAMENTE a nova senha junto com o perfil
  }

  ngOnInit() {
    this.userRole.set(this.authService.getUserRole());
    this.loadData();
  }

  loadData() {
    if (this.userRole() === 'PROFESSIONAL') {
      this.http.get(`${environment.apiUrl}/professionals/me`).subscribe({
        next: (data: any) => { 
          this.profile.set(data); 
          this.initialEmail = data.email;
          this.isLoading.set(false); 
        },
        error: () => this.isLoading.set(false)
      });
    } else {
      this.patientService.getMyProfile().subscribe({
        next: (data: any) => { 
          this.profile.set(data); 
          this.initialEmail = data.email;
          this.isLoading.set(false); 
        },
        error: () => this.isLoading.set(false)
      });
    }
  }

  updateProfile(field: string, value: any) {
    if (field === 'phone' || field === 'emergencyContactPhone') {
      value = this.applyPhoneMask(value);
    } else if (field === 'cpf') {
      value = this.applyCpfMask(value);
    }
    this.profile.update(p => ({ ...p, [field]: value }));
  }

  applyPhoneMask(val: string): string {
    if (!val) return '';
    let v = val.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    if (v.length > 10) return v.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
    if (v.length > 6) return v.replace(/^(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
    if (v.length > 2) return v.replace(/^(\d{2})(\d{0,5})/, '($1) $2');
    if (v.length > 0) return v.replace(/^(\d*)/, '($1');
    return v;
  }

  applyCpfMask(val: string): string {
    if (!val) return '';
    let v = val.replace(/\D/g, '');
    if (v.length > 11) v = v.substring(0, 11);
    if (v.length > 9) return v.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4');
    if (v.length > 6) return v.replace(/^(\d{3})(\d{3})(\d{0,3})/, '$1.$2.$3');
    if (v.length > 3) return v.replace(/^(\d{3})(\d{0,3})/, '$1.$2');
    return v;
  }

  saveProfile() {
    this.isSaving.set(true);

    if (this.userRole() === 'PROFESSIONAL') {
      const p = this.profile();
      const payload = {
        name:             p.name,
        email:            p.email,
        tipoRegistro:     p.tipoRegistro,
        registroConselho: p.registroConselho,
        specialty:        p.specialty,
        cpf:              p.cpf,
        phone:            p.phone,
        birthDate:        p.birthDate || null,
        gender:           p.gender || null,
        address:                p.address || null,
        modalidadeAtendimento:  p.modalidadeAtendimento || 'PRESENCIAL',
        newPassword:            this.newPassword || null
      };
      this.http.put(`${environment.apiUrl}/professionals/me`, payload, { responseType: 'text' }).subscribe({
        next: () => { 
          if (p.email !== this.initialEmail) {
            this.notify.success('E-mail atualizado com sucesso! Por favor, faça login novamente com seu novo e-mail.');
            this.authService.logout();
            return;
          }
          this.notify.success('Perfil atualizado com sucesso!');
          this.newPassword = ''; 
          this.isSaving.set(false); 
        },
        error: (err: any) => { this.notify.error(err.error?.message || 'Tente novamente.'); this.isSaving.set(false); }
      });
    } else {
      const p = this.profile();
      const payload = { 
        name: p.name,
        email: p.email,
        cpf: p.cpf,
        phone: p.phone,
        birthDate: p.birthDate || null,
        emergencyContactName: p.emergencyContactName || null,
        emergencyContactPhone: p.emergencyContactPhone || null,
        gender: p.gender || null,
        allergies: p.allergies || null,
        address: p.address || null,
        newPassword: this.newPassword || null 
      };
      this.patientService.updateMyProfile(payload).subscribe({
        next: () => { 
          if (p.email !== this.initialEmail) {
            this.notify.success('E-mail atualizado com sucesso! Por favor, faça login novamente com seu novo e-mail.');
            this.authService.logout();
            return;
          }
          this.notify.success('Perfil atualizado com sucesso!');
          this.newPassword = ''; 
          this.isSaving.set(false); 
        },
        error: (err: any) => { this.notify.error(err.error?.message || 'Tente novamente.'); this.isSaving.set(false); }
      });
    }
  }

  // --- MFA (SEC-02) ---

  iniciarSetupMfa() {
    this.mfaIsBusy.set(true);
    this.mfaService.setup().subscribe({
      next: async (data) => {
        this.mfaSetupData.set(data);
        // Renderiza o QR code no cliente (canvas → data URL) a partir da otpauth:// URI — o
        // secret nunca precisa ser enviado como imagem pelo backend.
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
        this.profile.update(p => ({ ...p, mfaEnabled: true }));
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
        this.profile.update(p => ({ ...p, mfaEnabled: false }));
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

  excluirConta() {
    if (confirm('Tem certeza que deseja excluir sua conta? Esta ação não pode ser desfeita e todos os seus dados serão removidos permanentemente.')) {
      this.patientService.deleteMyConta().subscribe({
        next: () => {
          this.notify.success('Conta excluída com sucesso.');
          this.authService.logout();
        },
        error: (err: any) => this.notify.error('Erro ao excluir conta: ' + (err.error?.message || 'Tente novamente.'))
      });
    }
  }
}