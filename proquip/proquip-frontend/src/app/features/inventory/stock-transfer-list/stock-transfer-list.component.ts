import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { StockTransfer, Warehouse } from '@shared/models/inventory.model';
import { PageResult, SelectOption } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';

/**
 * 在庫移動一覧コンポーネント
 * 在庫移動の一覧表示とフィルタリングを行う
 */
@Component({
  selector: 'app-stock-transfer-list',
  templateUrl: './stock-transfer-list.component.html',
  styleUrls: ['./stock-transfer-list.component.scss']
})
export class StockTransferListComponent implements OnInit, OnDestroy {

  /** 在庫移動一覧 */
  transfers: StockTransfer[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** ソートカラム */
  sortColumn = 'requestedDate';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'desc';

  /** 読み込み中フラグ */
  isLoading = false;

  /** フィルタ: ステータス */
  filterStatus = '';

  /** フィルタ: 移動元倉庫 */
  filterSourceWarehouse: number | null = null;

  /** フィルタ: 移動先倉庫 */
  filterDestWarehouse: number | null = null;

  /** フィルタ: 開始日 */
  filterDateFrom = '';

  /** フィルタ: 終了日 */
  filterDateTo = '';

  /** 倉庫一覧 */
  warehouses: Warehouse[] = [];

  /** ステータス選択肢 */
  statusOptions: SelectOption[] = [
    { value: '', label: 'すべて' },
    { value: 'PENDING', label: '処理待ち' },
    { value: 'IN_TRANSIT', label: '移動中' },
    { value: 'COMPLETED', label: '完了' },
    { value: 'CANCELLED', label: 'キャンセル' }
  ];

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'transferNumber', label: '移動番号', sortable: true, width: '130px', type: 'text' },
    { key: 'productName', label: '製品名', sortable: true, width: '180px', type: 'text' },
    { key: 'sourceWarehouseName', label: '移動元', sortable: true, width: '130px', type: 'text' },
    { key: 'destinationWarehouseName', label: '移動先', sortable: true, width: '130px', type: 'text' },
    { key: 'quantity', label: '数量', sortable: true, width: '80px', type: 'number' },
    { key: 'status', label: 'ステータス', sortable: true, width: '110px', type: 'status' },
    { key: 'requestedDate', label: '依頼日', sortable: true, width: '120px', type: 'date' }
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
    this.loadTransfers();
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

  /** 在庫移動一覧を読み込む */
  loadTransfers(): void {
    this.isLoading = true;

    this.inventoryService.getTransfers(this.currentPage - 1, this.pageSize).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (result: PageResult<StockTransfer>) => {
        this.transfers = this.applyLocalFilters(result.content || []);
        this.totalCount = result.totalElements;
      },
      error: (error) => {
        console.error('在庫移動一覧の取得に失敗しました', error);
      }
    });
  }

  /** ローカルフィルタ適用 */
  private applyLocalFilters(data: StockTransfer[]): StockTransfer[] {
    let filtered = [...data];

    if (this.filterStatus) {
      filtered = filtered.filter(t => t.status === this.filterStatus);
    }
    if (this.filterSourceWarehouse) {
      filtered = filtered.filter(t => t.sourceWarehouseId === this.filterSourceWarehouse);
    }
    if (this.filterDestWarehouse) {
      filtered = filtered.filter(t => t.destinationWarehouseId === this.filterDestWarehouse);
    }
    if (this.filterDateFrom) {
      const from = new Date(this.filterDateFrom);
      filtered = filtered.filter(t => new Date(t.requestedDate) >= from);
    }
    if (this.filterDateTo) {
      const to = new Date(this.filterDateTo);
      to.setHours(23, 59, 59, 999);
      filtered = filtered.filter(t => new Date(t.requestedDate) <= to);
    }

    return filtered;
  }

  /** ページ変更 */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.loadTransfers();
  }

  /** ソート変更 */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadTransfers();
  }

  /** フィルタ変更 */
  onFilterChange(): void {
    this.currentPage = 1;
    this.loadTransfers();
  }

  /** フィルタクリア */
  clearFilters(): void {
    this.filterStatus = '';
    this.filterSourceWarehouse = null;
    this.filterDestWarehouse = null;
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.currentPage = 1;
    this.loadTransfers();
  }

  /** 新規作成画面へ遷移 */
  navigateToCreate(): void {
    this.router.navigate(['/inventory/transfer/new']);
  }

  /** ステータスラベルを取得 */
  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'PENDING': '処理待ち',
      'IN_TRANSIT': '移動中',
      'COMPLETED': '完了',
      'CANCELLED': 'キャンセル'
    };
    return labels[status] || status;
  }
}
