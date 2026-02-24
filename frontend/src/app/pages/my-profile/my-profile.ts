import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientService } from '../patients/patients.service';
import { AuthService } from '../../security/auth.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-profile.html'
})
export class MyProfileComponent implements OnInit {
  private patientService = inject(PatientService);
  private authService = inject(AuthService);
  private http = inject(HttpClient);

  profile: any = {};
  userRole: string | null = '';
  isLoading = true;
  newPassword = '';

  ngOnInit() {
    this.userRole = this.authService.getUserRole();
    this.loadData();
  }

  loadData() {
    // Lógica inteligente: se for médico, não chama o patientService
    if (this.userRole === 'DOCTOR') {
      this.http.get('http://localhost:8081/doctors/me').subscribe({
        next: (data) => {
          this.profile = data;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Erro ao carregar perfil médico', err);
          this.isLoading = false;
        }
      });
    } else {
      this.patientService.getMyProfile().subscribe({
        next: (data) => {
          this.profile = data;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Erro ao carregar perfil paciente', err);
          this.isLoading = false;
        }
      });
    }
  }

  saveProfile() {
    // Implementaremos a edição de médico no futuro se necessário
    if (this.userRole === 'DOCTOR') {
      alert('Edição de perfil para médicos será implementada no Card 20! 🛠️');
      return;
    }

    const payload = {
      whatsapp: this.profile.whatsapp,
      healthInsurance: this.profile.healthInsurance,
      newPassword: this.newPassword
    };

    this.patientService.updateMyProfile(payload).subscribe({
      next: () => {
        alert('Perfil atualizado! ✅');
        this.newPassword = '';
      },
      error: (err) => alert('Erro ao atualizar')
    });
  }
}