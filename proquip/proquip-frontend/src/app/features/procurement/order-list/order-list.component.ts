import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { SupplierService } from '@shared/services/supplier.service';
import { PurchaseOrder } from '@shared/models/purchase-order.model';
import { Supplier } from '@shared/models/supplier.model';
import { PageResult, SelectOption } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';
import { getOrderStatusLabel } from '@shared/utils/status.utils';

/**
 * 発注書一覧コンポーネント
 *
 * 技術的負債: 複雑なフィルタロジックがコンポーネント内にあり、
 * URLクエリパラメータとの手動同期処理が含まれている
 */
@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.scss']
})
export class OrderListComponent implements OnInit, OnDestroy {

  /** テーブルデータ */
  orders: PurchaseOrder[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ番号 */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** ソートカラム */
  sortColumn = 'orderDate';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'desc';

  /** 読み込み中フラグ */
  isLoading = false;

  /** CSV出力中フラグ */
  isExporting = false;

  /** フィルタ: ステータス */
  filterStatus = '';

  /** フィルタ: サプライヤー */
  filterSupplierId: number | null = null;

  /** フィルタ: 開始日 */
  filterDateFrom = '';

  /** フィルタ: 終了日 */
  filterDateTo = '';

  /** フィルタ: 金額下限 */
  filterAmountMin: number | null = null;

  /** フィルタ: 金額上限 */
  filterAmountMax: number | null = null;

  /** サプライヤー一覧（フィルタ用） */
  suppliers: Supplier[] = [];

  /** ステータス選択肢 */
  statusOptions: SelectOption[] = [
    { value: '', label: 'すべて' },
    ...['DRAFT', 'SUBMITTED', 'PENDING_APPROVAL', 'APPROVED', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED']
      .map(s => ({ value: s, label: getOrderStatusLabel(s) }))
  ];

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'orderNumber', label: '発注番号', sortable: true, width: '140px', type: 'text' },
    { key: 'supplierName', label: 'サプライヤー', sortable: true, width: '180px', type: 'text' },
    { key: 'status', label: 'ステータス', sortable: true, width: '120px', type: 'status' },
    { key: 'orderDate', label: '発注日', sortable: true, width: '120px', type: 'date' },
    { key: 'expectedDeliveryDate', label: '納品予定日', sortable: true, width: '120px', type: 'date' },
    { key: 'totalAmount', label: '合計金額', sortable: true, width: '140px', type: 'currency' }
  ];

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private purchaseOrderService: PurchaseOrderService,
    private supplierService: SupplierService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadSuppliersForFilter();
    this.restoreFiltersFromUrl();
    this.loadOrders();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * URLクエリパラメータからフィルタを復元
   *
   * 技術的負債: URLクエリパラメータとの手動同期
   * Angular Routerのクエリパラメータ機能を適切に使うか、
   * 専用のフィルタ管理サービスを導入すべき
   */
  private restoreFiltersFromUrl(): void {
    const queryParams = this.route.snapshot.queryParams;

    if (queryParams['status']) {
      this.filterStatus = queryParams['status'];
    }
    if (queryParams['supplierId']) {
      this.filterSupplierId = Number(queryParams['supplierId']);
    }
    if (queryParams['dateFrom']) {
      this.filterDateFrom = queryParams['dateFrom'];
    }
    if (queryParams['dateTo']) {
      this.filterDateTo = queryParams['dateTo'];
    }
    if (queryParams['amountMin']) {
      this.filterAmountMin = Number(queryParams['amountMin']);
    }
    if (queryParams['amountMax']) {
      this.filterAmountMax = Number(queryParams['amountMax']);
    }
    if (queryParams['page']) {
      this.currentPage = Number(queryParams['page']);
    }
    if (queryParams['sort']) {
      this.sortColumn = queryParams['sort'];
    }
    if (queryParams['dir']) {
      this.sortDirection = queryParams['dir'] as 'asc' | 'desc';
    }
  }

  /**
   * フィルタ状態をURLクエリパラメータに同期
   *
   * 技術的負債: 手動でのURL同期ロジック
   * null/空文字のチェックが冗長で、共通化すべき
   */
  private syncFiltersToUrl(): void {
    const queryParams: any = {};

    if (this.filterStatus) queryParams['status'] = this.filterStatus;
    if (this.filterSupplierId) queryParams['supplierId'] = this.filterSupplierId;
    if (this.filterDateFrom) queryParams['dateFrom'] = this.filterDateFrom;
    if (this.filterDateTo) queryParams['dateTo'] = this.filterDateTo;
    if (this.filterAmountMin != null) queryParams['amountMin'] = this.filterAmountMin;
    if (this.filterAmountMax != null) queryParams['amountMax'] = this.filterAmountMax;
    if (this.currentPage > 1) queryParams['page'] = this.currentPage;
    if (this.sortColumn !== 'orderDate') queryParams['sort'] = this.sortColumn;
    if (this.sortDirection !== 'desc') queryParams['dir'] = this.sortDirection;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  /** フィルタ用サプライヤー一覧を読み込む */
  private loadSuppliersForFilter(): void {
    this.supplierService.getSuppliers(0, 100).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        this.suppliers = result.content || [];
      },
      error: (error) => {
        console.error('サプライヤー一覧の取得に失敗しました', error);
      }
    });
  }

  /** 発注書一覧を読み込む */
  loadOrders(): void {
    this.isLoading = true;

    this.purchaseOrderService.getOrders(
      this.currentPage - 1,
      this.pageSize,
      this.filterStatus || undefined
    ).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (result: PageResult<PurchaseOrder>) => {
        // 技術的負債: サーバーサイドのフィルタが不十分なため、クライアント側で追加フィルタ
        this.orders = this.applyClientSideFilters(result.content);
        this.totalCount = result.totalElements;
        this.syncFiltersToUrl();
      },
      error: (error) => {
        console.error('発注書一覧の取得に失敗しました', error);
      }
    });
  }

  /**
   * クライアントサイドの追加フィルタ
   *
   * 技術的負債: サプライヤー、日付範囲、金額範囲のフィルタが
   * サーバーサイドAPIに実装されていないため、クライアントで処理している
   * データ量が増えた場合にパフォーマンス問題が発生する
   */
  private applyClientSideFilters(data: PurchaseOrder[]): PurchaseOrder[] {
    let filtered = [...data];

    // サプライヤーフィルタ
    if (this.filterSupplierId) {
      filtered = filtered.filter(o => o.supplierId === this.filterSupplierId);
    }

    // 日付範囲フィルタ
    if (this.filterDateFrom) {
      const fromDate = new Date(this.filterDateFrom);
      filtered = filtered.filter(o => new Date(o.orderDate) >= fromDate);
    }
    if (this.filterDateTo) {
      const toDate = new Date(this.filterDateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(o => new Date(o.orderDate) <= toDate);
    }

    // 金額範囲フィルタ
    if (this.filterAmountMin != null) {
      filtered = filtered.filter(o => o.totalAmount >= this.filterAmountMin!);
    }
    if (this.filterAmountMax != null) {
      filtered = filtered.filter(o => o.totalAmount <= this.filterAmountMax!);
    }

    return filtered;
  }

  /** ページ変更ハンドラ */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.pageSize = event.pageSize;
    this.loadOrders();
  }

  /** ソート変更ハンドラ */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadOrders();
  }

  /** 行クリック → 詳細画面へ遷移 */
  onRowClick(order: PurchaseOrder): void {
    this.router.navigate(['/procurement/orders', order.id]);
  }

  /** フィルタ変更ハンドラ */
  onFilterChange(): void {
    this.currentPage = 1;
    this.loadOrders();
  }

  /** フィルタクリア */
  clearFilters(): void {
    this.filterStatus = '';
    this.filterSupplierId = null;
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.filterAmountMin = null;
    this.filterAmountMax = null;
    this.currentPage = 1;
    this.loadOrders();
  }

  /** 新規作成画面へ遷移 */
  navigateToCreate(): void {
    this.router.navigate(['/procurement/orders/new']);
  }

  /**
   * CSVエクスポート
   * 技術的負債: CSVの生成がクライアントサイドで行われている
   * バックエンドAPIにCSVエクスポートエンドポイントを用意すべき
   */
  exportCsv(): void {
    this.isExporting = true;

    // 技術的負債: 全データ取得のため大量リクエストが発生する可能性がある
    this.purchaseOrderService.getOrders(0, 10000, this.filterStatus || undefined).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isExporting = false)
    ).subscribe({
      next: (result) => {
        const data = this.applyClientSideFilters(result.content);
        this.generateCsv(data);
      },
      error: (error) => {
        console.error('CSVエクスポートに失敗しました', error);
      }
    });
  }

  /** CSV生成・ダウンロード */
  private generateCsv(data: PurchaseOrder[]): void {
    const headers = ['発注番号', 'サプライヤー', 'ステータス', '発注日', '納品予定日', '合計金額'];
    const rows = data.map(order => [
      order.orderNumber,
      order.supplierName,
      order.status,
      order.orderDate,
      order.expectedDeliveryDate || '',
      String(order.totalAmount)
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    // BOM付きUTF-8でダウンロード
    const bom = '\ufeff';
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `発注書一覧_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  /** フィルタが適用中かどうか */
  hasActiveFilters(): boolean {
    return !!(
      this.filterStatus ||
      this.filterSupplierId ||
      this.filterDateFrom ||
      this.filterDateTo ||
      this.filterAmountMin != null ||
      this.filterAmountMax != null
    );
  }

  /** 納品予定日が過ぎているか判定 */
  isOverdue(order: PurchaseOrder): boolean {
    if (!order.expectedDeliveryDate) return false;
    if (order.status === 'RECEIVED' || order.status === 'CANCELLED') return false;
    return new Date(order.expectedDeliveryDate) < new Date();
  }

  /** ステータスラベルを取得 */
  getStatusLabel(status: string): string {
    return getOrderStatusLabel(status);
  }
}
