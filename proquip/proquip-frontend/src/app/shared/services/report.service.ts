import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

/**
 * レポート出力結果
 */
export interface ReportResult {
  reportId: string;
  reportName: string;
  generatedAt: string;
  data: any;
}

/**
 * レポートサービス
 * レポート生成・エクスポートのAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class ReportService {

  private readonly basePath = '/reports';

  constructor(private api: ApiService) {}

  /** 調達サマリーレポートを取得 */
  getProcurementSummary(startDate: string, endDate: string): Observable<ReportResult> {
    return this.api.get<ReportResult>(`${this.basePath}/procurement-summary`, { startDate, endDate });
  }

  /** 在庫評価レポートを取得 */
  getInventoryValuation(warehouseId?: number): Observable<ReportResult> {
    const params: any = {};
    if (warehouseId) params.warehouseId = warehouseId;
    return this.api.get<ReportResult>(`${this.basePath}/inventory-valuation`, params);
  }

  /** サプライヤー評価レポートを取得 */
  getSupplierPerformance(startDate: string, endDate: string, supplierId?: number): Observable<ReportResult> {
    const params: any = { startDate, endDate };
    if (supplierId) params.supplierId = supplierId;
    return this.api.get<ReportResult>(`${this.basePath}/supplier-performance`, params);
  }

  /** 予算消化レポートを取得 */
  getBudgetReport(fiscalYear: number): Observable<ReportResult> {
    return this.api.get<ReportResult>(`${this.basePath}/budget-vs-actual`, { fiscalYear });
  }

  /** 発注履歴レポートを取得 */
  getOrderHistory(startDate: string, endDate: string): Observable<ReportResult> {
    return this.api.get<ReportResult>(`${this.basePath}/order-summary`, { startDate, endDate });
  }

  /** レポートをCSVとしてエクスポート */
  exportCsv(reportType: string, params: any): Observable<Blob> {
    const queryParams = new URLSearchParams({ ...params, format: 'csv' }).toString();
    return this.api.download(`${this.basePath}/${reportType}/export?${queryParams}`);
  }

  /** レポートをExcelとしてエクスポート */
  exportExcel(reportType: string, params: any): Observable<Blob> {
    const queryParams = new URLSearchParams({ ...params, format: 'xlsx' }).toString();
    return this.api.download(`${this.basePath}/${reportType}/export?${queryParams}`);
  }
}
