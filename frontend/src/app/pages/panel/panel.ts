import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../security/auth.service';
import { InactivityService } from '../../security/inactivity.service';
import { PatientService } from '../patients/patients.service';
import { ThemeService } from '../../theme.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './panel.html',
  styleUrl: './panel.css'
})
export class PanelComponent implements OnInit, OnDestroy {

  private router = inject(Router);
  private authService = inject(AuthService);
  private inactivityService = inject(InactivityService);
  private http = inject(HttpClient);
  private patientService = inject(PatientService);
  readonly theme = inject(ThemeService);

  userRole: string | null = '';
  userName = signal<string>('Carregando...');
  userRoleLabel = signal<string>('');

  today: Date = new Date();
  isSidebarOpen = false;

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  closeSidebar() {
    this.isSidebarOpen = false;
  }

  ngOnInit() {
    // SEC-06: inicia o timeout de inatividade ao entrar na área autenticada.
    this.inactivityService.start();

    this.userRole = this.authService.getUserRole();

    if (this.userRole === 'PROFESSIONAL') {
      this.userRoleLabel.set('Profissional');
      this.http.get<any>(`${environment.apiUrl}/professionals/me`).subscribe({
        next: (res) => this.userName.set(res.name),
        error: () => this.userName.set('Profissional')
      });
    } else if (this.userRole === 'PATIENT') {
      this.userRoleLabel.set('Paciente');
      this.patientService.getMyProfile().subscribe({
        next: (res: any) => this.userName.set(res.name),
        error: () => this.userName.set('Paciente')
      });
    } else {
      this.userRoleLabel.set('Admin');
      this.userName.set('Administrador');
    }

    if (this.router.url === '/panel' || this.router.url === '/panel/') {
      this.router.navigate(['/panel/dashboard']); // ✅ todos os roles vão para o dashboard
    }
  }

  ngOnDestroy() {
    // SEC-06: encerra o monitoramento ao sair da área autenticada.
    this.inactivityService.stop();
  }

  logout() {
    // SEC-01/SEC-06: o AuthService revoga a sessão no backend e redireciona.
    this.inactivityService.stop();
    this.authService.logout();
  }
}