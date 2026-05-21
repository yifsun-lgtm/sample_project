import { Component } from '@angular/core';

/**
 * フッターコンポーネント
 * アプリケーション下部のフッター表示
 */
@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent {
  /** 現在の年 */
  currentYear = new Date().getFullYear();
}
