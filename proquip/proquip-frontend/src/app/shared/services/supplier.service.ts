import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Supplier, SupplierContact, SupplierContract } from '@shared/models/supplier.model';
import { PageResult } from '@shared/models/common.model';

/**
 * サプライヤーサービス
 * サプライヤー関連のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class SupplierService {

  private readonly basePath = '/suppliers';

  constructor(private api: ApiService) {}

  /** サプライヤー一覧を取得 */
  getSuppliers(page: number = 0, size: number = 20): Observable<PageResult<Supplier>> {
    return this.api.get<PageResult<Supplier>>(this.basePath, { page, size });
  }

  /** サプライヤー詳細を取得 */
  getSupplier(id: number): Observable<Supplier> {
    return this.api.get<Supplier>(`${this.basePath}/${id}`);
  }

  /** サプライヤーを作成 */
  createSupplier(supplier: Partial<Supplier>): Observable<Supplier> {
    return this.api.post<Supplier>(this.basePath, supplier);
  }

  /** サプライヤーを更新 */
  updateSupplier(id: number, supplier: Partial<Supplier>): Observable<Supplier> {
    return this.api.put<Supplier>(`${this.basePath}/${id}`, supplier);
  }

  /** サプライヤーを削除 */
  deleteSupplier(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${id}`);
  }

  /** 連絡先一覧を取得 */
  getContacts(supplierId: number): Observable<SupplierContact[]> {
    return this.api.get<SupplierContact[]>(`${this.basePath}/${supplierId}/contacts`);
  }

  /** 契約一覧を取得 */
  getContracts(supplierId: number): Observable<SupplierContract[]> {
    return this.api.get<SupplierContract[]>(`${this.basePath}/${supplierId}/contracts`);
  }

  /** 評価履歴を取得 */
  getRatings(supplierId: number): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/${supplierId}/ratings`);
  }

  /** 認証情報を取得 */
  getCertifications(supplierId: number): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/${supplierId}/certifications`);
  }

  /** 取扱商品を取得 */
  getSupplierProducts(supplierId: number): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/${supplierId}/products`);
  }

  /** サプライヤーを比較 */
  compareSuppliers(supplierIds: number[]): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/compare`, { supplierIds: supplierIds.join(',') });
  }

  /** サプライヤーを評価 */
  rateSupplier(supplierId: number, ratingData: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/${supplierId}/rate`, ratingData);
  }

  /** 契約を作成 */
  createContract(supplierId: number, data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/${supplierId}/contracts`, data);
  }

  /** 契約を更新 */
  updateContract(supplierId: number, contractId: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${supplierId}/contracts/${contractId}`, data);
  }

  /** 契約を削除 */
  deleteContract(supplierId: number, contractId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${supplierId}/contracts/${contractId}`);
  }

  /** 認証を作成 */
  createCertification(supplierId: number, data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/${supplierId}/certifications`, data);
  }

  /** 認証を更新 */
  updateCertification(supplierId: number, certId: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/${supplierId}/certifications/${certId}`, data);
  }

  /** 認証を削除 */
  deleteCertification(supplierId: number, certId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${supplierId}/certifications/${certId}`);
  }

  /** 製品紐付けを追加 */
  addSupplierProduct(supplierId: number, data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/${supplierId}/products`, data);
  }

  /** 製品紐付けを解除 */
  removeSupplierProduct(supplierId: number, spId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${supplierId}/products/${spId}`);
  }
}
