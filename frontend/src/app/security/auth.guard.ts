import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';
// Uma função simples que o Angular usa para decidir se abre a rota ou não
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // SEC-01: Não faz mais o parse do JWT no client. Se isAuthenticated() for true,
  // deixa tentar acessar. Se o cookie expirou no backend, a requisição retornará
  // 401 e o authInterceptor redirecionará para /login automaticamente.
  if (authService.isAuthenticated()) {
    return true;
  } else {
    router.navigate(['/login']);
    return false;
  }
};