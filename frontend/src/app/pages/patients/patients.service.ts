// frontend/src/app/pages/patients/patients.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';


@Injectable({ providedIn: 'root' })
export class PatientService {
  private http = inject(HttpClient);

  getPatients() {
    return this.http.get(`${environment.apiUrl}/patients`);
  }

  createPatient(data: any) {
    return this.http.post(`${environment.apiUrl}/patients`, data, {
      responseType: 'text'
    }).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  getMyProfile() {
    return this.http.get<any>(`${environment.apiUrl}/patients/me`);
  }

  updateMyProfile(data: any) {
    return this.http.put(`${environment.apiUrl}/patients/me`, {
      phone:       data.phone,
      insurance:   data.insurance,
      newPassword: data.newPassword || null
    }, { responseType: 'text' }).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  // Adicionar ao patients.service.ts
  deleteMyConta() {
    return this.http.delete(`${environment.apiUrl}/patients/me`);
  }

  // Adicionar ao patients.service.ts
  deletePatient(id: number) {
    return this.http.delete(`${environment.apiUrl}/patients/${id}`).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => this.parseError(err)))
    );
  }

  private parseError(err: HttpErrorResponse): string {
    try {
      const body = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
      return body?.message || 'Erro desconhecido.';
    } catch {
      return err.error || 'Erro desconhecido.';
    }
  }
}