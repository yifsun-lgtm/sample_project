import { AfterViewInit, Directive, ElementRef } from '@angular/core';

/**
 * オートフォーカスディレクティブ
 * 要素が表示された際に自動的にフォーカスを当てる
 *
 * 使用例: <input appAutoFocus />
 */
@Directive({
  selector: '[appAutoFocus]'
})
export class AutoFocusDirective implements AfterViewInit {

  constructor(private elementRef: ElementRef) {}

  ngAfterViewInit(): void {
    // 描画完了後にフォーカスを設定
    setTimeout(() => {
      this.elementRef.nativeElement.focus();
    }, 0);
  }
}
