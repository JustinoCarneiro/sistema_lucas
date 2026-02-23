import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ExamService {
  private http = inject(HttpClient);

  // Rota cega: não manda o ID, o Java descobre pelo Token!
  getMyExams() {
    return this.http.get<any[]>('http://localhost:8081/exams/me');
  }
}