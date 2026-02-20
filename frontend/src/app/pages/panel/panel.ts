import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink,  Router } from '@angular/router';

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  templateUrl: './panel.html', // Cuidado com o nome do arquivo aqui
  styleUrl: './panel.css'
})
export class PanelComponent {
  
  private router = inject(Router);

  logout() {
    // 1. Destrói o crachá (Token)
    localStorage.removeItem('token');
    
    // 2. Manda de volta pro login
    this.router.navigate(['/login']);
  }
}