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

  login(email: string, senha: string) {
    return this.http.post<UsuarioSessao>(`${this.api}/auth/login`, { email, senha }).pipe(
      tap(sessao => {
        localStorage.setItem(this.chave, JSON.stringify(sessao));
        this.usuario.set(sessao);
      })
    );
  }

  sair() {
    localStorage.removeItem(this.chave);
    this.usuario.set(null);
    this.router.navigateByUrl('/login');
  }

  private lerSessao(): UsuarioSessao | null {
    const valor = localStorage.getItem(this.chave);
    return valor ? JSON.parse(valor) : null;
  }
}
