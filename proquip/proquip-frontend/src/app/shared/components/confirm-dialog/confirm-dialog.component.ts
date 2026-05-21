import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * 確認ダイアログコンポーネント
 * モーダル形式の確認・キャンセルダイアログ
 */
@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.scss']
})
export class ConfirmDialogComponent {

  @Input() title = '確認';
  @Input() message = 'この操作を実行してもよろしいですか？';
  @Input() confirmText = '確認';
  @Input() cancelText = 'キャンセル';
  @Input() visible = true;

  @Input() set confirmLabel(val: string) { this.confirmText = val; }
  @Input() set cancelLabel(val: string) { this.cancelText = val; }

  @Output() confirmed = new EventEmitter<boolean>();
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm(): void {
    this.confirmed.emit(true);
    this.confirm.emit();
    this.visible = false;
  }

  onCancel(): void {
    this.confirmed.emit(false);
    this.cancel.emit();
    this.visible = false;
  }

  /** オーバーレイクリック時にキャンセル */
  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.onCancel();
    }
  }
}
