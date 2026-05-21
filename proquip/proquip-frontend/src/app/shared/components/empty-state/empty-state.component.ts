import { Component, Input } from '@angular/core';

/**
 * 空状態コンポーネント
 * データが存在しない場合のプレースホルダー表示
 */
@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.scss']
})
export class EmptyStateComponent {

  /** メッセージテキスト */
  @Input() message = 'データがありません';

  /** アイコン（HTML エンティティ） */
  @Input() icon = '&#128196;';

  /** サブメッセージ */
  @Input() submessage = '';

  /** アクションボタンのラベル */
  @Input() actionLabel = '';
}
