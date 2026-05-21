import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { KeycloakProfile } from 'keycloak-js';

/**
 * 認証サービス
 * KeycloakServiceをラップし、アプリケーション固有の認証機能を提供
 *
 * 技術的負債: ユーザーロールをプレーンな配列で保持しており、Observable化されていない
 * ロール変更時にリアクティブに反映されない問題がある
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  /** ユーザーのロール一覧（技術的負債: Observableにすべき） */
  private userRoles: string[] = [];

  /** キャッシュされたユーザープロファイル */
  private userProfile: KeycloakProfile | null = null;

  constructor(private keycloakService: KeycloakService) {
    // 初期化時にロールを取得
    this.loadUserRoles();
  }

  /** ユーザーロールを読み込む */
  private loadUserRoles(): void {
    try {
      this.userRoles = this.keycloakService.getUserRoles(true);
    } catch (e) {
      // Keycloak未初期化の場合は空配列
      this.userRoles = [];
    }
  }

  /** Keycloakログインを実行 */
  login(): void {
    this.keycloakService.login();
  }

  /** ログアウトを実行し、リダイレクトする */
  logout(): void {
    this.keycloakService.logout(window.location.origin);
  }

  /** 認証済みかどうかを返す */
  isLoggedIn(): boolean {
    try {
      return this.keycloakService.isLoggedIn();
    } catch (e) {
      return false;
    }
  }

  /** 現在のアクセストークンを取得 */
  async getToken(): Promise<string> {
    return await this.keycloakService.getToken();
  }

  /** ユーザープロファイルを取得（キャッシュ付き） */
  async getUserProfile(): Promise<KeycloakProfile> {
    if (!this.userProfile) {
      this.userProfile = await this.keycloakService.loadUserProfile();
    }
    return this.userProfile;
  }

  /** 指定されたロールを持っているか確認 */
  hasRole(role: string): boolean {
    // 技術的負債: キャッシュされた配列から検索しているため、動的なロール変更に対応できない
    if (this.userRoles.length === 0) {
      this.loadUserRoles();
    }
    return this.userRoles.includes(role);
  }

  /** ユーザーの全ロールを取得 */
  getRoles(): string[] {
    if (this.userRoles.length === 0) {
      this.loadUserRoles();
    }
    return [...this.userRoles];
  }

  /** ユーザー名を取得 */
  getUsername(): string {
    try {
      return this.keycloakService.getUsername();
    } catch (e) {
      return '';
    }
  }
}
