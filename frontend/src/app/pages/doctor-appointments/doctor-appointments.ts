import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; // 1. IMPORTAÇÃO ADICIONADA

@Component({
  selector: 'app-doctor-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './doctor-appointments.html'
})
export class DoctorAppointmentsComponent implements OnInit {
  private http = inject(HttpClient);
  private router = inject(Router); // 2. INJEÇÃO ADICIONADA
  
  appointments: any[] = [];
  isLoading = true;
  today = new Date();

  ngOnInit() {
    this.fetchTodayAppointments();
  }

  fetchTodayAppointments() {
    this.isLoading = true;
    this.http.get<any[]>('http://localhost:8081/appointments/doctor/today').subscribe({
      next: (data) => {
        this.appointments = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao buscar agenda de hoje:', err);
        this.isLoading = false;
      }
    });
  }

  atender(app: any) {
    // Agora o Angular reconhece o 'this.router'
    this.router.navigate(['/panel/medical-record', app.id]);
  }

  marcarFalta(app: any) {
    // Pegando o nome corretamente do objeto (ajuste se for app.patient.name)
    const nome = app.patientName || (app.patient ? app.patient.name : 'Paciente');
    
    if (confirm(`Confirmar que o paciente ${nome} não compareceu?`)) {
      this.http.patch(`http://localhost:8081/appointments/${app.id}/no-show`, {}).subscribe({
        next: () => {
          alert('Status atualizado: Paciente Faltou.');
          this.fetchTodayAppointments();
        },
        error: (err) => alert('Erro ao registrar falta.')
      });
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'bg-blue-100 text-blue-700 border-blue-200';
      case 'CANCELLED': return 'bg-red-100 text-red-700 border-red-200';
      default: return 'bg-gray-100 text-gray-700';
    }
  }
}