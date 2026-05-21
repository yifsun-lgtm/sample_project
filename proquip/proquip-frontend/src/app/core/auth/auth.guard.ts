import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { KeycloakAuthGuard, KeycloakService } from 'keycloak-angular';

/**
 * 認証ガード
 * Keycloak認証状態を確認し、未認証の場合はログイン画面へリダイレクト
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard extends KeycloakAuthGuard implements CanActivate {

  constructor(
    protected override readonly router: Router,
    protected readonly keycloak: KeycloakService
  ) {
    super(router, keycloak);
  }

  /**
   * ルートへのアクセス可否を判定
   * 未認証の場合はKeycloakログインページへリダイレクト
   */
  async isAccessAllowed(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean> {
    // 未認証の場合、Keycloakログインへリダイレクト
    if (!this.authenticated) {
      await this.keycloak.login({
        redirectUri: window.location.origin + state.url
      });
      return false;
    }

    // ルートに必要なロールが指定されている場合、ロールチェック
    const requiredRoles = route.data['roles'] as string[];
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    // ユーザーが必要なロールのいずれかを持っているか確認
    return requiredRoles.some(role => this.roles.includes(role));
  }
}
