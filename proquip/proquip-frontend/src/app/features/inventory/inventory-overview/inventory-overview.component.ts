import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { InventoryItem, Warehouse } from '@shared/models/inventory.model';
import { PageResult, SelectOption } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';
import { getStockStatusLabel, getStockStatusColor, getStockStatusBgColor } from '@shared/utils/status.utils';

/** サマリーカード */
interface SummaryCard {
  label: string;
  value: number;
  unit: string;
  icon: string;
  color: string;
}

/**
 * 在庫一覧コンポーネント
 * サマリーカード、在庫テーブル、フィルタ、エクスポート機能を提供する
 *
 * 技術的負債: 在庫ステータスの色ロジックがコンポーネント内にハードコードされている
 */
@Component({
  selector: 'app-inventory-overview',
  templateUrl: './inventory-overview.component.html',
  styleUrls: ['./inventory-overview.component.scss']
})
export class InventoryOverviewComponent implements OnInit, OnDestroy {

  /** 在庫アイテム一覧 */
  inventoryItems: InventoryItem[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** ソートカラム */
  sortColumn = 'productName';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'asc';

  /** 読み込み中フラグ */
  isLoading = false;

  /** サマリーカード */
  summaryCards: SummaryCard[] = [];

  /** フィルタ: 倉庫 */
  filterWarehouseId: number | null = null;

  /** フィルタ: カテゴリ */
  filterCategory = '';

  /** フィルタ: 在庫ステータス */
  filterStockStatus = '';

  /** 倉庫一覧 */
  warehouses: Warehouse[] = [];

  /** カテゴリ選択肢 */
  categoryOptions: SelectOption[] = [
    { value: '', label: 'すべて' },
    { value: '電子部品', label: '電子部品' },
    { value: '機械部品', label: '機械部品' },
    { value: '消耗品', label: '消耗品' },
    { value: '包装資材', label: '包装資材' },
    { value: '工具', label: '工具' }
  ];

  /** 在庫ステータス選択肢 */
  stockStatusOptions: SelectOption[] = [
    { value: '', label: 'すべて' },
    { value: 'LOW', label: '在庫不足' },
    { value: 'NEAR_REORDER', label: '発注点近傍' },
    { value: 'OK', label: '適正在庫' },
    { value: 'OVERSTOCK', label: '過剰在庫' }
  ];

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'productName', label: '製品名', sortable: true, width: '200px', type: 'text' },
    { key: 'productSku', label: 'SKU', sortable: true, width: '120px', type: 'text' },
    { key: 'warehouseName', label: '倉庫', sortable: true, width: '130px', type: 'text' },
    { key: 'quantity', label: '数量', sortable: true, width: '80px', type: 'number' },
    { key: 'reservedQuantity', label: '引当数', sortable: true, width: '80px', type: 'number' },
    { key: 'availableQuantity', label: '有効在庫', sortable: true, width: '80px', type: 'number' },
    { key: 'reorderPoint', label: '発注点', sortable: true, width: '80px', type: 'number' },
    { key: 'stockStatus', label: 'ステータス', sortable: false, width: '110px', type: 'text' }
  ];

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private inventoryService: InventoryService,
    private warehouseService: WarehouseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
    this.loadInventory();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 倉庫一覧を読み込む */
  private loadWarehouses(): void {
    this.warehouseService.getAllWarehouses().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (warehouses) => {
        this.warehouses = warehouses;
      },
      error: (error) => {
        console.error('倉庫一覧の取得に失敗しました', error);
      }
    });
  }

  /** 在庫一覧を読み込む */
  loadInventory(): void {
    this.isLoading = true;

    this.inventoryService.getInventoryItems(
      this.currentPage - 1,
      this.pageSize,
      this.filterWarehouseId || undefined
    ).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (result: PageResult<InventoryItem>) => {
        let items = result.content || [];
        items = this.applyLocalFilters(items);
        this.inventoryItems = items;
        this.totalCount = result.totalElements;
        this.updateSummaryCards(items);
      },
      error: (error) => {
        console.error('在庫一覧の取得に失敗しました', error);
      }
    });
  }

  /** ローカルフィルタを適用 */
  private applyLocalFilters(items: InventoryItem[]): InventoryItem[] {
    let filtered = [...items];

    if (this.filterStockStatus) {
      filtered = filtered.filter(item => this.getStockStatus(item) === this.filterStockStatus);
    }

    return filtered;
  }

  /** サマリーカードを更新 */
  private updateSummaryCards(items: InventoryItem[]): void {
    const totalItems = items.length;
    const totalValue = items.reduce((sum, item) => sum + (item.quantity * 1000), 0);
    const lowStockCount = items.filter(item => item.availableQuantity < item.reorderPoint).length;
    const overstockCount = items.filter(item =>
      item.maximumStock > 0 && item.quantity > item.maximumStock
    ).length;

    this.summaryCards = [
      {
        label: '在庫品目数',
        value: totalItems,
        unit: '品目',
        icon: 'icon-package',
        color: '#1976d2'
      },
      {
        label: '在庫総額',
        value: totalValue,
        unit: '円',
        icon: 'icon-yen',
        color: '#7b1fa2'
      },
      {
        label: '在庫不足',
        value: lowStockCount,
        unit: '品目',
        icon: 'icon-alert-triangle',
        color: '#e53935'
      },
      {
        label: '過剰在庫',
        value: overstockCount,
        unit: '品目',
        icon: 'icon-trending-up',
        color: '#f57c00'
      }
    ];
  }

  /**
   * 在庫ステータスを判定する
   *
   * 技術的負債: ステータス判定ロジックがコンポーネント内にハードコードされている
   * 共通のサービスやユーティリティに移行すべき
   */
  getStockStatus(item: InventoryItem): string {
    if (item.availableQuantity <= 0 || item.availableQuantity < item.minimumStock) {
      return 'LOW';
    }
    if (item.availableQuantity < item.reorderPoint) {
      return 'NEAR_REORDER';
    }
    if (item.maximumStock > 0 && item.quantity > item.maximumStock) {
      return 'OVERSTOCK';
    }
    return 'OK';
  }

  getStockStatusLabel(item: InventoryItem): string {
    return getStockStatusLabel(this.getStockStatus(item));
  }

  getStockStatusColor(item: InventoryItem): string {
    return getStockStatusColor(this.getStockStatus(item));
  }

  getStockStatusBgColor(item: InventoryItem): string {
    return getStockStatusBgColor(this.getStockStatus(item));
  }

  /** ページ変更 */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.pageSize = event.pageSize;
    this.loadInventory();
  }

  /** ソート変更 */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadInventory();
  }

  /** 行クリック */
  onRowClick(item: InventoryItem): void {
    this.router.navigate(['/inventory', item.id]);
  }

  /** フィルタ変更 */
  onFilterChange(): void {
    this.currentPage = 1;
    this.loadInventory();
  }

  /** フィルタクリア */
  clearFilters(): void {
    this.filterWarehouseId = null;
    this.filterCategory = '';
    this.filterStockStatus = '';
    this.currentPage = 1;
    this.loadInventory();
  }

  /** エクスポート */
  exportInventory(): void {
    const headers = ['製品名', 'SKU', '倉庫', '数量', '引当数', '有効在庫', '発注点', 'ステータス'];
    const rows = this.inventoryItems.map(item => [
      item.productName,
      item.productSku,
      item.warehouseName,
      String(item.quantity),
      String(item.reservedQuantity),
      String(item.availableQuantity),
      String(item.reorderPoint),
      this.getStockStatusLabel(item)
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    const bom = '\ufeff';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `在庫一覧_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  }
}
