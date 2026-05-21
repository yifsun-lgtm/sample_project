import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '@shared/services/api.service';

/**
 * 価格アイテムインターフェース
 */
export interface PriceItem {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  standardUnitPrice: number;
  listPrice: number;
  discountRate: number;
  effectiveStartDate: string;
  effectiveEndDate: string;
  taxRate: number;
  taxIncludedPrice: number;
  isModified: boolean;
  isSelected: boolean;
}

/**
 * 価格リストヘッダー
 */
export interface PriceListHeader {
  id: number;
  name: string;
  currency: string;
  status: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
}

/**
 * 価格編集コンポーネント
 * 価格リスト内の個々の価格をインライン編集・一括更新する
 *
 * 技術的負債 #5: 価格計算ロジック（マークアップ/ディスカウント/税）がコンポーネント内に存在
 * 本来はサービスに分離し、ビジネスロジックをテスト可能にすべき
 *
 * 技術的負債: document.getElementByIdによるDOM直接操作でセルフォーカスを制御
 * Angular的にはViewChildやRendererを使用すべき
 *
 * 技術的負債: 配列の直接変更（ミュータブル）による状態管理
 * イミュータブルな更新パターンに移行すべき
 */
@Component({
  selector: 'app-price-edit',
  templateUrl: './price-edit.component.html',
  styleUrls: ['./price-edit.component.scss']
})
export class PriceEditComponent implements OnInit {

  /** 価格リストID */
  priceListId!: number;

  /** 価格リストヘッダー情報 */
  priceListHeader: PriceListHeader | null = null;

  /** 価格アイテム一覧 */
  priceItems: PriceItem[] = [];

  /** フィルター後の表示用データ */
  filteredItems: PriceItem[] = [];

  /** ローディング状態 */
  isLoading = false;

  /** 保存中フラグ */
  isSaving = false;

  /** 検索キーワード */
  searchKeyword = '';

  /** 変更されたアイテム数 */
  modifiedCount = 0;

  /** 全選択チェック */
  allSelected = false;

  /** 選択されたアイテム数 */
  selectedCount = 0;

  /** 一括更新モーダル表示 */
  showBulkUpdateModal = false;

  /** 一括更新: 変更タイプ */
  bulkUpdateType: 'discount' | 'markup' | 'fixed' = 'discount';

  /** 一括更新: 変更率/値 */
  bulkUpdateValue = 0;

  // --- インライン編集状態 ---
  /** 編集中のセルID（"row-col"形式） */
  editingCellId: string | null = null;

  /** 編集中の元の値 */
  editingOriginalValue: any = null;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 並び替えカラム */
  sortColumn = 'productName';

  /** 並び替え方向 */
  sortDirection: 'asc' | 'desc' = 'asc';

  /** 税率デフォルト値 */
  private readonly DEFAULT_TAX_RATE = 10;

  private readonly basePath = '/price-lists';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ApiService
  ) {}

  ngOnInit(): void {
    this.priceListId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadPriceList();
    this.loadPriceItems();
  }

  /** 価格リストヘッダーを読み込む */
  private loadPriceList(): void {
    this.api.get<PriceListHeader>(`${this.basePath}/${this.priceListId}`).subscribe({
      next: (header) => {
        this.priceListHeader = header;
      },
      error: (err) => {
        console.error('価格リストヘッダー取得エラー:', err);
        this.errorMessage = '価格リストヘッダーの取得に失敗しました。';
      }
    });
  }

  /** 価格アイテム一覧を読み込む */
  private loadPriceItems(): void {
    this.isLoading = true;

    this.api.get<PriceItem[]>(`${this.basePath}/${this.priceListId}/items`).subscribe({
      next: (items) => {
        this.priceItems = items.map(item => ({
          ...item,
          isModified: false,
          isSelected: false,
          taxIncludedPrice: this.calculateTaxIncludedPrice(item.listPrice, item.taxRate)
        }));
        this.applyFilter();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('価格アイテム取得エラー:', err);
        this.isLoading = false;
        this.errorMessage = '価格アイテムの取得に失敗しました。';
      }
    });
  }

  /**
   * 税込価格を計算する
   *
   * 技術的負債 #5: 価格計算ロジックがコンポーネント内に存在
   * PricingServiceやPriceCalculatorに切り出すべき
   */
  private calculateTaxIncludedPrice(price: number, taxRate: number): number {
    return Math.round(price * (1 + taxRate / 100));
  }

  /**
   * リスト価格を標準単価と割引率から計算する
   *
   * 技術的負債 #5: ビジネスロジックがコンポーネント内に存在
   */
  private calculateListPrice(standardPrice: number, discountRate: number): number {
    return Math.round(standardPrice * (1 - discountRate / 100));
  }

  /**
   * 割引率を標準単価とリスト価格から計算する
   *
   * 技術的負債 #5: ビジネスロジックがコンポーネント内に存在
   */
  private calculateDiscountRate(standardPrice: number, listPrice: number): number {
    if (standardPrice === 0) return 0;
    return Math.round((1 - listPrice / standardPrice) * 10000) / 100;
  }

  /**
   * マークアップ価格を計算する
   *
   * 技術的負債 #5: ビジネスロジックがコンポーネント内に存在
   */
  private calculateMarkupPrice(basePrice: number, markupPercent: number): number {
    return Math.round(basePrice * (1 + markupPercent / 100));
  }

  /** フィルターを適用 */
  applyFilter(): void {
    let items = [...this.priceItems];

    // 検索フィルター
    if (this.searchKeyword) {
      const keyword = this.searchKeyword.toLowerCase();
      items = items.filter(item =>
        item.productName.toLowerCase().includes(keyword) ||
        item.sku.toLowerCase().includes(keyword)
      );
    }

    // ソート
    items.sort((a, b) => {
      const valA = (a as any)[this.sortColumn];
      const valB = (b as any)[this.sortColumn];
      if (valA == null) return 1;
      if (valB == null) return -1;
      const cmp = typeof valA === 'string' ? valA.localeCompare(valB) : valA - valB;
      return this.sortDirection === 'desc' ? -cmp : cmp;
    });

    this.filteredItems = items;
    this.updateCounts();
  }

  /** 検索キーワード変更 */
  onSearchChange(keyword: string): void {
    this.searchKeyword = keyword;
    this.applyFilter();
  }

  /** ソート変更 */
  onSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applyFilter();
  }

  /** ソートアイコンを取得 */
  getSortIcon(column: string): string {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  // --- インライン編集 ---

  /**
   * セルを編集モードに切り替え
   *
   * 技術的負債: document.getElementByIdで直接DOM操作
   * Angular的にはViewChildやRenderer2を使用すべき
   */
  startEditing(rowIndex: number, column: string, currentValue: any): void {
    this.editingCellId = `${rowIndex}-${column}`;
    this.editingOriginalValue = currentValue;

    // 技術的負債: document.getElementByIdによるDOM直接操作
    setTimeout(() => {
      const inputElement = document.getElementById(`cell-input-${rowIndex}-${column}`);
      if (inputElement) {
        (inputElement as HTMLInputElement).focus();
        (inputElement as HTMLInputElement).select();
      }
    }, 50);
  }

  /** セルが編集中かどうか */
  isEditing(rowIndex: number, column: string): boolean {
    return this.editingCellId === `${rowIndex}-${column}`;
  }

  /**
   * セル編集を確定する
   *
   * 技術的負債: 配列の直接変更（ミュータブルな状態管理）
   * Object.assignやスプレッド構文でイミュータブルに更新すべき
   */
  confirmEdit(rowIndex: number, column: string, newValue: any): void {
    const item = this.filteredItems[rowIndex];
    if (!item) return;

    const numValue = Number(newValue);
    if (isNaN(numValue) && column !== 'effectiveStartDate' && column !== 'effectiveEndDate') {
      this.cancelEdit();
      return;
    }

    // 技術的負債: 直接変更（ミュータブル）
    switch (column) {
      case 'listPrice':
        // 技術的負債 #5: 価格計算ロジックがコンポーネント内
        item.listPrice = numValue;
        item.discountRate = this.calculateDiscountRate(item.standardUnitPrice, numValue);
        item.taxIncludedPrice = this.calculateTaxIncludedPrice(numValue, item.taxRate);
        break;

      case 'discountRate':
        // 技術的負債 #5: 価格計算ロジックがコンポーネント内
        item.discountRate = numValue;
        item.listPrice = this.calculateListPrice(item.standardUnitPrice, numValue);
        item.taxIncludedPrice = this.calculateTaxIncludedPrice(item.listPrice, item.taxRate);
        break;

      case 'standardUnitPrice':
        item.standardUnitPrice = numValue;
        item.listPrice = this.calculateListPrice(numValue, item.discountRate);
        item.taxIncludedPrice = this.calculateTaxIncludedPrice(item.listPrice, item.taxRate);
        break;

      case 'effectiveStartDate':
        item.effectiveStartDate = newValue;
        break;

      case 'effectiveEndDate':
        item.effectiveEndDate = newValue;
        break;

      default:
        (item as any)[column] = numValue;
    }

    // 変更フラグを立てる（技術的負債: ミュータブルな変更）
    item.isModified = true;
    this.editingCellId = null;
    this.editingOriginalValue = null;
    this.updateCounts();
  }

  /** セル編集をキャンセル */
  cancelEdit(): void {
    this.editingCellId = null;
    this.editingOriginalValue = null;
  }

  /** Enterキーで確定、Escapeキーでキャンセル */
  onCellKeyDown(event: KeyboardEvent, rowIndex: number, column: string): void {
    if (event.key === 'Enter') {
      const input = event.target as HTMLInputElement;
      this.confirmEdit(rowIndex, column, input.value);
    } else if (event.key === 'Escape') {
      this.cancelEdit();
    }
  }

  // --- 選択・一括更新 ---

  /** 全選択/全解除 */
  toggleSelectAll(): void {
    this.allSelected = !this.allSelected;
    // 技術的負債: 直接変更（ミュータブル）
    this.filteredItems.forEach(item => {
      item.isSelected = this.allSelected;
    });
    this.updateCounts();
  }

  /** 個別選択 */
  toggleSelect(item: PriceItem): void {
    // 技術的負債: 直接変更（ミュータブル）
    item.isSelected = !item.isSelected;
    this.allSelected = this.filteredItems.every(i => i.isSelected);
    this.updateCounts();
  }

  /** カウントを更新 */
  private updateCounts(): void {
    this.modifiedCount = this.priceItems.filter(i => i.isModified).length;
    this.selectedCount = this.filteredItems.filter(i => i.isSelected).length;
  }

  /** 一括更新モーダルを開く */
  openBulkUpdateModal(): void {
    if (this.selectedCount === 0) {
      this.errorMessage = '一括更新するアイテムを選択してください。';
      setTimeout(() => { this.errorMessage = ''; }, 3000);
      return;
    }
    this.bulkUpdateType = 'discount';
    this.bulkUpdateValue = 0;
    this.showBulkUpdateModal = true;
  }

  /** 一括更新モーダルを閉じる */
  closeBulkUpdateModal(): void {
    this.showBulkUpdateModal = false;
  }

  /**
   * 一括更新を実行する
   *
   * 技術的負債 #5: 価格計算ロジックがコンポーネント内
   * 技術的負債: 配列の直接変更（ミュータブル）
   */
  applyBulkUpdate(): void {
    const selectedItems = this.filteredItems.filter(i => i.isSelected);

    // 技術的負債: 直接変更（ミュータブル）+ ビジネスロジック埋め込み
    selectedItems.forEach(item => {
      switch (this.bulkUpdateType) {
        case 'discount':
          // 技術的負債 #5: 割引計算ロジックがコンポーネント内
          item.discountRate = this.bulkUpdateValue;
          item.listPrice = this.calculateListPrice(item.standardUnitPrice, item.discountRate);
          item.taxIncludedPrice = this.calculateTaxIncludedPrice(item.listPrice, item.taxRate);
          break;

        case 'markup':
          // 技術的負債 #5: マークアップ計算ロジックがコンポーネント内
          item.listPrice = this.calculateMarkupPrice(item.standardUnitPrice, this.bulkUpdateValue);
          item.discountRate = this.calculateDiscountRate(item.standardUnitPrice, item.listPrice);
          item.taxIncludedPrice = this.calculateTaxIncludedPrice(item.listPrice, item.taxRate);
          break;

        case 'fixed':
          // 技術的負債 #5: 固定価格設定ロジック
          item.listPrice = this.bulkUpdateValue;
          item.discountRate = this.calculateDiscountRate(item.standardUnitPrice, item.listPrice);
          item.taxIncludedPrice = this.calculateTaxIncludedPrice(item.listPrice, item.taxRate);
          break;
      }
      item.isModified = true;
    });

    this.updateCounts();
    this.closeBulkUpdateModal();
    this.successMessage = `${selectedItems.length} 件の価格を更新しました。`;
    setTimeout(() => { this.successMessage = ''; }, 3000);
  }

  /** 変更を保存する */
  saveChanges(): void {
    const modifiedItems = this.priceItems.filter(i => i.isModified);
    if (modifiedItems.length === 0) {
      this.errorMessage = '変更されたアイテムがありません。';
      setTimeout(() => { this.errorMessage = ''; }, 3000);
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';

    // 保存用データを整形
    const saveData = modifiedItems.map(item => ({
      id: item.id,
      productId: item.productId,
      listPrice: item.listPrice,
      discountRate: item.discountRate,
      effectiveStartDate: item.effectiveStartDate,
      effectiveEndDate: item.effectiveEndDate
    }));

    this.api.put<any>(`${this.basePath}/${this.priceListId}/items`, saveData).subscribe({
      next: () => {
        this.successMessage = `${modifiedItems.length} 件の価格を保存しました。`;
        this.isSaving = false;
        // 技術的負債: 直接変更（ミュータブル）
        modifiedItems.forEach(item => { item.isModified = false; });
        this.updateCounts();
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (err) => {
        console.error('価格保存エラー:', err);
        this.errorMessage = '価格の保存に失敗しました。';
        this.isSaving = false;
      }
    });
  }

  /** 変更を破棄する */
  discardChanges(): void {
    this.loadPriceItems();
    this.successMessage = '変更を破棄しました。';
    setTimeout(() => { this.successMessage = ''; }, 3000);
  }

  /** 戻る */
  goBack(): void {
    this.router.navigate(['/pricing']);
  }

  /** 数値フォーマット（通貨表示用） */
  formatCurrency(value: number): string {
    // 技術的負債: パイプを使わずにコンポーネント内でフォーマットしている
    return '¥' + value.toLocaleString('ja-JP');
  }

  /** パーセンテージフォーマット */
  formatPercent(value: number): string {
    return value.toFixed(1) + '%';
  }
}
