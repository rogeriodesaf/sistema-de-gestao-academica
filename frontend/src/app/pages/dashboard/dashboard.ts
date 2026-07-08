import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html'
})
export class DashboardPage implements OnInit {
  dados: Record<string, number> = {};

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.dashboard().subscribe(dados => this.dados = dados);
  }
}
