import { HttpInterceptorFn } from '@angular/common/http';

// Esta função intercepta TODAS as requisições HTTP que o Angular tenta fazer pro Backend
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');

  if (token) {
    // Se tiver um token salvo, ele "clona" a requisição original e adiciona o cabeçalho Authorization
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedReq); // Manda a requisição modificada pro Backend
  }

  // Se não tiver token (ex: na própria tela de login), manda a requisição normal
  return next(req);
};