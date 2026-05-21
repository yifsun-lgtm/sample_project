import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '@shared/services/user.service';
import { UserProfile, Role } from '@shared/models/user.model';

/**
 * ユーザー管理コンポーネント
 * ユーザープロファイルの検索・編集機能を提供する
 *
 * 注意: ユーザー作成はKeycloakで行う。ここではUserProfileの管理のみ
 *
 * 技術的負債: ロール管理にstring配列を使用しており型付きenumを使っていない
 */
@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {

  /** ユーザー一覧 */
  users: UserProfile[] = [];

  /** ロール一覧 */
  availableRoles: Role[] = [];

  /** ページネーション */
  currentPage = 1;
  pageSize = 20;
  totalCount = 0;

  /** ローディング */
  isLoading = false;

  /** 検索キーワード */
  searchKeyword = '';

  /** フィルター: ロール */
  filterRole = '';

  /** フィルター: 部門 */
  filterDepartment = '';

  /** フィルター: ステータス */
  filterStatus = '';

  /** 部門オプション */
  departmentOptions = [
    { value: '', label: 'すべての部門' },
    { value: '総務部', label: '総務部' },
    { value: '営業部', label: '営業部' },
    { value: '開発部', label: '開発部' },
    { value: '製造部', label: '製造部' },
    { value: '経理部', label: '経理部' },
    { value: '人事部', label: '人事部' }
  ];

  /** ステータスオプション */
  statusOptions = [
    { value: '', label: 'すべて' },
    { value: 'true', label: '有効' },
    { value: 'false', label: '無効' }
  ];

  /** 編集モーダル表示 */
  showEditModal = false;

  /** 編集対象ユーザー */
  editingUser: UserProfile | null = null;

  /** 編集フォーム */
  editForm!: FormGroup;

  /**
   * 技術的負債: ロール管理にstring配列を使用
   * 型付きenumに置き換えるべき
   */
  selectedRoles: string[] = [];

  /** 保存中 */
  isSaving = false;

  /** 成功メッセージ */
  successMessage = '';

  /** エラーメッセージ */
  errorMessage = '';

  /** フィルター後のユーザー一覧 */
  filteredUsers: UserProfile[] = [];

  constructor(
    private userService: UserService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadUsers();
    this.loadRoles();
  }

  /** フォーム初期化 */
  private initForm(): void {
    this.editForm = this.fb.group({
      department: ['', [Validators.required]],
      enabled: [true]
    });
  }

  /** ユーザー一覧を読み込む */
  loadUsers(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.userService.getUsers(this.currentPage - 1, this.pageSize).subscribe({
      next: (result) => {
        this.users = result.content || [];
        this.totalCount = result.totalElements || 0;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('ユーザー一覧取得エラー:', err);
        this.isLoading = false;
        this.errorMessage = 'ユーザー一覧の取得に失敗しました。';
        this.users = [];
        this.filteredUsers = [];
        this.totalCount = 0;
      }
    });
  }

  /** ロール一覧を読み込む */
  private loadRoles(): void {
    this.userService.getRoles().subscribe({
      next: (roles) => {
        this.availableRoles = roles;
      },
      error: (err) => {
        console.error('ロール一覧取得エラー:', err);
        this.errorMessage = 'ロール一覧の取得に失敗しました。';
        this.availableRoles = [];
      }
    });
  }

  /** フィルターを適用 */
  applyFilters(): void {
    let filtered = [...this.users];

    // キーワード検索
    if (this.searchKeyword) {
      const keyword = this.searchKeyword.toLowerCase();
      filtered = filtered.filter(u =>
        u.username.toLowerCase().includes(keyword) ||
        (u.lastName + u.firstName).toLowerCase().includes(keyword) ||
        u.email.toLowerCase().includes(keyword)
      );
    }

    // ロールフィルター
    if (this.filterRole) {
      // 技術的負債: string配列によるロール比較
      filtered = filtered.filter(u =>
        u.roles.some(r => r.name === this.filterRole)
      );
    }

    // 部門フィルター
    if (this.filterDepartment) {
      filtered = filtered.filter(u => u.department === this.filterDepartment);
    }

    // ステータスフィルター
    if (this.filterStatus !== '') {
      const isEnabled = this.filterStatus === 'true';
      filtered = filtered.filter(u => u.enabled === isEnabled);
    }

    this.filteredUsers = filtered;
  }

  /** 検索キーワード変更 */
  onSearchChange(keyword: string): void {
    this.searchKeyword = keyword;
    this.applyFilters();
  }

  /** 編集モーダルを開く */
  openEditModal(user: UserProfile): void {
    this.editingUser = user;
    this.editForm.patchValue({
      department: user.department,
      enabled: user.enabled
    });
    // 技術的負債: ロールをstring配列で管理
    this.selectedRoles = user.roles.map(r => r.name);
    this.showEditModal = true;
    this.errorMessage = '';
  }

  /** モーダルを閉じる */
  closeEditModal(): void {
    this.showEditModal = false;
    this.editingUser = null;
    this.errorMessage = '';
  }

  /**
   * ロールの選択/解除
   *
   * 技術的負債: string配列による管理で型安全性が低い
   */
  toggleRole(roleName: string): void {
    const index = this.selectedRoles.indexOf(roleName);
    if (index > -1) {
      // 技術的負債: string配列のミュータブルな操作
      this.selectedRoles.splice(index, 1);
    } else {
      this.selectedRoles.push(roleName);
    }
  }

  /** ロールが選択されているか */
  isRoleSelected(roleName: string): boolean {
    return this.selectedRoles.includes(roleName);
  }

  /** 保存 */
  onSave(): void {
    if (this.editForm.invalid || !this.editingUser) return;

    this.isSaving = true;
    this.errorMessage = '';

    const updateData = {
      department: this.editForm.value.department,
      enabled: this.editForm.value.enabled,
      // 技術的負債: ロールをstring配列で送信
      roles: this.selectedRoles
    };

    this.userService.updateUser(this.editingUser.id, updateData).subscribe({
      next: () => {
        this.successMessage = 'ユーザー情報を更新しました。';
        this.isSaving = false;
        this.closeEditModal();
        this.loadUsers();
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (err) => {
        console.error('ユーザー更新エラー:', err);
        this.errorMessage = 'ユーザー情報の更新に失敗しました。';
        this.isSaving = false;
      }
    });
  }

  /** ユーザー表示名を取得 */
  getDisplayName(user: UserProfile): string {
    return `${user.lastName} ${user.firstName}`;
  }

  /** ロール名のリストを表示用に取得 */
  getRoleNames(user: UserProfile): string {
    // 技術的負債: ロール名をstringとして結合
    return user.roles.map(r => r.name).join(', ');
  }
}
