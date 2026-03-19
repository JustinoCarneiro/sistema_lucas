import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './verify-email.html',
  styleUrl: './verify-email.css'
})
export class VerifyEmail implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  message = signal('Verificando seu e-mail...');
  isSuccess = signal(false);
  isError = signal(false);

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.message.set('Token de verificação não encontrado.');
      this.isError.set(true);
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (res) => {
        this.message.set(res);
        this.isSuccess.set(true);
      },
      error: (err) => {
        this.message.set(err.error || 'Erro ao verificar e-mail. O link pode ter expirado.');
        this.isError.set(true);
      }
    });
  }
}
