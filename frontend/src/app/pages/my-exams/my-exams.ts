import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExamService } from './exam.service';

@Component({
  selector: 'app-my-exams',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-exams.html',
  styleUrl: './my-exams.css' // ou .scss
})
export class MyExamsComponent implements OnInit {

  private examService = inject(ExamService);
  
  myExams: any[] = [];
  isLoading: boolean = true;

  ngOnInit() {
    this.loadExams();
  }

  loadExams() {
    this.examService.getMyExams().subscribe({
      next: (response) => {
        this.myExams = response; // O nosso backend já devolve uma Lista direto!
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erro ao buscar exames', err);
        this.isLoading = false;
      }
    });
  }
}