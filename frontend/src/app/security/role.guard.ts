// frontend/src/app/security/role.guard.ts
import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

// Segunda barreira, além do backend (@PreAuthorize): sem isso, qualquer usuário autenticado
// podia navegar direto por URL pras rotas filhas de /panel destinadas a outro perfil (ex.:
// paciente indo em /panel/professionals) — o componente renderizava e só falhava ao chamar a
// API, um estado quebrado em vez de um redirecionamento limpo.
export function roleGuard(allowedRoles: string[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const role = authService.getUserRole();
    if (role && allowedRoles.includes(role)) {
      return true;
    }

    router.navigate(['/panel/dashboard']);
    return false;
  };
}
