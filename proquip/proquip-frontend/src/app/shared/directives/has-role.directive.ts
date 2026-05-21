import { Directive, Input, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';

/**
 * ロールベース表示ディレクティブ
 * ユーザーが指定ロールを持っている場合のみ要素を表示する
 *
 * 使用例: <div *appHasRole="'ADMIN'">管理者のみ表示</div>
 * 使用例: <div *appHasRole="['ADMIN', 'MANAGER']">管理者・マネージャーのみ表示</div>
 */
@Directive({
  selector: '[appHasRole]'
})
export class HasRoleDirective implements OnInit {

  /** 必要なロール（文字列またはロール配列） */
  @Input('appHasRole') roles: string | string[] = [];

  private isVisible = false;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const requiredRoles = Array.isArray(this.roles) ? this.roles : [this.roles];

    // いずれかのロールを持っていれば表示
    const hasRole = requiredRoles.some(role => this.authService.hasRole(role));

    if (hasRole && !this.isVisible) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.isVisible = true;
    } else if (!hasRole && this.isVisible) {
      this.viewContainer.clear();
      this.isVisible = false;
    }
  }
}
