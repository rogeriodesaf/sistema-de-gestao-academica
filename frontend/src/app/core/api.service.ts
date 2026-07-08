import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly api = '/api';

  constructor(private http: HttpClient) {}

  listar(endpoint: string) {
    return this.http.get<any[]>(`${this.api}/${endpoint}`);
  }

  salvar(endpoint: string, dados: any) {
    return this.http.post(`${this.api}/${endpoint}`, dados);
  }

  excluir(endpoint: string, id: number) {
    return this.http.delete(`${this.api}/${endpoint}/${id}`);
  }

  dashboard() {
    return this.http.get<Record<string, number>>(`${this.api}/dashboard`);
  }
}
