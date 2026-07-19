import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();

  const requisicao = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(requisicao).pipe(
    catchError((erro: HttpErrorResponse) => {
      const sessaoInvalida = erro.status === 401 && !req.url.includes('/auth/login');
      if (sessaoInvalida) {
        auth.sair('sessao-expirada');
      }
      return throwError(() => normalizarErro(erro));
    })
  );
};

function normalizarErro(erro: HttpErrorResponse): HttpErrorResponse {
  const mensagem = String(erro.error?.mensagem || erro.message || '');
  const conteudoTecnico = /(exception|constraint|stack|sql|select\s|insert\s|update\s|delete\s|nullpointer|hibernate|jdbc|at\s+[\w.$]+\()/i.test(mensagem);
  if (erro.status < 500 && !conteudoTecnico) return erro;

  return new HttpErrorResponse({
    error: { mensagem: 'Ocorreu um erro ao processar a solicitação. Tente novamente.' },
    headers: erro.headers,
    status: erro.status,
    statusText: erro.statusText,
    url: erro.url || undefined
  });
}
