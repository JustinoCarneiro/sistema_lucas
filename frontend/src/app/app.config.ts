import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http'; // <-- Importe o withInterceptors
import { authInterceptor } from './security/auth.interceptor'; // <-- Importe o seu Interceptor

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    // Adiciona o Interceptor na frota de carteiros do Angular
    provideHttpClient(withInterceptors([authInterceptor])) 
  ]
};