import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../security/auth.service';
import { PatientService } from '../patients/patients.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './panel.html',
  styleUrl: './panel.css'
})
export class PanelComponent implements OnInit {
  
  private router = inject(Router);
  private authService = inject(AuthService);
  private http = inject(HttpClient);
  private patientService = inject(PatientService);

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

  logout() {
    this.authService.logout(); // Use o método do serviço se existir, ou limpe aqui
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }
}