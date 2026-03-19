// frontend/src/app/pages/patients/patients.ts
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientService } from './patients.service';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './patients.html',
  styleUrl: './patients.css'
})
export class Patients implements OnInit {
  private patientService = inject(PatientService);

  patientsList = signal<any[]>([]);
  selectedItem: any = null;

  openDetails(item: any) {
    this.selectedItem = item;
  }

  closeDetails() {
    this.selectedItem = null;
  }

  ngOnInit() { this.loadPatients(); }

  loadPatients() {
    this.patientService.getPatients().subscribe({
      next: (data: any) => this.patientsList.set(data.content ?? data),
      error: (err: any) => console.error('Erro ao buscar pacientes', err)
    });
  }

  deletePaciente(id: number, nome: string) {
    if (confirm(`Tem certeza que deseja remover o paciente ${nome}? Esta ação não pode ser desfeita.`)) {
      this.patientService.deletePatient(id).subscribe({
        next: () => { alert('Paciente removido com sucesso!'); this.loadPatients(); },
        error: (msg: string) => alert('Erro: ' + msg)
      });
    }
  }
}