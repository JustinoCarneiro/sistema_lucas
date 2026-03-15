// frontend/src/app/pages/my-profile/my-profile.ts
import { Component, inject, OnInit, signal } from '@angular/core';
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

  profile = signal<any>({});
  userRole = signal<string | null>(null);
  isLoading = signal(true);
  isSaving = signal(false);
  newPassword = '';

  ngOnInit() {
    this.userRole.set(this.authService.getUserRole());
    this.loadData();
  }

  loadData() {
    if (this.userRole() === 'PROFESSIONAL') {
      this.http.get(`${environment.apiUrl}/professionals/me`).subscribe({
        next: (data) => { this.profile.set(data); this.isLoading.set(false); },
        error: () => this.isLoading.set(false)
      });
    } else {
      this.patientService.getMyProfile().subscribe({
        next: (data) => { this.profile.set(data); this.isLoading.set(false); },
        error: () => this.isLoading.set(false)
      });
    }
  }

  updateProfile(key: string, value: any) {
    this.profile.update(p => ({ ...p, [key]: value }));
  }

  saveProfile() {
    this.isSaving.set(true);

    if (this.userRole() === 'PROFESSIONAL') {
      const p = this.profile();
      const payload = {
        tipoRegistro:     p.tipoRegistro,
        registroConselho: p.registroConselho,
        specialty:        p.specialty,
        newPassword:      this.newPassword || null
      };
      this.http.put(`${environment.apiUrl}/professionals/me`, payload).subscribe({
        next: () => { alert('Perfil atualizado com sucesso!'); this.newPassword = ''; this.isSaving.set(false); },
        error: (err: any) => { alert('Erro: ' + (err.error?.message || 'Tente novamente.')); this.isSaving.set(false); }
      });
    } else {
      const p = this.profile();
      const payload = { phone: p.phone, insurance: p.insurance, newPassword: this.newPassword || null };
      this.patientService.updateMyProfile(payload).subscribe({
        next: () => { alert('Perfil atualizado com sucesso!'); this.newPassword = ''; this.isSaving.set(false); },
        error: (err: any) => { alert('Erro: ' + (err.error?.message || 'Tente novamente.')); this.isSaving.set(false); }
      });
    }
  }
}