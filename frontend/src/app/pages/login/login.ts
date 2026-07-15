import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
  email = 'admin@sga.local';
  senha = 'admin123';
  erro = '';
  carregando = false;

  constructor(private auth: AuthService, private changeDetector: ChangeDetectorRef, public tema: ThemeService) {}

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
        this.erro = err?.error?.mensagem || 'Nao foi possivel entrar';
      }
    });
  }
}
