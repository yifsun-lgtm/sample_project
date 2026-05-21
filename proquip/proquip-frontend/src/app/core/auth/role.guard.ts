import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { NotificationService } from '@core/services/notification.service';

/**
 * ロールガード
 * ルートデータに指定されたロールをユーザーが持っているか確認する
 * 使用例: data: { roles: ['ADMIN', 'MANAGER'] }
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  /**
   * ロールベースのアクセス制御
   * ルートのdataプロパティに定義されたrolesのいずれかを持っていればアクセス許可
   */
  canActivate(
    route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot
  ): boolean {
    const requiredRoles = route.data['roles'] as string[];

    // ロール指定がない場合はアクセス許可
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    // いずれかの必要ロールを持っていればアクセス許可
    const hasAccess = requiredRoles.some(role => this.authService.hasRole(role));

    if (!hasAccess) {
      this.notificationService.error('このページにアクセスする権限がありません。');
      this.router.navigate(['/dashboard']);
      return false;
    }

    return true;
  }
}
