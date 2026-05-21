import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * カスタムバリデータ
 * アプリケーション固有のフォームバリデーションルールを提供
 */
export class CustomValidators {

  /**
   * SKU形式バリデータ
   * フロントエンド: [A-Za-z]{2,5}-[0-9]{4,8}
   *
   * 技術的負債 #17: バックエンドのSKUバリデーションパターンと異なる
   * バックエンドは [A-Z]{2,4}-[0-9]{4,6} を使用しており、
   * フロントエンドの方が許容範囲が広い
   */
  static skuFormat(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;

      // 技術的負債: バックエンドと異なる正規表現パターン
      const pattern = /^[A-Za-z]{2,5}-[0-9]{4,8}$/;
      if (!pattern.test(control.value)) {
        return { skuFormat: { message: 'SKU形式が正しくありません（例: ABC-12345）' } };
      }
      return null;
    };
  }

  /**
   * 最大金額バリデータ
   *
   * 技術的負債 #17: フロントエンドの最大値は1,000,000だが、
   * バックエンドは999,999.99を許可しており、境界値が一致しない
   */
  static maxAmount(max: number = 1000000): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (control.value == null) return null;

      // 技術的負債: バックエンドの上限値（999,999.99）と不一致
      if (control.value > max) {
        return {
          maxAmount: {
            message: `金額は${max.toLocaleString()}以下で入力してください`,
            max: max,
            actual: control.value
          }
        };
      }
      return null;
    };
  }

  /**
   * 条件付き必須バリデータ
   * 指定した条件が真の場合にのみ必須となる
   */
  static requiredIf(conditionFn: () => boolean): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!conditionFn()) return null;

      if (!control.value || (typeof control.value === 'string' && control.value.trim() === '')) {
        return { requiredIf: { message: 'この項目は必須です' } };
      }
      return null;
    };
  }

  /**
   * 日付範囲バリデータ
   * 開始日が終了日より前であることを検証する
   */
  static dateRange(startFieldName: string, endFieldName: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const startValue = control.get(startFieldName)?.value;
      const endValue = control.get(endFieldName)?.value;

      if (!startValue || !endValue) return null;

      const startDate = new Date(startValue);
      const endDate = new Date(endValue);

      if (startDate >= endDate) {
        return {
          dateRange: {
            message: '開始日は終了日より前の日付を指定してください',
            start: startValue,
            end: endValue
          }
        };
      }
      return null;
    };
  }
}
