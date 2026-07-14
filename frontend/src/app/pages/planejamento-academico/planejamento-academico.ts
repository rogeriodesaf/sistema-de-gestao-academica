import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-planejamento-academico',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './planejamento-academico.html',
  styleUrl: './planejamento-academico.scss'
})
export class PlanejamentoAcademicoPage implements OnInit {
  ofertas: any[] = [];
  disciplinas: any[] = [];
  professores: any[] = [];
  modulos: any[] = [];
  turmas: any[] = [];
  cursos: any[] = [];
  anosLetivos: any[] = [];
  periodosLetivos: any[] = [];
  mensagem = '';
  carregando = true;
  salvando = false;
  editandoId?: number;
  formulario: Record<string, any> = this.formularioVazio();

  constructor(private api: ApiService, private router: Router, private route: ActivatedRoute,
              private cd: ChangeDetectorRef) {}

  ngOnInit() {
    this.carregarTudo();
  }

  carregarTudo() {
    this.carregando = true;
    forkJoin({
      ofertas: this.api.listar('ofertas-disciplinas'),
      disciplinas: this.api.listar('disciplinas'),
      professores: this.api.listar('professores'),
      modulos: this.api.listar('modulos'),
      turmas: this.api.listar('turmas'),
      cursos: this.api.listar('cursos'),
      anosLetivos: this.api.listar('anos-letivos'),
      periodosLetivos: this.api.listar('periodos-letivos')
    }).pipe(finalize(() => {
      this.carregando = false;
      this.cd.detectChanges();
    })).subscribe({
      next: dados => {
        this.ofertas = dados.ofertas || [];
        this.disciplinas = dados.disciplinas || [];
        this.professores = dados.professores || [];
        this.modulos = dados.modulos || [];
        this.turmas = dados.turmas || [];
        this.cursos = dados.cursos || [];
        this.anosLetivos = dados.anosLetivos || [];
        this.periodosLetivos = dados.periodosLetivos || [];
        this.aplicarEdicaoDaUrl();
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel carregar o planejamento academico.';
      }
    });
  }

  salvar() {
    const erro = this.validarFormulario();
    if (erro) {
      this.mensagem = erro;
      return;
    }

    this.salvando = true;
    this.mensagem = '';
    const turmaPayload = this.montarTurma();
    const turmaRequest = this.formulario['turma.id']
      ? this.api.atualizar('turmas', Number(this.formulario['turma.id']), turmaPayload)
      : this.api.salvar('turmas', turmaPayload);

    turmaRequest.subscribe({
      next: (turma: any) => this.salvarOferta(turma),
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel salvar a turma da oferta.';
        this.salvando = false;
      }
    });
  }

  salvarOferta(turma: any) {
    const oferta = this.montarOferta(turma);
    const request = this.editandoId
      ? this.api.atualizar('ofertas-disciplinas', this.editandoId, oferta)
      : this.api.salvar('ofertas-disciplinas', oferta);

    request.subscribe({
      next: () => {
        this.mensagem = this.editandoId ? 'Oferta atualizada com sucesso.' : 'Oferta academica salva com sucesso.';
        this.cancelar();
        this.carregarTudo();
        this.salvando = false;
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel salvar a oferta academica.';
        this.salvando = false;
      }
    });
  }

  editar(oferta: any) {
    this.editandoId = oferta.id;
    this.formulario = {
      'anoLetivo.id': oferta.anoLetivo?.id || '',
      'periodoLetivo.id': oferta.periodoLetivo?.id || '',
      'disciplina.id': oferta.disciplina?.id || '',
      'modulo.id': oferta.modulo?.id || '',
      'professor.id': oferta.professor?.id || '',
      'turma.id': oferta.turma?.id || '',
      nomeTurma: oferta.turma?.nome || '',
      'curso.id': oferta.curso?.id || oferta.turma?.curso?.id || '',
      horario: oferta.horario || oferta.turma?.horario || '',
      sala: oferta.sala || oferta.turma?.sala || '',
      vagas: oferta.vagas || oferta.turma?.quantidadeMaximaAlunos || '',
      dataInicio: oferta.dataInicio || oferta.turma?.dataInicio || '',
      dataFim: oferta.dataFim || oferta.turma?.dataTermino || '',
      cargaHorariaPrevista: oferta.cargaHorariaPrevista || oferta.disciplina?.cargaHoraria || '',
      status: oferta.status || 'ABERTA'
    };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  selecionarPeriodoTecnico() {
    if (this.formulario['periodoLetivo.id']) return;
    const periodo = this.periodoTecnicoCompativel();
    if (periodo?.id) this.formulario['periodoLetivo.id'] = periodo.id;
  }

  cancelar() {
    this.editandoId = undefined;
    this.formulario = this.formularioVazio();
  }

  visualizar(oferta: any) {
    this.router.navigate(['/ofertas-disciplinas', oferta.id]);
  }

  disciplinaSelecionada() {
    const id = Number(this.formulario['disciplina.id']);
    return this.disciplinas.find(item => item.id === id);
  }

  moduloOfertaSelecionado() {
    const id = Number(this.formulario['modulo.id']);
    return this.modulos.find(item => item.id === id);
  }

  modulosDoAno() {
    const anoId = Number(this.formulario['anoLetivo.id']);
    const anuais = this.modulos.filter(modulo => modulo.anoLetivo?.id === anoId);
    if (anuais.length) return anuais;
    return this.modulos.filter(modulo => !modulo.anoLetivo?.id);
  }

  moduloOriginal(disciplina = this.disciplinaSelecionada()) {
    return disciplina?.moduloOriginal || disciplina?.modulo;
  }

  remanejada(oferta: any) {
    const original = this.moduloOriginal(oferta.disciplina);
    return !!original?.id && !!oferta.modulo?.id && original.id !== oferta.modulo.id;
  }

  remanejamentoFormulario() {
    const original = this.moduloOriginal();
    const oferta = this.moduloOfertaSelecionado();
    return !!original?.id && !!oferta?.id && original.id !== oferta.id;
  }

  rotulo(opcao: any) {
    if (!opcao) return '';
    const partes = [opcao.nome, opcao.codigo, opcao.ano, opcao.periodoLetivo?.nome].filter(Boolean);
    return partes.length ? partes.join(' - ') : `Registro ${opcao.id}`;
  }

  statusOfertas() {
    return ['PLANEJADA', 'ABERTA', 'EM_ANDAMENTO', 'ENCERRADA', 'CANCELADA'];
  }

  private formularioVazio() {
    return {
      'anoLetivo.id': '',
      'periodoLetivo.id': '',
      'disciplina.id': '',
      'modulo.id': '',
      'professor.id': '',
      'turma.id': '',
      nomeTurma: '',
      'curso.id': '',
      horario: '',
      sala: '',
      vagas: '',
      dataInicio: '',
      dataFim: '',
      cargaHorariaPrevista: '',
      status: 'ABERTA'
    };
  }

  private montarTurma() {
    return {
      id: this.formulario['turma.id'] ? Number(this.formulario['turma.id']) : undefined,
      nome: this.formulario['nomeTurma'],
      disciplina: { id: Number(this.formulario['disciplina.id']) },
      professor: { id: Number(this.formulario['professor.id']) },
      curso: this.formulario['curso.id'] ? { id: Number(this.formulario['curso.id']) } : undefined,
      turno: '',
      horario: this.formulario['horario'],
      sala: this.formulario['sala'],
      quantidadeMaximaAlunos: Number(this.formulario['vagas']),
      dataInicio: this.formulario['dataInicio'] || undefined,
      dataTermino: this.formulario['dataFim'] || undefined,
      status: this.statusTurma()
    };
  }

  private montarOferta(turma: any) {
    const periodoId = Number(this.formulario['periodoLetivo.id'] || 0);
    return {
      turma: { id: turma.id },
      anoLetivo: { id: Number(this.formulario['anoLetivo.id']) },
      periodoLetivo: periodoId ? { id: periodoId } : undefined,
      curso: this.formulario['curso.id'] ? { id: Number(this.formulario['curso.id']) } : undefined,
      modulo: { id: Number(this.formulario['modulo.id']) },
      disciplina: { id: Number(this.formulario['disciplina.id']) },
      professor: { id: Number(this.formulario['professor.id']) },
      vagas: Number(this.formulario['vagas']),
      horario: this.formulario['horario'],
      sala: this.formulario['sala'],
      cargaHorariaPrevista: this.formulario['cargaHorariaPrevista'] ? Number(this.formulario['cargaHorariaPrevista']) : this.disciplinaSelecionada()?.cargaHoraria,
      dataInicio: this.formulario['dataInicio'] || undefined,
      dataFim: this.formulario['dataFim'] || undefined,
      status: this.formulario['status']
    };
  }

  private validarFormulario() {
    if (!this.formulario['anoLetivo.id']) return 'Ano letivo obrigatorio.';
    if (!this.formulario['disciplina.id']) return 'Disciplina obrigatoria.';
    if (!this.formulario['modulo.id']) return 'Modulo de oferta obrigatorio.';
    if (!this.formulario['professor.id']) return 'Professor obrigatorio.';
    if (!this.formulario['nomeTurma']) return 'Nome da turma/oferta obrigatorio.';
    if (!this.formulario['horario']) return 'Horario obrigatorio.';
    if (!this.formulario['sala']) return 'Sala obrigatoria.';
    if (!this.formulario['vagas'] || Number(this.formulario['vagas']) <= 0) return 'Vagas obrigatorias.';
    if (!this.formulario['status']) return 'Situacao obrigatoria.';
    return '';
  }

  private statusTurma() {
    if (this.formulario['status'] === 'ENCERRADA') return 'ENCERRADA';
    if (this.formulario['status'] === 'CANCELADA') return 'CANCELADA';
    if (this.formulario['status'] === 'EM_ANDAMENTO') return 'EM_ANDAMENTO';
    return 'PLANEJADA';
  }

  private aplicarEdicaoDaUrl() {
    const editarId = Number(this.route.snapshot.queryParamMap.get('editar'));
    if (!editarId || this.editandoId) return;
    const oferta = this.ofertas.find(item => item.id === editarId);
    if (oferta) this.editar(oferta);
  }

  private periodoTecnicoId() {
    return Number(this.formulario['periodoLetivo.id'] || 0);
  }

  private periodoTecnicoCompativel() {
    const anoId = Number(this.formulario['anoLetivo.id']);
    return this.periodosLetivos.find(periodo => periodo.anoLetivo?.id === anoId)
      || this.periodosLetivos[0];
  }
}
