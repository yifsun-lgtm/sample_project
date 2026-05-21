import { Pipe, PipeTransform } from '@angular/core';

/**
 * テキスト切り詰めパイプ
 * 長いテキストを指定文字数で切り詰め、"..."を付与する
 *
 * 使用例: {{ longText | truncate:50 }}
 */
@Pipe({
  name: 'truncate'
})
export class TruncatePipe implements PipeTransform {

  transform(value: string | null | undefined, limit: number = 100, trail: string = '...'): string {
    if (!value) return '';
    return value.length > limit ? value.substring(0, limit) + trail : value;
  }
}
