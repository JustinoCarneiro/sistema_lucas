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
import { MyExamsComponent } from './pages/my-exams/my-exams';
import { MyProfileComponent } from './pages/my-profile/my-profile';
import { ProfessionalAppointmentsComponent } from './pages/professional-appointments/professional-appointments';
import { ProntuarioComponent } from './pages/prontuario/prontuario';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: Register },
  {
    path: 'panel',
    component: PanelComponent,
    canActivate: [authGuard],
    children: [
      { path: 'professionals', component: ProfessionalsComponent },
      { path: 'patients', component: Patients },
      { path: 'appointments', component: Appointments },
      { path: 'my-appointments', component: MyAppointmentsComponent },
      { path: 'my-exams', component: MyExamsComponent },
      { path: 'my-profile', component: MyProfileComponent },
      { path: 'professional-appointments', component: ProfessionalAppointmentsComponent },
      { path: 'professional-schedule', component: ProfessionalAppointmentsComponent },
      { path: 'prontuario/:id', component: ProntuarioComponent } // ✅ atualizado
    ]
  }
];