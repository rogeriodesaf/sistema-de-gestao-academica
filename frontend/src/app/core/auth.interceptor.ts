import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const sessao = localStorage.getItem('sga.sessao');
  let token: string | null = null;

  try {
    token = sessao ? JSON.parse(sessao).token : null;
  } catch {
    localStorage.removeItem('sga.sessao');
  }

  const requisicao = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(requisicao).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status === 401 && !req.url.includes('/auth/login')) {
        localStorage.removeItem('sga.sessao');
        router.navigateByUrl('/login');
      }
      return throwError(() => erro);
    })
  );
};
