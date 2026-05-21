import { Pipe, PipeTransform } from '@angular/core';

/**
 * 注文番号フォーマットパイプ
 * 注文番号を表示用にフォーマットする
 *
 * 使用例: {{ 'PO20240315001' | orderNumber }}
 * 出力例: PO-2024-0315-001
 */
@Pipe({
  name: 'orderNumber'
})
export class OrderNumberPipe implements PipeTransform {

  transform(value: string | null | undefined): string {
    if (!value) return '';

    // PO + 8桁 + 3桁の形式を整形
    if (value.length >= 13 && value.startsWith('PO')) {
      const prefix = value.substring(0, 2);
      const year = value.substring(2, 6);
      const date = value.substring(6, 10);
      const seq = value.substring(10);
      return `${prefix}-${year}-${date}-${seq}`;
    }

    return value;
  }
}
