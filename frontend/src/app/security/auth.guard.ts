import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { jwtDecode } from 'jwt-decode';

// Uma função simples que o Angular usa para decidir se abre a rota ou não
export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('token'); // Procura o crachá no cofre do navegador

  if (token) {
    try {
      const decodedToken: any = jwtDecode(token);
      const isExpired = decodedToken.exp && decodedToken.exp * 1000 < Date.now();
      
      if (!isExpired) {
        return true; 
      }
      
      // Se expirou, limpa o token e manda pro login
      localStorage.removeItem('token');
      router.navigate(['/login']);
      return false;
    } catch (e) {
      // Se o token for inválido, limpa e manda pro login
      localStorage.removeItem('token');
      router.navigate(['/login']);
      return false;
    }
  } else {
    router.navigate(['/login']); // Não tem? Chuta de volta pro Login!
    return false;
  }
};