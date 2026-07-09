import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-perfis',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './perfis.html',
  styleUrl: './perfis.scss'
})
export class PerfisPage implements OnInit {
  perfis: any[] = [];
  perfilSelecionado?: any;
  permissoes: any[] = [];
  mensagem = '';
  carregando = false;
  salvando = false;
  areas = ['ACADEMICO', 'PESSOAS', 'AVALIACAO', 'CONSULTAS', 'ADMINISTRACAO'];
  acoes = [
    { chave: 'visualizar', label: 'Visualizar' },
    { chave: 'criar', label: 'Criar' },
    { chave: 'editar', label: 'Editar' },
    { chave: 'excluir', label: 'Excluir' }
  ];

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.carregarPerfis();
  }

  carregarPerfis() {
    this.api.listar('perfis').subscribe({
      next: perfis => this.perfis = perfis || [],
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel carregar perfis'
    });
  }

  configurar(perfil: any) {
    this.perfilSelecionado = perfil;
    this.permissoes = [];
    this.carregando = true;
    this.api.buscarAcao('perfis', perfil.codigo, 'permissoes').subscribe({
      next: (resposta: any) => {
        this.permissoes = resposta?.permissoes || [];
        this.carregando = false;
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel carregar permissoes';
        this.carregando = false;
      }
    });
  }

  fecharConfiguracao() {
    this.perfilSelecionado = undefined;
    this.permissoes = [];
  }

  salvarPermissoes() {
    if (!this.perfilSelecionado) return;
    this.salvando = true;
    this.api.acao('perfis', this.perfilSelecionado.codigo, 'permissoes', { permissoes: this.permissoes }).subscribe({
      next: (resposta: any) => {
        this.permissoes = resposta?.permissoes || [];
        this.mensagem = 'Permissoes atualizadas com sucesso';
        this.salvando = false;
        this.fecharConfiguracao();
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel salvar permissoes';
        this.salvando = false;
      }
    });
  }

  permissoesDaArea(area: string) {
    return this.permissoes.filter(permissao => permissao.area === area);
  }

  areaTexto(area: string) {
    const textos: Record<string, string> = {
      ACADEMICO: 'Academico',
      PESSOAS: 'Pessoas',
      AVALIACAO: 'Avaliacao',
      CONSULTAS: 'Consultas',
      ADMINISTRACAO: 'Administracao'
    };
    return textos[area] || area;
  }

  totalMarcadas(perfil?: any) {
    if (!perfil || this.perfilSelecionado?.codigo !== perfil.codigo) return '';
    const marcadas = this.permissoes.reduce((total, permissao) =>
      total + this.acoes.filter(acao => permissao[acao.chave]).length, 0);
    return `${marcadas} permissoes marcadas`;
  }

  bloqueiaAdministrador(acao: string) {
    return this.perfilSelecionado?.codigo === 'ADMINISTRADOR' && ['visualizar', 'criar', 'editar', 'excluir'].includes(acao);
  }
}
