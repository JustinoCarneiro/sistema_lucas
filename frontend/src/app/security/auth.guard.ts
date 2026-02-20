import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

// Uma função simples que o Angular usa para decidir se abre a rota ou não
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('token'); // Procura o crachá no cofre do navegador

  if (token) {
    return true; // Tem crachá? Pode entrar na página!
  } else {
    router.navigate(['/login']); // Não tem? Chuta de volta pro Login!
    return false;
  }
};