import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, filter, take } from 'rxjs/operators';
import { throwError, BehaviorSubject } from 'rxjs';
import { AuthService } from './auth.service';

let isRefreshing = false;
let refreshTokenSubject = new BehaviorSubject<any>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  const clonedReq = req.clone({
    withCredentials: true
  });

  // /auth/logout também fica de fora do fluxo de refresh: sem isso, um 401 durante o logout
  // (cookie já expirado) disparava uma tentativa de refresh + reenvio do próprio logout, ou
  // corria com o próprio subscribe de logout() pra decidir quem navega pro /login.
  const isAuthRoute = req.url.includes('/auth/login') || req.url.includes('/auth/refresh') || req.url.includes('/auth/logout');

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isAuthRoute) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          // SEC-03: Tenta atualizar o token de forma silenciosa
          return authService.refreshToken().pipe(
            switchMap(() => {
              isRefreshing = false;
              refreshTokenSubject.next(true); // Libera a fila
              return next(clonedReq); // Repete a request original
            }),
            catchError((err) => {
              isRefreshing = false;
              // Libera a fila com sinal de falha — sem isso, requisições concorrentes
              // enfileiradas abaixo ficavam esperando pra sempre um valor que nunca chegava.
              refreshTokenSubject.next(false);
              authService.clearLocalSession();
              router.navigate(['/login']);
              return throwError(() => err);
            })
          );
        } else {
          // SEC-03: Fila de espera para requisições paradas no 401
          return refreshTokenSubject.pipe(
            filter(result => result !== null),
            take(1),
            switchMap(result => {
              // O refresh que essa requisição estava esperando falhou — propaga o erro em vez
              // de tentar repetir a chamada original (que só voltaria a dar 401).
              if (!result) {
                return throwError(() => error);
              }
              return next(clonedReq);
            })
          );
        }
      }

      // Se a própria tentativa de refresh retornar 401, a sessão morreu de vez
      if (error.status === 401 && req.url.includes('/auth/refresh')) {
        authService.clearLocalSession();
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};