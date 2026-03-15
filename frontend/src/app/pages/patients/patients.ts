// frontend/src/app/pages/patients/patients.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PatientService } from './patients.service';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patients.html',
  styleUrl: './patients.css'
})
export class Patients implements OnInit {
  private patientService = inject(PatientService);
  private fb = inject(FormBuilder);

  patientsList = signal<any[]>([]);
  patientForm: FormGroup;

  constructor() {
    this.patientForm = this.fb.group({
      name:            ['', Validators.required],
      email:           ['', [Validators.required, Validators.email]],
      password:        ['', Validators.required],
      cpf:             ['', Validators.required],
      healthInsurance: ['']
    });
  }

  ngOnInit() { this.loadPatients(); }

  loadPatients() {
    this.patientService.getPatients().subscribe({
      next: (data: any) => this.patientsList.set(data.content ?? data),
      error: (err: any) => console.error('Erro ao buscar pacientes', err)
    });
  }

  onSubmit() {
    if (this.patientForm.valid) {
      this.patientService.createPatient(this.patientForm.value).subscribe({
        next: () => { alert('Paciente cadastrado com sucesso!'); this.patientForm.reset(); this.loadPatients(); },
        error: (err: any) => alert('Erro: ' + (err.error?.message || 'Verifique os dados.'))
      });
    }
  }
}