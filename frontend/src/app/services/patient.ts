import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class PatientService {
  
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/patients';

  getPatients() {
    return this.http.get(this.apiUrl);
  }

  createPatient(patientData: any) {
    return this.http.post(this.apiUrl, patientData);
  }
}