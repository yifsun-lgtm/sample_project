import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Warehouse } from '@shared/models/inventory.model';
import { PageResult } from '@shared/models/common.model';

/**
 * 倉庫サービス
 * 倉庫管理のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class WarehouseService {

  private readonly basePath = '/warehouses';

  constructor(private api: ApiService) {}

  /** 倉庫一覧を取得 */
  getWarehouses(page: number = 0, size: number = 20): Observable<PageResult<Warehouse>> {
    return this.api.get<PageResult<Warehouse>>(this.basePath, { page, size });
  }

  /** 全倉庫を取得（セレクトボックス用） */
  getAllWarehouses(): Observable<Warehouse[]> {
    return this.api.get<Warehouse[]>(`${this.basePath}/all`);
  }

  /** 倉庫詳細を取得 */
  getWarehouse(id: number): Observable<Warehouse> {
    return this.api.get<Warehouse>(`${this.basePath}/${id}`);
  }

  /** 倉庫を作成 */
  createWarehouse(warehouse: Partial<Warehouse>): Observable<Warehouse> {
    return this.api.post<Warehouse>(this.basePath, warehouse);
  }

  /** 倉庫を更新 */
  updateWarehouse(id: number, warehouse: Partial<Warehouse>): Observable<Warehouse> {
    return this.api.put<Warehouse>(`${this.basePath}/${id}`, warehouse);
  }

  /** 倉庫を削除 */
  deleteWarehouse(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${id}`);
  }

  /** ゾーン一覧を取得 */
  getZones(warehouseId: number): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/${warehouseId}/zones`);
  }

  /** ゾーンを作成 */
  createZone(warehouseId: number, zone: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/${warehouseId}/zones`, zone);
  }

  /** ゾーンを更新 */
  updateZone(warehouseId: number, zoneId: number, zone: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${warehouseId}/zones/${zoneId}`, zone);
  }

  /** ゾーンを削除 */
  deleteZone(warehouseId: number, zoneId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${warehouseId}/zones/${zoneId}`);
  }

  /** ロケーション一覧を取得 */
  getLocations(warehouseId: number, zoneId: number): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/${warehouseId}/zones/${zoneId}/locations`);
  }

  /** ロケーションを作成 */
  createLocation(warehouseId: number, zoneId: number, location: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/${warehouseId}/zones/${zoneId}/locations`, location);
  }

  /** ロケーションを更新 */
  updateLocation(warehouseId: number, zoneId: number, locId: number, location: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${warehouseId}/zones/${zoneId}/locations/${locId}`, location);
  }

  /** ロケーションを削除 */
  deleteLocation(warehouseId: number, zoneId: number, locId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${warehouseId}/zones/${zoneId}/locations/${locId}`);
  }
}
