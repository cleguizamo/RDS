import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { StatisticsService } from '../../../core/services/statistics.service';
import { FinancialStatsResponse, BusinessStatsResponse } from '../../../core/models/statistics.model';
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency.pipe';

@Component({
  selector: 'app-financial-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, FormatCurrencyPipe],
  templateUrl: './financial-dashboard.component.html',
  styleUrl: './financial-dashboard.component.css'
})
export class FinancialDashboardComponent implements OnInit {
  financialStats = signal<FinancialStatsResponse | null>(null);
  businessStats = signal<BusinessStatsResponse | null>(null);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  
  // Filtros de fecha
  startDate = signal<string>('');
  endDate = signal<string>('');

  constructor(private statisticsService: StatisticsService) {
    // Por defecto, último mes
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    this.startDate.set(startDate.toISOString().split('T')[0]);
    this.endDate.set(endDate.toISOString().split('T')[0]);
  }

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const startDate = this.startDate() || undefined;
    const endDate = this.endDate() || undefined;

    // Cargar ambas estadísticas en paralelo para mayor velocidad
    forkJoin({
      financial: this.statisticsService.getFinancialStats(startDate, endDate),
      business: this.statisticsService.getBusinessStats()
    }).subscribe({
      next: (results) => {
        this.financialStats.set(results.financial);
        this.businessStats.set(results.business);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading stats:', error);
        this.errorMessage.set('Error al cargar estadísticas');
        this.loading.set(false);
        
        // Intentar cargar al menos una de las estadísticas
        this.statisticsService.getBusinessStats().subscribe({
          next: (stats) => {
            this.businessStats.set(stats);
          },
          error: (err) => {
            console.error('Error loading business stats:', err);
          }
        });
      }
    });
  }

  onDateRangeChange(): void {
    this.loadStats();
  }

  setDateRange(days: number): void {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);
    
    this.startDate.set(startDate.toISOString().split('T')[0]);
    this.endDate.set(endDate.toISOString().split('T')[0]);
    this.loadStats();
  }

  getProfitMargin(revenue: number, expenses: number): number {
    if (revenue === 0) return 0;
    return ((revenue - expenses) / revenue) * 100;
  }

  getCategoryColor(index: number): string {
    const colors = [
      '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', 
      '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E2'
    ];
    return colors[index % colors.length];
  }

  getMaxRevenue(): number {
    if (!this.financialStats()?.dailyStats.length) return 1;
    return Math.max(...this.financialStats()!.dailyStats.map(d => d.revenue), 1);
  }

  getMaxExpenses(): number {
    if (!this.financialStats()?.dailyStats.length) return 1;
    return Math.max(...this.financialStats()!.dailyStats.map(d => d.expenses), 1);
  }

  getDisplayedDailyStats() {
    if (!this.financialStats()?.dailyStats.length) return [];
    
    // Mostrar solo los últimos 14 días (2 semanas) empezando desde hoy hacia atrás
    const stats = [...this.financialStats()!.dailyStats];
    const today = new Date();
    
    // Filtrar solo los días que tienen datos o están en los últimos 14 días
    const filtered = stats
      .filter(stat => {
        const statDate = new Date(stat.date);
        const daysDiff = Math.floor((today.getTime() - statDate.getTime()) / (1000 * 60 * 60 * 24));
        return daysDiff >= 0 && daysDiff <= 14;
      })
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    
    // Si hay más de 14 días, tomar solo los últimos 14
    return filtered.slice(-14);
  }

  getMaxDisplayedValue(): number {
    const displayed = this.getDisplayedDailyStats();
    if (!displayed.length) return 1;
    const maxRevenue = Math.max(...displayed.map(d => d.revenue), 0);
    const maxExpenses = Math.max(...displayed.map(d => d.expenses), 0);
    return Math.max(maxRevenue, maxExpenses, 1) * 1.1; // 10% de margen superior
  }
}

