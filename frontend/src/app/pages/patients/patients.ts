import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PatientService } from './patients.service';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patients.html',
  styleUrl: './patients.css' // ou .scss
})
export class Patients implements OnInit {
  
  patientsList: any[] = [];
  patientForm: FormGroup;
  
  private patientService = inject(PatientService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  constructor() {
    this.patientForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      cpf: ['', Validators.required],
      healthInsurance: [''] // Opcional ou obrigatório, como preferir
    });
  }

  ngOnInit() {
    this.loadPatients();
  }

  loadPatients() {
    this.patientService.getPatients().subscribe({
      next: (data: any) => {
        this.patientsList = data.content ? data.content : data;
        this.cdr.detectChanges(); // Acorda o Angular para desenhar a tabela!
      },
      error: (err) => console.error('Erro ao buscar pacientes', err)
    });
  }

  onSubmit() {
    if (this.patientForm.valid) {
      this.patientService.createPatient(this.patientForm.value).subscribe({
        next: () => {
          alert('Paciente registado com sucesso!');
          this.patientForm.reset();
          this.loadPatients();
        },
        error: (err) => {
          console.error(err);
          alert('Erro: ' + (err.error?.message || 'Verifique os dados informados.'));
        }
      });
    }
  }
}