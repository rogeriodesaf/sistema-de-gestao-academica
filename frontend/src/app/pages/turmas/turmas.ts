import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-turmas',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeaderComponent],
  templateUrl: './turmas.html',
  styleUrl: './turmas.scss'
})
export class TurmasPage implements OnInit {
  turmas: any[] = [];
  disciplinas: any[] = [];
  professores: any[] = [];
  cursos: any[] = [];
  anosLetivos: any[] = [];
  periodosLetivos: any[] = [];
  matriculas: any[] = [];
  formulario: Record<string, any> = {};
  editandoId?: number;
  carregando = true;
  salvando = false;
  mensagem = '';
  erroCarregamento = '';

  constructor(private route: ActivatedRoute, private router: Router, private api: ApiService) {}

  ngOnInit() {
    this.limparFormulario();
    this.carregar();
  }

  carregar() {
    this.carregando = true;
    this.erroCarregamento = '';
    forkJoin({
      turmas: this.api.listar('turmas'),
      disciplinas: this.api.listar('disciplinas'),
      professores: this.api.listar('professores'),
      cursos: this.api.listar('cursos'),
      anosLetivos: this.api.listar('anos-letivos'),
      periodosLetivos: this.api.listar('periodos-letivos'),
      matriculas: this.api.listar('matriculas-disciplinas')
    }).subscribe({
      next: dados => {
        this.turmas = dados.turmas || [];
        this.disciplinas = dados.disciplinas || [];
        this.professores = dados.professores || [];
        this.cursos = dados.cursos || [];
        this.anosLetivos = dados.anosLetivos || [];
        this.periodosLetivos = dados.periodosLetivos || [];
        this.matriculas = dados.matriculas || [];
        this.carregando = false;
        this.aplicarEdicaoDaUrl();
      },
      error: err => {
        this.erroCarregamento = err?.error?.mensagem || 'Nao foi possivel carregar as turmas.';
        this.carregando = false;
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
    const payload = this.montarPayload();
    const request = this.editandoId
      ? this.api.atualizar('turmas', this.editandoId, payload)
      : this.api.salvar('turmas', payload);

    request.subscribe({
      next: () => {
        this.mensagem = this.editandoId ? 'Turma atualizada com sucesso.' : 'Turma cadastrada com sucesso.';
        this.salvando = false;
        this.limparFormulario();
        this.carregar();
      },
      error: err => {
        this.mensagem = err?.error?.mensagem || 'Nao foi possivel salvar a turma.';
        this.salvando = false;
      }
    });
  }

  editar(turma: any) {
    this.editandoId = turma.id;
    this.formulario = {
      nome: turma.nome || '',
      'disciplina.id': turma.disciplina?.id || '',
      'professor.id': turma.professor?.id || '',
      'anoLetivo.id': turma.anoLetivo?.id || '',
      'periodoLetivo.id': turma.periodoLetivo?.id || '',
      'curso.id': turma.curso?.id || '',
      turno: turma.turno || '',
      horario: turma.horario || '',
      sala: turma.sala || '',
      quantidadeMaximaAlunos: turma.quantidadeMaximaAlunos || '',
      dataInicio: turma.dataInicio || '',
      dataTermino: turma.dataTermino || '',
      status: turma.status || 'ABERTA',
      descricao: turma.descricao || ''
    };
    this.mensagem = `Editando ${turma.nome}`;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  visualizar(turma: any) {
    this.router.navigate(['/turmas', turma.id]);
  }

  limparFormulario() {
    this.editandoId = undefined;
    this.formulario = {
      nome: '',
      'disciplina.id': '',
      'professor.id': '',
      'anoLetivo.id': '',
      'periodoLetivo.id': '',
      'curso.id': '',
      turno: '',
      horario: '',
      sala: '',
      quantidadeMaximaAlunos: 30,
      dataInicio: '',
      dataTermino: '',
      status: 'ABERTA',
      descricao: ''
    };
  }

  private aplicarEdicaoDaUrl() {
    const editarId = Number(this.route.snapshot.queryParamMap.get('editar'));
    if (!editarId) return;
    const turma = this.turmas.find(item => item.id === editarId);
    if (turma) this.editar(turma);
  }

  sugerirNome() {
    if (this.editandoId || this.formulario['nome']) return;
    const disciplina = this.disciplinas.find(item => item.id === Number(this.formulario['disciplina.id']));
    const ano = this.anoSelecionado();
    if (!disciplina?.nome || !this.formulario['turno'] || !ano) return;
    this.formulario['nome'] = [disciplina.nome, this.formulario['turno'], ano].join(' - ');
  }

  alunosAtivos(turma: any) {
    return this.matriculas.filter(matricula =>
      matricula.ofertaDisciplina?.turma?.id === turma.id
      && !['CANCELADA', 'TRANCADA'].includes(matricula.status)
    );
  }

  ocupacao(turma: any) {
    const total = turma.quantidadeMaximaAlunos || 0;
    return `${this.alunosAtivos(turma).length}/${total}`;
  }

  vagasDisponiveis(turma: any) {
    const total = turma.quantidadeMaximaAlunos || 0;
    return Math.max(total - this.alunosAtivos(turma).length, 0);
  }

  lotada(turma: any) {
    const total = turma.quantidadeMaximaAlunos || 0;
    return total > 0 && this.alunosAtivos(turma).length >= total;
  }

  statusLabel(status: string) {
    const labels: Record<string, string> = {
      ABERTA: 'Aberta',
      EM_ANDAMENTO: 'Em andamento',
      ENCERRADA: 'Encerrada',
      CANCELADA: 'Cancelada',
      PLANEJADA: 'Planejada',
      CONCLUIDA: 'Concluida'
    };
    return labels[status] || status || 'Nao informada';
  }

  rotulo(opcao: any) {
    if (!opcao) return '';
    return opcao.nome || opcao.codigo || opcao.ano || `Registro ${opcao.id}`;
  }

  private anoSelecionado() {
    const ano = this.anosLetivos.find(item => item.id === Number(this.formulario['anoLetivo.id']));
    return ano?.ano || ano?.nome || '';
  }

  private validarFormulario() {
    if (!this.formulario['disciplina.id']) return 'Selecione a disciplina.';
    if (!this.formulario['professor.id']) return 'Selecione o professor.';
    if (!this.formulario['nome']) return 'Informe o nome da turma.';
    if (!this.formulario['horario']) return 'Informe o dia e horario da turma.';
    if (!this.formulario['sala']) return 'Informe a sala.';
    if (!this.formulario['quantidadeMaximaAlunos'] || Number(this.formulario['quantidadeMaximaAlunos']) <= 0) return 'Informe a quantidade maxima de alunos.';
    return '';
  }

  private montarPayload() {
    return {
      nome: this.formulario['nome'],
      disciplina: { id: Number(this.formulario['disciplina.id']) },
      professor: { id: Number(this.formulario['professor.id']) },
      anoLetivo: this.formulario['anoLetivo.id'] ? { id: Number(this.formulario['anoLetivo.id']) } : null,
      periodoLetivo: this.formulario['periodoLetivo.id'] ? { id: Number(this.formulario['periodoLetivo.id']) } : null,
      curso: this.formulario['curso.id'] ? { id: Number(this.formulario['curso.id']) } : null,
      turno: this.formulario['turno'] || null,
      horario: this.formulario['horario'],
      sala: this.formulario['sala'],
      quantidadeMaximaAlunos: Number(this.formulario['quantidadeMaximaAlunos']),
      dataInicio: this.formulario['dataInicio'] || null,
      dataTermino: this.formulario['dataTermino'] || null,
      status: this.formulario['status'] || 'ABERTA',
      descricao: this.formulario['descricao'] || null
    };
  }
}
