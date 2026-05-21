import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { InventoryItem, StockTransfer } from '@shared/models/inventory.model';
import { PageResult } from '@shared/models/common.model';

/**
 * 在庫サービス
 * 在庫管理のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class InventoryService {

  private readonly basePath = '/inventory';

  constructor(private api: ApiService) {}

  /** 在庫一覧を取得 */
  getInventoryItems(page: number = 0, size: number = 20, warehouseId?: number): Observable<PageResult<InventoryItem>> {
    const params: any = { page, size };
    if (warehouseId) params.warehouseId = warehouseId;
    return this.api.get<PageResult<InventoryItem>>(this.basePath, params);
  }

  /** 在庫詳細を取得 */
  getInventoryItem(id: number): Observable<InventoryItem> {
    return this.api.get<InventoryItem>(`${this.basePath}/${id}`);
  }

  /** 在庫数量を調整 */
  adjustStock(id: number, adjustment: { quantity: number; reason: string }): Observable<InventoryItem> {
    return this.api.post<InventoryItem>(`${this.basePath}/${id}/adjust`, adjustment);
  }

  /** 在庫アラート一覧（最低在庫を下回る製品） */
  getLowStockAlerts(): Observable<InventoryItem[]> {
    return this.api.get<InventoryItem[]>(`${this.basePath}/alerts/low-stock`);
  }

  /** 在庫移動を作成 */
  createTransfer(transfer: Partial<StockTransfer>): Observable<StockTransfer> {
    return this.api.post<StockTransfer>(`${this.basePath}/transfers`, transfer);
  }

  /** 在庫移動一覧を取得 */
  getTransfers(page: number = 0, size: number = 20): Observable<PageResult<StockTransfer>> {
    return this.api.get<PageResult<StockTransfer>>(`${this.basePath}/transfers`, { page, size });
  }

  /** 在庫移動を完了 */
  completeTransfer(id: number): Observable<StockTransfer> {
    return this.api.post<StockTransfer>(`${this.basePath}/transfers/${id}/complete`, {});
  }

  /** 棚卸し用の在庫一覧を取得 */
  getStockCheckList(warehouseId: number): Observable<InventoryItem[]> {
    return this.api.get<InventoryItem[]>(`${this.basePath}/stock-check`, { warehouseId });
  }

  /** 棚卸し結果を保存 */
  saveStockCheckResults(results: { itemId: number; actualQuantity: number }[]): Observable<void> {
    return this.api.post<void>(`${this.basePath}/stock-check/save`, { results });
  }

  /** 製品別の在庫サマリーを取得 */
  getProductInventorySummary(productId: number): Observable<InventoryItem[]> {
    return this.api.get<InventoryItem[]>(`${this.basePath}/product/${productId}`);
  }
}
