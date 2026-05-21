import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';

/**
 * 検索ボックスコンポーネント
 * デバウンス付きの検索入力フィールド
 *
 * 技術的負債 #6: デバウンスをsetTimeoutで実装している
 * RxJSのdebounceTimeを使用すべき
 */
@Component({
  selector: 'app-search-box',
  templateUrl: './search-box.component.html',
  styleUrls: ['./search-box.component.scss']
})
export class SearchBoxComponent implements OnDestroy {

  /** プレースホルダーテキスト */
  @Input() placeholder = '検索...';

  /** デバウンス時間（ミリ秒） */
  @Input() debounceTime = 300;

  /** 検索イベント */
  @Output() searchChange = new EventEmitter<string>();

  /** 検索キーワード */
  searchTerm = '';

  /**
   * 技術的負債: setTimeoutを使ったデバウンス
   * RxJSのSubject + debounceTimeパイプに置き換えるべき
   */
  private debounceTimer: any = null;

  /** 入力変更時のハンドラ */
  onInputChange(): void {
    // 技術的負債: setTimeoutベースのデバウンス実装
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    this.debounceTimer = setTimeout(() => {
      this.searchChange.emit(this.searchTerm);
    }, this.debounceTime);
  }

  /** 検索クリア */
  onClear(): void {
    this.searchTerm = '';
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
    this.searchChange.emit('');
  }

  ngOnDestroy(): void {
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
    }
  }
}
