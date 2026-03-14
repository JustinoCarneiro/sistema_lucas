// frontend/src/app/pages/esqueci-senha/esqueci-senha.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-esqueci-senha',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './esqueci-senha.html'
})
export class EsqueciSenhaComponent {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  form: FormGroup;
  isLoading = false;
  enviado = false;

  constructor() {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit() {
    if (this.form.invalid) return;

    this.isLoading = true;

    this.http.post(`${environment.apiUrl}/auth/esqueci-senha`, this.form.value).subscribe({
      next: () => {
        this.enviado = true;
        this.isLoading = false;
      },
      error: () => {
        // Mesmo em erro mostramos sucesso — não revelamos se o e-mail existe
        this.enviado = true;
        this.isLoading = false;
      }
    });
  }
}