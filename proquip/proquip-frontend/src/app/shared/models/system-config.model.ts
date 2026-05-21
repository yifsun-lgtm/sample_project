/**
 * システム設定モデル定義
 */

/** システム設定（AdminService内にも定義があるが、モデルとして独立させたもの） */
export interface SystemConfig {
  key: string;
  value: string;
  description: string;
  category: string;
  dataType: 'string' | 'number' | 'boolean' | 'json';
  updatedAt: string;
  updatedBy: string;
}

/** 監査ログエントリ（AdminService内にも定義があるが、モデルとして独立させたもの） */
export interface AuditLogEntry {
  id: number;
  action: string;
  entityType: string;
  entityId: string;
  username: string;
  timestamp: string;
  details: string;
  ipAddress: string;
  userAgent: string;
  sessionId: string;
}

/** インポートジョブ */
export interface ImportJob {
  id: string;
  type: 'PRODUCTS' | 'SUPPLIERS' | 'CATEGORIES' | 'INVENTORY';
  fileName: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  totalRows: number;
  processedRows: number;
  errorRows: number;
  errors: ImportError[];
  startedAt: string;
  completedAt: string | null;
  createdBy: string;
}

/** インポートエラー */
export interface ImportError {
  row: number;
  field: string;
  value: string;
  message: string;
}
