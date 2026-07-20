import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-area-professor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './area-professor.html',
  styleUrl: './area-professor.scss'
})
export class AreaProfessorPage implements OnInit {
  ofertas: any[] = [];
  ofertaSelecionadaId?: number;
  ofertaSelecionada?: any;
  alunos: any[] = [];
  aulas: any[] = [];
  aulaSelecionada?: any;
  chamada: any[] = [];
  avaliacoes: any[] = [];
  avaliacaoSelecionada?: any;
  notasAvaliacao: any[] = [];
  resultados: any[] = [];
  arquivos: any[] = [];
  arquivoSelecionado?: File;
  novaAvaliacao = this.formularioAvaliacaoInicial();
  novoArquivo = { titulo: '', tipoVinculo: 'DISCIPLINA', referenciaId: undefined as number | undefined };
  mensagem = '';
  tipoMensagem: 'sucesso' | 'erro' = 'sucesso';
  carregandoOfertas = false;
  carregandoAlunos = false;
  alunosCarregados = false;
  salvandoAula = false;
  aulaEmEdicaoId?: number;
  carregandoChamada = false;
  salvandoChamada = false;
  salvandoAvaliacao = false;
  carregandoAvaliacoes = false;
  carregandoNotas = false;
  salvandoNotas = false;
  enviandoArquivo = false;
  encerrandoDiario = false;
  pendenciasEncerramento: string[] = [];
  novaAula = this.formularioAulaInicial();
  private ofertasCarregadas = false;
  private posicaoListaAulas = 0;
  private chamadasPorAula = new Map<number, any[]>();

  get ofertasFuturas() { return this.ofertas.filter(oferta => oferta.status === 'PLANEJADA'); }
  get ofertasEmAndamento() { return this.ofertas.filter(oferta => ['ABERTA', 'EM_ANDAMENTO'].includes(oferta.status)); }
  get ofertasConcluidas() { return this.ofertas.filter(oferta => ['ENCERRADA', 'CONCLUIDA'].includes(oferta.status)); }
  get ofertasCanceladas() { return this.ofertas.filter(oferta => oferta.status === 'CANCELADA'); }

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private changeDetector: ChangeDetectorRef
  ) {}

  ngOnInit() {
    if (!this.auth.logado()) {
      this.mensagem = 'Sua sessao expirou. Entre novamente.';
      return;
    }
    this.carregarOfertas();
  }

  carregarOfertas() {
    if (!this.auth.logado()) {
      this.mensagem = 'Sua sessao expirou. Entre novamente.';
      return;
    }
    if (this.ofertasCarregadas) return;
    this.mensagem = '';
    this.carregandoOfertas = true;
    this.api.listar('professor/ofertas').pipe(
      finalize(() => {
        this.carregandoOfertas = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: ofertas => {
        this.ofertas = Array.isArray(ofertas) ? ofertas : [];
        this.ofertasCarregadas = true;
        if (!this.ofertas.length) {
          this.mensagem = 'Nenhuma disciplina vinculada ao professor.';
          return;
        }

        const ofertaComAlunos = this.ofertas.find(oferta => (oferta.alunosMatriculados || 0) > 0);
        this.selecionarOferta((ofertaComAlunos || this.ofertas[0]).id);
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel carregar as disciplinas do professor.';
      }
    });
  }

  selecionarOferta(ofertaId: number | string | undefined) {
    const id = Number(ofertaId);
    this.ofertaSelecionadaId = Number.isFinite(id) ? id : undefined;
    this.ofertaSelecionada = this.ofertas.find(oferta => oferta.id === this.ofertaSelecionadaId);
    this.alunos = [];
    this.aulas = [];
    this.aulaSelecionada = undefined;
    this.chamada = [];
    this.chamadasPorAula.clear();
    this.avaliacoes = [];
    this.avaliacaoSelecionada = undefined;
    this.notasAvaliacao = [];
    this.resultados = [];
    this.arquivos = [];
    this.alunosCarregados = false;
    this.novaAula = this.formularioAulaInicial();
    this.aulaEmEdicaoId = undefined;
    this.novaAvaliacao = this.formularioAvaliacaoInicial();
    this.pendenciasEncerramento = [];
  }

  carregarAlunos() {
    if (!this.auth.logado()) {
      this.mensagem = 'Sua sessao expirou. Entre novamente.';
      return;
    }
    if (!this.ofertaSelecionadaId) {
      this.mensagem = 'Selecione uma disciplina.';
      return;
    }

    this.mensagem = '';
    this.carregandoAlunos = true;
    this.alunosCarregados = false;
    this.api.buscar('professor/ofertas', `${this.ofertaSelecionadaId}/diario`).subscribe({
      next: diario => {
        if (diario?.oferta) {
          this.ofertaSelecionada = { ...this.ofertaSelecionada, ...diario.oferta };
          this.atualizarOfertaSelecionada();
        }
        this.alunos = Array.isArray(diario?.matriculas) ? diario.matriculas : [];
        this.aulas = Array.isArray(diario?.aulas) ? diario.aulas : [];
        this.carregandoAlunos = false;
        this.alunosCarregados = true;
        this.carregarComplementos();
        this.changeDetector.detectChanges();
      },
      error: err => {
        this.alunos = [];
        this.carregandoAlunos = false;
        this.alunosCarregados = true;
        this.mensagem = err?.error?.mensagem || 'Não foi possível carregar os alunos da oferta.';
        this.changeDetector.detectChanges();
      }
    });
  }

  get diarioBloqueado(): boolean {
    return !!this.ofertaSelecionada && this.ofertaSelecionada.status !== 'EM_ANDAMENTO';
  }

  encerrarDiario() {
    if (!this.ofertaSelecionadaId || this.diarioBloqueado) return;
    this.encerrandoDiario = true;
    this.pendenciasEncerramento = [];
    this.api.salvar(`professor/ofertas/${this.ofertaSelecionadaId}/encerrar-diario`, {}).pipe(
      finalize(() => {
        this.encerrandoDiario = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: (resposta: any) => {
        this.pendenciasEncerramento = resposta?.pendencias || [];
        if (this.pendenciasEncerramento.length) {
          this.mostrarMensagem(resposta.mensagem || 'O diário possui pendências.', 'erro');
          return;
        }
        this.ofertaSelecionada = { ...this.ofertaSelecionada, status: resposta.status };
        this.atualizarOfertaSelecionada();
        this.mostrarMensagem(resposta.mensagem || 'Diário encerrado.', 'sucesso');
      },
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Não foi possível encerrar o diário.', 'erro')
    });
  }

  private atualizarOfertaSelecionada() {
    this.ofertas = this.ofertas.map(oferta => oferta.id === this.ofertaSelecionadaId
      ? { ...oferta, ...this.ofertaSelecionada } : oferta);
  }

  registrarAula() {
    if (!this.ofertaSelecionadaId) {
      this.mostrarMensagem('Selecione uma disciplina.', 'erro');
      return;
    }
    if (!this.novaAula.dataAula || !this.novaAula.conteudoMinistrado.trim()) {
      this.mostrarMensagem('Informe a data e o conteúdo ministrado.', 'erro');
      return;
    }
    if (!this.novaAula.cargaHorariaAula || this.novaAula.cargaHorariaAula <= 0) {
      this.mostrarMensagem('A carga horária deve ser maior que zero.', 'erro');
      return;
    }

    this.salvandoAula = true;
    const requisicao = this.aulaEmEdicaoId
      ? this.api.atualizar('professor/aulas', this.aulaEmEdicaoId, this.novaAula)
      : this.api.salvar(`professor/ofertas/${this.ofertaSelecionadaId}/aulas`, this.novaAula);
    requisicao.subscribe({
      next: aula => {
        const aulaSalva = aula as any;
        const editando = !!this.aulaEmEdicaoId;
        this.aulas = editando
          ? this.aulas.map(item => item.id === aulaSalva.id ? aulaSalva : item)
          : [aulaSalva, ...this.aulas];
        this.novaAula = this.formularioAulaInicial();
        this.aulaEmEdicaoId = undefined;
        this.salvandoAula = false;
        this.mostrarMensagem(editando ? 'Aula atualizada com sucesso.' : 'Aula registrada com sucesso. Preencha a chamada.', 'sucesso');
        this.carregarResultados();
        if (!editando) this.abrirChamada(aulaSalva);
      },
      error: err => {
        this.salvandoAula = false;
        this.mostrarMensagem(err?.error?.mensagem || 'Não foi possível registrar a aula.', 'erro');
        this.changeDetector.detectChanges();
      }
    });
  }

  editarAula(aula: any) {
    this.aulaEmEdicaoId = aula.id;
    this.novaAula = {
      dataAula: aula.dataAula,
      conteudoMinistrado: aula.conteudoMinistrado || '',
      cargaHorariaAula: aula.cargaHorariaAula,
      observacoes: aula.observacoes || ''
    };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelarEdicaoAula() {
    this.aulaEmEdicaoId = undefined;
    this.novaAula = this.formularioAulaInicial();
  }

  excluirAula(aula: any) {
    if (!confirm('Deseja excluir esta aula?')) return;
    this.api.remover(`professor/aulas/${aula.id}`).subscribe({
      next: () => {
        this.aulas = this.aulas.filter(item => item.id !== aula.id);
        this.chamadasPorAula.delete(aula.id);
        if (this.aulaEmEdicaoId === aula.id) this.cancelarEdicaoAula();
        this.carregarResultados();
        this.mostrarMensagem('Aula excluida com sucesso.', 'sucesso');
      },
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel excluir a aula.', 'erro')
    });
  }

  abrirChamada(aula: any) {
    if (!aula?.id) return;
    this.posicaoListaAulas = window.scrollY;
    this.aulaSelecionada = aula;
    this.chamada = [];
    const chamadaEmCache = this.chamadasPorAula.get(aula.id);
    if (chamadaEmCache) {
      this.chamada = chamadaEmCache.map(item => ({ ...item }));
      this.carregandoChamada = false;
      return;
    }
    this.carregandoChamada = true;
    this.api.buscarAcao('professor/aulas', aula.id, 'frequencias').subscribe({
      next: presencas => {
        this.chamada = Array.isArray(presencas) ? presencas : [];
        this.chamadasPorAula.set(aula.id, this.chamada.map(item => ({ ...item })));
        this.carregandoChamada = false;
        if (!this.chamada.length) {
          this.mostrarMensagem('Não há alunos matriculados para realizar a chamada.', 'erro');
        }
        this.changeDetector.detectChanges();
      },
      error: err => {
        this.carregandoChamada = false;
        this.mostrarMensagem(err?.error?.mensagem || 'Não foi possível abrir a chamada.', 'erro');
        this.changeDetector.detectChanges();
      }
    });
  }

  marcarTodosPresentes() {
    this.chamada = this.chamada.map(item => ({ ...item, status: 'PRESENTE' }));
  }

  voltarParaAulas() {
    this.aulaSelecionada = undefined;
    this.chamada = [];
    this.carregandoChamada = false;
    window.setTimeout(() => window.scrollTo({ top: this.posicaoListaAulas, behavior: 'auto' }));
  }

  salvarChamada() {
    if (!this.aulaSelecionada?.id) {
      this.mostrarMensagem('Selecione uma aula antes de salvar a chamada.', 'erro');
      return;
    }
    if (!this.chamada.length) {
      this.mostrarMensagem('Não há alunos matriculados para realizar a chamada.', 'erro');
      return;
    }

    this.salvandoChamada = true;
    const dados = {
      presencas: this.chamada.map(item => ({
        matriculaId: item.matriculaId,
        status: item.status,
        observacao: item.observacao || null
      }))
    };
    this.api.acao('professor/aulas', this.aulaSelecionada.id, 'frequencias', dados).subscribe({
      next: resposta => {
        this.salvandoChamada = false;
        this.chamadasPorAula.set(this.aulaSelecionada.id, this.chamada.map(item => ({ ...item, salva: true })));
        this.aulas = this.aulas.map(aula => aula.id === this.aulaSelecionada.id
          ? { ...aula, chamadaPreenchida: true }
          : aula);
        this.carregarResultados();
        this.voltarParaAulas();
        this.mostrarMensagem((resposta as any)?.mensagem || 'Chamada salva com sucesso.', 'sucesso');
        this.changeDetector.detectChanges();
      },
      error: err => {
        this.salvandoChamada = false;
        this.mostrarMensagem(err?.error?.mensagem || 'Não foi possível salvar a chamada.', 'erro');
        this.changeDetector.detectChanges();
      }
    });
  }

  salvarAvaliacao() {
    if (!this.ofertaSelecionadaId || !this.novaAvaliacao.nome.trim()) {
      this.mostrarMensagem('Informe ao menos o nome da avaliacao.', 'erro');
      return;
    }
    if (this.novaAvaliacao.ordem <= 0 || this.novaAvaliacao.notaMaxima <= 0 || this.novaAvaliacao.peso <= 0) {
      this.mostrarMensagem('Ordem, nota maxima e peso devem ser maiores que zero.', 'erro');
      return;
    }
    this.salvandoAvaliacao = true;
    const requisicao = this.novaAvaliacao.id
      ? this.api.atualizar('professor/avaliacoes', this.novaAvaliacao.id, this.novaAvaliacao)
      : this.api.salvar(`professor/ofertas/${this.ofertaSelecionadaId}/avaliacoes`, this.novaAvaliacao);
    requisicao.subscribe({
      next: () => {
        this.salvandoAvaliacao = false;
        this.novaAvaliacao = this.formularioAvaliacaoInicial();
        this.carregarAvaliacoes();
        this.carregarResultados();
        this.mostrarMensagem('Avaliacao salva com sucesso.', 'sucesso');
      },
      error: err => {
        this.salvandoAvaliacao = false;
        this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel salvar a avaliacao.', 'erro');
      }
    });
  }

  editarAvaliacao(avaliacao: any) {
    this.novaAvaliacao = { ...avaliacao, data: avaliacao.data || '' };
  }

  cancelarEdicaoAvaliacao() {
    this.novaAvaliacao = this.formularioAvaliacaoInicial();
  }

  excluirAvaliacao(avaliacao: any) {
    this.api.remover(`professor/avaliacoes/${avaliacao.id}`).subscribe({
      next: () => {
        this.carregarAvaliacoes();
        this.carregarResultados();
        this.mostrarMensagem('Avaliacao excluida.', 'sucesso');
      },
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel excluir a avaliacao.', 'erro')
    });
  }

  abrirNotas(avaliacao: any) {
    this.avaliacaoSelecionada = avaliacao;
    this.notasAvaliacao = [];
    this.carregandoNotas = true;
    this.api.buscarAcao('professor/avaliacoes', avaliacao.id, 'notas').subscribe({
      next: notas => {
        this.notasAvaliacao = (Array.isArray(notas) ? notas : []).map((item: any) => ({
          ...item,
          notaOriginal: item.nota,
          observacaoOriginal: item.observacao || null
        }));
        this.carregandoNotas = false;
        this.changeDetector.detectChanges();
      },
      error: err => {
        this.carregandoNotas = false;
        this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel carregar as notas.', 'erro');
      }
    });
  }

  salvarNotas() {
    if (!this.avaliacaoSelecionada?.id || this.salvandoNotas) return;
    const avaliacaoId = this.avaliacaoSelecionada.id;
    const notas = this.notasAvaliacao
      .filter(item => item.nota !== null && item.nota !== undefined && item.nota !== ''
        && ((item.notaOriginal === null || item.notaOriginal === undefined || item.notaOriginal === '')
          || Number(item.nota) !== Number(item.notaOriginal)
          || (item.observacao || null) !== item.observacaoOriginal))
      .map(item => ({ matriculaId: item.matriculaId, nota: Number(item.nota), observacao: item.observacao || null }));
    if (!notas.length) {
      this.mostrarMensagem('Nenhuma nota foi alterada.', 'erro');
      return;
    }
    this.salvandoNotas = true;
    this.api.acao('professor/avaliacoes', avaliacaoId, 'notas', { notas }).pipe(
      finalize(() => {
        this.salvandoNotas = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: resposta => {
        this.avaliacaoSelecionada = undefined;
        this.notasAvaliacao = [];
        this.carregarResultados();
        this.mostrarMensagem((resposta as any)?.mensagem || 'Notas salvas com sucesso.', 'sucesso');
      },
      error: err => {
        this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel salvar as notas.', 'erro');
      }
    });
  }

  selecionarPdf(evento: Event) {
    const arquivo = (evento.target as HTMLInputElement).files?.[0];
    if (!arquivo) return;
    if (arquivo.type !== 'application/pdf' || !arquivo.name.toLowerCase().endsWith('.pdf')) {
      this.arquivoSelecionado = undefined;
      this.mostrarMensagem('Selecione um arquivo PDF.', 'erro');
      return;
    }
    this.arquivoSelecionado = arquivo;
  }

  enviarPdf() {
    if (!this.ofertaSelecionadaId || !this.arquivoSelecionado || !this.novoArquivo.titulo.trim()) {
      this.mostrarMensagem('Informe o titulo e selecione um PDF.', 'erro');
      return;
    }
    if (this.novoArquivo.tipoVinculo === 'AVALIACAO' && !this.novoArquivo.referenciaId) {
      this.mostrarMensagem(this.avaliacoes.length
        ? 'Selecione a avaliacao vinculada.'
        : 'Cadastre uma avaliação antes de enviar um PDF.', 'erro');
      return;
    }
    if (this.novoArquivo.tipoVinculo === 'AULA' && !this.novoArquivo.referenciaId) {
      this.mostrarMensagem('Selecione a aula ou avaliacao vinculada.', 'erro');
      return;
    }
    const dados = new FormData();
    dados.append('arquivo', this.arquivoSelecionado);
    dados.append('titulo', this.novoArquivo.titulo.trim());
    dados.append('ofertaDisciplinaId', String(this.ofertaSelecionadaId));
    if (this.novoArquivo.tipoVinculo === 'AULA') dados.append('aulaId', String(this.novoArquivo.referenciaId));
    if (this.novoArquivo.tipoVinculo === 'AVALIACAO') dados.append('avaliacaoId', String(this.novoArquivo.referenciaId));
    this.enviandoArquivo = true;
    this.api.enviarFormulario('professor/arquivos', dados).pipe(
      finalize(() => {
        this.enviandoArquivo = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: (arquivo: any) => {
        this.arquivoSelecionado = undefined;
        this.novoArquivo = { titulo: '', tipoVinculo: 'DISCIPLINA', referenciaId: undefined };
        this.arquivos = [arquivo, ...this.arquivos.filter(item => item.id !== arquivo.id)];
        this.mostrarMensagem('PDF enviado com sucesso.', 'sucesso');
      },
      error: err => {
        this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel enviar o PDF.', 'erro');
      }
    });
  }

  abrirPdf(arquivo: any) {
    this.api.baixar(`professor/arquivos/${arquivo.id}`).subscribe({
      next: conteudo => {
        const url = URL.createObjectURL(new Blob([conteudo], { type: 'application/pdf' }));
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60000);
      },
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel abrir o PDF.', 'erro')
    });
  }

  removerPdf(arquivo: any) {
    this.api.remover(`professor/arquivos/${arquivo.id}`).subscribe({
      next: () => {
        this.carregarArquivos();
        this.mostrarMensagem('PDF removido.', 'sucesso');
      },
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel remover o PDF.', 'erro')
    });
  }

  arquivosDaAula(aulaId: number) {
    return this.arquivos.filter(arquivo => arquivo.aulaId === aulaId);
  }

  arquivosDaAvaliacao(avaliacaoId: number) {
    return this.arquivos.filter(arquivo => arquivo.avaliacaoId === avaliacaoId);
  }

  notaResultado(resultado: any, avaliacaoId: number) {
    return resultado?.notas?.find((nota: any) => nota.avaliacaoId === avaliacaoId)?.nota;
  }

  tipoVinculoTexto(tipo: string) {
    return ({ DISCIPLINA: 'Disciplina', AULA: 'Aula', AVALIACAO: 'Avaliacao' } as Record<string, string>)[tipo] || tipo;
  }

  uploadPdfDesabilitado() {
    if (this.enviandoArquivo || !this.novoArquivo.tipoVinculo) return true;
    return this.novoArquivo.tipoVinculo === 'AVALIACAO'
      && (this.carregandoAvaliacoes || !this.novoArquivo.referenciaId);
  }

  situacaoFrequenciaTexto(situacao: string) {
    return ({
      OK: 'OK',
      LIMITE: 'Limite',
      REPROVADO_POR_FALTA: 'Reprovado por falta',
      SEM_CHAMADAS: 'Sem chamadas'
    } as Record<string, string>)[situacao] || situacao;
  }

  private carregarComplementos() {
    this.carregarAvaliacoes();
    this.carregarArquivos();
    this.carregarResultados();
  }

  private carregarAvaliacoes() {
    if (!this.ofertaSelecionadaId) return;
    this.carregandoAvaliacoes = true;
    this.api.buscarAcao('professor/ofertas', this.ofertaSelecionadaId, 'avaliacoes').pipe(
      finalize(() => {
        this.carregandoAvaliacoes = false;
        this.changeDetector.detectChanges();
      })
    ).subscribe({
      next: dados => {
        this.avaliacoes = Array.isArray(dados) ? dados : [];
      },
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel carregar as avaliacoes.', 'erro')
    });
  }

  private carregarResultados() {
    if (!this.ofertaSelecionadaId) return;
    this.api.buscarAcao('professor/ofertas', this.ofertaSelecionadaId, 'resultados').subscribe({
      next: dados => {
        this.resultados = Array.isArray(dados) ? dados : [];
        this.changeDetector.detectChanges();
      },
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel carregar as medias.', 'erro')
    });
  }

  private carregarArquivos() {
    if (!this.ofertaSelecionadaId) return;
    this.api.buscarAcao('professor/ofertas', this.ofertaSelecionadaId, 'arquivos').pipe(
      finalize(() => this.changeDetector.detectChanges())
    ).subscribe({
      next: dados => this.arquivos = Array.isArray(dados) ? dados : [],
      error: err => this.mostrarMensagem(err?.error?.mensagem || 'Nao foi possivel carregar os arquivos.', 'erro')
    });
  }

  statusTexto(status: string) {
    return ({ PRESENTE: 'Presente', AUSENTE: 'Ausente', JUSTIFICADO: 'Justificado' } as Record<string, string>)[status] || status;
  }

  statusOfertaTexto(status: string) {
    return ({
      PLANEJADA: 'Planejada', ABERTA: 'Aberta', EM_ANDAMENTO: 'Em andamento',
      AGUARDANDO_HOMOLOGACAO: 'Aguardando homologação', ENCERRADA: 'Encerrada',
      CONCLUIDA: 'Concluída', CANCELADA: 'Cancelada'
    } as Record<string, string>)[status] || status;
  }

  resultadoTexto(situacao: string) {
    return ({
      EM_ANDAMENTO: 'Em andamento', APROVADO: 'Aprovado',
      REPROVADO_POR_NOTA: 'Reprovado por nota',
      REPROVADO_POR_FREQUENCIA: 'Reprovado por frequência',
      REPROVADO_POR_NOTA_E_FREQUENCIA: 'Reprovado por nota e frequência'
    } as Record<string, string>)[situacao] || situacao;
  }

  private mostrarMensagem(texto: string, tipo: 'sucesso' | 'erro') {
    this.mensagem = texto;
    this.tipoMensagem = tipo;
  }

  private formularioAulaInicial() {
    const hoje = new Date();
    const dataLocal = new Date(hoje.getTime() - hoje.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    return { dataAula: dataLocal, conteudoMinistrado: '', cargaHorariaAula: 1, observacoes: '' };
  }

  private formularioAvaliacaoInicial() {
    return { id: undefined as number | undefined, nome: '', descricao: '', ordem: 1, data: '', notaMaxima: 10, peso: 1 };
  }
}
