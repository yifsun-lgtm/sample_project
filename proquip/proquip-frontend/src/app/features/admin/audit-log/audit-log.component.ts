import { Component, OnInit } from '@angular/core';
import { AdminService, AuditLogEntry } from '@shared/services/admin.service';

/**
 * 監査ログ詳細（old/new値比較用）
 */
export interface AuditLogDetail {
  fieldName: string;
  oldValue: string;
  newValue: string;
}

/**
 * 拡張監査ログエントリ
 */
export interface ExtendedAuditLogEntry extends AuditLogEntry {
  parsedDetails: AuditLogDetail[];
}

/**
 * 監査ログコンポーネント
 * システムの操作ログを閲覧・フィルター・エクスポートする
 *
 * 技術的負債: 全ての監査ログをクライアントサイドに読み込んでフィルタリング
 * 大量データの場合パフォーマンス問題が発生する
 * サーバーサイドフィルタリング+ページネーションに移行すべき
 */
@Component({
  selector: 'app-audit-log',
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss']
})
export class AuditLogComponent implements OnInit {

  /** 全ての監査ログ（技術的負債: 全件クライアントサイドに保持） */
  allLogs: ExtendedAuditLogEntry[] = [];

  /** フィルター後のログ */
  filteredLogs: ExtendedAuditLogEntry[] = [];

  /** フィルター: 開始日 */
  filterStartDate = '';

  /** フィルター: 終了日 */
  filterEndDate = '';

  /** フィルター: ユーザー */
  filterUser = '';

  /** フィルター: 操作種別 */
  filterAction = '';

  /** フィルター: エンティティ種別 */
  filterEntityType = '';

  /** 操作種別オプション */
  actionOptions = [
    { value: '', label: 'すべての操作' },
    { value: 'CREATE', label: '作成' },
    { value: 'UPDATE', label: '更新' },
    { value: 'DELETE', label: '削除' },
    { value: 'LOGIN', label: 'ログイン' },
    { value: 'LOGOUT', label: 'ログアウト' },
    { value: 'APPROVE', label: '承認' },
    { value: 'REJECT', label: '却下' },
    { value: 'EXPORT', label: 'エクスポート' }
  ];

  /** エンティティ種別オプション */
  entityTypeOptions = [
    { value: '', label: 'すべて' },
    { value: 'Product', label: '製品' },
    { value: 'Supplier', label: 'サプライヤー' },
    { value: 'PurchaseOrder', label: '発注' },
    { value: 'Inventory', label: '在庫' },
    { value: 'User', label: 'ユーザー' },
    { value: 'SystemConfig', label: 'システム設定' }
  ];

  /** ローディング */
  isLoading = false;

  /** エクスポート中 */
  isExporting = false;

  /** 詳細モーダル表示 */
  showDetailModal = false;

  /** 選択中のログエントリ */
  selectedLog: ExtendedAuditLogEntry | null = null;

  /** エラーメッセージ */
  errorMessage = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.initDateRange();
    this.loadAuditLogs();
  }

  /** 日付範囲初期化 */
  private initDateRange(): void {
    const now = new Date();
    this.filterEndDate = now.toISOString().split('T')[0];
    const sevenDaysAgo = new Date(now);
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    this.filterStartDate = sevenDaysAgo.toISOString().split('T')[0];
  }

  /**
   * 監査ログを読み込む
   *
   * 技術的負債: 全件読み込んでクライアントサイドでフィルタリング
   * 大量データの場合、ブラウザのメモリを圧迫する
   */
  loadAuditLogs(): void {
    this.isLoading = true;
    this.errorMessage = '';

    // 技術的負債: 全件を一度に読み込む（サーバーサイドフィルタリングすべき）
    this.adminService.getAuditLogs(0, 10000).subscribe({
      next: (result) => {
        const entries: AuditLogEntry[] = result.content || result || [];
        this.allLogs = entries.map(entry => this.extendLogEntry(entry));
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('監査ログ取得エラー:', err);
        this.isLoading = false;
        this.errorMessage = '監査ログの取得に失敗しました。';
        this.allLogs = [];
        this.filteredLogs = [];
      }
    });
  }

  /** ログエントリを拡張する（詳細をパース） */
  private extendLogEntry(entry: AuditLogEntry): ExtendedAuditLogEntry {
    let parsedDetails: AuditLogDetail[] = [];
    try {
      const detailObj = JSON.parse(entry.details || '{}');
      parsedDetails = Object.keys(detailObj).map(key => {
        const detail = detailObj[key];
        if (typeof detail === 'object' && detail !== null && 'old' in detail) {
          return {
            fieldName: key,
            oldValue: detail.old != null ? String(detail.old) : '(なし)',
            newValue: detail.new != null ? String(detail.new) : '(なし)'
          };
        }
        return {
          fieldName: key,
          oldValue: '',
          newValue: String(detail)
        };
      });
    } catch {
      // JSONパースエラー
    }
    return { ...entry, parsedDetails };
  }

  /**
   * フィルターを適用する
   *
   * 技術的負債: クライアントサイドフィルタリング
   * サーバーサイドに委譲すべき
   */
  applyFilters(): void {
    // 技術的負債: 全件をメモリ上でフィルタリング
    let filtered = [...this.allLogs];

    // 日付フィルター
    if (this.filterStartDate) {
      const start = new Date(this.filterStartDate);
      filtered = filtered.filter(log => new Date(log.timestamp) >= start);
    }
    if (this.filterEndDate) {
      const end = new Date(this.filterEndDate + 'T23:59:59');
      filtered = filtered.filter(log => new Date(log.timestamp) <= end);
    }

    // ユーザーフィルター
    if (this.filterUser) {
      const keyword = this.filterUser.toLowerCase();
      filtered = filtered.filter(log => log.username.toLowerCase().includes(keyword));
    }

    // 操作種別フィルター
    if (this.filterAction) {
      filtered = filtered.filter(log => log.action === this.filterAction);
    }

    // エンティティ種別フィルター
    if (this.filterEntityType) {
      filtered = filtered.filter(log => log.entityType === this.filterEntityType);
    }

    // タイムスタンプ降順でソート
    filtered.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());

    this.filteredLogs = filtered;
  }

  /** 詳細モーダルを開く */
  openDetailModal(log: ExtendedAuditLogEntry): void {
    this.selectedLog = log;
    this.showDetailModal = true;
  }

  /** 詳細モーダルを閉じる */
  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedLog = null;
  }

  /** 操作種別のラベルを取得 */
  getActionLabel(action: string): string {
    const labels: { [key: string]: string } = {
      'CREATE': '作成', 'UPDATE': '更新', 'DELETE': '削除',
      'LOGIN': 'ログイン', 'LOGOUT': 'ログアウト',
      'APPROVE': '承認', 'REJECT': '却下', 'EXPORT': 'エクスポート'
    };
    return labels[action] || action;
  }

  /** 操作種別のCSSクラスを取得 */
  getActionClass(action: string): string {
    const classes: { [key: string]: string } = {
      'CREATE': 'action-create', 'UPDATE': 'action-update', 'DELETE': 'action-delete',
      'LOGIN': 'action-login', 'LOGOUT': 'action-logout',
      'APPROVE': 'action-approve', 'REJECT': 'action-reject', 'EXPORT': 'action-export'
    };
    return classes[action] || 'action-default';
  }

  /** CSVエクスポート */
  exportCsv(): void {
    this.isExporting = true;

    // CSVヘッダー
    const headers = ['日時', 'ユーザー', '操作', 'エンティティ', 'エンティティID', 'IPアドレス', '詳細'];
    const rows = this.filteredLogs.map(log => [
      log.timestamp,
      log.username,
      this.getActionLabel(log.action),
      log.entityType,
      log.entityId,
      log.ipAddress,
      log.details
    ]);

    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n');
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `監査ログ_${this.filterStartDate}_${this.filterEndDate}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);

    this.isExporting = false;
  }
}
