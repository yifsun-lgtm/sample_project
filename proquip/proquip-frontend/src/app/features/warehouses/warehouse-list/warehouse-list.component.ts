import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { WarehouseService } from '@shared/services/warehouse.service';
import { Warehouse } from '@shared/models/inventory.model';
import { PageResult } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';

/**
 * 倉庫一覧コンポーネント
 * 倉庫の一覧表示とキャパシティバーの可視化を行う
 */
@Component({
  selector: 'app-warehouse-list',
  templateUrl: './warehouse-list.component.html',
  styleUrls: ['./warehouse-list.component.scss']
})
export class WarehouseListComponent implements OnInit, OnDestroy {

  /** 倉庫一覧 */
  warehouses: Warehouse[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** ソートカラム */
  sortColumn = 'name';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'asc';

  /** 読み込み中フラグ */
  isLoading = false;

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'code', label: 'コード', sortable: true, width: '100px', type: 'text' },
    { key: 'name', label: '倉庫名', sortable: true, width: '180px', type: 'text' },
    { key: 'address', label: '住所', sortable: false, width: '200px', type: 'text' },
    { key: 'capacity', label: 'キャパシティ', sortable: true, width: '120px', type: 'number' },
    { key: 'utilization', label: '使用率', sortable: false, width: '160px', type: 'text' },
    { key: 'zones', label: 'ゾーン数', sortable: false, width: '80px', type: 'number' },
    { key: 'status', label: 'ステータス', sortable: true, width: '100px', type: 'status' }
  ];

  /** 作成モーダル */
  showCreateModal = false;
  isSaving = false;
  createForm = { code: '', name: '', address: '', capacity: 0 };
  successMessage = '';

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private warehouseService: WarehouseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 倉庫一覧を読み込む */
  loadWarehouses(): void {
    this.isLoading = true;

    this.warehouseService.getWarehouses(this.currentPage - 1, this.pageSize).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (result: PageResult<Warehouse>) => {
        this.warehouses = (result.content || []).map((w: any) => ({
          ...w,
          utilization: w.utilizationPercentage != null ? w.utilizationPercentage + '%' : '',
          zones: w.zoneCount ?? w.zones ?? '',
          status: w.status || (w.active ? 'ACTIVE' : 'INACTIVE')
        }));
        this.totalCount = result.totalElements;
      },
      error: (error) => {
        console.error('倉庫一覧の取得に失敗しました', error);
      }
    });
  }

  /** ページ変更 */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.loadWarehouses();
  }

  /** ソート変更 */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadWarehouses();
  }

  /** 行クリック → 詳細画面へ遷移 */
  onRowClick(warehouse: Warehouse): void {
    this.router.navigate(['/warehouses', warehouse.id]);
  }

  /** 使用率を計算 */
  getUtilizationPercent(warehouse: Warehouse): number {
    if (warehouse.capacity === 0) return 0;
    return Math.round((warehouse.currentOccupancy / warehouse.capacity) * 100);
  }

  /** 使用率の色を取得 */
  getUtilizationColor(warehouse: Warehouse): string {
    const percent = this.getUtilizationPercent(warehouse);
    if (percent >= 90) return '#e53935';
    if (percent >= 75) return '#f57c00';
    if (percent >= 50) return '#fdd835';
    return '#43a047';
  }

  /** ステータスラベルを取得 */
  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'ACTIVE': '稼働中',
      'MAINTENANCE': 'メンテナンス中',
      'CLOSED': '閉鎖',
      'PLANNED': '計画中'
    };
    return labels[status] || status;
  }

  /** ステータス色を取得 */
  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'ACTIVE': '#43a047',
      'MAINTENANCE': '#f57c00',
      'CLOSED': '#9e9e9e',
      'PLANNED': '#1976d2'
    };
    return colors[status] || '#9e9e9e';
  }

  openCreateModal(): void {
    this.createForm = { code: '', name: '', address: '', capacity: 0 };
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  submitCreate(): void {
    this.isSaving = true;
    this.warehouseService.createWarehouse(this.createForm).subscribe({
      next: () => {
        this.isSaving = false;
        this.showCreateModal = false;
        this.loadWarehouses();
        this.successMessage = '倉庫を作成しました。';
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (error) => {
        console.error('倉庫作成エラー:', error);
        this.isSaving = false;
      }
    });
  }
}
