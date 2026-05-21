import { Component, Input, OnChanges } from '@angular/core';

/**
 * ステータスバッジコンポーネント
 * ステータス値に応じた色付きバッジを表示
 *
 * 技術的負債 #4: ステータスと色のマッピングがハードコードされている
 * 設定ファイルやサービスで管理すべき
 */
@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.scss']
})
export class StatusBadgeComponent implements OnChanges {

  /** ステータス文字列 */
  @Input() status = '';

  /** CSSクラス名 */
  badgeClass = '';

  /** 表示ラベル */
  displayLabel = '';

  /**
   * 技術的負債 #4: ハードコードされたステータス-色マッピング
   * 定数ファイルやAPIから取得すべき
   */
  private readonly statusMap: { [key: string]: { label: string; cssClass: string } } = {
    // 発注・購買ステータス
    'DRAFT': { label: '下書き', cssClass: 'badge-draft' },
    'SUBMITTED': { label: '申請中', cssClass: 'badge-submitted' },
    'PENDING_APPROVAL': { label: '承認待ち', cssClass: 'badge-pending' },
    'APPROVED': { label: '承認済み', cssClass: 'badge-approved' },
    'REJECTED': { label: '却下', cssClass: 'badge-rejected' },
    'ORDERED': { label: '発注済み', cssClass: 'badge-ordered' },
    'PARTIALLY_ORDERED': { label: '一部発注済み', cssClass: 'badge-ordered' },
    'SHIPPED': { label: '出荷済み', cssClass: 'badge-shipped' },
    'DELIVERED': { label: '納品済み', cssClass: 'badge-delivered' },
    'PARTIALLY_DELIVERED': { label: '一部納品', cssClass: 'badge-delivered' },
    'RECEIVED': { label: '入荷済み', cssClass: 'badge-delivered' },
    'PARTIALLY_RECEIVED': { label: '一部入荷', cssClass: 'badge-delivered' },
    'INVOICED': { label: '請求済み', cssClass: 'badge-ordered' },
    'CANCELLED': { label: 'キャンセル', cssClass: 'badge-cancelled' },
    'CLOSED': { label: '完了', cssClass: 'badge-approved' },
    'PENDING': { label: '保留中', cssClass: 'badge-pending' },
    // 在庫ステータス
    'IN_STOCK': { label: '在庫あり', cssClass: 'badge-in-stock' },
    'LOW_STOCK': { label: '在庫少', cssClass: 'badge-low-stock' },
    'OUT_OF_STOCK': { label: '在庫切れ', cssClass: 'badge-out-of-stock' },
    // サプライヤーステータス
    'ACTIVE': { label: '有効', cssClass: 'badge-active' },
    'INACTIVE': { label: '無効', cssClass: 'badge-inactive' },
    'SUSPENDED': { label: '停止中', cssClass: 'badge-suspended' },
    // 製品ステータス
    'AVAILABLE': { label: '販売中', cssClass: 'badge-available' },
    'DISCONTINUED': { label: '販売終了', cssClass: 'badge-discontinued' }
  };

  ngOnChanges(): void {
    const mapped = this.statusMap[this.status];
    if (mapped) {
      this.badgeClass = mapped.cssClass;
      this.displayLabel = mapped.label;
    } else {
      this.badgeClass = 'badge-default';
      this.displayLabel = this.status;
    }
  }
}
