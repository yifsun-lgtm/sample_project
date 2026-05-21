import { Pipe, PipeTransform } from '@angular/core';

/**
 * 日本語日付パイプ
 * 日付をYYYY年MM月DD日形式にフォーマットする
 *
 * 使用例: {{ someDate | japaneseDate }}
 * 出力例: 2024年03月15日
 */
@Pipe({
  name: 'japaneseDate'
})
export class JapaneseDatePipe implements PipeTransform {

  transform(value: string | Date | null | undefined, includeTime: boolean = false): string {
    if (!value) return '';

    const cleaned = typeof value === 'string' ? value.replace(/\[.*\]$/, '') : value;
    const date = cleaned instanceof Date ? cleaned : new Date(cleaned);

    if (isNaN(date.getTime())) {
      return '';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    let result = `${year}年${month}月${day}日`;

    if (includeTime) {
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      result += ` ${hours}:${minutes}`;
    }

    return result;
  }
}
