import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

/**
 * ダッシュボードサマリーデータ
 */
export interface DashboardSummary {
  totalProducts: number;
  activeSuppliers: number;
  pendingOrders: number;
  lowStockItems: number;
  budgetUtilization: Record<string, unknown>;
  recentOrders: any[];
  pendingApprovals: number;
}

/**
 * ダッシュボードサービス
 * ダッシュボード画面用のサマリーデータ取得API呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private readonly basePath = '/dashboard';

  constructor(private api: ApiService) {}

  /** ダッシュボードサマリーを取得 */
  getSummary(): Observable<DashboardSummary> {
    return this.api.get<DashboardSummary>(`${this.basePath}/summary`);
  }

  /** 月別発注金額推移を取得 */
  getMonthlySpendingTrend(months: number = 12): Observable<any> {
    return this.api.get<any>(`${this.basePath}/spending-trend`, { months });
  }

  /** カテゴリ別支出割合を取得 */
  getCategorySpending(): Observable<any> {
    return this.api.get<any>(`${this.basePath}/category-spending`);
  }

  /** 最近の活動ログを取得 */
  getRecentActivity(limit: number = 10): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/recent-activity`, { limit });
  }
}
