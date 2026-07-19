import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-status-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-page.html',
  styleUrl: './status-page.scss'
})
export class StatusPage {
  readonly codigo: number;
  readonly titulo: string;
  readonly mensagem: string;
  readonly botao: string;

  constructor(route: ActivatedRoute, private router: Router, private auth: AuthService) {
    this.codigo = route.snapshot.data['codigo'];
    this.titulo = route.snapshot.data['titulo'];
    this.mensagem = route.snapshot.data['mensagem'];
    this.botao = route.snapshot.data['botao'];
  }

  voltar() {
    this.router.navigateByUrl(this.auth.logado() ? this.auth.rotaInicial() : '/login');
  }
}
