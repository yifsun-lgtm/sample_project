const ORDER_STATUS_LABELS: { [key: string]: string } = {
  DRAFT: '下書き',
  SUBMITTED: '申請中',
  PENDING_APPROVAL: '承認待ち',
  APPROVED: '承認済み',
  REJECTED: '却下',
  ORDERED: '発注済み',
  PARTIALLY_ORDERED: '一部発注済み',
  PARTIALLY_RECEIVED: '一部入荷',
  RECEIVED: '入荷済み',
  INVOICED: '請求済み',
  DELIVERED: '納品済み',
  PARTIALLY_DELIVERED: '一部納品',
  CLOSED: '完了',
  COMPLETED: '完了',
  CANCELLED: 'キャンセル',
  CONVERTED: '発注変換済み'
};

const ORDER_STATUS_COLORS: { [key: string]: string } = {
  DRAFT: '#9e9e9e',
  SUBMITTED: '#1976d2',
  PENDING_APPROVAL: '#ff9800',
  APPROVED: '#43a047',
  REJECTED: '#e53935',
  ORDERED: '#7b1fa2',
  PARTIALLY_ORDERED: '#7b1fa2',
  PARTIALLY_RECEIVED: '#0097a7',
  RECEIVED: '#2e7d32',
  INVOICED: '#1976d2',
  CLOSED: '#2e7d32',
  CANCELLED: '#757575',
  CONVERTED: '#7b1fa2'
};

const ORDER_STATUS_CLASSES: { [key: string]: string } = {
  DRAFT: 'status-draft',
  SUBMITTED: 'status-pending',
  PENDING: 'status-pending',
  PENDING_APPROVAL: 'status-pending',
  APPROVED: 'status-approved',
  REJECTED: 'status-rejected',
  ORDERED: 'status-ordered',
  DELIVERED: 'status-delivered',
  RECEIVED: 'status-delivered',
  CANCELLED: 'status-cancelled',
  COMPLETED: 'status-completed',
  CLOSED: 'status-completed'
};

const PRODUCT_STATUS_LABELS: { [key: string]: string } = {
  ACTIVE: '有効',
  INACTIVE: '無効',
  DISCONTINUED: '廃番',
  PENDING: '保留'
};

const PRODUCT_STATUS_CLASSES: { [key: string]: string } = {
  ACTIVE: 'status-active',
  INACTIVE: 'status-inactive',
  DISCONTINUED: 'status-discontinued',
  PENDING: 'status-pending'
};

const STOCK_STATUS_LABELS: { [key: string]: string } = {
  LOW: '在庫不足',
  NEAR_REORDER: '要発注',
  OK: '適正',
  OVERSTOCK: '過剰'
};

const STOCK_STATUS_COLORS: { [key: string]: string } = {
  LOW: '#e53935',
  NEAR_REORDER: '#f57c00',
  OK: '#43a047',
  OVERSTOCK: '#ff9800'
};

const STOCK_STATUS_BG_COLORS: { [key: string]: string } = {
  LOW: '#ffebee',
  NEAR_REORDER: '#fff3e0',
  OK: '#e8f5e9',
  OVERSTOCK: '#fff3e0'
};

export function getOrderStatusLabel(status: string): string {
  return ORDER_STATUS_LABELS[status] || status;
}

export function getOrderStatusClass(status: string): string {
  return ORDER_STATUS_CLASSES[status] || 'status-default';
}

export function getOrderStatusColor(status: string): string {
  return ORDER_STATUS_COLORS[status] || '#9e9e9e';
}

export function getProductStatusLabel(status: string): string {
  return PRODUCT_STATUS_LABELS[status] || status;
}

export function getProductStatusClass(status: string): string {
  return PRODUCT_STATUS_CLASSES[status] || 'status-default';
}

export function getStockStatusLabel(status: string): string {
  return STOCK_STATUS_LABELS[status] || status;
}

export function getStockStatusColor(status: string): string {
  return STOCK_STATUS_COLORS[status] || '#9e9e9e';
}

export function getStockStatusBgColor(status: string): string {
  return STOCK_STATUS_BG_COLORS[status] || '#f5f5f5';
}
