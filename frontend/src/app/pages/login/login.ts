import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class LoginPage {
  email = 'admin@sga.local';
  senha = 'admin123';
  erro = '';
  carregando = false;

  constructor(private auth: AuthService, private router: Router) {}

  entrar() {
    this.erro = '';
    this.carregando = true;
    this.auth.login(this.email, this.senha).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: err => {
        this.erro = err?.error?.mensagem || 'Nao foi possivel entrar';
        this.carregando = false;
      }
    });
  }
}
