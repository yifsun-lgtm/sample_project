import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { PurchaseOrder, PurchaseOrderItem } from '@shared/models/purchase-order.model';
import { ApiService } from '@shared/services/api.service';
import { PageResult, SelectOption } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent } from '@shared/components/data-table/data-table.component';

/** 返品レコード */
interface ReturnRecord {
  id: number;
  returnNumber: string;
  orderId: number;
  orderNumber: string;
  supplierName: string;
  status: string;
  reason: string;
  totalAmount: number;
  createdAt: string;
  items: ReturnItem[];
}

/** 返品明細 */
interface ReturnItem {
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  reason: string;
}

/**
 * 返品管理コンポーネント
 * サプライヤーへの返品の一覧表示と新規返品の作成を行う
 */
@Component({
  selector: 'app-return-management',
  templateUrl: './return-management.component.html',
  styleUrls: ['./return-management.component.scss']
})
export class ReturnManagementComponent implements OnInit, OnDestroy {

  /** 返品一覧 */
  returns: ReturnRecord[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** 読み込み中フラグ */
  isLoading = false;

  /** 作成モード表示 */
  showCreateForm = false;

  /** 作成中フラグ */
  isCreating = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 選択中の発注書（返品元） */
  selectedOrder: PurchaseOrder | null = null;

  /** 発注書検索キーワード */
  orderSearchKeyword = '';

  /** 検索候補 */
  orderCandidates: PurchaseOrder[] = [];

  /** 検索ドロップダウン表示 */
  showOrderDropdown = false;

  /** 返品明細（作成用） */
  newReturnItems: {
    item: PurchaseOrderItem;
    selected: boolean;
    returnQuantity: number;
    reason: string;
  }[] = [];

  /** 返品理由 */
  returnReason = '';

  /** 返品理由選択肢 */
  reasonOptions: SelectOption[] = [
    { value: 'DEFECTIVE', label: '不良品' },
    { value: 'EXCESS_QUANTITY', label: '数量過剰' },
    { value: 'WRONG_ITEM', label: '品違い' },
    { value: 'DAMAGED', label: '破損' },
    { value: 'QUALITY_ISSUE', label: '品質問題' },
    { value: 'OTHER', label: 'その他' }
  ];

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'returnNumber', label: '返品番号', sortable: true, width: '130px', type: 'text' },
    { key: 'orderNumber', label: '発注番号', sortable: true, width: '130px', type: 'text' },
    { key: 'supplierName', label: 'サプライヤー', sortable: true, width: '180px', type: 'text' },
    { key: 'reason', label: '理由', sortable: false, width: '120px', type: 'text' },
    { key: 'totalAmount', label: '金額', sortable: true, width: '120px', type: 'currency' },
    { key: 'status', label: 'ステータス', sortable: true, width: '110px', type: 'status' },
    { key: 'createdAt', label: '作成日', sortable: true, width: '120px', type: 'date' }
  ];

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private purchaseOrderService: PurchaseOrderService,
    private apiService: ApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReturns();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 返品一覧を読み込む */
  loadReturns(): void {
    this.isLoading = true;

    this.apiService.get<PageResult<ReturnRecord>>('/returns', {
      page: this.currentPage - 1,
      size: this.pageSize
    }).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (result) => {
        this.returns = result.content || [];
        this.totalCount = result.totalElements;
      },
      error: (error) => {
        console.error('返品一覧の取得に失敗しました', error);
      }
    });
  }

  /** ページ変更 */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.loadReturns();
  }

  /** 新規返品作成フォームを表示 */
  toggleCreateForm(): void {
    this.showCreateForm = !this.showCreateForm;
    if (this.showCreateForm) {
      this.resetCreateForm();
    }
  }

  /** 作成フォームをリセット */
  private resetCreateForm(): void {
    this.selectedOrder = null;
    this.orderSearchKeyword = '';
    this.newReturnItems = [];
    this.returnReason = '';
    this.errorMessage = '';
  }

  /** 発注書検索 */
  onOrderSearch(keyword: string): void {
    this.orderSearchKeyword = keyword;
    this.showOrderDropdown = true;

    if (!keyword || keyword.length < 2) {
      this.orderCandidates = [];
      return;
    }

    // 入荷済みの発注書を検索
    this.purchaseOrderService.getOrders(0, 20, 'RECEIVED').pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        const lowerKeyword = keyword.toLowerCase();
        this.orderCandidates = (result.content || []).filter(o =>
          o.orderNumber.toLowerCase().includes(lowerKeyword) ||
          o.supplierName.toLowerCase().includes(lowerKeyword)
        );
      }
    });
  }

  /** 発注書を選択 */
  selectOrderForReturn(order: PurchaseOrder): void {
    this.selectedOrder = order;
    this.orderSearchKeyword = order.orderNumber;
    this.showOrderDropdown = false;

    // 返品可能な明細を構築
    this.newReturnItems = order.items.map(item => ({
      item,
      selected: false,
      returnQuantity: 0,
      reason: ''
    }));
  }

  /** ドロップダウンを非表示 */
  hideOrderDropdown(): void {
    setTimeout(() => {
      this.showOrderDropdown = false;
    }, 200);
  }

  /** 返品合計金額を計算 */
  getReturnTotal(): number {
    return this.newReturnItems
      .filter(ri => ri.selected && ri.returnQuantity > 0)
      .reduce((sum, ri) => sum + (ri.returnQuantity * ri.item.unitPrice), 0);
  }

  /** 返品作成バリデーション */
  isReturnValid(): boolean {
    if (!this.selectedOrder) return false;
    if (!this.returnReason) return false;

    const selectedItems = this.newReturnItems.filter(ri => ri.selected);
    if (selectedItems.length === 0) return false;

    return selectedItems.every(ri =>
      ri.returnQuantity > 0 && ri.returnQuantity <= ri.item.receivedQuantity
    );
  }

  /** 返品を作成 */
  submitReturn(): void {
    if (!this.isReturnValid() || !this.selectedOrder) return;

    this.isCreating = true;
    this.errorMessage = '';

    const returnData = {
      orderId: this.selectedOrder.id,
      reason: this.returnReason,
      items: this.newReturnItems
        .filter(ri => ri.selected && ri.returnQuantity > 0)
        .map(ri => ({
          productName: ri.item.productName,
          productSku: ri.item.productSku,
          quantity: ri.returnQuantity,
          unitPrice: ri.item.unitPrice,
          reason: ri.reason || this.returnReason
        }))
    };

    this.apiService.post<ReturnRecord>('/returns', returnData).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isCreating = false)
    ).subscribe({
      next: () => {
        this.successMessage = '返品を作成しました';
        this.showCreateForm = false;
        this.loadReturns();
      },
      error: (error) => {
        console.error('返品の作成に失敗しました', error);
        this.errorMessage = '返品の作成に失敗しました';
      }
    });
  }

  /** ステータスラベルを取得 */
  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'DRAFT': '下書き',
      'PENDING': '処理待ち',
      'PENDING_APPROVAL': '承認待ち',
      'APPROVED': '承認済み',
      'SHIPPED': '返送済み',
      'RECEIVED_BY_SUPPLIER': 'サプライヤー受領',
      'CREDIT_ISSUED': 'クレジット発行済み',
      'CLOSED': '完了',
      'COMPLETED': '完了',
      'CANCELLED': 'キャンセル'
    };
    return labels[status] || status;
  }

  /** 返品理由ラベルを取得 */
  getReasonLabel(reason: string): string {
    const labels: { [key: string]: string } = {
      'DEFECTIVE': '不良品',
      'EXCESS_QUANTITY': '数量過剰',
      'WRONG_ITEM': '品違い',
      'DAMAGED': '破損',
      'QUALITY_ISSUE': '品質問題',
      'EXPIRED': '期限切れ',
      'NOT_AS_DESCRIBED': '説明不一致',
      'OTHER': 'その他'
    };
    return labels[reason] || reason;
  }
}
