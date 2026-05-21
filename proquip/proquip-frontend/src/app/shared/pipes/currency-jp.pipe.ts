import { Pipe, PipeTransform } from '@angular/core';

/**
 * 日本円フォーマットパイプ
 * 数値を日本円形式（¥1,234,567）にフォーマットする
 *
 * 使用例: {{ price | currencyJp }}
 * 出力例: ¥1,234,567
 */
@Pipe({
  name: 'currencyJp'
})
export class CurrencyJpPipe implements PipeTransform {

  transform(value: number | null | undefined, showSymbol: boolean = true): string {
    if (value == null) return '';

    const formatted = value.toLocaleString('ja-JP', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    });

    return showSymbol ? `¥${formatted}` : formatted;
  }
}
