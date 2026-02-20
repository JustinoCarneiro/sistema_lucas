import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login'; 
import { PanelComponent } from './pages/panel/panel'; 
import { DoctorsComponent } from './pages/doctors/doctors'; // <-- 1. Importe a tela de médicos
import { authGuard } from './security/auth.guard'; 

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  
  { 
    path: 'panel', 
    component: PanelComponent, 
    canActivate: [authGuard],
    children: [ // <-- 2. ROTAS FILHAS!
      // Se acessar só /panel, podemos carregar algo ou deixar vazio. 
      // Quando acessar /panel/doctors, carrega a tela de médicos DENTRO do painel
      { path: 'doctors', component: DoctorsComponent }
    ]
  } 
];