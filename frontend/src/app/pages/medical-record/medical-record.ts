import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-medical-record',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './medical-record.html'
})
export class MedicalRecordComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  appointmentId: string | null = '';
  currentPatientName = 'Carregando...';
  newNotes = '';
  history: any[] = [];

  ngOnInit() {
    // Pega o ID da consulta pela URL (Ex: /panel/medical-record/5)
    this.appointmentId = this.route.snapshot.paramMap.get('id');
    this.loadRecordData();
  }

  loadRecordData() {
    this.http.get<any>(`http://localhost:8081/appointments/${this.appointmentId}`).subscribe({
      next: (app) => {
        this.currentPatientName = app.patientName;
        this.loadHistory(app.patientId);
      }
    });
  }

  loadHistory(patientId: number) {
    this.http.get<any[]>(`http://localhost:8081/medical-records/patient/${patientId}`).subscribe({
      next: (data) => this.history = data
    });
  }

  saveMedicalRecord() {
    const payload = {
      appointmentId: this.appointmentId,
      notes: this.newNotes
    };

    this.http.post('http://localhost:8081/medical-records', payload).subscribe({
      next: () => {
        alert('Prontuário salvo com sucesso! Atendimento finalizado.');
        this.router.navigate(['/panel/doctor-appointments']);
      }
    });
  }
}