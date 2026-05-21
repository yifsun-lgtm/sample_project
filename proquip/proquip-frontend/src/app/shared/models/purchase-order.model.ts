/**
 * 発注モデル定義
 */

/** 発注書 */
export interface PurchaseOrder {
  id: number;
  orderNumber: string;
  supplierId: number;
  supplierName: string;
  status: string;
  orderDate: string;
  expectedDeliveryDate: string;
  actualDeliveryDate: string | null;
  totalAmount: number;
  currency: string;
  notes: string;
  createdBy: string;
  approvedBy: string | null;
  approvedAt: string | null;
  items: PurchaseOrderItem[];
  approvalSteps: ApprovalStep[];
  createdAt: string;
  updatedAt: string;
}

/** 発注書明細 */
export interface PurchaseOrderItem {
  id: number;
  purchaseOrderId: number;
  productId: number;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  receivedQuantity: number;
  unit: string;
  notes: string;
}

/** 承認ステップ */
export interface ApprovalStep {
  id: number;
  purchaseOrderId: number;
  stepOrder: number;
  approverRole: string;
  approverName: string | null;
  status: string;
  comment: string;
  actionDate: string | null;
}

/** 購買依頼 */
export interface Requisition {
  id: number;
  requisitionNumber: string;
  title: string;
  requestedBy: string;
  department: string;
  status: string;
  priority: string;
  requiredDate: string;
  justification: string;
  items: RequisitionItem[];
  createdAt: string;
  updatedAt: string;
}

/** 購買依頼明細 */
export interface RequisitionItem {
  id: number;
  requisitionId: number;
  productId: number;
  productName: string;
  quantity: number;
  estimatedUnitPrice: number;
  notes: string;
}
