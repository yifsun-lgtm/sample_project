import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PurchaseOrder, PurchaseOrderItem } from '@shared/models/purchase-order.model';
import { PageResult } from '@shared/models/common.model';

/**
 * 発注サービス
 * 発注書のCRUDおよび承認ワークフロー、入荷処理のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class PurchaseOrderService {

  private readonly basePath = '/purchase-orders';

  constructor(private api: ApiService) {}

  /** 発注書一覧を取得 */
  getOrders(page: number = 0, size: number = 20, status?: string): Observable<PageResult<PurchaseOrder>> {
    const params: any = { page, size };
    if (status) params.status = status;
    return this.api.get<PageResult<PurchaseOrder>>(this.basePath, params);
  }

  /** 発注書詳細を取得 */
  getOrder(id: number): Observable<PurchaseOrder> {
    return this.api.get<PurchaseOrder>(`${this.basePath}/${id}`);
  }

  /** 発注書を作成 */
  createOrder(order: Partial<PurchaseOrder>): Observable<PurchaseOrder> {
    return this.api.post<PurchaseOrder>(this.basePath, order);
  }

  /** 発注書を更新 */
  updateOrder(id: number, order: Partial<PurchaseOrder>): Observable<PurchaseOrder> {
    return this.api.put<PurchaseOrder>(`${this.basePath}/${id}`, order);
  }

  /** 発注書を削除（下書きのみ） */
  deleteOrder(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${id}`);
  }

  /** 承認申請を送信 */
  submitForApproval(id: number): Observable<PurchaseOrder> {
    return this.api.post<PurchaseOrder>(`${this.basePath}/${id}/submit`, {});
  }

  /** 発注書を承認 */
  approveOrder(id: number, comment: string): Observable<PurchaseOrder> {
    return this.api.post<PurchaseOrder>(`${this.basePath}/${id}/approve`, { comment });
  }

  /** 発注書を却下 */
  rejectOrder(id: number, comment: string): Observable<PurchaseOrder> {
    return this.api.post<PurchaseOrder>(`${this.basePath}/${id}/reject`, { comment });
  }

  /** 入荷処理（検収） */
  receiveGoods(id: number, items: { itemId: number; receivedQuantity: number }[]): Observable<PurchaseOrder> {
    return this.api.post<PurchaseOrder>(`${this.basePath}/${id}/receive`, { items });
  }

  /** 発注書をキャンセル */
  cancelOrder(id: number, reason: string): Observable<PurchaseOrder> {
    return this.api.post<PurchaseOrder>(`${this.basePath}/${id}/cancel`, { reason });
  }

  /** 発注書明細を追加 */
  addItem(orderId: number, item: Partial<PurchaseOrderItem>): Observable<PurchaseOrderItem> {
    return this.api.post<PurchaseOrderItem>(`${this.basePath}/${orderId}/items`, item);
  }

  /** 発注書明細を更新 */
  updateItem(orderId: number, itemId: number, item: Partial<PurchaseOrderItem>): Observable<PurchaseOrderItem> {
    return this.api.put<PurchaseOrderItem>(`${this.basePath}/${orderId}/items/${itemId}`, item);
  }

  /** 発注書明細を削除 */
  removeItem(orderId: number, itemId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${orderId}/items/${itemId}`);
  }

  /** 承認待ち一覧を取得 */
  getPendingApprovals(page: number = 0, size: number = 20): Observable<PageResult<PurchaseOrder>> {
    return this.api.get<PageResult<PurchaseOrder>>(`${this.basePath}/pending-approvals`, { page, size });
  }
}
