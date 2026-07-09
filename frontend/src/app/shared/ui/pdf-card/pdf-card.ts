import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pdf-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pdf-card.html',
  styleUrl: './pdf-card.scss'
})
export class PdfCardComponent {
  @Input() nomeArquivo = '';
  @Input() titulo = 'Documento';
  @Input() enviadoTexto = 'Documento enviado';
  @Input() vazioTexto = 'Nenhum PDF enviado';
  @Input() uploadTexto = 'Enviar PDF';

  @Output() abrir = new EventEmitter<void>();
  @Output() baixar = new EventEmitter<void>();
  @Output() remover = new EventEmitter<void>();
  @Output() arquivoSelecionado = new EventEmitter<File>();

  menuAberto = false;
  confirmandoRemocao = false;

  get temArquivo() {
    return !!this.nomeArquivo;
  }

  selecionarArquivo(event: Event) {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (arquivo) this.arquivoSelecionado.emit(arquivo);
    input.value = '';
    this.menuAberto = false;
  }

  solicitarRemocao() {
    this.menuAberto = false;
    this.confirmandoRemocao = true;
  }

  confirmarRemocao() {
    this.confirmandoRemocao = false;
    this.remover.emit();
  }
}
