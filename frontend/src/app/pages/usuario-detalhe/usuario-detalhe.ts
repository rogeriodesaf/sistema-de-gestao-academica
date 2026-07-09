import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-usuario-detalhe',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent],
  templateUrl: './usuario-detalhe.html',
  styleUrl: './usuario-detalhe.scss'
})
export class UsuarioDetalhePage implements OnInit {
  usuario = signal<any | undefined>(undefined);
  mensagem = signal('');
  carregando = signal(true);

  constructor(private route: ActivatedRoute, private router: Router, private api: ApiService) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.mensagem.set('Usuario nao encontrado.');
      this.carregando.set(false);
      return;
    }
    this.api.buscar('usuarios', id).subscribe({
      next: usuario => {
        this.usuario.set(usuario);
        this.carregando.set(false);
      },
      error: err => {
        this.mensagem.set(err?.error?.mensagem || 'Nao foi possivel carregar o usuario.');
        this.carregando.set(false);
      }
    });
  }

  editar() {
    this.router.navigate(['/usuarios'], { queryParams: { editar: this.usuario()?.id } });
  }

  redefinirSenha() {
    this.router.navigate(['/usuarios'], { queryParams: { senha: this.usuario()?.id } });
  }

  perfilTexto(perfil: string) {
    const nomes: Record<string, string> = {
      ADMINISTRADOR: 'Administrador',
      COORDENADOR: 'Coordenador',
      SECRETARIA: 'Secretaria',
      PROFESSOR: 'Professor',
      ALUNO: 'Aluno'
    };
    return nomes[perfil] || perfil;
  }

  dataTexto(valor: string) {
    if (!valor) return 'Nao registrado';
    return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(valor));
  }
}
