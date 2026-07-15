import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

interface UsuarioForm {
  nome: string;
  email: string;
  senha: string;
  confirmarSenha: string;
  perfil: string;
  ativo: boolean;
  observacoes: string;
}

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss'
})
export class UsuariosPage implements OnInit {
  usuarios: any[] = [];
  mensagem = '';
  carregando = false;
  registroEditandoId?: number;
  senhaUsuario?: any;
  senhaGerada = '';
  senhaForm = { senha: '', confirmarSenha: '', gerarAutomatica: true };
  filtros = { busca: '', perfil: '', situacao: '' };
  paginaAtual = 1;
  itensPorPagina = 10;
  tamanhosPagina = [10, 25, 50, 100];
  perfis = [
    { id: 'ADMINISTRADOR', nome: 'Administrador' },
    { id: 'COORDENADOR', nome: 'Coordenador' },
    { id: 'SECRETARIA', nome: 'Secretaria' },
    { id: 'PROFESSOR', nome: 'Professor' },
    { id: 'ALUNO', nome: 'Aluno' }
  ];
  formulario: UsuarioForm = this.formularioVazio();
  private acaoInicialAplicada = false;

  constructor(
    private api: ApiService,
    private router: Router,
    private route: ActivatedRoute,
    private changeDetector: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.carregarPerfis();
    this.carregar();
  }

  carregarPerfis() {
    this.api.listar('perfis').subscribe({
      next: perfis => this.perfis = (perfis || []).map(perfil => ({ id: perfil.codigo, nome: perfil.nome })),
      error: () => {}
    });
  }

  carregar() {
    this.api.listar('usuarios').subscribe({
      next: usuarios => {
        this.usuarios = usuarios || [];
        this.aplicarAcaoInicial();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel carregar usuarios'
    });
  }

  salvar() {
    const erro = this.validarFormulario();
    if (erro) {
      this.mensagem = erro;
      return;
    }
    this.carregando = true;
    const dados: any = {
      nome: this.formulario.nome.trim(),
      email: this.formulario.email.trim(),
      perfil: this.formulario.perfil,
      ativo: this.formulario.ativo,
      observacoes: this.formulario.observacoes
    };
    const requisicao = this.registroEditandoId
      ? this.api.atualizar('usuarios', this.registroEditandoId, dados)
      : this.api.salvar('usuarios', { ...dados, senha: this.formulario.senha, confirmarSenha: this.formulario.confirmarSenha });

    requisicao.pipe(
      finalize(() => {
        this.carregando = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: () => {
        this.mensagem = this.registroEditandoId ? 'Usuario atualizado com sucesso' : 'Usuario cadastrado com sucesso';
        this.cancelarEdicao();
        this.carregar();
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || (err?.name === 'TimeoutError'
          ? 'O servidor demorou para responder. Tente novamente.'
          : 'Nao foi possivel salvar usuario');
      }
    });
  }

  editar(usuario: any) {
    this.registroEditandoId = usuario.id;
    this.formulario = {
      nome: usuario.nome || '',
      email: usuario.email || '',
      senha: '',
      confirmarSenha: '',
      perfil: usuario.perfil || '',
      ativo: !!usuario.ativo,
      observacoes: usuario.observacoes || ''
    };
    this.mensagem = `Editando ${usuario.nome}`;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelarEdicao() {
    this.registroEditandoId = undefined;
    this.formulario = this.formularioVazio();
  }

  visualizar(usuario: any) {
    this.router.navigate(['/usuarios', usuario.id]);
  }

  abrirRedefinirSenha(usuario: any) {
    this.senhaUsuario = usuario;
    this.senhaGerada = '';
    this.senhaForm = { senha: '', confirmarSenha: '', gerarAutomatica: true };
  }

  fecharRedefinirSenha() {
    this.senhaUsuario = undefined;
    this.senhaGerada = '';
    this.senhaForm = { senha: '', confirmarSenha: '', gerarAutomatica: true };
  }

  redefinirSenha() {
    if (!this.senhaUsuario) return;
    if (!this.senhaForm.gerarAutomatica && this.senhaForm.senha.length < 8) {
      this.mensagem = 'A senha deve ter no minimo 8 caracteres';
      return;
    }
    if (!this.senhaForm.gerarAutomatica && this.senhaForm.senha !== this.senhaForm.confirmarSenha) {
      this.mensagem = 'A confirmacao de senha nao confere';
      return;
    }
    if (!confirm(`Deseja redefinir a senha de ${this.senhaUsuario.nome}?`)) return;

    this.api.acao('usuarios', this.senhaUsuario.id, 'redefinir-senha', this.senhaForm).subscribe({
      next: (resposta: any) => {
        this.senhaGerada = resposta?.senhaProvisoria || '';
        this.mensagem = 'Senha redefinida com sucesso';
        if (!this.senhaForm.gerarAutomatica) this.fecharRedefinirSenha();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel redefinir a senha'
    });
  }

  inativar(usuario: any) {
    if (!confirm(`Deseja inativar ${usuario.nome}?`)) return;
    this.api.excluir('usuarios', usuario.id).subscribe({
      next: () => {
        this.mensagem = 'Usuario inativado com sucesso';
        this.carregar();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel inativar usuario'
    });
  }

  reativar(usuario: any) {
    this.api.acao('usuarios', usuario.id, 'reativar', {}).subscribe({
      next: () => {
        this.mensagem = 'Usuario reativado com sucesso';
        this.carregar();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel reativar usuario'
    });
  }

  usuariosFiltrados() {
    const busca = this.normalizarBusca(this.filtros.busca);
    return this.usuarios
      .filter(usuario => !busca || this.normalizarBusca(`${usuario.nome} ${usuario.email}`).includes(busca))
      .filter(usuario => !this.filtros.perfil || usuario.perfil === this.filtros.perfil)
      .filter(usuario => !this.filtros.situacao || (this.filtros.situacao === 'ATIVO' ? usuario.ativo : !usuario.ativo))
      .sort((a, b) => (a.nome || '').localeCompare(b.nome || ''));
  }

  usuariosPaginados() {
    const inicio = (this.paginaAtual - 1) * this.itensPorPagina;
    return this.usuariosFiltrados().slice(inicio, inicio + this.itensPorPagina);
  }

  totalPaginas() {
    return Math.max(Math.ceil(this.usuariosFiltrados().length / this.itensPorPagina), 1);
  }

  mudarPagina(delta: number) {
    this.paginaAtual = Math.min(Math.max(this.paginaAtual + delta, 1), this.totalPaginas());
  }

  atualizarFiltro() {
    this.paginaAtual = 1;
  }

  mudarTamanhoPagina() {
    this.paginaAtual = 1;
  }

  limparFiltros() {
    this.filtros = { busca: '', perfil: '', situacao: '' };
    this.paginaAtual = 1;
  }

  perfilTexto(perfil: string) {
    return this.perfis.find(item => item.id === perfil)?.nome || perfil;
  }

  dataTexto(valor: string) {
    if (!valor) return 'Nao registrado';
    return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(valor));
  }

  private validarFormulario() {
    if (!this.formulario.nome.trim()) return 'Nome obrigatorio';
    if (!this.formulario.email.trim()) return 'E-mail obrigatorio';
    if (!this.formulario.perfil) return 'Perfil obrigatorio';
    if (!this.registroEditandoId && this.formulario.senha.length < 8) return 'A senha deve ter no minimo 8 caracteres';
    if (!this.registroEditandoId && !this.formulario.confirmarSenha) return 'Confirmar senha obrigatoria';
    if (!this.registroEditandoId && this.formulario.senha !== this.formulario.confirmarSenha) return 'A confirmacao de senha nao confere';
    return '';
  }

  private formularioVazio(): UsuarioForm {
    return {
      nome: '',
      email: '',
      senha: '',
      confirmarSenha: '',
      perfil: '',
      ativo: true,
      observacoes: ''
    };
  }

  private normalizarBusca(valor: string) {
    return (valor || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
  }

  private aplicarAcaoInicial() {
    if (this.acaoInicialAplicada) return;
    this.acaoInicialAplicada = true;
    const editarId = Number(this.route.snapshot.queryParamMap.get('editar'));
    const senhaId = Number(this.route.snapshot.queryParamMap.get('senha'));
    if (editarId) {
      const usuario = this.usuarios.find(item => item.id === editarId);
      if (usuario) this.editar(usuario);
    }
    if (senhaId) {
      const usuario = this.usuarios.find(item => item.id === senhaId);
      if (usuario) this.abrirRedefinirSenha(usuario);
    }
  }
}
