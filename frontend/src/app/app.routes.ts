import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login'; 
import { PanelComponent } from './pages/panel/panel'; 
import { DoctorsComponent } from './pages/doctors/doctors';
import { authGuard } from './security/auth.guard'; 
import { Patients } from './pages/patients/patients';
import { Appointments } from './pages/appointments/appointments';
import { Register } from './pages/register/register';
import { MyAppointmentsComponent } from './pages/my-appointments/my-appointments';
import { MyExamsComponent } from './pages/my-exams/my-exams';
import { MyProfileComponent } from './pages/my-profile/my-profile';
import { DoctorAppointmentsComponent } from './pages/doctor-appointments/doctor-appointments';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: Register },
  
  { 
    path: 'panel', 
    component: PanelComponent, 
    canActivate: [authGuard],
    children: [
      { path: 'doctors', component: DoctorsComponent },
      { path: 'patients', component: Patients },
      { path: 'appointments', component: Appointments },
      { path: 'my-appointments', component: MyAppointmentsComponent },
      { path: 'my-exams', component: MyExamsComponent },
      { path: 'my-profile', component: MyProfileComponent },
      
      // CONFIGURAÇÃO DAS ROTAS DO MÉDICO (Remova os comentários!)
      { path: 'doctor-appointments', component: DoctorAppointmentsComponent },
      { path: 'doctor-schedule', component: DoctorAppointmentsComponent }, // Temporário se não tiver o componente criado
    ]
  } 
];