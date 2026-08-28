// frontend/src/app/app.routes.ts
import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login';
import { PanelComponent } from './pages/panel/panel';
import { ProfessionalsComponent } from './pages/professionals/professionals';
import { authGuard } from './security/auth.guard';
import { roleGuard } from './security/role.guard';
import { Patients } from './pages/patients/patients';
import { Appointments } from './pages/appointments/appointments';
import { Register } from './pages/register/register';
import { MyAppointmentsComponent } from './pages/my-appointments/my-appointments';
import { MyDocumentsComponent } from './pages/my-documents/my-documents';
import { DocumentManagementComponent } from './pages/document-management/document-management';
import { MyProfileComponent } from './pages/my-profile/my-profile';
import { SecuritySettingsComponent } from './pages/security-settings/security-settings';
import { ProfessionalAppointmentsComponent } from './pages/professional-appointments/professional-appointments';
import { MedicalRecordComponent } from './pages/medical-record/medical-record';
import { DashboardComponent } from './pages/dashboard/dashboard'; 
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password';
import { ResetPasswordComponent } from './pages/reset-password/reset-password';
import { VerifyEmail } from './pages/verify-email/verify-email';
import { MyAvailabilityComponent } from './pages/my-availability/my-availability';
import { PrivacyPolicyComponent } from './pages/privacy-policy/privacy-policy';
import { NpsAvaliacaoComponent } from './pages/nps-avaliacao/nps-avaliacao';
import { WaitlistComponent } from './pages/waitlist/waitlist';
import { WaitlistOfertaComponent } from './pages/waitlist-oferta/waitlist-oferta';
import { MfaVerifyComponent } from './pages/mfa-verify/mfa-verify';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: Register },
  { path: 'privacidade', component: PrivacyPolicyComponent },
  { path: 'verify-email', component: VerifyEmail },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'avaliar', component: NpsAvaliacaoComponent },
  { path: 'lista-espera/confirmar', component: WaitlistOfertaComponent },
  // MFA (SEC-02): pré-sessão — sem authGuard (o usuário ainda não tem sessão nesse ponto, só o
  // cookie mfa_pending_token curto emitido por /auth/login quando mfaEnabled=true).
  { path: 'mfa-verify', component: MfaVerifyComponent },
  {
    path: 'panel',
    component: PanelComponent,
    canActivate: [authGuard],
    children: [
      // Telas de ADMIN, PROFESSIONAL e PATIENT ficavam todas navegáveis por URL direta pra
      // qualquer usuário autenticado — o backend (@PreAuthorize) já barrava a chamada de API,
      // mas o componente chegava a renderizar num estado quebrado antes de falhar. roleGuard
      // fecha essa segunda camada, redirecionando pro dashboard antes de montar a tela.
      { path: 'professionals', component: ProfessionalsComponent, canActivate: [roleGuard(['ADMIN'])] },
      { path: 'patients', component: Patients, canActivate: [roleGuard(['ADMIN'])] },
      { path: 'appointments', component: Appointments, canActivate: [roleGuard(['ADMIN'])] },
      { path: 'my-appointments', component: MyAppointmentsComponent, canActivate: [roleGuard(['PATIENT'])] },
      { path: 'my-documents', component: MyDocumentsComponent, canActivate: [roleGuard(['PATIENT'])] },
      { path: 'waitlist', component: WaitlistComponent, canActivate: [roleGuard(['PATIENT'])] },
      { path: 'document-management', component: DocumentManagementComponent, canActivate: [roleGuard(['PROFESSIONAL'])] },
      { path: 'professional-appointments', component: ProfessionalAppointmentsComponent, canActivate: [roleGuard(['PROFESSIONAL'])] },
      { path: 'my-availability', component: MyAvailabilityComponent, canActivate: [roleGuard(['PROFESSIONAL'])] },
      // ADMIN também pode ver o histórico (backend: GET /prontuarios/paciente/{id} aceita
      // ADMIN e PROFESSIONAL) — só a criação de prontuário (POST) é exclusiva do profissional
      // dono da consulta, e isso já é garantido pelo backend independente do que o front mostra.
      { path: 'medical-record/:id', component: MedicalRecordComponent, canActivate: [roleGuard(['PROFESSIONAL', 'ADMIN'])] },
      { path: 'my-profile', component: MyProfileComponent, canActivate: [roleGuard(['PROFESSIONAL', 'PATIENT'])] },
      // ADMIN não tem "meu perfil" nesse sistema — esta tela existe só pra ele conseguir
      // gerenciar o próprio MFA (mesmo <app-mfa-settings> usado dentro de my-profile).
      { path: 'seguranca', component: SecuritySettingsComponent, canActivate: [roleGuard(['ADMIN'])] },
      // dashboard renderiza conteúdo diferente por role internamente — sem restrição de guard.
      { path: 'dashboard', component: DashboardComponent }
    ]
  }
];