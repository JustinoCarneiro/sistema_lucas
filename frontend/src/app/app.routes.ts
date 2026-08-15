// frontend/src/app/app.routes.ts
import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login';
import { PanelComponent } from './pages/panel/panel';
import { ProfessionalsComponent } from './pages/professionals/professionals';
import { authGuard } from './security/auth.guard';
import { Patients } from './pages/patients/patients';
import { Appointments } from './pages/appointments/appointments';
import { Register } from './pages/register/register';
import { MyAppointmentsComponent } from './pages/my-appointments/my-appointments';
import { MyDocumentsComponent } from './pages/my-documents/my-documents';
import { DocumentManagementComponent } from './pages/document-management/document-management';
import { MyProfileComponent } from './pages/my-profile/my-profile';
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
  {
    path: 'panel',
    component: PanelComponent,
    canActivate: [authGuard],
    children: [
      { path: 'professionals', component: ProfessionalsComponent },
      { path: 'patients', component: Patients },
      { path: 'appointments', component: Appointments },
      { path: 'my-appointments', component: MyAppointmentsComponent },
      { path: 'my-documents', component: MyDocumentsComponent },
      { path: 'document-management', component: DocumentManagementComponent },
      { path: 'my-profile', component: MyProfileComponent },
      { path: 'professional-appointments', component: ProfessionalAppointmentsComponent },
      { path: 'my-availability', component: MyAvailabilityComponent },
      { path: 'medical-record/:id', component: MedicalRecordComponent },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'waitlist', component: WaitlistComponent }
    ]
  }
];