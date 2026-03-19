import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { VerificationBanner } from './components/verification-banner/verification-banner';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, VerificationBanner],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('sistema-lucas-ui');
}
