import { Component, EventEmitter, OnInit, OnDestroy, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { ApiService } from '@shared/services/api.service';
import { KeycloakProfile } from 'keycloak-js';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit, OnDestroy {

  @Output() toggleSidebar = new EventEmitter<void>();

  username = '';
  userRole = '';
  showUserMenu = false;
  notificationCount = 0;
  searchKeyword = '';

  private pollingTimer: any = null;

  constructor(
    private authService: AuthService,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.loadUnreadCount();
    this.pollingTimer = setInterval(() => this.loadUnreadCount(), 60000);
  }

  ngOnDestroy(): void {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }
  }

  private loadUnreadCount(): void {
    this.apiService.get<any>('/notifications/unread-count').subscribe(
      (result) => {
        this.notificationCount = result.unreadCount || 0;
      },
      () => {
        this.notificationCount = 0;
      }
    );
  }

  /** ユーザー情報を非同期で読み込む */
  private async loadUserInfo(): Promise<void> {
    try {
      const profile: KeycloakProfile = await this.authService.getUserProfile();
      this.username = profile.firstName
        ? `${profile.lastName} ${profile.firstName}`
        : this.authService.getUsername();

      const roles = this.authService.getRoles();
      this.userRole = roles.length > 0 ? roles[0] : '一般ユーザー';
    } catch (e) {
      this.username = 'ゲスト';
      this.userRole = '';
    }
  }

  /** サイドバーの開閉を切り替え */
  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }

  /** ユーザーメニューの表示切替 */
  onToggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  /** ユーザーメニューを閉じる */
  onCloseUserMenu(): void {
    this.showUserMenu = false;
  }

  /** ログアウト */
  onLogout(): void {
    this.authService.logout();
  }

  /** 検索実行 — キーワードに基づいて適切なページへ遷移 */
  onSearch(): void {
    const keyword = this.searchKeyword.trim();
    if (!keyword) return;

    const lower = keyword.toLowerCase();
    if (lower.startsWith('po-') || lower.startsWith('po') && /\d/.test(lower)) {
      this.router.navigate(['/procurement'], { queryParams: { search: keyword } });
    } else if (lower.includes('サプライヤー') || lower.includes('仕入')) {
      this.router.navigate(['/suppliers'], { queryParams: { search: keyword } });
    } else {
      this.router.navigate(['/products'], { queryParams: { search: keyword } });
    }
    this.searchKeyword = '';
  }
}
