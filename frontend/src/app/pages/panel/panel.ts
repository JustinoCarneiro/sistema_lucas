import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // <-- Necessário para o *ngIf funcionar
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { AuthService } from '../../security/auth.service'; // <-- O seu serviço que lê o token

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink], // <-- CommonModule adicionado aqui!
  templateUrl: './panel.html',
  styleUrl: './panel.css'
})
export class PanelComponent implements OnInit {
  
  private router = inject(Router);
  private authService = inject(AuthService); // Injeta o serviço

  userRole: string | null = ''; // <-- A variável que o HTML procura!

  ngOnInit() {
    // Mal o painel abre, descobre quem é o utilizador
    this.userRole = this.authService.getUserRole();

    // 👇 REDIRECIONAMENTO INTELIGENTE 👇
    // Se o utilizador acabou de entrar na raiz do painel (/panel)
    if (this.router.url === '/panel') {
      if (this.userRole === 'PATIENT') {
        // Se for paciente, joga direto para as consultas dele
        this.router.navigate(['/panel/my-appointments']);
      } else if (this.userRole === 'ADMIN') {
        // Se for admin, joga direto para a agenda geral da clínica
        this.router.navigate(['/panel/appointments']); 
      }
    }
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }
}