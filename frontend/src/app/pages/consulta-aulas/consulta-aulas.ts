import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-consulta-aulas', standalone: true,
  imports: [CommonModule, FormsModule], templateUrl: './consulta-aulas.html'
})
export class ConsultaAulasPage implements OnInit {
  aulas: any[] = [];
  detalhe?: any;
  carregando = false;
  mensagem = '';
  filtros = { anoLetivoId: '', moduloId: '', ofertaId: '', professorId: '', disciplinaId: '', inicio: '', fim: '' };

  constructor(private api: ApiService) {}
  ngOnInit() { this.carregar(); }

  carregar() {
    this.carregando = true;
    this.api.listar('coordenador/aulas').pipe(finalize(() => this.carregando = false)).subscribe({
      next: aulas => this.aulas = Array.isArray(aulas) ? aulas : [],
      error: erro => this.mensagem = erro?.error?.mensagem || 'Não foi possível carregar as aulas.'
    });
  }

  visualizar(aula: any) {
    this.carregando = true;
    this.api.buscar('coordenador/aulas', aula.id).pipe(finalize(() => this.carregando = false)).subscribe({
      next: detalhe => this.detalhe = detalhe,
      error: erro => this.mensagem = erro?.error?.mensagem || 'Não foi possível abrir a aula.'
    });
  }

  aulasFiltradas() {
    return this.aulas.filter(aula =>
      (!this.filtros.anoLetivoId || aula.anoLetivoId === Number(this.filtros.anoLetivoId)) &&
      (!this.filtros.moduloId || aula.moduloId === Number(this.filtros.moduloId)) &&
      (!this.filtros.ofertaId || aula.ofertaId === Number(this.filtros.ofertaId)) &&
      (!this.filtros.professorId || aula.professorId === Number(this.filtros.professorId)) &&
      (!this.filtros.disciplinaId || aula.disciplinaId === Number(this.filtros.disciplinaId)) &&
      (!this.filtros.inicio || aula.data >= this.filtros.inicio) &&
      (!this.filtros.fim || aula.data <= this.filtros.fim));
  }

  opcoes(chaveId: string, chaveRotulo: string) {
    const unicas = new Map<number, string>();
    this.aulas.forEach(aula => { if (aula[chaveId] != null) unicas.set(aula[chaveId], aula[chaveRotulo] ?? aula[chaveId]); });
    return Array.from(unicas, ([id, nome]) => ({ id, nome })).sort((a, b) => String(a.nome).localeCompare(String(b.nome)));
  }

  limpar() {
    this.filtros = { anoLetivoId: '', moduloId: '', ofertaId: '', professorId: '', disciplinaId: '', inicio: '', fim: '' };
  }

  abrirPdf(arquivo: any) {
    this.api.baixar(`coordenador/arquivos/${arquivo.id}`).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    });
  }
}
