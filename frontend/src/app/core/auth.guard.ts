import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = route => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.logado()) {
    return router.parseUrl('/login');
  }

  const perfisPermitidos = route.data?.['perfis'] as string[] | undefined;
  const perfil = auth.usuario()?.perfil?.trim().toUpperCase();
  if (!perfisPermitidos?.length || (perfil && perfisPermitidos.includes(perfil))) {
    return true;
  }

  return router.parseUrl(auth.rotaInicial(perfil));
};
