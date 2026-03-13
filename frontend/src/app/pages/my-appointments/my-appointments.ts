import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppointmentService } from '../appointments/appointment.service';
import { ProfessionalService } from '../professionals/professionals.service';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.css'
})
export class MyAppointmentsComponent implements OnInit {
  private appointmentService = inject(AppointmentService);
  private professionalService = inject(ProfessionalService);
  private fb = inject(FormBuilder);
  
  // Inicializados como arrays vazios para evitar erro de "length"
  myAppointments: any[] = [];
  professionals: any[] = []; 
  
  isLoading: boolean = true;
  isScheduling: boolean = false;
  scheduleForm: FormGroup;

  constructor() {
    this.scheduleForm = this.fb.group({
      professionalId: ['', Validators.required],
      startTime: ['', Validators.required],
      reason: ['']
    });
  }

  ngOnInit() {
    this.loadAppointments();
    this.loadProfessionals();
  }

  loadAppointments() {
    this.isLoading = true;
    this.appointmentService.getMyAppointments().subscribe({
      next: (response: any) => {
        // Suporta tanto o formato Page do Spring quanto List simples
        this.myAppointments = response.content || response || []; 
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao buscar consultas', err);
        this.myAppointments = [];
        this.isLoading = false;
      }
    });
  }

  loadProfessionals() {
    this.professionalService.getProfessionals().subscribe({
      next: (response: any) => {
        this.professionals = response.content || response || [];
      },
      error: (err: any) => console.error('Erro ao buscar profissionais', err)
    });
  }

  openScheduleModal() {
    this.scheduleForm.reset();
    this.isScheduling = true;
  }

  closeScheduleModal() {
    this.isScheduling = false;
  }

  onSubmitSchedule() {
    if (this.scheduleForm.valid) {
      const formValues = this.scheduleForm.value;
      const startStr = formValues.startTime;

      // Cálculo automático: Consulta de 1 hora
      const startDate = new Date(startStr);
      const endDate = new Date(startDate.getTime() + (60 * 60 * 1000));
      
      const pad = (n: number) => n < 10 ? '0' + n : n;
      const endStr = `${endDate.getFullYear()}-${pad(endDate.getMonth() + 1)}-${pad(endDate.getDate())}T${pad(endDate.getHours())}:${pad(endDate.getMinutes())}:00`;

      const payload = {
        professionalId: formValues.professionalId,
        startTime: startStr + ':00',
        endTime: endStr,
        reason: formValues.reason
      };

      this.appointmentService.schedulePatientAppointment(payload).subscribe({
        next: () => {
          alert('🎉 Consulta agendada com sucesso!');
          this.closeScheduleModal();
          this.loadAppointments();
        },
        error: (err) => {
          console.error(err);
          alert('Erro ao agendar: ' + (err.error?.message || 'Horário indisponível.'));
        }
      });
    }
  }

  cancelAppointment(id: number) {
    if (confirm('Tem a certeza que deseja desmarcar esta consulta?')) {
      this.appointmentService.cancelPatientAppointment(id).subscribe({
        next: () => {
          alert('✅ Consulta desmarcada com sucesso!');
          // Atualização reativa na lista
          this.myAppointments = this.myAppointments.map(app => 
            app.id === id ? { ...app, status: 'CANCELLED' } : app
          );
        },
        error: (err) => {
          console.error(err);
          alert('Erro ao desmarcar consulta.');
        }
      });
    }
  }
}