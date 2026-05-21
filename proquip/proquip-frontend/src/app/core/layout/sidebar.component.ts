import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';

/**
 * サイドバーナビゲーション用メニューアイテム
 */
interface MenuItem {
  label: string;
  icon: string;
  route: string;
}

/**
 * サイドバーコンポーネント
 * 左サイドナビゲーション。折りたたみ機能、アクティブルートハイライト付き。
 */
@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {

  /** 折りたたみ状態 */
  @Input() collapsed = false;

  /** ナビゲーションメニュー項目 */
  menuItems: MenuItem[] = [
    { label: 'ダッシュボード', icon: '&#9632;', route: '/dashboard' },
    { label: '製品カタログ', icon: '&#9733;', route: '/products' },
    { label: 'サプライヤー', icon: '&#9830;', route: '/suppliers' },
    { label: '調達管理', icon: '&#9998;', route: '/procurement' },
    { label: '在庫管理', icon: '&#9635;', route: '/inventory' },
    { label: '倉庫管理', icon: '&#8962;', route: '/warehouses' },
    { label: '価格管理', icon: '&#165;', route: '/pricing' },
    { label: 'レポート', icon: '&#9776;', route: '/reports' },
    { label: '管理者設定', icon: '&#9881;', route: '/admin' },
    { label: 'インポート/エクスポート', icon: '&#8693;', route: '/import-export' }
  ];

  constructor(private router: Router) {}

  /**
   * 指定ルートがアクティブかどうかを判定
   * 現在のURLが指定ルートで始まるか確認
   */
  isActive(route: string): boolean {
    return this.router.url.startsWith(route);
  }
}
