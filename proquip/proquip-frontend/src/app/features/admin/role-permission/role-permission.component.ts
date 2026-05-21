import { Component, OnInit } from '@angular/core';
import { UserService } from '@shared/services/user.service';
import { ApiService } from '@shared/services/api.service';
import { AdminService, PermissionDefinition } from '@shared/services/admin.service';
import { Role } from '@shared/models/user.model';

/**
 * パーミッション定義
 */
export interface Permission {
  key: string;
  label: string;
  category: string;
}

/**
 * パーミッションマトリクスのセル
 */
export interface PermissionCell {
  roleId: string;
  permissionKey: string;
  granted: boolean;
}

/**
 * ロール・権限管理コンポーネント
 * ロールとパーミッションのマトリクス管理を行う
 *
 * 技術的負債: パーミッションマトリクス全体を一つの大きなオブジェクトとして読み込む
 * ページネーションや遅延読み込みを検討すべき
 *
 * 技術的負債: チェックボックスの変更を1つずつ個別リクエストで保存（Nリクエスト発生）
 * バッチ保存に変更すべき
 */
@Component({
  selector: 'app-role-permission',
  templateUrl: './role-permission.component.html',
  styleUrls: ['./role-permission.component.scss']
})
export class RolePermissionComponent implements OnInit {

  /** ロール一覧 */
  roles: Role[] = [];

  /** パーミッション定義一覧 */
  permissions: Permission[] = [];

  /** パーミッションカテゴリ一覧 */
  permissionCategories: string[] = [];

  /**
   * パーミッションマトリクス
   * Key: "roleId:permissionKey", Value: boolean
   *
   * 技術的負債: マトリクス全体を1つのオブジェクトで管理
   */
  permissionMatrix: { [key: string]: boolean } = {};

  /** ローディング */
  isLoading = false;

  /** 保存中のセル */
  savingCells: Set<string> = new Set();

  /** 成功メッセージ */
  successMessage = '';

  /** エラーメッセージ */
  errorMessage = '';

  /** 変更件数 */
  changeCount = 0;

  constructor(
    private userService: UserService,
    private api: ApiService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.loadPermissions();
    this.loadRolesAndMatrix();
  }

  /** パーミッション定義をAPIから読み込む */
  private loadPermissions(): void {
    this.adminService.getPermissions().subscribe({
      next: (definitions: PermissionDefinition[]) => {
        this.permissions = definitions.map(def => ({
          key: def.code,
          label: def.name,
          category: def.resource
        }));

        // カテゴリ一覧を抽出
        const categorySet = new Set<string>();
        this.permissions.forEach(p => categorySet.add(p.category));
        this.permissionCategories = Array.from(categorySet);
      },
      error: (err) => {
        console.error('パーミッション定義取得エラー:', err);
        this.errorMessage = 'パーミッション定義の取得に失敗しました。';
        this.permissions = [];
        this.permissionCategories = [];
      }
    });
  }

  /** ロールとマトリクスを読み込む */
  private loadRolesAndMatrix(): void {
    this.isLoading = true;

    this.userService.getRoles().subscribe({
      next: (roles) => {
        this.roles = roles;
        this.buildMatrix();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('ロール取得エラー:', err);
        this.isLoading = false;
        this.errorMessage = 'ロール一覧の取得に失敗しました。';
        this.roles = [];
      }
    });
  }

  /**
   * パーミッションマトリクスを構築する
   *
   * 技術的負債: マトリクス全体を1つのオブジェクトとして読み込む
   * 大量のロール/パーミッションがある場合パフォーマンスに問題
   */
  private buildMatrix(): void {
    this.permissionMatrix = {};
    // 技術的負債: ネストされたループで巨大オブジェクトを構築
    for (const role of this.roles) {
      for (const perm of this.permissions) {
        const key = `${role.id}:${perm.key}`;
        this.permissionMatrix[key] = role.permissions.includes(perm.key);
      }
    }
  }

  /** マトリクスキーを取得 */
  getMatrixKey(roleId: string, permissionKey: string): string {
    return `${roleId}:${permissionKey}`;
  }

  /** パーミッションが付与されているか */
  isGranted(roleId: string, permissionKey: string): boolean {
    return this.permissionMatrix[this.getMatrixKey(roleId, permissionKey)] || false;
  }

  /** セルが保存中か */
  isCellSaving(roleId: string, permissionKey: string): boolean {
    return this.savingCells.has(this.getMatrixKey(roleId, permissionKey));
  }

  /**
   * パーミッションのトグル
   *
   * 技術的負債: チェックボックスの変更を1つずつ個別リクエストで保存
   * Nリクエストが発生し、パフォーマンスに問題がある
   * バッチ保存（一括更新API）に変更すべき
   */
  togglePermission(roleId: string, permissionKey: string): void {
    const key = this.getMatrixKey(roleId, permissionKey);
    const newValue = !this.permissionMatrix[key];
    this.permissionMatrix[key] = newValue;

    // 技術的負債: 1つずつ個別にAPIリクエストを送信
    this.savingCells.add(key);

    this.api.put<any>(`/admin/roles/${roleId}/permissions`, {
      permissionKey: permissionKey,
      granted: newValue
    }).subscribe({
      next: () => {
        this.savingCells.delete(key);
        this.changeCount++;
        this.successMessage = 'パーミッションを更新しました。';
        setTimeout(() => { this.successMessage = ''; }, 2000);
      },
      error: (err) => {
        console.error('パーミッション更新エラー:', err);
        // ロールバック
        this.permissionMatrix[key] = !newValue;
        this.savingCells.delete(key);
        this.errorMessage = 'パーミッションの更新に失敗しました。';
        setTimeout(() => { this.errorMessage = ''; }, 3000);
      }
    });
  }

  /** カテゴリ内のパーミッションを取得 */
  getPermissionsByCategory(category: string): Permission[] {
    return this.permissions.filter(p => p.category === category);
  }
}
