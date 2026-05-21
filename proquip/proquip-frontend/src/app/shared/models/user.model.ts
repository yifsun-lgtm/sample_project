/**
 * ユーザーモデル定義
 */

/** ユーザープロファイル */
export interface UserProfile {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  roles: Role[];
  enabled: boolean;
  createdAt: string;
}

/** ロール */
export interface Role {
  id: string;
  name: string;
  description: string;
  permissions: string[];
}
