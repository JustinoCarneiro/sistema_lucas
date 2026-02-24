import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-doctor-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './doctor-appointments.html'
})
export class DoctorAppointmentsComponent implements OnInit {
  private http = inject(HttpClient);
  
  appointments: any[] = [];
  isLoading = true;

  ngOnInit() {
    this.fetchAppointments();
  }

  fetchAppointments() {
    this.isLoading = true;
    // O endpoint /doctor-me foi o que definimos no Controller do Java
    this.http.get<any[]>('http://localhost:8081/appointments/doctor-me').subscribe({
      next: (data) => {
        this.appointments = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao buscar agenda do médico:', err);
        this.isLoading = false;
      }
    });
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'SCHEDULED': 'Agendado',
      'COMPLETED': 'Concluído',
      'CANCELLED': 'Cancelado'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'bg-blue-100 text-blue-700 border-blue-200';
      case 'COMPLETED': return 'bg-green-100 text-green-700 border-green-200';
      case 'CANCELLED': return 'bg-red-100 text-red-700 border-red-200';
      default: return 'bg-gray-100 text-gray-700';
    }
  }
}