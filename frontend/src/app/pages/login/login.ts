import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../security/auth.service';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.html'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = ''; // Variável para exibir erro na tela

  private authService = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.authService.login(this.loginForm.value) 
        .subscribe({
          next: (response: any) => {
            localStorage.setItem('token', response.token);
            this.router.navigate(['/panel']); 
          },
          error: (msg: string) => {
            // 'msg' agora vem mastigada pelo catchError do AuthService
            this.errorMessage = msg; 
            console.error('Falha no login:', msg);
          }
        });
    } else {
      this.errorMessage = 'Preencha os campos corretamente!';
    }
  }
}