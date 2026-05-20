import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './privacy-policy.html'
})
export class PrivacyPolicyComponent {
  // Deve refletir a versão configurada no backend (app.lgpd.terms-version).
  versao = '1.0';
}
