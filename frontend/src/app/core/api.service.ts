import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly api = '/api';

  constructor(private http: HttpClient) {}

  listar(endpoint: string) {
    return this.http.get<any[]>(`${this.api}/${endpoint}?tamanho=500`);
  }

  salvar(endpoint: string, dados: any) {
    return this.http.post(`${this.api}/${endpoint}`, dados);
  }

  buscar(endpoint: string, id: number) {
    return this.http.get<any>(`${this.api}/${endpoint}/${id}`);
  }

  atualizar(endpoint: string, id: number, dados: any) {
    return this.http.put(`${this.api}/${endpoint}/${id}`, dados);
  }

  acao(endpoint: string, id: number, acao: string, dados: any) {
    return this.http.put(`${this.api}/${endpoint}/${id}/${acao}`, dados);
  }

  excluir(endpoint: string, id: number) {
    return this.http.delete(`${this.api}/${endpoint}/${id}`);
  }

  enviarArquivo(endpoint: string, id: number, acao: string, arquivo: File) {
    const dados = new FormData();
    dados.append('arquivo', arquivo);
    return this.http.post(`${this.api}/${endpoint}/${id}/${acao}`, dados);
  }

  removerArquivo(endpoint: string, id: number, acao: string) {
    return this.http.delete(`${this.api}/${endpoint}/${id}/${acao}`);
  }

  baixarArquivo(endpoint: string, id: number, acao: string) {
    return this.http.get(`${this.api}/${endpoint}/${id}/${acao}`, { responseType: 'blob' });
  }

  dashboard() {
    return this.http.get<Record<string, any>>(`${this.api}/dashboard`);
  }
}
