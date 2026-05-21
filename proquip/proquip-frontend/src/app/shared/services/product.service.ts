import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Product, ProductDetail, Category } from '@shared/models/product.model';
import { PageResult } from '@shared/models/common.model';

/**
 * 製品サービス
 * 製品関連のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class ProductService {

  /** APIパス */
  private readonly basePath = '/products';

  constructor(private api: ApiService) {}

  /** 製品一覧を取得（ページネーション・フィルタ付き） */
  getProducts(page: number = 0, size: number = 20, sort?: string, filters?: {
    keyword?: string;
    categoryId?: number;
    manufacturerId?: number;
    status?: string;
  }): Observable<PageResult<Product>> {
    const params: any = { page, size };
    if (sort) params.sort = sort;
    if (filters?.keyword) params.keyword = filters.keyword;
    if (filters?.categoryId) params.categoryId = filters.categoryId;
    if (filters?.manufacturerId) params.manufacturerId = filters.manufacturerId;
    if (filters?.status) params.status = filters.status;
    return this.api.get<PageResult<Product>>(this.basePath, params);
  }

  /** 製品詳細を取得 */
  getProduct(id: number): Observable<ProductDetail> {
    return this.api.get<ProductDetail>(`${this.basePath}/${id}`);
  }

  /** 製品を作成 */
  createProduct(product: Partial<Product>): Observable<Product> {
    return this.api.post<Product>(this.basePath, product);
  }

  /** 製品を更新 */
  updateProduct(id: number, product: Partial<Product>): Observable<Product> {
    return this.api.put<Product>(`${this.basePath}/${id}`, product);
  }

  /** 製品を削除 */
  deleteProduct(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${id}`);
  }

  /** 製品を検索 */
  searchProducts(keyword: string, page: number = 0, size: number = 20): Observable<PageResult<Product>> {
    return this.api.get<PageResult<Product>>(`${this.basePath}/search`, {
      keyword,
      page,
      size
    });
  }

  /** カテゴリ一覧を取得 */
  getCategories(): Observable<Category[]> {
    return this.api.get<Category[]>(`${this.basePath}/categories`);
  }

  /** メーカー一覧を取得 */
  getManufacturers(): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/manufacturers`);
  }

  /** 変更履歴を取得 */
  getChangeLog(productId: number): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/${productId}/change-log`);
  }

  /** SKUの重複チェック */
  checkSkuExists(sku: string): Observable<boolean> {
    return this.api.get<boolean>(`${this.basePath}/check-sku`, { sku });
  }

  /** 画像をアップロード */
  uploadImage(productId: number, file: File, isPrimary: boolean = false): Observable<any> {
    return this.api.upload(`${this.basePath}/${productId}/images`, file, {
      fileName: file.name,
      isPrimary: String(isPrimary)
    });
  }

  /** 画像を削除 */
  deleteImage(productId: number, imageId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${productId}/images/${imageId}`);
  }

  /** ドキュメントをアップロード */
  uploadDocument(productId: number, file: File, docType: string = 'DATASHEET'): Observable<any> {
    return this.api.upload(`${this.basePath}/${productId}/documents`, file, {
      fileName: file.name,
      docType: docType
    });
  }

  /** ドキュメントを削除 */
  deleteDocument(productId: number, docId: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/${productId}/documents/${docId}`);
  }

  /** カテゴリを作成 */
  createCategory(data: { name: string; description?: string; parentId?: number | null }): Observable<Category> {
    return this.api.post<Category>(`${this.basePath}/categories`, data);
  }

  /** カテゴリを更新 */
  updateCategory(id: number, data: { name: string; description?: string; parentId?: number | null }): Observable<Category> {
    return this.api.put<Category>(`${this.basePath}/categories/${id}`, data);
  }

  /** カテゴリを削除 */
  deleteCategory(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/categories/${id}`);
  }

  /** バンドル一覧を取得 */
  getBundles(): Observable<any[]> {
    return this.api.get<any[]>(`${this.basePath}/bundles`);
  }

  /** バンドルを作成 */
  createBundle(data: any): Observable<any> {
    return this.api.post<any>(`${this.basePath}/bundles`, data);
  }

  /** バンドルを更新 */
  updateBundle(id: number, data: any): Observable<any> {
    return this.api.put<any>(`${this.basePath}/bundles/${id}`, data);
  }

  /** バンドルを削除 */
  deleteBundle(id: number): Observable<void> {
    return this.api.delete<void>(`${this.basePath}/bundles/${id}`);
  }
}
