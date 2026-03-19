// frontend/src/app/pages/my-profile/my-profile.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; // ✅ importado mas NÃO vai no imports do @Component
import { PatientService } from '../patients/patients.service';
import { AuthService } from '../../security/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, FormsModule], // ✅ Router removido daqui
  templateUrl: './my-profile.html'
})
export class MyProfileComponent implements OnInit {
  private patientService = inject(PatientService);
  private authService = inject(AuthService);
  private http = inject(HttpClient);
  private router = inject(Router); // ✅ injetado corretamente via inject()

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
        address:          p.address || null,
        newPassword:      this.newPassword || null
      };
      this.http.put(`${environment.apiUrl}/professionals/me`, payload, { responseType: 'text' }).subscribe({
        next: () => { 
          if (p.email !== this.initialEmail) {
            alert('E-mail atualizado com sucesso! Por favor, faça login novamente com seu novo e-mail.');
            this.authService.logout();
            return;
          }
          alert('Perfil atualizado com sucesso!'); 
          this.newPassword = ''; 
          this.isSaving.set(false); 
        },
        error: (err: any) => { alert('Erro: ' + (err.error?.message || 'Tente novamente.')); this.isSaving.set(false); }
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
            alert('E-mail atualizado com sucesso! Por favor, faça login novamente com seu novo e-mail.');
            this.authService.logout();
            return;
          }
          alert('Perfil atualizado com sucesso!'); 
          this.newPassword = ''; 
          this.isSaving.set(false); 
        },
        error: (err: any) => { alert('Erro: ' + (err.error?.message || 'Tente novamente.')); this.isSaving.set(false); }
      });
    }
  }

  excluirConta() {
    if (confirm('Tem certeza que deseja excluir sua conta? Esta ação não pode ser desfeita e todos os seus dados serão removidos permanentemente.')) {
      this.patientService.deleteMyConta().subscribe({
        next: () => {
          alert('Conta excluída com sucesso.');
          this.authService.logout();
        },
        error: (err: any) => alert('Erro ao excluir conta: ' + (err.error?.message || 'Tente novamente.'))
      });
    }
  }
}