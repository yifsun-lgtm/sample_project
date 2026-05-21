import { Component } from '@angular/core';

/**
 * アプリケーションのルートコンポーネント
 * レイアウトの骨格を提供する
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  /** アプリケーションタイトル */
  title = 'ProQuip - 調達・在庫管理システム';

  /** サイドバーの折りたたみ状態 */
  sidebarCollapsed = false;

  /** サイドバーの開閉を切り替える */
  onSidebarToggle(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }
}
