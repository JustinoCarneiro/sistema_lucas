// frontend/src/app/pages/login/login.ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.html'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage = signal('');
  isLoading = signal(false);

  private authService = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  constructor() {
    this.loginForm = this.fb.group({
      email:    ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit() {
    this.errorMessage.set('');

    if (this.loginForm.invalid) {
      this.errorMessage.set('Preencha os campos corretamente.');
      return;
    }

    this.isLoading.set(true);

    this.authService.login(this.loginForm.value).subscribe({
      next: (response: any) => {
        localStorage.setItem('token', response.token);
        this.isLoading.set(false);
        this.router.navigate(['/panel']);
      },
      error: (mensagem: any) => {
        this.errorMessage.set(
          typeof mensagem === 'string'
            ? mensagem
            : 'E-mail ou senha inválidos.'
        );
        this.isLoading.set(false);
      }
    });
  }
}