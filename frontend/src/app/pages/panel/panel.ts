import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, Router } from '@angular/router';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './panel.html',
  styleUrl: './panel.css'
})
export class PanelComponent implements OnInit {
  
  private router = inject(Router);
  private authService = inject(AuthService);

  userRole: string | null = '';

  today: Date = new Date();

  ngOnInit() {
    this.userRole = this.authService.getUserRole();

    if (this.router.url === '/panel' || this.router.url === '/panel/') {
      this.router.navigate(['/panel/dashboard']); // ✅ todos os roles vão para o dashboard
    }
  }

  logout() {
    this.authService.logout(); // Use o método do serviço se existir, ou limpe aqui
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }
}