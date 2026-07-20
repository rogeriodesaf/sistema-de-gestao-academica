import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, timeout } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly api = '/api';

  constructor(private http: HttpClient, private auth: AuthService) {}

  listar(endpoint: string) {
    return this.http.get<any[]>(`${this.api}/${endpoint}?tamanho=500`, this.opcoesAutenticadas());
  }

  obter(endpoint: string) {
    return this.obterComFallback(endpoint);
  }

  buscarComFallback(endpoint: string, id: number | string) {
    return this.obterComFallback(`${endpoint}/${id}`);
  }

  private obterComFallback(endpoint: string) {
    const requisicao = this.http.get<any>(`${this.api}/${endpoint}`, this.opcoesAutenticadas());
    const ambienteLocal = ['localhost', '127.0.0.1'].includes(window.location.hostname);
    if (ambienteLocal) return requisicao.pipe(timeout(15000));

    return requisicao.pipe(
      timeout(8000),
      catchError(() => this.http.get<any>(
        `https://sga-backend-7y3i.onrender.com/api/${endpoint}`,
        this.opcoesAutenticadas()
      ).pipe(timeout(15000)))
    );
  }

  salvar(endpoint: string, dados: any) {
    const ambienteLocal = ['localhost', '127.0.0.1'].includes(window.location.hostname);
    const url = ambienteLocal
      ? `${this.api}/${endpoint}`
      : `https://sga-backend-7y3i.onrender.com/api/${endpoint}`;
    return this.http.post(url, dados, this.opcoesAutenticadas()).pipe(timeout(15000));
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
    const ambienteLocal = ['localhost', '127.0.0.1'].includes(window.location.hostname);
    const url = ambienteLocal
      ? `${this.api}/${endpoint}`
      : `https://sga-backend-7y3i.onrender.com/api/${endpoint}`;
    return this.http.post(url, dados, this.opcoesAutenticadas()).pipe(timeout(30000));
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
