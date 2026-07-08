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
  opcoes: Record<string, any[]> = {};
  cursos: any[] = [];
  cursoSelecionado?: number;
  matriz?: any;
  mensagem = '';
  carregando = false;

  private selects: Record<string, string> = {
    aluno: 'alunos',
    aula: 'aulas',
    curso: 'cursos',
    disciplina: 'disciplinas',
    modulo: 'modulos',
    professor: 'professores',
    turma: 'turmas',
    anoLetivo: 'anos-letivos',
    periodoLetivo: 'periodos-letivos',
    ofertaDisciplina: 'ofertas-disciplinas'
  };

  private labels: Record<string, string> = {
    'aluno.id': 'Aluno',
    'aula.id': 'Aula',
    'curso.id': 'Curso',
    'disciplina.id': 'Disciplina',
    'modulo.id': 'Modulo',
    'professor.id': 'Professor',
    'turma.id': 'Turma',
    'anoLetivo.id': 'Ano letivo',
    'periodoLetivo.id': 'Periodo letivo',
    'ofertaDisciplina.id': 'Oferta de disciplina',
    ativo: 'Ativo',
    avaliacaoFinal: 'Avaliacao final',
    bibliografiaBasica: 'Bibliografia basica',
    bibliografiaComplementar: 'Bibliografia complementar',
    cargaHoraria: 'Carga horaria',
    cargaHorariaPrevista: 'Carga horaria prevista',
    cargaHorariaMinistrada: 'Carga horaria ministrada',
    conteudoMinistrado: 'Conteudo ministrado',
    conteudoProgramatico: 'Conteudo programatico',
    criteriosAvaliacao: 'Criterios de avaliacao',
    dataAula: 'Data da aula',
    dataFim: 'Data final',
    dataIngresso: 'Data de ingresso',
    dataInicio: 'Data inicial',
    dataMatricula: 'Data da matricula',
    dataNascimento: 'Data de nascimento',
    ementaResumo: 'Resumo da ementa',
    frequenciaFinal: 'Frequencia final',
    notaFinal: 'Nota final',
    nota1: 'Nota 1',
    nota2: 'Nota 2',
    observacoes: 'Observacoes',
    quantidadeMaximaAlunos: 'Quantidade maxima de alunos'
  };

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.titulo = data['titulo'];
      this.endpoint = data['endpoint'];
      this.campos = data['campos'];
      this.formulario = {};
      this.opcoes = {};
      this.carregar();
      this.carregarOpcoes();
    });
  }

  carregar() {
    if (this.endpoint === 'relatorios') {
      this.registros = [];
      return;
    }
    if (this.endpoint === 'matriz-curricular') {
      this.api.listar('cursos').subscribe(cursos => {
        this.cursos = cursos;
        this.cursoSelecionado = this.cursoSelecionado || cursos[0]?.id;
        this.carregarMatriz();
      });
      return;
    }
    if (!this.campos.length) {
      this.registros = [];
      return;
    }
    this.api.listar(this.endpoint).subscribe(registros => this.registros = registros);
  }

  carregarMatriz() {
    if (!this.cursoSelecionado) return;
    this.api.buscar('matriz-curricular', this.cursoSelecionado).subscribe(matriz => this.matriz = matriz);
  }

  salvar() {
    const dados = this.montarObjeto();
    this.carregando = true;
    this.api.salvar(this.endpoint, dados).subscribe({
      next: () => {
        this.mensagem = 'Registro salvo com sucesso';
        this.formulario = {};
        this.carregar();
        this.carregando = false;
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel salvar';
        this.carregando = false;
      }
    });
  }

  excluir(id: number) {
    if (!confirm('Deseja excluir este registro?')) return;
    this.api.excluir(this.endpoint, id).subscribe(() => this.carregar());
  }

  chaves(registro: any) {
    return Object.keys(registro).filter(chave => !Array.isArray(registro[chave]) && !chave.toLowerCase().includes('senha') && !chave.toLowerCase().includes('caminho'));
  }

  label(campo: string) {
    return this.labels[campo] || campo.replace('.id', '').replace(/([A-Z])/g, ' $1').trim();
  }

  isSelect(campo: string) {
    return campo.endsWith('.id') || ['status', 'tipo', 'ativo', 'presente'].includes(campo);
  }

  isTextoLongo(campo: string) {
    return campo.includes('observ') || campo.includes('ementa') || campo.includes('bibliografia') || campo.includes('conteudo') || campo.includes('metodologia') || campo.includes('objetivos') || campo.includes('descricao');
  }

  tipoCampo(campo: string) {
    if (campo.toLowerCase().includes('data')) return 'date';
    if (['ano', 'ordem', 'vagas', 'cargaHoraria', 'cargaHorariaTotal', 'cargaHorariaPrevista', 'cargaHorariaMinistrada', 'cargaHorariaAula', 'quantidadeMaximaAlunos', 'nota1', 'nota2', 'trabalho', 'avaliacaoFinal', 'notaFinal', 'frequenciaFinal'].includes(campo)) return 'number';
    return 'text';
  }

  permitePdf() {
    return this.endpoint === 'disciplinas' || this.endpoint === 'planos-ensino';
  }

  opcoesCampo(campo: string) {
    if (campo === 'ativo' || campo === 'presente') return [{ id: true, nome: 'Sim' }, { id: false, nome: 'Nao' }];
    if (campo === 'status') return this.opcoesStatus();
    if (campo === 'tipo') return [{ id: 'MODULO', nome: 'Modulo' }, { id: 'SEMESTRE', nome: 'Semestre' }, { id: 'BIMESTRE', nome: 'Bimestre' }];
    return this.opcoes[campo] || [];
  }

  rotuloOpcao(opcao: any) {
    if (opcao === null || opcao === undefined) return '';
    if (typeof opcao !== 'object') return String(opcao);
    const partes = [
      opcao.nome,
      opcao.codigo,
      opcao.ano,
      opcao.disciplina?.nome,
      opcao.modulo?.nome,
      opcao.turma?.nome,
      opcao.periodoLetivo?.nome
    ].filter(Boolean);
    return partes.length ? partes.join(' - ') : `Registro ${opcao.id}`;
  }

  valor(registro: any, chave: string) {
    const valor = registro[chave];
    if (valor && typeof valor === 'object') return this.rotuloOpcao(valor);
    if (typeof valor === 'boolean') return valor ? 'Sim' : 'Nao';
    return valor ?? '';
  }

  enviarPdf(registro: any, event: Event) {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) return;
    if (arquivo.type !== 'application/pdf') {
      this.mensagem = 'Envie um arquivo PDF';
      return;
    }
    const acao = this.endpoint === 'disciplinas' ? 'ementa-pdf' : 'plano-pdf';
    this.api.enviarArquivo(this.endpoint, registro.id, acao, arquivo).subscribe({
      next: () => {
        this.mensagem = 'PDF enviado com sucesso';
        this.carregar();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel enviar o PDF'
    });
  }

  removerPdf(registro: any) {
    const acao = this.endpoint === 'disciplinas' ? 'ementa-pdf' : 'plano-pdf';
    this.api.removerArquivo(this.endpoint, registro.id, acao).subscribe(() => {
      this.mensagem = 'PDF removido';
      this.carregar();
    });
  }

  linkPdf(registro: any) {
    const acao = this.endpoint === 'disciplinas' ? 'ementa-pdf' : 'plano-pdf';
    return `/api/${this.endpoint}/${registro.id}/${acao}`;
  }

  temPdf(registro: any) {
    return this.endpoint === 'disciplinas' ? !!registro.ementaPdfNome : !!registro.planoPdfNome;
  }

  nomePdf(registro: any) {
    return this.endpoint === 'disciplinas' ? registro.ementaPdfNome : registro.planoPdfNome;
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

  private carregarOpcoes() {
    for (const campo of this.campos.filter(campo => campo.endsWith('.id'))) {
      const chave = campo.split('.')[0];
      const endpoint = this.selects[chave];
      if (!endpoint || this.opcoes[campo]) continue;
      this.api.listar(endpoint).subscribe(opcoes => this.opcoes[campo] = opcoes);
    }
  }

  private opcoesStatus() {
    if (this.endpoint === 'matriculas-disciplinas') {
      return ['ATIVA', 'CONCLUIDA', 'CANCELADA', 'REPROVADA', 'TRANCADA'].map(valor => ({ id: valor, nome: valor }));
    }
    if (this.endpoint === 'ofertas-disciplinas' || this.endpoint === 'montagem-periodo') {
      return ['PLANEJADA', 'ABERTA', 'EM_ANDAMENTO', 'ENCERRADA', 'CANCELADA'].map(valor => ({ id: valor, nome: valor }));
    }
    return ['ATIVO', 'INATIVO', 'PLANEJADO', 'EM_ANDAMENTO', 'ABERTA', 'ENCERRADA', 'PENDENTE', 'APROVADO', 'REPROVADO', 'CURSANDO'].map(valor => ({ id: valor, nome: valor }));
  }
}
