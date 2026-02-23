import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../security/auth.service'; // <-- Importamos o nosso serviço!

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.css' // ou .scss
})
export class Register {
  
  registerForm: FormGroup;
  
  private authService = inject(AuthService); // <-- Injetamos o Serviço
  private fb = inject(FormBuilder);
  private router = inject(Router);

  constructor() {
    this.registerForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      cpf: ['', Validators.required],
      whatsapp: ['', Validators.required]
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      // Usamos o serviço para fazer o registo!
      this.authService.registerPatient(this.registerForm.value).subscribe({
        next: () => {
          alert('🎉 Conta criada com sucesso! Faça login para continuar.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          console.error(err);
          alert('Erro ao criar conta: ' + (err.error?.message || 'Verifique os dados informados.'));
        }
      });
    }
  }
}