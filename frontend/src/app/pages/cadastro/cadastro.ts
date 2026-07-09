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
  alunoSelecionado?: number;
  ofertasPorModulo: any[] = [];
  ofertasSelecionadas: Record<number, boolean> = {};
  disciplinasModulo: Record<number, Record<number, boolean>> = {};
  mensagem = '';
  carregando = false;

  private selects: Record<string, string> = {
    aluno: 'alunos',
    aula: 'aulas',
    curso: 'cursos',
    disciplina: 'disciplinas',
    modulo: 'modulos',
    professor: 'professores',
    professorResponsavel: 'professores',
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
    'professorResponsavel.id': 'Professor responsavel',
    'turma.id': 'Turma',
    'anoLetivo.id': 'Ano letivo',
    'periodoLetivo.id': 'Periodo letivo',
    'ofertaDisciplina.id': 'Oferta de disciplina',
    ativo: 'Ativo',
    avaliacaoFinal: 'Avaliacao final',
    bibliografiaBasica: 'Bibliografia basica',
    bibliografiaComplementar: 'Bibliografia complementar',
    cargaHoraria: 'Carga horaria',
    creditos: 'Creditos',
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
    this.api.listar(this.endpoint).subscribe(registros => {
      this.registros = registros;
      if (this.endpoint === 'matriculas-disciplinas') this.carregarOfertasAgrupadas();
    });
  }

  carregarMatriz() {
    if (!this.cursoSelecionado) return;
    this.api.buscar('matriz-curricular', this.cursoSelecionado).subscribe(matriz => this.matriz = matriz);
  }

  salvar() {
    if (this.endpoint === 'matriculas-disciplinas' && Object.values(this.ofertasSelecionadas).some(Boolean)) {
      this.salvarMatriculasSelecionadas();
      return;
    }
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
    return Object.keys(registro).filter(chave => {
      const normalizada = chave.toLowerCase();
      return chave !== 'id'
        && !Array.isArray(registro[chave])
        && !normalizada.includes('senha')
        && !normalizada.includes('caminho')
        && !normalizada.includes('pdf');
    });
  }

  label(campo: string) {
    return this.labels[campo] || campo.replace('.id', '').replace(/([A-Z])/g, ' $1').trim();
  }

  isSelect(campo: string) {
    return campo.endsWith('.id') || ['status', 'tipo', 'ativo', 'presente'].includes(campo);
  }

  isTextoLongo(campo: string) {
    return campo.includes('observ') || campo.includes('ementaResumo') || campo.includes('bibliografia') || campo.includes('conteudo') || campo.includes('metodologia') || campo.includes('objetivos') || campo.includes('descricao');
  }

  tipoCampo(campo: string) {
    if (campo.toLowerCase().includes('data')) return 'date';
    if (['ano', 'ordem', 'vagas', 'creditos', 'cargaHoraria', 'cargaHorariaTotal', 'cargaHorariaPrevista', 'cargaHorariaMinistrada', 'cargaHorariaAula', 'quantidadeMaximaAlunos', 'nota1', 'nota2', 'trabalho', 'avaliacaoFinal', 'notaFinal', 'frequenciaFinal'].includes(campo)) return 'number';
    return 'text';
  }

  permitePdf() {
    return this.endpoint === 'cursos' || this.endpoint === 'disciplinas' || this.endpoint === 'planos-ensino';
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
    const acao = this.acaoPdf();
    this.api.enviarArquivo(this.endpoint, registro.id, acao, arquivo).subscribe({
      next: () => {
        this.mensagem = 'PDF enviado com sucesso';
        this.carregar();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel enviar o PDF'
    });
  }

  removerPdf(registro: any) {
    const acao = this.acaoPdf();
    this.api.removerArquivo(this.endpoint, registro.id, acao).subscribe(() => {
      this.mensagem = 'PDF removido';
      this.carregar();
    });
  }

  linkPdf(registro: any) {
    const acao = this.acaoPdf();
    return `/api/${this.endpoint}/${registro.id}/${acao}`;
  }

  linkDownloadPdf(registro: any) {
    return `${this.linkPdf(registro)}/download`;
  }

  temPdf(registro: any) {
    if (this.endpoint === 'cursos') return !!registro.gradePdfNome;
    return this.endpoint === 'disciplinas' ? !!registro.ementaPdfNome : !!registro.planoPdfNome;
  }

  nomePdf(registro: any) {
    if (this.endpoint === 'cursos') return registro.gradePdfNome;
    return this.endpoint === 'disciplinas' ? registro.ementaPdfNome : registro.planoPdfNome;
  }

  mostrarMatriculaGuiada() {
    return this.endpoint === 'matriculas-disciplinas' && this.opcoes['aluno.id']?.length;
  }

  camposFormulario() {
    if (this.endpoint === 'matriculas-disciplinas') {
      return this.campos.filter(campo => !['aluno.id', 'ofertaDisciplina.id'].includes(campo));
    }
    return this.campos;
  }

  permiteVinculoDisciplinas() {
    return this.endpoint === 'modulos' && this.opcoes['disciplina.id']?.length;
  }

  disciplinasDoCurso(modulo: any) {
    return (this.opcoes['disciplina.id'] || []).filter(disciplina => !modulo?.curso?.id || disciplina.curso?.id === modulo.curso.id);
  }

  disciplinaMarcada(modulo: any, disciplina: any) {
    if (!this.disciplinasModulo[modulo.id]) {
      const marcadas: Record<number, boolean> = {};
      this.disciplinasDoCurso(modulo).forEach(item => marcadas[item.id] = item.modulo?.id === modulo.id);
      this.disciplinasModulo[modulo.id] = marcadas;
    }
    return this.disciplinasModulo[modulo.id][disciplina.id];
  }

  alternarDisciplinaModulo(modulo: any, disciplina: any, event: Event) {
    const input = event.target as HTMLInputElement;
    if (!this.disciplinasModulo[modulo.id]) this.disciplinaMarcada(modulo, disciplina);
    this.disciplinasModulo[modulo.id][disciplina.id] = input.checked;
  }

  salvarDisciplinasModulo(modulo: any) {
    const disciplinasIds = Object.entries(this.disciplinasModulo[modulo.id] || {})
      .filter(([, marcado]) => marcado)
      .map(([id]) => Number(id));
    this.api.acao('modulos', modulo.id, 'disciplinas', { disciplinasIds }).subscribe({
      next: () => {
        this.mensagem = 'Disciplinas vinculadas ao modulo';
        this.disciplinasModulo = {};
        this.carregar();
        this.carregarOpcoes();
      },
      error: err => this.mensagem = err?.error?.mensagem || 'Nao foi possivel vincular disciplinas'
    });
  }

  salvarMatriculasSelecionadas() {
    if (!this.alunoSelecionado) {
      this.mensagem = 'Selecione um aluno';
      return;
    }
    const ofertas = Object.entries(this.ofertasSelecionadas)
      .filter(([, selecionada]) => selecionada)
      .map(([id]) => Number(id));
    if (!ofertas.length) {
      this.mensagem = 'Selecione ao menos uma disciplina';
      return;
    }
    this.carregando = true;
    let concluidas = 0;
    let falhas: string[] = [];
    ofertas.forEach(ofertaId => {
      this.api.salvar(this.endpoint, {
        aluno: { id: this.alunoSelecionado },
        ofertaDisciplina: { id: ofertaId },
        status: 'ATIVA'
      }).subscribe({
        next: () => {
          concluidas++;
          this.finalizarLoteMatriculas(concluidas, ofertas.length, falhas);
        },
        error: err => {
          falhas.push(err?.error?.mensagem || 'Nao foi possivel matricular uma disciplina');
          concluidas++;
          this.finalizarLoteMatriculas(concluidas, ofertas.length, falhas);
        }
      });
    });
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
    if (this.endpoint === 'matriculas-disciplinas') {
      this.api.listar('ofertas-disciplinas').subscribe(() => this.carregarOfertasAgrupadas());
    }
    if (this.endpoint === 'modulos') {
      this.api.listar('disciplinas').subscribe(opcoes => this.opcoes['disciplina.id'] = opcoes);
    }
  }

  private opcoesStatus() {
    if (this.endpoint === 'modulos') {
      return ['ABERTO', 'FECHADO', 'INATIVO'].map(valor => ({ id: valor, nome: valor }));
    }
    if (this.endpoint === 'matriculas-disciplinas') {
      return ['ATIVA', 'CONCLUIDA', 'CANCELADA', 'REPROVADA', 'TRANCADA'].map(valor => ({ id: valor, nome: valor }));
    }
    if (this.endpoint === 'ofertas-disciplinas' || this.endpoint === 'montagem-periodo') {
      return ['PLANEJADA', 'ABERTA', 'EM_ANDAMENTO', 'ENCERRADA', 'CANCELADA'].map(valor => ({ id: valor, nome: valor }));
    }
    return ['ATIVO', 'INATIVO', 'PLANEJADO', 'EM_ANDAMENTO', 'ABERTA', 'ENCERRADA', 'PENDENTE', 'APROVADO', 'REPROVADO', 'CURSANDO'].map(valor => ({ id: valor, nome: valor }));
  }

  private acaoPdf() {
    if (this.endpoint === 'cursos') return 'grade-pdf';
    if (this.endpoint === 'disciplinas') return 'ementa-pdf';
    return 'plano-pdf';
  }

  private carregarOfertasAgrupadas() {
    this.api.listar('ofertas-disciplinas').subscribe(ofertas => {
      const grupos = new Map<string, any>();
      ofertas
        .filter(oferta => ['ABERTA', 'EM_ANDAMENTO', 'PLANEJADA'].includes(oferta.status))
        .forEach(oferta => {
          const modulo = oferta.modulo?.nome || 'Sem modulo';
          if (!grupos.has(modulo)) grupos.set(modulo, { nome: modulo, ofertas: [] });
          grupos.get(modulo).ofertas.push(oferta);
        });
      this.ofertasPorModulo = Array.from(grupos.values());
    });
  }

  private finalizarLoteMatriculas(concluidas: number, total: number, falhas: string[]) {
    if (concluidas !== total) return;
    this.carregando = false;
    this.ofertasSelecionadas = {};
    this.mensagem = falhas.length ? falhas.join('; ') : 'Matriculas realizadas com sucesso';
    this.carregar();
  }
}
