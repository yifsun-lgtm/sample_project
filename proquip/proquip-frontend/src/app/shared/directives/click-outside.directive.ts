import { Directive, ElementRef, EventEmitter, HostListener, Output } from '@angular/core';

/**
 * 要素外クリック検出ディレクティブ
 * 要素の外側がクリックされた場合にイベントを発火する
 * ドロップダウンメニューやモーダルの閉じる処理に使用
 *
 * 使用例: <div (appClickOutside)="onCloseMenu()">...</div>
 */
@Directive({
  selector: '[appClickOutside]'
})
export class ClickOutsideDirective {

  /** 要素外クリック時のイベント */
  @Output() appClickOutside = new EventEmitter<void>();

  constructor(private elementRef: ElementRef) {}

  /**
   * ドキュメント全体のクリックを監視
   * クリック位置が要素の外側であればイベントを発火
   */
  @HostListener('document:click', ['$event.target'])
  onClick(target: HTMLElement): void {
    const clickedInside = this.elementRef.nativeElement.contains(target);
    if (!clickedInside) {
      this.appClickOutside.emit();
    }
  }
}
