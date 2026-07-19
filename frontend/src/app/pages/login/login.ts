import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ThemeService } from '../../core/theme.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class LoginPage {
  email = '';
  senha = '';
  erro = '';
  carregando = false;

  constructor(
    private auth: AuthService,
    private changeDetector: ChangeDetectorRef,
    private route: ActivatedRoute,
    public tema: ThemeService
  ) {
    if (this.route.snapshot.queryParamMap.get('motivo') === 'sessao-expirada') {
      this.erro = 'Sua sessão expirou. Entre novamente.';
    }
  }

  entrar() {
    if (this.carregando) return;

    this.erro = '';
    this.carregando = true;
    this.auth.login(this.email, this.senha).pipe(
      finalize(() => {
        this.carregando = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: sessao => {
        window.location.replace(this.auth.rotaInicial(sessao.perfil));
      },
      error: err => {
        this.erro = err?.status === 401
          ? 'Usuário ou senha inválidos.'
          : err?.error?.mensagem || 'Ocorreu um erro ao processar a solicitação. Tente novamente.';
      }
    });
  }
}
