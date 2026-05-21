/**
 * 共通モデル定義
 */

/** ページネーション付きレスポンス */
export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

/** APIエラーレスポンス */
export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  path: string;
  errors?: FieldError[];
}

/** フィールドエラー */
export interface FieldError {
  field: string;
  message: string;
  rejectedValue: any;
}

/** セレクトオプション */
export interface SelectOption {
  value: string | number;
  label: string;
  disabled?: boolean;
}
