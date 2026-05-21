/**
 * 価格モデル定義
 */

/** 価格表 */
export interface PriceList {
  id: number;
  name: string;
  description: string;
  status: 'ACTIVE' | 'INACTIVE' | 'DRAFT' | 'EXPIRED';
  effectiveDate: string;
  expirationDate: string | null;
  currency: string;
  supplierId: number | null;
  supplierName: string | null;
  items: PriceListItem[];
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

/** 価格表明細 */
export interface PriceListItem {
  id: number;
  priceListId: number;
  productId: number;
  productName: string;
  productSku: string;
  basePrice: number;
  discountPercent: number;
  finalPrice: number;
  minimumQuantity: number;
  notes: string;
}
