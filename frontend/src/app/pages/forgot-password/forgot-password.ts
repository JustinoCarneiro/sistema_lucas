// frontend/src/app/pages/forgot-password/forgot-password.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.html'
})
export class ForgotPasswordComponent {
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  form: FormGroup;
  isLoading = signal(false);
  enviado = signal(false);

  constructor() {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit() {
    if (this.form.invalid) return;

    this.isLoading.set(true);

    this.http.post(`${environment.apiUrl}/auth/esqueci-senha`, this.form.value, {
      responseType: 'text'
    }).subscribe({
      next: () => {
        this.enviado.set(true);
        this.isLoading.set(false);
      },
      error: () => {
        // Mesmo em erro mostramos sucesso — não revelamos se o e-mail existe
        this.enviado.set(true);
        this.isLoading.set(false);
      }
    });
  }
}