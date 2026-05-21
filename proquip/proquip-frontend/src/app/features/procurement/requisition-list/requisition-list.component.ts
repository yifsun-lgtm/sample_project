import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { RequisitionService } from '@shared/services/requisition.service';
import { Requisition } from '@shared/models/purchase-order.model';
import { PageResult, SelectOption } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';
import { getOrderStatusLabel } from '@shared/utils/status.utils';

/**
 * 購買依頼一覧コンポーネント
 * 依頼番号、依頼者、部門、金額、ステータス、作成日を表示し、
 * ステータス・部門・日付範囲でフィルタリング可能
 */
@Component({
  selector: 'app-requisition-list',
  templateUrl: './requisition-list.component.html',
  styleUrls: ['./requisition-list.component.scss']
})
export class RequisitionListComponent implements OnInit, OnDestroy {

  /** テーブルデータ */
  requisitions: Requisition[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ番号 */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** ソートカラム */
  sortColumn = 'createdAt';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'desc';

  /** 読み込み中フラグ */
  isLoading = false;

  /** フィルタ: ステータス */
  filterStatus = '';

  /** フィルタ: 部門 */
  filterDepartment = '';

  /** フィルタ: 開始日 */
  filterDateFrom = '';

  /** フィルタ: 終了日 */
  filterDateTo = '';

  /** ステータス選択肢 */
  statusOptions: SelectOption[] = [
    { value: '', label: 'すべて' },
    ...['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED']
      .map(s => ({ value: s, label: getOrderStatusLabel(s) }))
  ];

  /** 部門選択肢 */
  departmentOptions: SelectOption[] = [
    { value: '', label: 'すべて' },
    { value: '総務部', label: '総務部' },
    { value: '営業部', label: '営業部' },
    { value: '技術部', label: '技術部' },
    { value: '製造部', label: '製造部' },
    { value: '品質管理部', label: '品質管理部' },
    { value: '物流部', label: '物流部' },
    { value: '経理部', label: '経理部' }
  ];

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'requisitionNumber', label: '依頼番号', sortable: true, width: '140px', type: 'text' },
    { key: 'requestedBy', label: '依頼者', sortable: true, width: '120px', type: 'text' },
    { key: 'department', label: '部門', sortable: true, width: '120px', type: 'text' },
    { key: 'totalAmount', label: '金額', sortable: true, width: '130px', type: 'currency' },
    { key: 'status', label: 'ステータス', sortable: true, width: '110px', type: 'status' },
    { key: 'createdAt', label: '作成日', sortable: true, width: '120px', type: 'date' }
  ];

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private requisitionService: RequisitionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRequisitions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 購買依頼一覧を読み込む */
  loadRequisitions(): void {
    this.isLoading = true;

    this.requisitionService.getRequisitions(
      this.currentPage - 1,
      this.pageSize,
      this.filterStatus || undefined
    ).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (result: PageResult<Requisition>) => {
        this.requisitions = this.applyLocalFilters(result.content);
        this.totalCount = result.totalElements;
      },
      error: (error) => {
        console.error('購買依頼一覧の取得に失敗しました', error);
      }
    });
  }

  /**
   * ローカルフィルタを適用する
   * 技術的負債: 部門フィルタと日付フィルタはクライアントサイドで実行している
   * バックエンドAPIのクエリパラメータに追加すべき
   */
  private applyLocalFilters(data: Requisition[]): Requisition[] {
    let filtered = [...data];

    if (this.filterDepartment) {
      filtered = filtered.filter(r => r.department === this.filterDepartment);
    }

    if (this.filterDateFrom) {
      const fromDate = new Date(this.filterDateFrom);
      filtered = filtered.filter(r => new Date(r.createdAt) >= fromDate);
    }

    if (this.filterDateTo) {
      const toDate = new Date(this.filterDateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(r => new Date(r.createdAt) <= toDate);
    }

    return filtered;
  }

  /** ページ変更ハンドラ */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.pageSize = event.pageSize;
    this.loadRequisitions();
  }

  /** ソート変更ハンドラ */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadRequisitions();
  }

  /** 行クリック → 詳細画面へ遷移 */
  onRowClick(requisition: Requisition): void {
    this.router.navigate(['/procurement/requisitions', requisition.id]);
  }

  /** フィルタ変更ハンドラ */
  onFilterChange(): void {
    this.currentPage = 1;
    this.loadRequisitions();
  }

  /** フィルタクリア */
  clearFilters(): void {
    this.filterStatus = '';
    this.filterDepartment = '';
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.currentPage = 1;
    this.loadRequisitions();
  }

  /** 新規作成画面へ遷移 */
  navigateToCreate(): void {
    this.router.navigate(['/procurement/requisitions/new']);
  }

  /** クイックアクション: 申請提出 */
  submitRequisition(requisition: Requisition, event: Event): void {
    event.stopPropagation();
    if (requisition.status !== 'DRAFT') return;

    this.requisitionService.submitForApproval(requisition.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadRequisitions();
      },
      error: (error) => {
        console.error('申請の提出に失敗しました', error);
      }
    });
  }

  /** クイックアクション: キャンセル */
  cancelRequisition(requisition: Requisition, event: Event): void {
    event.stopPropagation();
    if (requisition.status !== 'DRAFT' && requisition.status !== 'SUBMITTED') return;

    this.requisitionService.deleteRequisition(requisition.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadRequisitions();
      },
      error: (error) => {
        console.error('キャンセルに失敗しました', error);
      }
    });
  }

  /**
   * 合計金額を計算する
   * 技術的負債: APIレスポンスに合計金額が含まれていないため、クライアント側で計算
   */
  calculateTotalAmount(requisition: Requisition): number {
    if (!requisition.items || requisition.items.length === 0) return 0;
    return requisition.items.reduce((sum, item) => {
      return sum + (item.quantity * item.estimatedUnitPrice);
    }, 0);
  }
}
