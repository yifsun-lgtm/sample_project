import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ApiService } from '@shared/services/api.service';
import { TableColumn } from '@shared/components/data-table/data-table.component';

/**
 * 価格リストインターフェース
 */
export interface PriceList {
  id: number;
  name: string;
  description: string;
  currency: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  status: string;
  itemCount: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 価格リスト管理コンポーネント
 * 価格リストのCRUD操作を提供する
 *
 * 技術的負債: モーダルの表示状態管理がコンポーネントに混在している
 * 本来はサービスやダイアログマネージャーで管理すべき
 */
@Component({
  selector: 'app-price-list-management',
  templateUrl: './price-list-management.component.html',
  styleUrls: ['./price-list-management.component.scss']
})
export class PriceListManagementComponent implements OnInit {

  /** 価格リスト一覧 */
  priceLists: PriceList[] = [];

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'name', label: '価格リスト名', sortable: true, type: 'text' },
    { key: 'currency', label: '通貨', sortable: true, type: 'text', width: '80px' },
    { key: 'effectiveStartDate', label: '有効開始日', sortable: true, type: 'date' },
    { key: 'effectiveEndDate', label: '有効終了日', sortable: true, type: 'date' },
    { key: 'status', label: 'ステータス', sortable: true, type: 'status', width: '120px' },
    { key: 'itemCount', label: 'アイテム数', sortable: true, type: 'number', width: '100px' }
  ];

  /** ページネーション */
  currentPage = 1;
  pageSize = 20;
  totalCount = 0;

  /** ローディング状態 */
  isLoading = false;

  /** 検索キーワード */
  searchKeyword = '';

  /** フィルター: ステータス */
  filterStatus = '';

  /** 利用可能なステータスリスト */
  statusOptions = [
    { value: '', label: 'すべて' },
    { value: 'ACTIVE', label: '有効' },
    { value: 'DRAFT', label: '下書き' },
    { value: 'EXPIRED', label: '期限切れ' },
    { value: 'INACTIVE', label: '無効' }
  ];

  /** 通貨オプション */
  currencyOptions = [
    { value: 'JPY', label: '日本円 (JPY)' },
    { value: 'USD', label: '米ドル (USD)' },
    { value: 'EUR', label: 'ユーロ (EUR)' },
    { value: 'CNY', label: '人民元 (CNY)' }
  ];

  // --- 技術的負債: モーダル状態管理がコンポーネントに混在 ---
  /** モーダル表示フラグ */
  showModal = false;

  /** 編集モードフラグ */
  isEditMode = false;

  /** 編集中の価格リストID */
  editingPriceListId: number | null = null;

  /** モーダルフォーム */
  priceListForm!: FormGroup;

  /** 削除確認ダイアログ表示フラグ */
  showDeleteConfirm = false;

  /** 削除対象の価格リスト */
  deletingPriceList: PriceList | null = null;

  /** 保存中フラグ */
  isSaving = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  private readonly basePath = '/price-lists';

  constructor(
    private api: ApiService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadPriceLists();
  }

  /** フォームの初期化 */
  private initForm(): void {
    this.priceListForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(500)]],
      currency: ['JPY', [Validators.required]],
      effectiveStartDate: ['', [Validators.required]],
      effectiveEndDate: ['', [Validators.required]],
      status: ['DRAFT', [Validators.required]]
    });
  }

  /** 価格リスト一覧を読み込む */
  loadPriceLists(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const params: any = {
      page: this.currentPage - 1,
      size: this.pageSize
    };
    if (this.searchKeyword) {
      params.keyword = this.searchKeyword;
    }
    if (this.filterStatus) {
      params.status = this.filterStatus;
    }

    this.api.get<any>(this.basePath, params).subscribe({
      next: (response) => {
        this.priceLists = (response.content || []).map((pl: any) => ({
          ...pl,
          effectiveStartDate: pl.effectiveStartDate || pl.effectiveFrom || pl.effectiveDate,
          effectiveEndDate: pl.effectiveEndDate || pl.effectiveTo || pl.expirationDate
        }));
        this.totalCount = response.totalElements || 0;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('価格リスト取得エラー:', err);
        this.errorMessage = '価格リストの読み込みに失敗しました。';
        this.isLoading = false;
      }
    });
  }

  /** 検索キーワード変更 */
  onSearchChange(keyword: string): void {
    this.searchKeyword = keyword;
    this.currentPage = 1;
    this.loadPriceLists();
  }

  /** ステータスフィルター変更 */
  onStatusFilterChange(): void {
    this.currentPage = 1;
    this.loadPriceLists();
  }

  /** ページ変更 */
  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.pageSize;
    this.loadPriceLists();
  }

  /** 新規作成モーダルを開く */
  openCreateModal(): void {
    // 技術的負債: モーダル状態管理がコンポーネント変数に依存
    this.isEditMode = false;
    this.editingPriceListId = null;
    this.priceListForm.reset({
      name: '',
      description: '',
      currency: 'JPY',
      effectiveStartDate: '',
      effectiveEndDate: '',
      status: 'DRAFT'
    });
    this.showModal = true;
    this.errorMessage = '';
  }

  /** 編集モーダルを開く */
  openEditModal(priceList: PriceList): void {
    // 技術的負債: モーダル状態管理がコンポーネント変数に依存
    this.isEditMode = true;
    this.editingPriceListId = priceList.id;
    this.priceListForm.patchValue({
      name: priceList.name,
      description: priceList.description,
      currency: priceList.currency,
      effectiveStartDate: priceList.effectiveStartDate,
      effectiveEndDate: priceList.effectiveEndDate,
      status: priceList.status
    });
    this.showModal = true;
    this.errorMessage = '';
  }

  /** モーダルを閉じる */
  closeModal(): void {
    this.showModal = false;
    this.editingPriceListId = null;
    this.errorMessage = '';
  }

  /** 保存処理 */
  onSave(): void {
    if (this.priceListForm.invalid) {
      // 技術的負債: 全フィールドにtouchedを設定してバリデーションメッセージを表示
      Object.keys(this.priceListForm.controls).forEach(key => {
        this.priceListForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';
    const fv = this.priceListForm.value;
    const formValue = {
      ...fv,
      effectiveFrom: fv.effectiveStartDate,
      effectiveTo: fv.effectiveEndDate
    };

    if (this.isEditMode && this.editingPriceListId) {
      this.api.put<PriceList>(`${this.basePath}/${this.editingPriceListId}`, formValue).subscribe({
        next: () => {
          this.successMessage = '価格リストを更新しました。';
          this.isSaving = false;
          this.closeModal();
          this.loadPriceLists();
          this.clearSuccessMessage();
        },
        error: (err) => {
          console.error('価格リスト更新エラー:', err);
          this.errorMessage = '価格リストの更新に失敗しました。';
          this.isSaving = false;
        }
      });
    } else {
      this.api.post<PriceList>(this.basePath, formValue).subscribe({
        next: () => {
          this.successMessage = '価格リストを作成しました。';
          this.isSaving = false;
          this.closeModal();
          this.loadPriceLists();
          this.clearSuccessMessage();
        },
        error: (err) => {
          console.error('価格リスト作成エラー:', err);
          this.errorMessage = '価格リストの作成に失敗しました。';
          this.isSaving = false;
        }
      });
    }
  }

  /** 削除確認ダイアログを開く */
  openDeleteConfirm(priceList: PriceList): void {
    this.deletingPriceList = priceList;
    this.showDeleteConfirm = true;
  }

  /** 削除処理 */
  onDeleteConfirmed(confirmed: boolean): void {
    if (!confirmed || !this.deletingPriceList) {
      this.showDeleteConfirm = false;
      this.deletingPriceList = null;
      return;
    }

    this.api.delete<void>(`${this.basePath}/${this.deletingPriceList.id}`).subscribe({
      next: () => {
        this.successMessage = '価格リストを削除しました。';
        this.showDeleteConfirm = false;
        this.deletingPriceList = null;
        this.loadPriceLists();
        this.clearSuccessMessage();
      },
      error: (err) => {
        console.error('価格リスト削除エラー:', err);
        this.errorMessage = '価格リストの削除に失敗しました。';
        this.showDeleteConfirm = false;
        this.deletingPriceList = null;
      }
    });
  }

  /** 行クリック: 価格編集画面へ遷移 */
  onRowClick(priceList: PriceList): void {
    // 技術的負債: Router注入を省略し、window.locationで遷移している箇所がある（ここではRouterを使うべき）
    // 実際にはRouter.navigateを使用すべき
  }

  /** ステータスに応じたCSSクラスを取得 */
  getStatusClass(status: string): string {
    const classMap: { [key: string]: string } = {
      'ACTIVE': 'status-active',
      'DRAFT': 'status-draft',
      'EXPIRED': 'status-expired',
      'INACTIVE': 'status-inactive'
    };
    return classMap[status] || 'status-default';
  }

  /** ステータスの日本語ラベルを取得 */
  getStatusLabel(status: string): string {
    const labelMap: { [key: string]: string } = {
      'ACTIVE': '有効',
      'DRAFT': '下書き',
      'EXPIRED': '期限切れ',
      'INACTIVE': '無効'
    };
    return labelMap[status] || status;
  }

  /** 成功メッセージを一定時間後にクリア */
  private clearSuccessMessage(): void {
    setTimeout(() => {
      this.successMessage = '';
    }, 3000);
  }
}
