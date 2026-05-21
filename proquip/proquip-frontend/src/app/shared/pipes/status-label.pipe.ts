import { Pipe, PipeTransform } from '@angular/core';

/**
 * ステータスラベルパイプ
 * 英語のステータスコードを日本語ラベルに変換する
 *
 * 技術的負債 #4: ステータスマッピングがハードコードされている
 * StatusBadgeComponentと重複した定義がある（DRY原則違反）
 * 設定ファイルやAPIから取得すべき
 *
 * 使用例: {{ 'PENDING_APPROVAL' | statusLabel }}
 * 出力例: 承認待ち
 */
@Pipe({
  name: 'statusLabel'
})
export class StatusLabelPipe implements PipeTransform {

  /** 技術的負債: ハードコードされたステータスマッピング */
  private readonly statusMap: { [key: string]: string } = {
    // 発注・購買ステータス
    'DRAFT': '下書き',
    'SUBMITTED': '申請中',
    'PENDING_APPROVAL': '承認待ち',
    'APPROVED': '承認済み',
    'REJECTED': '却下',
    'ORDERED': '発注済み',
    'PARTIALLY_ORDERED': '一部発注済み',
    'SHIPPED': '出荷済み',
    'DELIVERED': '納品済み',
    'PARTIALLY_DELIVERED': '一部納品',
    'RECEIVED': '入荷済み',
    'PARTIALLY_RECEIVED': '一部入荷',
    'INVOICED': '請求済み',
    'CANCELLED': 'キャンセル',
    'CLOSED': '完了',
    // 在庫ステータス
    'IN_STOCK': '在庫あり',
    'LOW_STOCK': '在庫少',
    'OUT_OF_STOCK': '在庫切れ',
    // サプライヤーステータス
    'ACTIVE': '有効',
    'INACTIVE': '無効',
    'SUSPENDED': '停止中',
    // 製品ステータス
    'AVAILABLE': '販売中',
    'DISCONTINUED': '販売終了',
    // 承認ステータス
    'PENDING': '保留中',
    'IN_REVIEW': 'レビュー中'
  };

  transform(value: string | null | undefined): string {
    if (!value) return '';
    return this.statusMap[value] || value;
  }
}
