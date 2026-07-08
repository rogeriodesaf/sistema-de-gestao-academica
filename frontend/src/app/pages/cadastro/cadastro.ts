import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cadastro.html'
})
export class CadastroPage implements OnInit {
  titulo = '';
  endpoint = '';
  campos: string[] = [];
  registros: any[] = [];
  formulario: Record<string, any> = {};
  mensagem = '';

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.titulo = data['titulo'];
      this.endpoint = data['endpoint'];
      this.campos = data['campos'];
      this.formulario = {};
      this.carregar();
    });
  }

  carregar() {
    if (this.endpoint === 'relatorios') {
      this.registros = [];
      return;
    }
    this.api.listar(this.endpoint).subscribe(registros => this.registros = registros);
  }

  salvar() {
    const dados = this.montarObjeto();
    this.api.salvar(this.endpoint, dados).subscribe({
      next: () => {
        this.mensagem = 'Registro salvo com sucesso';
        this.formulario = {};
        this.carregar();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel salvar'
    });
  }

  excluir(id: number) {
    this.api.excluir(this.endpoint, id).subscribe(() => this.carregar());
  }

  chaves(registro: any) {
    return Object.keys(registro).filter(chave => !Array.isArray(registro[chave]) && typeof registro[chave] !== 'object');
  }

  private montarObjeto() {
    const saida: any = {};
    for (const campo of this.campos) {
      const valor = this.formulario[campo];
      if (valor === undefined || valor === '') continue;
      if (campo.endsWith('.id')) {
        const nome = campo.split('.')[0];
        saida[nome] = { id: Number(valor) };
      } else if (valor === 'true' || valor === 'false') {
        saida[campo] = valor === 'true';
      } else {
        saida[campo] = valor;
      }
    }
    return saida;
  }
}
