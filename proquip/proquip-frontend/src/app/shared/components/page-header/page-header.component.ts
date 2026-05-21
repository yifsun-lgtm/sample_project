import { Component, Input } from '@angular/core';

/**
 * パンくずリスト項目
 */
export interface BreadcrumbItem {
  label: string;
  route?: string;
}

/**
 * ページヘッダーコンポーネント
 * ページタイトル、パンくずリスト、アクションボタンエリアを提供
 */
@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.component.html',
  styleUrls: ['./page-header.component.scss']
})
export class PageHeaderComponent {

  /** ページタイトル */
  @Input() title = '';

  /** サブタイトル */
  @Input() subtitle = '';

  /** 説明テキスト */
  @Input() description = '';

  /** パンくずリスト */
  @Input() breadcrumbs: BreadcrumbItem[] = [];
}
