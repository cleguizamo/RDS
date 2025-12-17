import { Component, OnInit, OnDestroy, OnChanges, SimpleChanges, Input, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, NavigationEnd } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
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
export class FinancialDashboardComponent implements OnInit, OnDestroy, OnChanges {
  @Input() refreshKey?: string; // Input para forzar recarga cuando cambia el tab
  
  financialStats = signal<FinancialStatsResponse | null>(null);
  businessStats = signal<BusinessStatsResponse | null>(null);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  
  // Filtros de fecha
  startDate = signal<string>('');
  endDate = signal<string>('');

  // Tooltip para el gráfico
  tooltipVisible = signal<boolean>(false);
  tooltipPosition = signal<{ x: number; y: number }>({ x: 0, y: 0 });
  tooltipData = signal<{ date: string; revenue: number; expenses: number }>({
    date: '',
    revenue: 0,
    expenses: 0
  });

  private routerSubscription?: Subscription;
  private lastRefreshKey?: string;

  constructor(
    private statisticsService: StatisticsService,
    private router: Router
  ) {
    // Por defecto, último mes
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    this.startDate.set(startDate.toISOString().split('T')[0]);
    this.endDate.set(endDate.toISOString().split('T')[0]);
  }

  ngOnInit(): void {
    this.loadStats();
    
    // Recargar estadísticas cada vez que el usuario navega a este componente
    // Esto asegura que los datos se actualicen después de crear pedidos/deliveries
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        // Recargar siempre que se navegue al admin panel
        if (event.url && event.url.includes('/admin')) {
          console.log('Navegación detectada al admin panel, recargando estadísticas...');
          // Pequeño delay para asegurar que la navegación se complete
          setTimeout(() => {
            this.loadStats();
          }, 200);
        }
      });
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Si el refreshKey cambia (tab activo cambia), recargar estadísticas
    if (changes['refreshKey']) {
      const currentKey = changes['refreshKey'].currentValue;
      // Recargar siempre que cambie el refreshKey
      if (currentKey && currentKey !== this.lastRefreshKey) {
        this.lastRefreshKey = currentKey;
        console.log('[FinancialDashboard] refreshKey cambió, recargando estadísticas desde BD...', currentKey);
        // Recargar inmediatamente cuando se activa el tab
        setTimeout(() => {
          this.loadStats();
        }, 50);
      }
    }
  }

  loadStats(): void {
    console.log('[FinancialDashboard] loadStats() - Iniciando carga de estadísticas...');
    this.loading.set(true);
    this.errorMessage.set(null);

    const startDate = this.startDate() || undefined;
    const endDate = this.endDate() || undefined;
    
    console.log('[FinancialDashboard] Rango de fechas:', { startDate, endDate });

    // Cargar ambas estadísticas en paralelo para mayor velocidad
    forkJoin({
      financial: this.statisticsService.getFinancialStats(startDate, endDate),
      business: this.statisticsService.getBusinessStats()
    }).subscribe({
      next: (results) => {
        console.log('[FinancialDashboard] Estadísticas financieras cargadas:', {
          totalRevenue: results.financial.totalRevenue,
          totalExpenses: results.financial.totalExpenses,
          netProfit: results.financial.netProfit,
          dailyStatsCount: results.financial.dailyStats?.length || 0,
          timestamp: new Date().toISOString()
        });
        console.log('[FinancialDashboard] Estadísticas de negocio cargadas:', {
          totalOrders: results.business.totalOrders,
          totalDeliveries: results.business.totalDeliveries,
          totalReservations: results.business.totalReservations,
          totalCustomers: results.business.totalCustomers,
          ordersToday: results.business.todayStats.ordersCount,
          deliveriesToday: results.business.todayStats.deliveriesCount,
          timestamp: new Date().toISOString()
        });
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

  // Exportación
  exportToExcel(): void {
    if (!this.startDate() || !this.endDate()) {
      alert('Por favor selecciona un rango de fechas para exportar');
      return;
    }

    this.loading.set(true);
    this.statisticsService.exportFinancialStatsToExcel(this.startDate()!, this.endDate()!).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `estadisticas_financieras_${this.startDate()}_${this.endDate()}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error exporting to Excel:', error);
        alert('Error al exportar a Excel');
        this.loading.set(false);
      }
    });
  }

  exportToPdf(): void {
    if (!this.startDate() || !this.endDate()) {
      alert('Por favor selecciona un rango de fechas para exportar');
      return;
    }

    this.loading.set(true);
    this.statisticsService.exportFinancialStatsToPdf(this.startDate()!, this.endDate()!).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `estadisticas_financieras_${this.startDate()}_${this.endDate()}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error exporting to PDF:', error);
        alert('Error al exportar a PDF');
        this.loading.set(false);
      }
    });
  }

  // Métodos para tooltip del gráfico
  showTooltip(event: MouseEvent, day: { date: string; revenue: number; expenses: number }): void {
    this.tooltipData.set(day);
    this.updateTooltipPosition(event);
    this.tooltipVisible.set(true);
  }

  hideTooltip(): void {
    this.tooltipVisible.set(false);
  }

  updateTooltipPosition(event: MouseEvent): void {
    const offsetX = 15;
    const offsetY = -10;
    
    this.tooltipPosition.set({
      x: event.clientX + offsetX,
      y: event.clientY + offsetY
    });
  }
}

