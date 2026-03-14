import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-redefinir-senha',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './redefinir-senha.html'
})
export class RedefinirSenhaComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  private fb = inject(FormBuilder);

  token: string | null = null;
  form: FormGroup;
  isLoading = false;
  sucesso = false;
  erro = '';

  constructor() {
    this.form = this.fb.group({
      novaSenha:        ['', [Validators.required, Validators.minLength(6)]],
      confirmarSenha:   ['', Validators.required]
    }, { validators: this.senhasIguais });
  }

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.erro = 'Link inválido. Solicite um novo.';
    }
  }

  senhasIguais(group: FormGroup) {
    const nova = group.get('novaSenha')?.value;
    const confirmar = group.get('confirmarSenha')?.value;
    return nova === confirmar ? null : { senhasDiferentes: true };
  }

  onSubmit() {
    if (this.form.invalid || !this.token) return;

    this.isLoading = true;
    this.erro = '';

    const payload = {
      token: this.token,
      novaSenha: this.form.value.novaSenha
    };

    this.http.post(`${environment.apiUrl}/auth/redefinir-senha`, payload).subscribe({
      next: () => {
        this.sucesso = true;
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/login']), 3000);
      },
      error: (err: any) => {
        this.erro = err.error?.message || 'Link inválido ou expirado. Solicite um novo.';
        this.isLoading = false;
      }
    });
  }
}