// frontend/src/app/pages/register/register.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  registerForm: FormGroup;
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  constructor() {
    this.registerForm = this.fb.group({
      name:     ['', Validators.required],
      email:    ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      cpf:      ['', Validators.required],
      whatsapp: ['', Validators.required]
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      const { name, email, password, cpf, whatsapp } = this.registerForm.value;
  
      const payload = {
        name,
        email,
        password,
        cpf,
        phone: whatsapp, // ✅ o campo no backend se chama phone
        role: 'PATIENT'
      };
  
      this.authService.registerPatient(payload).subscribe({
        next: () => {
          alert('🎉 Conta criada com sucesso! Faça login para continuar.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          alert('Erro ao criar conta: ' + (err || 'Verifique os dados informados.'));
        }
      });
    }
  }
}