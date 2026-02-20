import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], // <-- Importamos o módulo de formulários
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  
  // Injetando as ferramentas do Angular
  private http = inject(HttpClient);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  constructor() {
    // Criando o nosso formulário com validações
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      // Dispara o POST para o nosso Backend!
      this.http.post('http://localhost:8081/auth/login', this.loginForm.value)
        .subscribe({
          next: (response: any) => {
            localStorage.setItem('token', response.token);
            // alert('Acesso Liberado! Bem-vindo(a)!'); <-- Pode apagar o alert se quiser!
            
            // Leva o usuário logado para dentro do sistema
            this.router.navigate(['/panel']); 
          },
          error: (err) => {
            console.error(err);
            alert('Acesso Negado. Verifique e-mail e senha.');
          }
        });
    } else {
      alert('Preencha os campos corretamente!');
    }
  }
}