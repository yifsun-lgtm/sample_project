import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { ApiService } from '@shared/services/api.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { Warehouse } from '@shared/models/inventory.model';
import { PageResult, SelectOption } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';

/** 在庫取引ログ */
interface InventoryTransaction {
  id: number;
  transactionDate: string;
  productName: string;
  productSku: string;
  warehouseName: string;
  transactionType: string;
  quantity: number;
  referenceNumber: string;
  performedBy: string;
  notes: string;
}

/**
 * 在庫取引履歴コンポーネント
 * 在庫の入出庫や調整などの取引ログを表示する
 */
@Component({
  selector: 'app-transaction-history',
  templateUrl: './transaction-history.component.html',
  styleUrls: ['./transaction-history.component.scss']
})
export class TransactionHistoryComponent implements OnInit, OnDestroy {

  /** 取引履歴 */
  transactions: InventoryTransaction[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** ソートカラム */
  sortColumn = 'transactionDate';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'desc';

  /** 読み込み中フラグ */
  isLoading = false;

  /** フィルタ: 製品名 */
  filterProduct = '';

  /** フィルタ: 倉庫 */
  filterWarehouseId: number | null = null;

  /** フィルタ: 取引種別 */
  filterTransactionType = '';

  /** フィルタ: 開始日 */
  filterDateFrom = '';

  /** フィルタ: 終了日 */
  filterDateTo = '';

  /** 倉庫一覧 */
  warehouses: Warehouse[] = [];

  /** 取引種別選択肢 */
  transactionTypeOptions: SelectOption[] = [
    { value: '', label: 'すべて' },
    { value: 'RECEIPT', label: '入荷' },
    { value: 'ISSUE', label: '出荷' },
    { value: 'TRANSFER_IN', label: '移動入庫' },
    { value: 'TRANSFER_OUT', label: '移動出庫' },
    { value: 'ADJUSTMENT', label: '調整' },
    { value: 'RETURN', label: '返品' },
    { value: 'COUNT', label: '棚卸し' }
  ];

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'transactionDate', label: '日時', sortable: true, width: '140px', type: 'date' },
    { key: 'productName', label: '製品', sortable: true, width: '180px', type: 'text' },
    { key: 'warehouseName', label: '倉庫', sortable: true, width: '120px', type: 'text' },
    { key: 'transactionType', label: '種別', sortable: true, width: '100px', type: 'text' },
    { key: 'quantity', label: '数量', sortable: true, width: '80px', type: 'number' },
    { key: 'referenceNumber', label: '参照', sortable: false, width: '130px', type: 'text' },
    { key: 'performedBy', label: '担当者', sortable: true, width: '120px', type: 'text' }
  ];

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private apiService: ApiService,
    private warehouseService: WarehouseService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
    this.loadTransactions();
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
      }
    });
  }

  /** 取引履歴を読み込む */
  loadTransactions(): void {
    this.isLoading = true;

    const params: any = {
      page: this.currentPage - 1,
      size: this.pageSize
    };

    if (this.filterProduct) params.product = this.filterProduct;
    if (this.filterWarehouseId) params.warehouseId = this.filterWarehouseId;
    if (this.filterTransactionType) params.type = this.filterTransactionType;
    if (this.filterDateFrom) params.dateFrom = this.filterDateFrom;
    if (this.filterDateTo) params.dateTo = this.filterDateTo;

    this.apiService.get<PageResult<InventoryTransaction>>('/inventory/transactions', params).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (result) => {
        this.transactions = result.content || [];
        this.totalCount = result.totalElements;
      },
      error: (error) => {
        console.error('取引履歴の取得に失敗しました', error);
      }
    });
  }

  /** ページ変更 */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.loadTransactions();
  }

  /** ソート変更 */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadTransactions();
  }

  /** フィルタ変更 */
  onFilterChange(): void {
    this.currentPage = 1;
    this.loadTransactions();
  }

  /** フィルタクリア */
  clearFilters(): void {
    this.filterProduct = '';
    this.filterWarehouseId = null;
    this.filterTransactionType = '';
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.currentPage = 1;
    this.loadTransactions();
  }

  /** 取引種別ラベルを取得 */
  getTransactionTypeLabel(type: string): string {
    const found = this.transactionTypeOptions.find(o => o.value === type);
    return found ? found.label : type;
  }

  /** 取引種別色を取得 */
  getTransactionTypeColor(type: string): string {
    const colors: { [key: string]: string } = {
      'RECEIPT': '#43a047',
      'ISSUE': '#e53935',
      'TRANSFER_IN': '#1976d2',
      'TRANSFER_OUT': '#f57c00',
      'ADJUSTMENT': '#7b1fa2',
      'RETURN': '#ff9800',
      'COUNT': '#78909c'
    };
    return colors[type] || '#9e9e9e';
  }
}
