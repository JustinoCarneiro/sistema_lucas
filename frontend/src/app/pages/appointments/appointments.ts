import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppointmentService } from '../../services/appointment';
import { DoctorService } from '../../services/doctor';   // <-- Importa o serviço de Médicos
import { PatientService } from '../../services/patient'; // <-- Importa o serviço de Pacientes

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './appointments.html',
  styleUrl: './appointments.css'
})
export class Appointments implements OnInit {
  
  appointmentsList: any[] = [];
  doctorsList: any[] = [];
  patientsList: any[] = [];
  
  appointmentForm: FormGroup;
  
  private appointmentService = inject(AppointmentService);
  private doctorService = inject(DoctorService);
  private patientService = inject(PatientService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  constructor() {
    this.appointmentForm = this.fb.group({
      doctorId: ['', Validators.required],
      patientId: ['', Validators.required],
      startTime: ['', Validators.required],
      endTime: ['', Validators.required],
      reason: ['']
    });
  }

  ngOnInit() {
    this.loadInitialData();
  }

  // Carrega tudo ao mesmo tempo!
  loadInitialData() {
    this.loadAppointments();
    this.loadDoctors();
    this.loadPatients();
  }

  loadAppointments() {
    this.appointmentService.getAppointments().subscribe({
      next: (data: any) => {
        this.appointmentsList = data.content ? data.content : data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erro ao buscar consultas', err)
    });
  }

  loadDoctors() {
    this.doctorService.getDoctors().subscribe({
      next: (data: any) => {
        this.doctorsList = data.content ? data.content : data;
        this.cdr.detectChanges();
      }
    });
  }

  loadPatients() {
    this.patientService.getPatients().subscribe({
      next: (data: any) => {
        this.patientsList = data.content ? data.content : data;
        this.cdr.detectChanges();
      }
    });
  }

  onSubmit() {
    if (this.appointmentForm.valid) {
      this.appointmentService.createAppointment(this.appointmentForm.value).subscribe({
        next: () => {
          alert('Consulta marcada com sucesso!');
          this.appointmentForm.reset();
          this.loadAppointments(); // Atualiza a tabela
        },
        error: (err) => {
          console.error(err);
          // O nosso backend envia mensagens de erro muito boas (ex: conflito de horário)
          alert('Erro: ' + (err.error?.message || 'Não foi possível marcar a consulta.'));
        }
      });
    }
  }
}