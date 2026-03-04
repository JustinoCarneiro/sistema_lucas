import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms'; // <-- Importes de Formulário
import { AppointmentService } from '../appointments/appointment.service';
import { ProfessionalService } from '../professionals/professional.service'; // <-- Precisamos do serviço de médicos!

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], // <-- Adicione o ReactiveFormsModule aqui
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.css'
})
export class MyAppointmentsComponent implements OnInit {

  private appointmentService = inject(AppointmentService);
  private professionalService = inject(ProfessionalService);
  private fb = inject(FormBuilder);
  
  myAppointments: any[] = [];
  professionals: any[] = []; // Guarda a lista de médicos
  
  isLoading: boolean = true;
  isScheduling: boolean = false; // Controla se a janela do formulário está aberta
  
  scheduleForm: FormGroup;

  constructor() {
    // Monta a estrutura do formulário
    this.scheduleForm = this.fb.group({
      professionalId: ['', Validators.required],
      startTime: ['', Validators.required], // Usaremos um campo de Data/Hora
      reason: ['']
    });
  }

  ngOnInit() {
    this.loadAppointments();
    this.loadProfessionals(); // Carrega os médicos logo que a tela abre
  }

  loadAppointments() {
    this.isLoading = true;
    this.appointmentService.getMyAppointments().subscribe({
      next: (response) => {
        this.myAppointments = response.content; 
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao buscar consultas', err);
        this.isLoading = false;
      }
    });
  }

  loadProfessionals() {
    // Assume que tem um método getProfessionals() no seu ProfessionalService.
    // Se for diferente (ex: listAll), ajuste o nome abaixo!
    this.professionalService.getProfessionals().subscribe({
      next: (response: any) => {
        // Dependendo de como o seu Spring Boot devolve, pode ser 'response' direto ou 'response.content'
        this.professionals = response.content || response;
      },
      error: (err) => console.error('Erro ao buscar médicos', err)
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
      const startStr = formValues.startTime; // Vem do HTML no formato: "YYYY-MM-DDTHH:mm"

      // Truque de Mestre: Como a consulta dura 1 hora, calculamos o fim automaticamente!
      const startDate = new Date(startStr);
      const endDate = new Date(startDate.getTime() + (60 * 60 * 1000)); // Soma 1 hora
      
      // Formata a data de fim para "YYYY-MM-DDTHH:mm:00"
      const pad = (n: number) => n < 10 ? '0' + n : n;
      const endStr = `${endDate.getFullYear()}-${pad(endDate.getMonth() + 1)}-${pad(endDate.getDate())}T${pad(endDate.getHours())}:${pad(endDate.getMinutes())}:00`;

      // Monta a "encomenda" para o Java
      const payload = {
        professionalId: formValues.professionalId,
        startTime: startStr + ':00', // Adiciona os segundos para o Java não reclamar
        endTime: endStr,
        reason: formValues.reason
      };

      this.appointmentService.schedulePatientAppointment(payload).subscribe({
        next: () => {
          alert('🎉 Consulta agendada com sucesso!');
          this.closeScheduleModal();
          this.loadAppointments(); // Atualiza a tabela na hora!
        },
        error: (err) => {
          console.error(err);
          alert('Erro ao agendar: ' + (err.error?.message || 'Verifique se o médico está disponível neste horário.'));
        }
      });
    }
  }

  cancelAppointment(id: number) {
    if (confirm('Tem a certeza que deseja desmarcar esta consulta? Esta ação não pode ser desfeita.')) {
      
      this.appointmentService.cancelPatientAppointment(id).subscribe({
        next: () => {
          alert('✅ Consulta desmarcada com sucesso!');
          
          // ✨ O TRUQUE DE MESTRE: Atualiza o status diretamente na memória!
          const consultaCancelada = this.myAppointments.find(app => app.id === id);
          if (consultaCancelada) {
            consultaCancelada.status = 'CANCELLED';
          }
          
        },
        error: (err) => {
          console.error(err);
          alert('Erro ao desmarcar consulta. Tente novamente mais tarde.');
        }
      });
      
    }
  }
}