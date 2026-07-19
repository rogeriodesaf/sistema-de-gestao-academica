import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export interface UsuarioSessao {
  token: string;
  usuarioId: number;
  nome: string;
  email: string;
  perfil: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly chave = 'sga.sessao';
  private readonly api = '/api';
  usuario = signal<UsuarioSessao | null>(this.lerSessao());

  constructor(private http: HttpClient, private router: Router) {}

  logado(): boolean {
    return !!this.usuario()?.token;
  }

  token(): string | null {
    return this.usuario()?.token || null;
  }

  rotaInicial(perfil = this.usuario()?.perfil): string {
    const perfilNormalizado = this.normalizarPerfil(perfil);
    if (perfilNormalizado === 'PROFESSOR') return '/area-professor';
    if (perfilNormalizado === 'ALUNO') return '/area-aluno';
    return '/dashboard';
  }

  login(email: string, senha: string) {
    return this.http.post<UsuarioSessao>(`${this.api}/auth/login`, { email, senha }).pipe(
      tap(sessao => {
        const sessaoNormalizada = this.normalizarSessao(sessao);
        localStorage.setItem(this.chave, JSON.stringify(sessaoNormalizada));
        this.usuario.set(sessaoNormalizada);
      })
    );
  }

  sair(motivo?: string) {
    localStorage.removeItem(this.chave);
    this.usuario.set(null);
    this.router.navigate(['/login'], {
      queryParams: motivo ? { motivo } : undefined,
      replaceUrl: true
    });
  }

  private lerSessao(): UsuarioSessao | null {
    const valor = localStorage.getItem(this.chave);
    return valor ? this.normalizarSessao(JSON.parse(valor)) : null;
  }

  private normalizarSessao(sessao: UsuarioSessao): UsuarioSessao {
    return { ...sessao, perfil: this.normalizarPerfil(sessao.perfil) };
  }

  private normalizarPerfil(perfil?: string): string {
    return (perfil || '').trim().toUpperCase();
  }
}
