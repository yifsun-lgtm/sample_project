/**
 * 在庫モデル定義
 */

/** 在庫アイテム */
export interface InventoryItem {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  warehouseId: number;
  warehouseName: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  minimumStock: number;
  maximumStock: number;
  reorderPoint: number;
  status: string;
  lastStockCheckDate: string;
  updatedAt: string;
}

/** 倉庫 */
export interface Warehouse {
  id: number;
  code: string;
  name: string;
  address: string;
  manager: string;
  capacity: number;
  currentOccupancy: number;
  status: string;
  phone: string;
  notes: string;
}

/** 在庫移動 */
export interface StockTransfer {
  id: number;
  transferNumber: string;
  sourceWarehouseId: number;
  sourceWarehouseName: string;
  destinationWarehouseId: number;
  destinationWarehouseName: string;
  productId: number;
  productName: string;
  quantity: number;
  status: string;
  requestedBy: string;
  requestedDate: string;
  completedDate: string | null;
  notes: string;
}
