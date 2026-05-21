import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Budget, BudgetLineItem } from '@shared/models/budget.model';
import { PageResult } from '@shared/models/common.model';

/**
 * 予算サービス
 * 予算管理のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class BudgetService {

  private readonly basePath = '/budgets';

  constructor(private api: ApiService) {}

  /** 予算一覧を取得 */
  getBudgets(fiscalYear?: number): Observable<Budget[]> {
    const params: any = {};
    if (fiscalYear) params.fiscalYear = fiscalYear;
    return this.api.get<Budget[]>(this.basePath, params);
  }

  /** 予算詳細を取得 */
  getBudget(id: number): Observable<Budget> {
    return this.api.get<Budget>(`${this.basePath}/${id}`);
  }

  /** 予算を作成 */
  createBudget(budget: Partial<Budget>): Observable<Budget> {
    return this.api.post<Budget>(this.basePath, budget);
  }

  /** 予算を更新 */
  updateBudget(id: number, budget: Partial<Budget>): Observable<Budget> {
    return this.api.put<Budget>(`${this.basePath}/${id}`, budget);
  }

  /** 予算消化状況を取得 */
  getBudgetUtilization(id: number): Observable<Budget> {
    return this.api.get<Budget>(`${this.basePath}/${id}/utilization`);
  }

  /** 部門別予算サマリーを取得 */
  getDepartmentSummary(fiscalYear: number): Observable<Budget[]> {
    return this.api.get<Budget[]>(`${this.basePath}/summary`, { fiscalYear });
  }
}
