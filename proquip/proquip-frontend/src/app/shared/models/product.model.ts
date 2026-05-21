/**
 * 製品モデル定義
 */

/** 製品一覧用インターフェース */
export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  categoryId: number;
  categoryName: string;
  manufacturerId: number;
  manufacturerName: string;
  unitPrice: number;
  unit: string;
  status: string;
  minimumOrderQuantity: number;
  leadTimeDays: number;
  createdAt: string;
  updatedAt: string;
}

/** 製品詳細インターフェース */
export interface ProductDetail extends Product {
  specifications: string;
  notes: string;
  weight: number;
  dimensions: string;
  imageUrl: string;
  suppliers: ProductSupplier[];
  inventoryItems: ProductInventoryItem[];
  priceHistory: PriceHistoryEntry[];
  images?: ProductImageApi[];
  documents?: ProductDocumentApi[];
}

/** API画像データ */
export interface ProductImageApi {
  id: number;
  fileName: string;
  filePath: string;
  mimeType: string;
  primary: boolean;
  sortOrder: number;
}

/** APIドキュメントデータ */
export interface ProductDocumentApi {
  id: number;
  docType: string;
  fileName: string;
  filePath: string;
  docVersion: string;
}

/** 製品-サプライヤー関連 */
export interface ProductSupplier {
  supplierId: number;
  supplierName: string;
  supplierSku: string;
  unitPrice: number;
  leadTimeDays: number;
  isPreferred: boolean;
}

/** 製品在庫情報 */
export interface ProductInventoryItem {
  warehouseId: number;
  warehouseName: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
}

/** 価格履歴エントリ */
export interface PriceHistoryEntry {
  effectiveDate: string;
  price: number;
  changedBy: string;
}

/** カテゴリ */
export interface Category {
  id: number;
  name: string;
  description: string;
  parentId: number | null;
  productCount: number;
}

/** メーカー */
export interface Manufacturer {
  id: number;
  name: string;
  country: string;
  website: string;
  contactEmail: string;
}
