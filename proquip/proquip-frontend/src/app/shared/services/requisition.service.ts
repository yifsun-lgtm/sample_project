import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Requisition } from '@shared/models/purchase-order.model';
import { PageResult } from '@shared/models/common.model';

/**
 * 購買依頼サービス
 * 購買依頼のCRUDおよび承認ワークフローのAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class RequisitionService {

  private readonly basePath = '/requisitions';

  constructor(private api: ApiService) {}

  /** 購買依頼一覧を取得 */
  getRequisitions(page: number = 0, size: number = 20, status?: string): Observable<PageResult<Requisition>> {
    const params: any = { page, size };
    if (status) params.status = status;
    return this.api.get<PageResult<Requisition>>(this.basePath, params);
  }

  /** 購買依頼詳細を取得 */
  getRequisition(id: number): Observable<Requisition> {
    return this.api.get<Requisition>(`${this.basePath}/${id}`);
  }

  /** 購買依頼を作成 */
  createRequisition(requisition: Partial<Requisition>): Observable<Requisition> {
    return this.api.post<Requisition>(this.basePath, requisition);
  }

  /** 購買依頼を更新 */
  updateRequisition(id: number, requisition: Partial<Requisition>): Observable<Requisition> {
    return this.api.put<Requisition>(`${this.basePath}/${id}`, requisition);
  }

  /** 購買依頼を削除 */
  deleteRequisition(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${id}`);
  }

  /** 承認申請を送信 */
  submitForApproval(id: number): Observable<Requisition> {
    return this.api.post<Requisition>(`${this.basePath}/${id}/submit`, {});
  }

  /** 購買依頼を承認 */
  approveRequisition(id: number, comment: string): Observable<Requisition> {
    return this.api.post<Requisition>(`${this.basePath}/${id}/approve`, { comment });
  }

  /** 購買依頼を却下 */
  rejectRequisition(id: number, comment: string): Observable<Requisition> {
    return this.api.post<Requisition>(`${this.basePath}/${id}/reject`, { comment });
  }

  /** 購買依頼から発注書を生成 */
  convertToOrder(id: number): Observable<any> {
    return this.api.post<any>(`${this.basePath}/${id}/convert-to-order`, {});
  }
}
