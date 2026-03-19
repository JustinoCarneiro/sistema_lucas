import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../security/auth.service';

@Component({
  selector: 'app-verification-banner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './verification-banner.html',
  styleUrl: './verification-banner.css'
})
export class VerificationBanner {
  private authService = inject(AuthService);

  showBanner(): boolean {
    return this.authService.isAuthenticated() && !this.authService.isVerified();
  }
}
