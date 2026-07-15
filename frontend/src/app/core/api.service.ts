import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly api = '/api';

  constructor(private http: HttpClient, private auth: AuthService) {}

  listar(endpoint: string) {
    return this.http.get<any[]>(`${this.api}/${endpoint}?tamanho=500`, this.opcoesAutenticadas());
  }

  obter(endpoint: string) {
    return this.http.get<any>(`${this.api}/${endpoint}`, this.opcoesAutenticadas());
  }

  salvar(endpoint: string, dados: any) {
    return this.http.post(`${this.api}/${endpoint}`, dados, this.opcoesAutenticadas());
  }

  buscar(endpoint: string, id: number | string) {
    return this.http.get<any>(`${this.api}/${endpoint}/${id}`, this.opcoesAutenticadas());
  }

  buscarDisciplina(id: number) {
    return this.http.get<any>(`${this.api}/disciplinas/${id}`, this.opcoesAutenticadas());
  }

  atualizar(endpoint: string, id: number, dados: any) {
    return this.http.put(`${this.api}/${endpoint}/${id}`, dados, this.opcoesAutenticadas());
  }

  buscarAcao(endpoint: string, id: number | string, acao: string) {
    return this.http.get<any>(`${this.api}/${endpoint}/${id}/${acao}`, this.opcoesAutenticadas());
  }

  acao(endpoint: string, id: number | string, acao: string, dados: any) {
    return this.http.put(`${this.api}/${endpoint}/${id}/${acao}`, dados, this.opcoesAutenticadas());
  }

  enviarFormulario(endpoint: string, dados: FormData) {
    return this.http.post(`${this.api}/${endpoint}`, dados, this.opcoesAutenticadas());
  }

  baixar(endpoint: string) {
    return this.http.get(`${this.api}/${endpoint}`, { ...this.opcoesAutenticadas(), responseType: 'blob' });
  }

  remover(endpoint: string) {
    return this.http.delete(`${this.api}/${endpoint}`, this.opcoesAutenticadas());
  }

  excluir(endpoint: string, id: number) {
    return this.http.delete(`${this.api}/${endpoint}/${id}`, this.opcoesAutenticadas());
  }

  enviarArquivo(endpoint: string, id: number, acao: string, arquivo: File) {
    const dados = new FormData();
    dados.append('arquivo', arquivo);
    return this.http.post(`${this.api}/${endpoint}/${id}/${acao}`, dados, this.opcoesAutenticadas());
  }

  removerArquivo(endpoint: string, id: number, acao: string) {
    return this.http.delete(`${this.api}/${endpoint}/${id}/${acao}`, this.opcoesAutenticadas());
  }

  baixarArquivo(endpoint: string, id: number, acao: string) {
    return this.http.get(`${this.api}/${endpoint}/${id}/${acao}`, { ...this.opcoesAutenticadas(), responseType: 'blob' });
  }

  dashboard() {
    return this.http.get<Record<string, any>>(`${this.api}/dashboard`, this.opcoesAutenticadas());
  }

  private opcoesAutenticadas() {
    const token = this.auth.token();
    return token ? { headers: { Authorization: `Bearer ${token}` } } : {};
  }
}
