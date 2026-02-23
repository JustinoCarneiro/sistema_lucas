import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientService } from '../patients/patients.service';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-profile.html'
})
export class MyProfileComponent implements OnInit {
  private patientService = inject(PatientService);

  profile: any = {
    name: '',
    email: '',
    cpf: '',
    whatsapp: '',
    healthInsurance: ''
  };

  newPassword = '';
  isLoading = true;

  ngOnInit() {
    this.patientService.getMyProfile().subscribe({
      next: (data) => {
        this.profile = data;
        this.isLoading = false;
      },
      error: (err) => console.error('Erro ao carregar perfil', err)
    });
  }

  saveProfile() {
    const payload = {
      whatsapp: this.profile.whatsapp,
      healthInsurance: this.profile.healthInsurance,
      newPassword: this.newPassword
    };

    this.patientService.updateMyProfile(payload).subscribe({
      next: () => {
        alert('Perfil atualizado com sucesso! ✅');
        this.newPassword = ''; // Limpa o campo de senha
      },
      error: (err) => alert('Erro ao atualizar perfil')
    });
  }
}