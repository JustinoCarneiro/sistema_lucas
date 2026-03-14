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
import { MeusDocumentosComponent } from './pages/meus-documentos/meus-documentos';
import { GestaoDocumentosComponent } from './pages/gestao-documentos/gestao-documentos';
import { MyProfileComponent } from './pages/my-profile/my-profile';
import { ProfessionalAppointmentsComponent } from './pages/professional-appointments/professional-appointments';
import { ProntuarioComponent } from './pages/prontuario/prontuario';
import { DashboardComponent } from './pages/dashboard/dashboard'; 
import { EsqueciSenhaComponent } from './pages/esqueci-senha/esqueci-senha';
import { RedefinirSenhaComponent } from './pages/redefinir-senha/redefinir-senha';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: Register },
  { path: 'esqueci-senha', component: EsqueciSenhaComponent },
  { path: 'redefinir-senha', component: RedefinirSenhaComponent },
  {
    path: 'panel',
    component: PanelComponent,
    canActivate: [authGuard],
    children: [
      { path: 'professionals', component: ProfessionalsComponent },
      { path: 'patients', component: Patients },
      { path: 'appointments', component: Appointments },
      { path: 'my-appointments', component: MyAppointmentsComponent },
      { path: 'meus-documentos', component: MeusDocumentosComponent },
      { path: 'gestao-documentos', component: GestaoDocumentosComponent },
      { path: 'my-profile', component: MyProfileComponent },
      { path: 'professional-appointments', component: ProfessionalAppointmentsComponent },
      { path: 'prontuario/:id', component: ProntuarioComponent },
      { path: 'dashboard', component: DashboardComponent }
    ]
  }
];