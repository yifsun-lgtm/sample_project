import { Component, Input } from '@angular/core';

/**
 * フォームフィールドラッパーコンポーネント
 * ラベル、必須マーク、エラーメッセージ表示を提供
 */
@Component({
  selector: 'app-form-field',
  templateUrl: './form-field.component.html',
  styleUrls: ['./form-field.component.scss']
})
export class FormFieldComponent {

  /** フィールドラベル */
  @Input() label = '';

  /** 必須フィールドかどうか */
  @Input() required = false;

  /** エラーメッセージ */
  @Input() errorMessage = '';

  /** ヒントテキスト */
  @Input() hint = '';

  /** フィールドID（ラベルのfor属性用） */
  @Input() fieldId = '';

  /** エラー状態かどうか */
  @Input() hasError = false;
}
