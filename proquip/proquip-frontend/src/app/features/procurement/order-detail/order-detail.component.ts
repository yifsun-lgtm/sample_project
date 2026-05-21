import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { PurchaseOrder, PurchaseOrderItem, ApprovalStep } from '@shared/models/purchase-order.model';
import { getOrderStatusLabel, getOrderStatusColor } from '@shared/utils/status.utils';

/**
 * 発注書詳細コンポーネント
 *
 * 技術的負債: 条件分岐表示ロジックに10以上のngIfブロックがあり、
 * ステータス文字列を直接比較している。enumや定数に置き換えるべき。
 */
@Component({
  selector: 'app-order-detail',
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss']
})
export class OrderDetailComponent implements OnInit, OnDestroy {

  /** 発注書 */
  order: PurchaseOrder | null = null;

  /** 読み込み中フラグ */
  isLoading = true;

  /** アクション実行中フラグ */
  isProcessing = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 承認コメント */
  approvalComment = '';

  /** 却下理由 */
  rejectReason = '';

  /** キャンセル理由 */
  cancelReason = '';

  /** コメントモーダル表示 */
  showApprovalModal = false;

  /** 却下モーダル表示 */
  showRejectModal = false;

  /** キャンセルモーダル表示 */
  showCancelModal = false;

  /** ステータス履歴（タイムライン用） */
  statusHistory: { status: string; date: string; user: string; comment: string }[] = [];

  /** 承認ワークフローステップ定義 */
  approvalWorkflowSteps = [
    { label: '作成', status: 'DRAFT' },
    { label: '申請', status: 'SUBMITTED' },
    { label: '承認', status: 'APPROVED' },
    { label: '発注', status: 'ORDERED' },
    { label: '入荷', status: 'RECEIVED' }
  ];

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private purchaseOrderService: PurchaseOrderService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadOrder(id);
    } else {
      this.errorMessage = '発注書IDが無効です';
      this.isLoading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 発注書を読み込む */
  private loadOrder(id: number): void {
    this.isLoading = true;
    this.purchaseOrderService.getOrder(id).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (order) => {
        this.order = order;
        this.buildStatusHistory();
      },
      error: (error) => {
        console.error('発注書の取得に失敗しました', error);
        this.errorMessage = '発注書の取得に失敗しました';
      }
    });
  }

  /**
   * ステータス履歴を構築する
   * 技術的負債: タイムラインデータの構築がコンポーネント内にある
   */
  private buildStatusHistory(): void {
    if (!this.order) return;

    this.statusHistory = [];

    // 作成
    this.statusHistory.push({
      status: 'DRAFT',
      date: this.order.createdAt,
      user: this.order.createdBy,
      comment: '発注書を作成しました'
    });

    // 承認ステップ
    if (this.order.approvalSteps) {
      this.order.approvalSteps
        .filter(step => step.actionDate)
        .sort((a, b) => new Date(a.actionDate!).getTime() - new Date(b.actionDate!).getTime())
        .forEach(step => {
          this.statusHistory.push({
            status: step.status,
            date: step.actionDate!,
            user: step.approverName || step.approverRole,
            comment: step.comment || this.getStatusLabel(step.status)
          });
        });
    }

    // 入荷日
    if (this.order.actualDeliveryDate) {
      this.statusHistory.push({
        status: 'RECEIVED',
        date: this.order.actualDeliveryDate,
        user: '',
        comment: '入荷が完了しました'
      });
    }
  }

  /** ステータスラベルを取得 */
  getStatusLabel(status: string): string {
    return getOrderStatusLabel(status);
  }

  /** ステータス色を取得 */
  getStatusColor(status: string): string {
    return getOrderStatusColor(status);
  }

  /** 明細行の小計を計算 */
  getItemSubtotal(item: PurchaseOrderItem): number {
    return item.quantity * item.unitPrice;
  }

  /** 入荷率を計算 */
  getReceiptRate(item: PurchaseOrderItem): number {
    if (item.quantity === 0) return 0;
    return Math.round((item.receivedQuantity / item.quantity) * 100);
  }

  /** ワークフローステップの進捗状態を取得 */
  getStepStatus(stepStatus: string): 'completed' | 'current' | 'pending' {
    if (!this.order) return 'pending';

    const statusOrder = ['DRAFT', 'SUBMITTED', 'APPROVED', 'ORDERED', 'RECEIVED'];
    const currentIndex = statusOrder.indexOf(this.order.status);
    const stepIndex = statusOrder.indexOf(stepStatus);

    // 技術的負債: キャンセル状態の表示処理が分岐で複雑
    if (this.order.status === 'CANCELLED') {
      return 'pending';
    }
    if (this.order.status === 'PARTIALLY_RECEIVED' && stepStatus === 'RECEIVED') {
      return 'current';
    }

    if (stepIndex < currentIndex) return 'completed';
    if (stepIndex === currentIndex) return 'current';
    return 'pending';
  }

  /** アクション: 承認申請 */
  submitForApproval(): void {
    if (!this.order) return;
    this.isProcessing = true;
    this.clearMessages();

    this.purchaseOrderService.submitForApproval(this.order.id).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (updated) => {
        this.order = updated;
        this.buildStatusHistory();
        this.successMessage = '承認申請を提出しました';
      },
      error: (error) => {
        console.error('承認申請の提出に失敗しました', error);
        this.errorMessage = '承認申請の提出に失敗しました';
      }
    });
  }

  /** アクション: 承認 */
  approveOrder(): void {
    if (!this.order) return;
    this.isProcessing = true;
    this.clearMessages();
    this.showApprovalModal = false;

    this.purchaseOrderService.approveOrder(this.order.id, this.approvalComment).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (updated) => {
        this.order = updated;
        this.buildStatusHistory();
        this.successMessage = '発注書を承認しました';
        this.approvalComment = '';
      },
      error: (error) => {
        console.error('承認処理に失敗しました', error);
        this.errorMessage = '承認処理に失敗しました';
      }
    });
  }

  /** アクション: 却下 */
  rejectOrder(): void {
    if (!this.order || !this.rejectReason) return;
    this.isProcessing = true;
    this.clearMessages();
    this.showRejectModal = false;

    this.purchaseOrderService.rejectOrder(this.order.id, this.rejectReason).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (updated) => {
        this.order = updated;
        this.buildStatusHistory();
        this.successMessage = '発注書を却下しました';
        this.rejectReason = '';
      },
      error: (error) => {
        console.error('却下処理に失敗しました', error);
        this.errorMessage = '却下処理に失敗しました';
      }
    });
  }

  /** アクション: キャンセル */
  cancelOrder(): void {
    if (!this.order || !this.cancelReason) return;
    this.isProcessing = true;
    this.clearMessages();
    this.showCancelModal = false;

    this.purchaseOrderService.cancelOrder(this.order.id, this.cancelReason).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (updated) => {
        this.order = updated;
        this.buildStatusHistory();
        this.successMessage = '発注書をキャンセルしました';
        this.cancelReason = '';
      },
      error: (error) => {
        console.error('キャンセル処理に失敗しました', error);
        this.errorMessage = 'キャンセル処理に失敗しました';
      }
    });
  }

  /** 入荷処理画面へ遷移 */
  navigateToGoodsReceipt(): void {
    this.router.navigate(['/procurement/goods-receipt'], {
      queryParams: { orderId: this.order?.id }
    });
  }

  /** メッセージをクリア */
  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  /** 一覧画面へ戻る */
  goBack(): void {
    this.router.navigate(['/procurement/orders']);
  }

  /** 承認モーダルを開く */
  openApprovalModal(): void {
    this.showApprovalModal = true;
  }

  /** 却下モーダルを開く */
  openRejectModal(): void {
    this.showRejectModal = true;
  }

  /** キャンセルモーダルを開く */
  openCancelModal(): void {
    this.showCancelModal = true;
  }

  /** モーダルを閉じる */
  closeModals(): void {
    this.showApprovalModal = false;
    this.showRejectModal = false;
    this.showCancelModal = false;
  }

  /**
   * アクションボタン表示条件
   *
   * 技術的負債: ステータス文字列を直接比較する条件分岐が多数ある
   */
  canSubmit(): boolean {
    return this.order?.status === 'DRAFT';
  }

  canApprove(): boolean {
    return this.order?.status === 'SUBMITTED' || this.order?.status === 'PENDING_APPROVAL';
  }

  canReject(): boolean {
    return this.order?.status === 'SUBMITTED' || this.order?.status === 'PENDING_APPROVAL';
  }

  canCancel(): boolean {
    return this.order?.status === 'DRAFT' ||
           this.order?.status === 'SUBMITTED' ||
           this.order?.status === 'APPROVED';
  }

  canProcessReceipt(): boolean {
    return this.order?.status === 'ORDERED' || this.order?.status === 'PARTIALLY_RECEIVED';
  }

  /** 納品予定日が過ぎているか */
  isOverdue(): boolean {
    if (!this.order?.expectedDeliveryDate) return false;
    if (this.order.status === 'RECEIVED' || this.order.status === 'CANCELLED') return false;
    return new Date(this.order.expectedDeliveryDate) < new Date();
  }
}
