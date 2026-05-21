import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { PurchaseOrder, PurchaseOrderItem } from '@shared/models/purchase-order.model';

/** 入荷明細行 */
interface ReceiptLine {
  itemId: number;
  productName: string;
  productSku: string;
  orderedQuantity: number;
  previouslyReceived: number;
  remainingQuantity: number;
  receivedQuantity: number;
  qualityNotes: string;
  isAccepted: boolean;
}

/**
 * 入荷検収コンポーネント
 * 発注書に対する入荷処理を行う
 *
 * 技術的負債: ローディング状態の管理がない、送信ボタンがリクエスト中に無効化されない
 * ↑ 意図的にこの負債を実装に反映している
 */
@Component({
  selector: 'app-goods-receipt',
  templateUrl: './goods-receipt.component.html',
  styleUrls: ['./goods-receipt.component.scss']
})
export class GoodsReceiptComponent implements OnInit, OnDestroy {

  /** 選択中の発注書 */
  selectedOrder: PurchaseOrder | null = null;

  /** 発注書一覧（検索用） */
  availableOrders: PurchaseOrder[] = [];

  /** 入荷明細行 */
  receiptLines: ReceiptLine[] = [];

  /** 発注書番号検索 */
  orderSearchKeyword = '';

  /** 検索結果表示フラグ */
  showOrderResults = false;

  /** フィルタ済み発注書 */
  filteredOrders: PurchaseOrder[] = [];

  // 技術的負債: isLoadingやisSubmittingのフラグがあるが、
  // テンプレートで一部使用されていない（意図的な負債）
  /** 読み込み中フラグ */
  isLoading = false;

  /** 送信中フラグ — 技術的負債: テンプレートのボタンで参照されていない */
  isSubmitting = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 入荷日 */
  receiptDate: string = '';

  /** 全体備考 */
  receiptNotes = '';

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private purchaseOrderService: PurchaseOrderService
  ) {}

  ngOnInit(): void {
    // 本日の日付を初期値に
    const today = new Date();
    this.receiptDate = today.toISOString().split('T')[0];

    // URLパラメータから発注書IDを取得
    const orderId = this.route.snapshot.queryParams['orderId'];
    if (orderId) {
      this.loadOrder(Number(orderId));
    }

    this.loadAvailableOrders();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 入荷可能な発注書一覧を読み込む */
  private loadAvailableOrders(): void {
    this.purchaseOrderService.getOrders(0, 100, 'ORDERED').pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        // 「発注済み」と「一部入荷」の発注書を取得
        this.availableOrders = result.content || [];

        // 一部入荷の発注書も追加取得
        this.purchaseOrderService.getOrders(0, 100, 'PARTIALLY_RECEIVED').pipe(
          takeUntil(this.destroy$)
        ).subscribe({
          next: (partialResult) => {
            this.availableOrders = [
              ...this.availableOrders,
              ...(partialResult.content || [])
            ];
            this.filteredOrders = [...this.availableOrders];
          }
        });
      },
      error: (error) => {
        console.error('発注書一覧の取得に失敗しました', error);
      }
    });
  }

  /** 発注書を読み込む */
  private loadOrder(id: number): void {
    this.isLoading = true;
    this.purchaseOrderService.getOrder(id).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (order) => {
        this.selectOrder(order);
      },
      error: (error) => {
        console.error('発注書の取得に失敗しました', error);
        this.errorMessage = '発注書の取得に失敗しました';
      }
    });
  }

  /** 発注書を検索 */
  onOrderSearch(keyword: string): void {
    this.orderSearchKeyword = keyword;
    this.showOrderResults = true;

    if (!keyword) {
      this.filteredOrders = [...this.availableOrders];
    } else {
      const lowerKeyword = keyword.toLowerCase();
      this.filteredOrders = this.availableOrders.filter(o =>
        o.orderNumber.toLowerCase().includes(lowerKeyword) ||
        o.supplierName.toLowerCase().includes(lowerKeyword)
      );
    }
  }

  /** 発注書を選択 */
  selectOrder(order: PurchaseOrder): void {
    this.selectedOrder = order;
    this.orderSearchKeyword = order.orderNumber;
    this.showOrderResults = false;
    this.clearMessages();

    // 入荷明細行を構築
    this.receiptLines = order.items.map(item => ({
      itemId: item.id,
      productName: item.productName,
      productSku: item.productSku,
      orderedQuantity: item.quantity,
      previouslyReceived: item.receivedQuantity,
      remainingQuantity: item.quantity - item.receivedQuantity,
      receivedQuantity: item.quantity - item.receivedQuantity,
      qualityNotes: '',
      isAccepted: true
    }));
  }

  /** 検索結果を非表示 */
  hideOrderResults(): void {
    setTimeout(() => {
      this.showOrderResults = false;
    }, 200);
  }

  /** 全量入荷 */
  fillAllQuantities(): void {
    this.receiptLines.forEach(line => {
      line.receivedQuantity = line.remainingQuantity;
    });
  }

  /** 入荷数量をクリア */
  clearAllQuantities(): void {
    this.receiptLines.forEach(line => {
      line.receivedQuantity = 0;
    });
  }

  /** 入荷数量バリデーション */
  isQuantityValid(line: ReceiptLine): boolean {
    return line.receivedQuantity >= 0 && line.receivedQuantity <= line.remainingQuantity;
  }

  /** 全体バリデーション */
  isFormValid(): boolean {
    if (!this.selectedOrder) return false;
    if (!this.receiptDate) return false;

    // 少なくとも1つの明細に入荷数量がある
    const hasReceivedItems = this.receiptLines.some(line => line.receivedQuantity > 0);
    if (!hasReceivedItems) return false;

    // 全ての入荷数量がバリデーション通過
    return this.receiptLines.every(line => this.isQuantityValid(line));
  }

  /** 入荷率を計算 */
  getReceiptProgress(line: ReceiptLine): number {
    if (line.orderedQuantity === 0) return 0;
    return Math.round(((line.previouslyReceived + line.receivedQuantity) / line.orderedQuantity) * 100);
  }

  /**
   * 入荷処理を実行
   *
   * 技術的負債: 送信ボタンがリクエスト中に無効化されない
   * isSubmittingフラグは設定されるが、テンプレートのボタンの[disabled]に反映されていない
   */
  submitReceipt(): void {
    if (!this.selectedOrder || !this.isFormValid()) return;

    this.clearMessages();
    // 技術的負債: isSubmittingを設定するが、テンプレートで使われていない
    this.isSubmitting = true;

    const receiptItems = this.receiptLines
      .filter(line => line.receivedQuantity > 0)
      .map(line => ({
        itemId: line.itemId,
        receivedQuantity: line.receivedQuantity
      }));

    this.purchaseOrderService.receiveGoods(this.selectedOrder.id, receiptItems).pipe(
      takeUntil(this.destroy$)
      // 技術的負債: finalizeでisSubmittingをリセットすべきだが、されていない
    ).subscribe({
      next: (updated) => {
        this.isSubmitting = false;
        this.successMessage = '入荷処理が完了しました';
        this.selectedOrder = updated;

        // 入荷完了後、明細を更新
        this.receiptLines = updated.items.map(item => ({
          itemId: item.id,
          productName: item.productName,
          productSku: item.productSku,
          orderedQuantity: item.quantity,
          previouslyReceived: item.receivedQuantity,
          remainingQuantity: item.quantity - item.receivedQuantity,
          receivedQuantity: 0,
          qualityNotes: '',
          isAccepted: true
        }));
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('入荷処理に失敗しました', error);
        this.errorMessage = '入荷処理に失敗しました。再度お試しください。';
      }
    });
  }

  /** メッセージクリア */
  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  /** 一覧画面へ戻る */
  goBack(): void {
    this.router.navigate(['/procurement/orders']);
  }

  /** 発注書詳細へ遷移 */
  navigateToOrderDetail(): void {
    if (this.selectedOrder) {
      this.router.navigate(['/procurement/orders', this.selectedOrder.id]);
    }
  }
}
