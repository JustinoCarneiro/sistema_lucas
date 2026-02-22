import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login'; 
import { PanelComponent } from './pages/panel/panel'; 
import { DoctorsComponent } from './pages/doctors/doctors';
import { authGuard } from './security/auth.guard'; 
import { Patients } from './pages/patients/patients';
import { Appointments } from './pages/appointments/appointments';
import { Register } from './pages/register/register';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: Register },
  
  { 
    path: 'panel', 
    component: PanelComponent, 
    canActivate: [authGuard],
    children: [ // <-- 2. ROTAS FILHAS!
      // Se acessar só /panel, podemos carregar algo ou deixar vazio. 
      // Quando acessar /panel/doctors, carrega a tela de médicos DENTRO do painel
      { path: 'doctors', component: DoctorsComponent },
      { path: 'patients', component: Patients },
      { path: 'appointments', component: Appointments }
    ]
  } 
];