// frontend/src/app/pages/security-settings/security-settings.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MfaSettingsComponent } from '../../security/mfa-settings/mfa-settings';

// Tela dedicada pro ADMIN (que não tem "meu perfil" nesse sistema — Patient/Professional têm,
// ADMIN não) conseguir gerenciar o próprio MFA. PROFESSIONAL/PATIENT continuam usando a seção
// dentro de my-profile; esta tela existe só pra fechar o buraco de acesso do ADMIN.
@Component({
  selector: 'app-security-settings',
  standalone: true,
  imports: [CommonModule, MfaSettingsComponent],
  templateUrl: './security-settings.html'
})
export class SecuritySettingsComponent {}
