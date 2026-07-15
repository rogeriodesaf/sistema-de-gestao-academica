import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ThemeService } from '../../core/theme.service';

@Component({
  selector: 'app-recuperacao-senha',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './recuperacao-senha.html',
  styleUrl: './recuperacao-senha.scss'
})
export class RecuperacaoSenhaPage {
  readonly redefinindo: boolean;
  readonly token: string;
  email = '';
  novaSenha = '';
  confirmacaoSenha = '';
  carregando = signal(false);
  mensagem = signal('');
  erro = signal('');
  concluido = signal(false);

  constructor(private http: HttpClient, route: ActivatedRoute, public tema: ThemeService) {
    this.redefinindo = route.snapshot.routeConfig?.path === 'redefinir-senha';
    this.token = route.snapshot.queryParamMap.get('token') || '';
    if (this.redefinindo && !this.token) this.erro.set('Link de redefinição inválido.');
  }

  enviarInstrucoes() {
    if (this.carregando()) return;
    this.erro.set('');
    this.mensagem.set('');
    this.carregando.set(true);
    this.http.post<any>('/api/auth/esqueci-senha', { email: this.email }).pipe(
      finalize(() => this.carregando.set(false))
    ).subscribe({
      next: resposta => this.mensagem.set(resposta.mensagem),
      error: () => this.mensagem.set(
        'Se o e-mail estiver cadastrado, você receberá as instruções para redefinir sua senha.'
      )
    });
  }

  redefinirSenha() {
    if (this.carregando() || !this.token) return;
    this.erro.set('');
    this.mensagem.set('');
    if (this.novaSenha.length < 8) {
      this.erro.set('A senha deve ter no mínimo 8 caracteres.');
      return;
    }
    if (this.novaSenha !== this.confirmacaoSenha) {
      this.erro.set('A confirmação de senha não confere.');
      return;
    }

    this.carregando.set(true);
    this.http.post<any>('/api/auth/redefinir-senha', {
      token: this.token,
      novaSenha: this.novaSenha,
      confirmacaoSenha: this.confirmacaoSenha
    }).pipe(
      finalize(() => this.carregando.set(false))
    ).subscribe({
      next: resposta => {
        this.concluido.set(true);
        this.mensagem.set(resposta.mensagem);
      },
      error: resposta => this.erro.set(
        resposta?.error?.mensagem || 'Não foi possível redefinir a senha. Tente novamente.'
      )
    });
  }
}
