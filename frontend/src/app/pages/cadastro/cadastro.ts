import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';
import { PdfCardComponent } from '../../shared/ui/pdf-card/pdf-card';

@Component({
  selector: 'app-cadastro',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent, PdfCardComponent],
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
  buscaHistorico = '';
  carregando = false;
  registroEditandoId?: number;
  ementaPlanoPendente?: File;
  matriculaVisualizada?: any;
  desempenhoMatricula?: any;
  filtros = {
    busca: '',
    cursoId: '',
    moduloId: '',
    professorId: '',
    status: '',
    tipo: ''
  };
  buscaDisciplinaModulo = '';
  moduloGerenciado?: any;
  buscaDisciplinasModal = '';
  paginaAtual = 1;
  itensPorPagina = 10;
  tamanhosPagina = [10, 25, 50, 100];

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
    'periodoLetivo.id': 'Modulo de oferta',
    'ofertaDisciplina.id': 'Oferta de disciplina',
    ativo: 'Ativo',
    ano: 'Ano',
    anoPeriodo: 'Ano/periodo',
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
    dataFim: 'Data de término',
    dataIngresso: 'Data de ingresso',
    dataInicio: 'Data de início',
    dataMatricula: 'Data da matricula',
    dataNascimento: 'Data de nascimento',
    dataTermino: 'Data de termino',
    descricao: 'Descricao',
    ementaResumo: 'Resumo da ementa',
    ementaStatus: 'Ementa',
    email: 'E-mail',
    endereco: 'Endereco',
    formacao: 'Formacao',
    frequenciaFinal: 'Frequencia final',
    horario: 'Horario',
    justificativa: 'Justificativa',
    metodologia: 'Metodologia',
    moduloAtual: 'Modulo atual',
    moduloOriginalOferta: 'Modulo original',
    nome: 'Nome',
    notaFinal: 'Nota final',
    nota1: 'Nota 1',
    nota2: 'Nota 2',
    observacoes: 'Observacoes',
    observacao: 'Observacao',
    objetivos: 'Objetivos',
    ordem: 'Ordem',
    periodoCursado: 'Modulo cursado',
    presente: 'Presenca',
    quantidadeMaximaAlunos: 'Quantidade maxima de alunos',
    sala: 'Sala',
    situacao: 'Situacao',
    status: 'Situacao',
    telefone: 'Telefone',
    trabalho: 'Trabalho',
    turno: 'Turno',
    vagas: 'Vagas'
  };

  constructor(private route: ActivatedRoute, private router: Router, private api: ApiService) {}

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.titulo = data['titulo'];
      this.endpoint = data['endpoint'];
      if (this.endpoint === 'historicos') this.titulo = 'Historico escolar';
      if (this.endpoint === 'planos-ensino') this.titulo = 'Plano de Ensino';
      if (this.endpoint === 'matriculas-disciplinas') this.titulo = 'Matrículas em Disciplinas';
      this.campos = data['campos'];
      this.cancelarEdicao();
      this.aplicarEstadoDaUrl();
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
    if (this.endpoint === 'anos-letivos' && !this.validarAnoLetivo()) return;
    const dados = this.montarObjeto();
    this.carregando = true;
    const requisicao = this.registroEditandoId
      ? this.api.atualizar(this.endpoint, this.registroEditandoId, dados)
      : this.api.salvar(this.endpoint, dados);
    requisicao.subscribe({
      next: (registro: any) => {
        this.mensagem = this.registroEditandoId ? 'Registro atualizado com sucesso' : 'Registro salvo com sucesso';
        if (this.endpoint === 'planos-ensino' && this.ementaPlanoPendente) {
          this.enviarEmentaAposSalvar(registro);
          return;
        }
        this.cancelarEdicao();
        this.carregar();
        this.carregando = false;
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel salvar';
        this.carregando = false;
      }
    });
  }

  editar(registro: any) {
    this.registroEditandoId = registro.id;
    this.formulario = {};
    for (const campo of this.campos) {
      if (campo.endsWith('.id')) {
        const nome = campo.split('.')[0];
        this.formulario[campo] = registro[nome]?.id ?? '';
      } else if (registro[campo] !== undefined && registro[campo] !== null) {
        this.formulario[campo] = registro[campo];
      }
    }
    this.mensagem = `Editando ${this.rotuloOpcao(registro)}`;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelarEdicao() {
    this.formulario = this.endpoint === 'matriculas-disciplinas'
      ? { status: 'ATIVA', dataMatricula: new Date().toISOString().slice(0, 10) }
      : {};
    this.registroEditandoId = undefined;
    this.ementaPlanoPendente = undefined;
  }

  excluir(id: number) {
    const cancelamento = this.endpoint === 'matriculas-disciplinas';
    if (!confirm(cancelamento ? 'Deseja cancelar esta matrícula?' : 'Deseja excluir este registro?')) return;
    this.api.excluir(this.endpoint, id).subscribe({
      next: () => {
        this.mensagem = cancelamento ? 'Matrícula cancelada com sucesso' : 'Registro excluido com sucesso';
        this.carregar();
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel excluir. Verifique se este registro esta vinculado a outros cadastros.';
      }
    });
  }

  chaves(registro: any) {
    if (this.endpoint === 'planos-ensino') {
      return ['codigoDisciplina', 'nomeDisciplina', 'ementaPdf', 'ultimaAtualizacao', 'situacaoPlano'];
    }
    if (this.endpoint === 'anos-letivos') {
      return ['ano', 'dataInicio', 'dataFim', 'status'];
    }
    if (this.endpoint === 'disciplinas') {
      return ['nome', 'codigo', 'curso', 'modulo', 'professorResponsavel', 'ativo', 'ementaStatus'];
    }
    if (this.endpoint === 'modulos') {
      return ['nome', 'descricao', 'ordem', 'curso', 'status', 'ativo', 'disciplinasVinculadas'];
    }
    if (this.endpoint === 'turmas') {
      return ['nome', 'disciplina', 'professor', 'anoLetivo', 'turno', 'horario', 'sala', 'quantidadeMaximaAlunos', 'status'];
    }
    if (this.endpoint === 'ofertas-disciplinas' || this.endpoint === 'montagem-periodo') {
      return ['disciplina', 'professor', 'moduloOriginalOferta', 'modulo', 'turma', 'vagas', 'horario', 'sala', 'status'];
    }
    if (this.endpoint === 'matriculas-disciplinas') {
      return ['aluno', 'curso', 'periodoLetivo', 'moduloOferta', 'ofertaDisciplina', 'dataMatricula', 'status', 'resultadoAcademico', 'observacoes'];
    }
    if (this.endpoint === 'historicos') {
      return ['aluno', 'curso', 'disciplina', 'codigo', 'modulo', 'periodoCursado', 'cargaHoraria',
        'creditos', 'notaFinal', 'frequenciaFinal', 'situacao', 'professor'];
    }
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
    if (this.endpoint === 'matriculas-disciplinas') {
      const labelsMatricula: Record<string, string> = {
        aluno: 'Aluno', curso: 'Curso', periodoLetivo: 'Período Letivo', moduloOferta: 'Módulo',
        ofertaDisciplina: 'Oferta da Disciplina', dataMatricula: 'Data da Matrícula',
        status: 'Situação da Matrícula', resultadoAcademico: 'Resultado Acadêmico', observacoes: 'Observações'
      };
      if (labelsMatricula[campo]) return labelsMatricula[campo];
    }
    if (this.endpoint === 'planos-ensino') {
      const labelsPlano: Record<string, string> = {
        'disciplina.id': 'Disciplina',
        objetivos: 'Objetivos',
        conteudoProgramatico: 'Conteúdo programático',
        metodologia: 'Metodologia oficial',
        bibliografiaBasica: 'Bibliografia básica',
        bibliografiaComplementar: 'Bibliografia complementar',
        observacoes: 'Observações da disciplina',
        codigoDisciplina: 'Código da disciplina',
        nomeDisciplina: 'Disciplina',
        ementaPdf: 'Ementa oficial em PDF',
        ultimaAtualizacao: 'Última atualização',
        situacaoPlano: 'Situação'
      };
      if (labelsPlano[campo]) return labelsPlano[campo];
    }
    return this.labels[campo] || campo.replace('.id', '').replace(/([A-Z])/g, ' $1').trim();
  }

  subtitulo() {
    if (this.endpoint === 'relatorios') return 'Relatorios disponiveis para acompanhamento academico e administrativo.';
    if (this.endpoint === 'matriz-curricular') return 'Grade oficial do curso. A oferta atual pode usar outro modulo quando houver remanejamento.';
    const textos: Record<string, string> = {
      alunos: 'Cadastre e acompanhe os alunos vinculados aos cursos do seminario.',
      professores: 'Mantenha professores, contatos e situacao cadastral atualizados.',
      cursos: 'Cadastre e mantenha os cursos oferecidos pelo seminario.',
      disciplinas: 'Organize disciplinas, ementas, carga horaria e professores responsaveis.',
      modulos: 'Estruture os modulos academicos e vincule suas disciplinas.',
      turmas: 'Gerencie a oferta real de disciplinas por turma, professor, modulo de oferta, horario, sala e vagas.',
      'anos-letivos': 'Organize os anos letivos utilizados no planejamento academico.',
      'periodos-letivos': 'Cadastro tecnico mantido para compatibilidade interna das ofertas.',
      'ofertas-disciplinas': 'Planeje a oferta real das disciplinas, com modulo de oferta, professor, turma, horarios, salas e vagas.',
      'matriculas-disciplinas': 'Matricule alunos nas disciplinas ofertadas por módulo.',
      'planos-ensino': 'Cadastre e mantenha o plano institucional das disciplinas.',
      aulas: 'Controle aulas ministradas, conteudo e carga horaria.',
      frequencias: 'Registre presencas, justificativas e observacoes.',
      notas: 'Lance notas e acompanhe resultados academicos.',
      historicos: 'Consulte os resultados consolidados automaticamente pela homologacao.'
    };
    return textos[this.endpoint] || 'Cadastro, consulta e manutencao dos registros academicos.';
  }

  tituloFormulario() {
    if (this.endpoint === 'matriculas-disciplinas') return 'Matrícula em Disciplina';
    if (this.endpoint === 'planos-ensino') {
      return this.registroEditandoId ? 'Edição de Plano de Ensino' : 'Cadastro de Plano de Ensino';
    }
    if (this.registroEditandoId) return `Edicao de ${this.titulo.replaceAll('-', ' ')}`;
    return `Cadastro de ${this.titulo.replaceAll('-', ' ')}`;
  }

  somenteConsulta() {
    return this.endpoint === 'historicos';
  }

  textoPdf(registro: any) {
    if (this.temPdf(registro)) return this.nomePdf(registro);
    if (this.endpoint === 'cursos') return 'Nenhuma grade curricular enviada';
    if (this.endpoint === 'disciplinas') return 'Nenhuma ementa enviada';
    return 'Nenhuma ementa oficial enviada';
  }

  labelPdf() {
    if (this.endpoint === 'cursos') return 'Grade curricular';
    if (this.endpoint === 'disciplinas') return 'Ementa';
    return 'Ementa oficial em PDF';
  }

  textoPdfEnviado() {
    if (this.endpoint === 'cursos') return 'Grade curricular enviada';
    if (this.endpoint === 'disciplinas') return 'Ementa enviada';
    return 'Ementa oficial enviada';
  }

  textoUploadPdf() {
    if (this.endpoint === 'cursos') return 'Enviar Grade Curricular';
    if (this.endpoint === 'disciplinas') return 'Enviar Ementa';
    return 'Enviar ou substituir ementa';
  }

  isSelect(campo: string) {
    return campo.endsWith('.id') || ['status', 'tipo', 'ativo', 'presente'].includes(campo);
  }

  isTextoLongo(campo: string) {
    return campo.includes('observ') || campo.includes('ementaResumo') || campo.includes('bibliografia') || campo.includes('conteudo') || campo.includes('metodologia') || campo.includes('objetivos') || campo.includes('descricao');
  }

  tipoCampo(campo: string) {
    if (campo.toLowerCase().includes('data')) return 'date';
    if (['ano', 'ordem', 'vagas', 'creditos', 'creditosTotais', 'cargaHoraria', 'cargaHorariaTotal', 'cargaHorariaPrevista', 'cargaHorariaMinistrada', 'cargaHorariaAula', 'quantidadeMaximaAlunos', 'nota1', 'nota2', 'trabalho', 'avaliacaoFinal', 'notaFinal', 'frequenciaFinal'].includes(campo)) return 'number';
    return 'text';
  }

  permitePdf() {
    return this.endpoint === 'cursos' || this.endpoint === 'planos-ensino';
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
    if (opcao.disciplina && (opcao.modulo || opcao.anoLetivo || opcao.horario)) {
      const modulo = opcao.modulo?.nome || 'Sem módulo';
      const ano = opcao.anoLetivo?.ano || opcao.periodoLetivo?.anoLetivo?.ano || '';
      const horario = opcao.horario ? ` — ${opcao.horario}` : '';
      return `${opcao.disciplina.nome} — ${modulo}${ano ? '/' + ano : ''}${horario}`;
    }
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
    if (this.endpoint === 'planos-ensino') {
      if (chave === 'codigoDisciplina') return registro.disciplina?.codigo || '-';
      if (chave === 'nomeDisciplina') return registro.disciplina?.nome || '-';
      if (chave === 'ementaPdf') return registro.planoPdfNome ? 'PDF enviado' : 'Sem PDF';
      if (chave === 'ultimaAtualizacao') return registro.ultimaAtualizacao ? new Date(registro.ultimaAtualizacao).toLocaleString('pt-BR') : '-';
      if (chave === 'situacaoPlano') return registro.disciplina?.ativo ? 'ATIVO' : 'INATIVO';
    }
    if (this.endpoint === 'anos-letivos' && ['dataInicio', 'dataFim'].includes(chave) && registro[chave]) {
      return registro[chave].split('-').reverse().join('/');
    }
    if (this.endpoint === 'disciplinas' && chave === 'ativo') return registro.ativo ? 'Ativa' : 'Inativa';
    if (this.endpoint === 'disciplinas' && chave === 'ementaStatus') return registro.ementaPdfNome ? 'Ementa enviada' : 'Sem ementa';
    if (this.endpoint === 'modulos' && chave === 'disciplinasVinculadas') return this.quantidadeDisciplinasModulo(registro);
    if ((this.endpoint === 'ofertas-disciplinas' || this.endpoint === 'montagem-periodo') && chave === 'moduloOriginalOferta') {
      return registro.disciplina?.moduloOriginal?.nome || registro.disciplina?.modulo?.nome || 'Sem modulo original';
    }
    if (this.endpoint === 'matriculas-disciplinas' && chave === 'moduloOferta') {
      return registro.ofertaDisciplina?.modulo?.nome || 'Sem módulo';
    }
    if (this.endpoint === 'matriculas-disciplinas' && chave === 'status') {
      return registro.status === 'TRANCADO' ? 'TRANCADA' : registro.status === 'CANCELADO' ? 'CANCELADA' : registro.status;
    }
    const valor = registro[chave];
    if (valor && typeof valor === 'object') return this.rotuloOpcao(valor);
    if (typeof valor === 'boolean') return valor ? 'Sim' : 'Nao';
    return valor ?? '';
  }

  enviarPdf(registro: any, event: Event) {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) return;
    this.enviarPdfArquivo(registro, arquivo);
  }

  enviarPdfArquivo(registro: any, arquivo: File) {
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

  abrirPdf(registro: any) {
    this.abrirArquivoPdf(this.endpoint, registro.id, this.acaoPdf(), this.nomePdf(registro));
  }

  baixarPdf(registro: any) {
    this.baixarArquivoPdf(this.endpoint, registro.id, this.acaoPdf(), this.nomePdf(registro));
  }

  abrirPdfCurso(curso: any) {
    this.abrirArquivoPdf('cursos', curso.id, 'grade-pdf', curso.gradePdfNome || 'grade-curricular.pdf');
  }

  abrirPdfDisciplina(disciplina: any) {
    this.abrirArquivoPdf('disciplinas', disciplina.id, 'ementa-pdf', disciplina.ementaPdfNome || 'ementa.pdf');
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
    return this.endpoint === 'matriculas-disciplinas' && !this.registroEditandoId && this.opcoes['aluno.id']?.length;
  }

  camposFormulario() {
    if (this.endpoint === 'matriculas-disciplinas') {
      return this.campos.filter(campo => !['aluno.id', 'ofertaDisciplina.id'].includes(campo));
    }
    if (this.endpoint === 'ofertas-disciplinas' || this.endpoint === 'montagem-periodo') {
      return this.campos.filter(campo => campo !== 'periodoLetivo.id');
    }
    if (this.endpoint === 'turmas') {
      return this.campos.filter(campo => campo !== 'periodoLetivo.id');
    }
    return this.campos;
  }

  permiteVinculoDisciplinas() {
    return false;
  }

  disciplinasDoCurso(modulo: any) {
    const busca = this.normalizarBusca(this.buscaDisciplinaModulo);
    return (this.opcoes['disciplina.id'] || [])
      .filter(disciplina => !modulo?.curso?.id || disciplina.curso?.id === modulo.curso.id)
      .filter(disciplina => !busca || this.normalizarBusca(`${disciplina.nome} ${disciplina.codigo}`).includes(busca));
  }

  abrirGerenciamentoDisciplinas(modulo: any) {
    this.moduloGerenciado = modulo;
    this.buscaDisciplinasModal = '';
    if (!this.disciplinasModulo[modulo.id]) {
      const marcadas: Record<number, boolean> = {};
      this.disciplinasDoCurso(modulo).forEach(item => marcadas[item.id] = item.modulo?.id === modulo.id);
      this.disciplinasModulo[modulo.id] = marcadas;
    }
  }

  fecharGerenciamentoDisciplinas() {
    this.moduloGerenciado = undefined;
    this.buscaDisciplinasModal = '';
  }

  disciplinasModal(modulo: any) {
    const buscaAnterior = this.buscaDisciplinaModulo;
    this.buscaDisciplinaModulo = this.buscaDisciplinasModal;
    const disciplinas = this.disciplinasDoCurso(modulo).filter(disciplina => disciplina.ativo || disciplina.modulo?.id === modulo.id);
    this.buscaDisciplinaModulo = buscaAnterior;
    return disciplinas;
  }

  quantidadeDisciplinasModulo(modulo: any) {
    return (this.opcoes['disciplina.id'] || []).filter(disciplina => disciplina.modulo?.id === modulo.id).length;
  }

  quantidadeSelecionadasModulo(modulo: any) {
    return Object.values(this.disciplinasModulo[modulo.id] || {}).filter(Boolean).length;
  }

  registrosFiltrados() {
    if (this.endpoint === 'historicos') {
      const busca = this.normalizarBusca(this.buscaHistorico);
      return this.registros.filter(registro => !busca
        || this.normalizarBusca(`${registro.aluno} ${registro.disciplina} ${registro.codigo}`).includes(busca));
    }
    if (this.endpoint !== 'disciplinas') return this.registros;
    const busca = this.normalizarBusca(this.filtros.busca);
    return this.registros.filter(registro => {
      const tipo = this.tipoDisciplina(registro);
      const status = registro.ativo ? 'ATIVA' : 'INATIVA';
      return (!busca || this.normalizarBusca(`${registro.nome} ${registro.codigo}`).includes(busca))
        && (!this.filtros.cursoId || registro.curso?.id === Number(this.filtros.cursoId))
        && (!this.filtros.moduloId || registro.modulo?.id === Number(this.filtros.moduloId))
        && (!this.filtros.professorId || registro.professorResponsavel?.id === Number(this.filtros.professorId))
        && (!this.filtros.status || status === this.filtros.status)
        && (!this.filtros.tipo || tipo === this.filtros.tipo);
    });
  }

  selecionarRegistro(registro: any) {
    if (this.endpoint === 'disciplinas') {
      this.atualizarEstadoNaUrl(false);
      this.router.navigate(['/disciplinas', registro.id], { queryParams: this.estadoListagem() });
      return;
    }
    if (this.endpoint === 'modulos') {
      this.router.navigate(['/modulos', registro.id]);
    }
  }

  tipoDisciplina(registro: any) {
    if (registro?.tipoComponente) return registro.tipoComponente;
    return this.normalizarBusca(registro?.nome || '').includes('optativa') ? 'OPTATIVA' : 'OBRIGATORIA';
  }

  labelTipoDisciplina(registro: any) {
    const tipo = this.tipoDisciplina(registro);
    if (tipo === 'OPTATIVA') return 'Optativa';
    if (tipo === 'COMPLEMENTAR') return 'Complementar';
    return 'Obrigatoria';
  }

  limparFiltros() {
    this.filtros = { busca: '', cursoId: '', moduloId: '', professorId: '', status: '', tipo: '' };
    this.paginaAtual = 1;
    this.atualizarEstadoNaUrl();
  }

  atualizarFiltro() {
    this.paginaAtual = 1;
    this.atualizarEstadoNaUrl();
  }

  registrosPaginados() {
    if (this.endpoint !== 'disciplinas') return this.registrosFiltrados();
    const inicio = (this.paginaAtual - 1) * this.itensPorPagina;
    return this.registrosFiltrados().slice(inicio, inicio + this.itensPorPagina);
  }

  totalPaginas() {
    return Math.max(Math.ceil(this.registrosFiltrados().length / this.itensPorPagina), 1);
  }

  indiceInicial() {
    if (!this.registrosFiltrados().length) return 0;
    return (this.paginaAtual - 1) * this.itensPorPagina + 1;
  }

  indiceFinal() {
    return Math.min(this.paginaAtual * this.itensPorPagina, this.registrosFiltrados().length);
  }

  mudarPagina(delta: number) {
    this.paginaAtual = Math.min(Math.max(this.paginaAtual + delta, 1), this.totalPaginas());
    this.atualizarEstadoNaUrl();
  }

  mudarTamanhoPagina() {
    this.paginaAtual = 1;
    this.atualizarEstadoNaUrl();
  }

  private normalizarBusca(valor: string) {
    return (valor || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }

  private aplicarEstadoDaUrl() {
    const params = this.route.snapshot.queryParamMap;
    this.filtros = {
      busca: params.get('busca') || '',
      cursoId: params.get('cursoId') || '',
      moduloId: params.get('moduloId') || '',
      professorId: params.get('professorId') || '',
      status: params.get('status') || '',
      tipo: params.get('tipo') || ''
    };
    const tamanho = Number(params.get('tamanho') || 10);
    this.itensPorPagina = this.tamanhosPagina.includes(tamanho) ? tamanho : 10;
    this.paginaAtual = Math.max(Number(params.get('pagina') || 1), 1);
  }

  private atualizarEstadoNaUrl(replaceUrl = true) {
    if (this.endpoint !== 'disciplinas') return;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: this.estadoListagem(),
      replaceUrl
    });
  }

  private estadoListagem() {
    return {
      busca: this.filtros.busca || null,
      cursoId: this.filtros.cursoId || null,
      moduloId: this.filtros.moduloId || null,
      professorId: this.filtros.professorId || null,
      status: this.filtros.status || null,
      tipo: this.filtros.tipo || null,
      pagina: this.paginaAtual,
      tamanho: this.itensPorPagina
    };
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
        this.fecharGerenciamentoDisciplinas();
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
        dataMatricula: this.formulario['dataMatricula'] || undefined,
        status: 'ATIVA',
        observacoes: this.formulario['observacoes'] || undefined
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
    if ((this.endpoint === 'ofertas-disciplinas' || this.endpoint === 'montagem-periodo') && !this.formulario['periodoLetivo.id']) {
      const periodo = this.periodoTecnicoCompativel();
      if (periodo?.id) this.formulario['periodoLetivo.id'] = periodo.id;
    }
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
    if (this.endpoint === 'anos-letivos') {
      return ['PLANEJADO', 'EM_ANDAMENTO', 'ENCERRADO'].map(valor => ({ id: valor, nome: valor }));
    }
    if (this.endpoint === 'modulos') {
      return ['ABERTO', 'FECHADO', 'INATIVO'].map(valor => ({ id: valor, nome: valor }));
    }
    if (this.endpoint === 'matriculas-disciplinas') {
      return [
        { id: 'ATIVA', nome: 'ATIVA' },
        { id: 'TRANCADO', nome: 'TRANCADA' },
        { id: 'CANCELADO', nome: 'CANCELADA' }
      ];
    }
    if (this.endpoint === 'ofertas-disciplinas' || this.endpoint === 'montagem-periodo') {
      return ['PLANEJADA', 'ABERTA', 'EM_ANDAMENTO', 'ENCERRADA', 'CANCELADA'].map(valor => ({ id: valor, nome: valor }));
    }
    return ['ATIVO', 'INATIVO', 'PLANEJADO', 'EM_ANDAMENTO', 'ABERTA', 'ENCERRADA', 'PENDENTE', 'APROVADO', 'REPROVADO', 'CURSANDO'].map(valor => ({ id: valor, nome: valor }));
  }

  private acaoPdf() {
    if (this.endpoint === 'cursos') return 'grade-pdf';
    if (this.endpoint === 'disciplinas') return 'ementa-pdf';
    return 'ementa-pdf';
  }

  private abrirArquivoPdf(endpoint: string, id: number, acao: string, nome: string) {
    this.api.baixarArquivo(endpoint, id, acao).subscribe({
      next: arquivo => {
        const url = URL.createObjectURL(new Blob([arquivo], { type: 'application/pdf' }));
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: err => this.mensagem = err?.error?.mensagem || `Nao foi possivel abrir ${nome}`
    });
  }

  private baixarArquivoPdf(endpoint: string, id: number, acao: string, nome: string) {
    this.api.baixarArquivo(endpoint, id, acao).subscribe({
      next: arquivo => {
        const url = URL.createObjectURL(new Blob([arquivo], { type: 'application/pdf' }));
        const link = document.createElement('a');
        link.href = url;
        link.download = nome || 'arquivo.pdf';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: err => this.mensagem = err?.error?.mensagem || `Nao foi possivel baixar ${nome}`
    });
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
    this.mensagem = falhas.length ? falhas.join('; ') : 'Matrículas realizadas com sucesso';
    this.carregar();
  }

  visualizarMatricula(registro: any) {
    this.matriculaVisualizada = registro;
    this.desempenhoMatricula = undefined;
  }

  verDesempenho(registro: any) {
    this.matriculaVisualizada = registro;
    this.api.buscarAcao('matriculas-disciplinas', registro.id, 'desempenho').subscribe({
      next: desempenho => this.desempenhoMatricula = desempenho,
      error: err => this.mensagem = err?.error?.mensagem || 'Não foi possível carregar o desempenho'
    });
  }

  fecharMatricula() {
    this.matriculaVisualizada = undefined;
    this.desempenhoMatricula = undefined;
  }

  selecionarEmentaPlano(event: Event) {
    const arquivo = (event.target as HTMLInputElement).files?.[0];
    if (!arquivo) return;
    if (arquivo.type !== 'application/pdf' || !arquivo.name.toLowerCase().endsWith('.pdf')) {
      this.mensagem = 'Selecione um arquivo PDF';
      return;
    }
    this.ementaPlanoPendente = arquivo;
  }

  private enviarEmentaAposSalvar(registro: any) {
    const arquivo = this.ementaPlanoPendente!;
    this.api.enviarArquivo('planos-ensino', registro.id, 'ementa-pdf', arquivo).subscribe({
      next: () => {
        this.mensagem = 'Plano de Ensino e ementa salvos com sucesso';
        this.cancelarEdicao();
        this.carregar();
        this.carregando = false;
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Plano salvo, mas não foi possível enviar a ementa';
        this.cancelarEdicao();
        this.carregar();
        this.carregando = false;
      }
    });
  }

  private periodoTecnicoCompativel() {
    const anoId = Number(this.formulario['anoLetivo.id']);
    return (this.opcoes['periodoLetivo.id'] || []).find(periodo => periodo.anoLetivo?.id === anoId)
      || (this.opcoes['periodoLetivo.id'] || [])[0];
  }

  private validarAnoLetivo() {
    const ano = Number(this.formulario['ano']);
    const inicio = this.formulario['dataInicio'];
    const fim = this.formulario['dataFim'];
    const status = this.formulario['status'];
    if (!ano || !inicio || !fim || !status) {
      this.mensagem = 'Informe ano, data de início, data de término e situação';
      return false;
    }
    if (inicio > fim) {
      this.mensagem = 'A data de início deve ser anterior à data de término';
      return false;
    }
    if (this.registros.some(registro => Number(registro.ano) === ano && registro.id !== this.registroEditandoId)) {
      this.mensagem = 'Já existe um Ano Letivo cadastrado para este ano';
      return false;
    }
    return true;
  }
}
