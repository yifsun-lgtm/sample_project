import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

/**
 * システム設定
 */
export interface SystemConfig {
  key: string;
  value: string;
  description: string;
  category: string;
  updatedAt: string;
  updatedBy: string;
}

/**
 * パーミッション定義（API応答）
 */
export interface PermissionDefinition {
  id: number;
  code: string;
  name: string;
  resource: string;
  action: string;
}

/**
 * 監査ログエントリ
 */
export interface AuditLogEntry {
  id: number;
  action: string;
  entityType: string;
  entityId: string;
  username: string;
  timestamp: string;
  details: string;
  ipAddress: string;
}

/**
 * 管理者サービス
 * システム管理機能のAPI呼び出しを管理する
 */
@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private readonly basePath = '/admin';

  constructor(private api: ApiService) {}

  /** システム設定一覧を取得 */
  getSystemConfigs(): Observable<SystemConfig[]> {
    return this.api.get<SystemConfig[]>(`${this.basePath}/system-config`);
  }

  /** システム設定を更新 */
  updateSystemConfig(key: string, value: string): Observable<SystemConfig> {
    return this.api.put<SystemConfig>(`${this.basePath}/system-config/${key}`, { value });
  }

  /** 監査ログを取得 */
  getAuditLogs(page: number = 0, size: number = 50, entityType?: string): Observable<any> {
    const params: any = { page, size };
    if (entityType) params.entityType = entityType;
    return this.api.get<any>(`${this.basePath}/audit-logs`, params);
  }

  /** パーミッション定義一覧を取得 */
  getPermissions(): Observable<PermissionDefinition[]> {
    return this.api.get<PermissionDefinition[]>(`${this.basePath}/permissions`);
  }

  /** マスターデータのインポート */
  importMasterData(file: File, type: string): Observable<any> {
    return this.api.upload(`${this.basePath}/import/${type}`, file);
  }

  /** マスターデータのエクスポート */
  exportMasterData(type: string): Observable<Blob> {
    return this.api.download(`${this.basePath}/export/${type}`);
  }

  /** システムヘルスチェック */
  getHealthStatus(): Observable<any> {
    return this.api.get<any>(`${this.basePath}/health`);
  }
}
