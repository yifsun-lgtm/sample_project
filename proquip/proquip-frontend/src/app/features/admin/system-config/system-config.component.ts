import { Component, OnInit } from '@angular/core';
import { AdminService, SystemConfig } from '@shared/services/admin.service';

/**
 * 設定値の型定義
 */
export type ConfigDataType = 'text' | 'number' | 'boolean' | 'json';

/**
 * 拡張設定アイテム（UI用）
 */
export interface ConfigItem extends SystemConfig {
  dataType: ConfigDataType;
  isEditing: boolean;
  editValue: string;
  isSaving: boolean;
}

/**
 * システム設定コンポーネント
 * カテゴリ別のシステム設定をKey-Value形式で管理する
 *
 * 技術的負債: JSONフィールドのバリデーションが存在しない
 * 不正なJSONが保存される可能性がある
 */
@Component({
  selector: 'app-system-config',
  templateUrl: './system-config.component.html',
  styleUrls: ['./system-config.component.scss']
})
export class SystemConfigComponent implements OnInit {

  /** カテゴリタブ */
  categories = ['一般', '調達', '在庫', '通知', '承認', 'セキュリティ', '画面'];

  /** 現在のタブ */
  activeCategory = '一般';

  /** 全設定アイテム */
  allConfigs: ConfigItem[] = [];

  /** 表示用設定アイテム（カテゴリフィルター後） */
  displayConfigs: ConfigItem[] = [];

  /** ローディング */
  isLoading = false;

  /** 成功メッセージ */
  successMessage = '';

  /** エラーメッセージ */
  errorMessage = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadConfigs();
  }

  /** 設定を読み込む */
  loadConfigs(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.adminService.getSystemConfigs().subscribe({
      next: (configs) => {
        this.allConfigs = configs.map(config => ({
          ...config,
          dataType: this.detectDataType(config.key, config.value),
          isEditing: false,
          editValue: config.value,
          isSaving: false
        }));
        this.filterByCategory();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('システム設定取得エラー:', err);
        this.isLoading = false;
        this.errorMessage = 'システム設定の取得に失敗しました。';
        this.allConfigs = [];
        this.displayConfigs = [];
      }
    });
  }

  /** データ型を自動検出する */
  private detectDataType(key: string, value: string): ConfigDataType {
    if (value === 'true' || value === 'false') return 'boolean';
    if (!isNaN(Number(value)) && value.trim() !== '') return 'number';
    try {
      JSON.parse(value);
      if (value.startsWith('{') || value.startsWith('[')) return 'json';
    } catch {
      // JSONでない
    }
    return 'text';
  }

  /** カテゴリタブ切り替え */
  switchCategory(category: string): void {
    this.activeCategory = category;
    this.filterByCategory();
  }

  /** カテゴリでフィルター */
  private filterByCategory(): void {
    this.displayConfigs = this.allConfigs.filter(c => c.category === this.activeCategory);
  }

  /** 編集モードに切り替え */
  startEditing(config: ConfigItem): void {
    config.isEditing = true;
    config.editValue = config.value;
  }

  /** 編集をキャンセル */
  cancelEditing(config: ConfigItem): void {
    config.isEditing = false;
    config.editValue = config.value;
  }

  /**
   * 設定値を保存する
   *
   * 技術的負債: JSONフィールドのバリデーションが存在しない
   */
  saveConfig(config: ConfigItem): void {
    // 技術的負債: JSONフィールドのバリデーションなし
    // 不正なJSONが保存される可能性がある
    config.isSaving = true;
    this.errorMessage = '';

    this.adminService.updateSystemConfig(config.key, config.editValue).subscribe({
      next: (updated) => {
        config.value = config.editValue;
        config.isEditing = false;
        config.isSaving = false;
        config.updatedAt = new Date().toISOString();
        this.successMessage = `「${config.key}」を更新しました。`;
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (err) => {
        console.error('設定更新エラー:', err);
        config.isSaving = false;
        this.errorMessage = `「${config.key}」の更新に失敗しました。`;
        setTimeout(() => { this.errorMessage = ''; }, 3000);
      }
    });
  }

  /** ブールトグル */
  toggleBoolean(config: ConfigItem): void {
    config.editValue = config.value === 'true' ? 'false' : 'true';
    this.saveConfig(config);
  }
}
