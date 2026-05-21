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

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/login') && !req.url.includes('/auth/refresh')) {
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
              authService.clearLocalSession();
              router.navigate(['/login']);
              return throwError(() => err);
            })
          );
        } else {
          // SEC-03: Fila de espera para requisições paradas no 401
          return refreshTokenSubject.pipe(
            filter(result => result != null),
            take(1),
            switchMap(() => next(clonedReq))
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