// frontend/src/app/pages/my-profile/my-profile.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { PatientService } from '../patients/patients.service';
import { AuthService } from '../../security/auth.service';
import { environment } from '../../../environments/environment';

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
  isSaving = false;
  newPassword = '';

  ngOnInit() {
    this.userRole = this.authService.getUserRole();
    this.loadData();
  }

  loadData() {
    if (this.userRole === 'PROFESSIONAL') {
      this.http.get(`${environment.apiUrl}/professionals/me`).subscribe({
        next: (data) => { this.profile = data; this.isLoading = false; },
        error: () => this.isLoading = false
      });
    } else {
      this.patientService.getMyProfile().subscribe({
        next: (data) => { this.profile = data; this.isLoading = false; },
        error: () => this.isLoading = false
      });
    }
  }

  saveProfile() {
    this.isSaving = true;

    if (this.userRole === 'PROFESSIONAL') {
      const payload = {
        tipoRegistro:     this.profile.tipoRegistro,
        registroConselho: this.profile.registroConselho,
        specialty:        this.profile.specialty,
        newPassword:      this.newPassword || null
      };
      this.http.put(`${environment.apiUrl}/professionals/me`, payload).subscribe({
        next: () => {
          alert('Perfil atualizado com sucesso!');
          this.newPassword = '';
          this.isSaving = false;
        },
        error: (err: any) => {
          alert('Erro: ' + (err.error?.message || 'Tente novamente.'));
          this.isSaving = false;
        }
      });
    } else {
      const payload = {
        phone:       this.profile.phone,
        insurance:   this.profile.insurance,
        newPassword: this.newPassword || null
      };
      this.patientService.updateMyProfile(payload).subscribe({
        next: () => {
          alert('Perfil atualizado com sucesso!');
          this.newPassword = '';
          this.isSaving = false;
        },
        error: (err: any) => {
          alert('Erro: ' + (err.error?.message || 'Tente novamente.'));
          this.isSaving = false;
        }
      });
    }
  }
}