import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { RequisitionService } from '@shared/services/requisition.service';
import { PurchaseOrder } from '@shared/models/purchase-order.model';
import { Requisition } from '@shared/models/purchase-order.model';

/** 承認待ちアイテムの型 */
interface ApprovalItem {
  id: number;
  type: 'requisition' | 'order';
  number: string;
  requestedBy: string;
  department: string;
  totalAmount: number;
  status: string;
  createdAt: string;
  original: any;
}

/**
 * 承認待ちキューコンポーネント
 * 現在のユーザーに割り当てられた承認待ちアイテムを表示する
 */
@Component({
  selector: 'app-approval-queue',
  templateUrl: './approval-queue.component.html',
  styleUrls: ['./approval-queue.component.scss']
})
export class ApprovalQueueComponent implements OnInit, OnDestroy {

  /** 承認待ちアイテム（全種類） */
  allItems: ApprovalItem[] = [];

  /** フィルタ済みアイテム */
  filteredItems: ApprovalItem[] = [];

  /** 現在のタブ */
  activeTab: 'all' | 'requisition' | 'order' = 'all';

  /** タブ別件数 */
  tabCounts = {
    all: 0,
    requisition: 0,
    order: 0
  };

  /** 読み込み中フラグ */
  isLoading = false;

  /** アクション実行中フラグ */
  isProcessing = false;

  /** クイック承認/却下コメントモーダル */
  showCommentModal = false;

  /** モーダルアクション種別 */
  modalAction: 'approve' | 'reject' = 'approve';

  /** モーダル対象アイテム */
  modalTarget: ApprovalItem | null = null;

  /** コメント入力 */
  commentText = '';

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private purchaseOrderService: PurchaseOrderService,
    private requisitionService: RequisitionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadApprovalQueue();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 承認待ちキューを読み込む */
  loadApprovalQueue(): void {
    this.isLoading = true;
    this.allItems = [];

    // 発注書の承認待ち
    this.purchaseOrderService.getPendingApprovals(0, 100).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result: any) => {
        const orders = Array.isArray(result) ? result : (result.content || []);
        const orderItems: ApprovalItem[] = orders.map((order: any) => ({
          id: order.id,
          type: 'order' as const,
          number: order.orderNumber,
          requestedBy: order.createdBy,
          department: '',
          totalAmount: order.totalAmount,
          status: order.status,
          createdAt: order.createdAt,
          original: order
        }));

        this.allItems = [...this.allItems, ...orderItems];
        this.updateCounts();
        this.filterItems();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('承認待ち発注書の取得に失敗しました', error);
        this.isLoading = false;
      }
    });

    // 購買依頼の承認待ち
    this.requisitionService.getRequisitions(0, 100, 'SUBMITTED').pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        const reqItems: ApprovalItem[] = (result.content || []).map(req => ({
          id: req.id,
          type: 'requisition' as const,
          number: req.requisitionNumber,
          requestedBy: req.requestedBy,
          department: req.department,
          totalAmount: req.items.reduce((sum, item) => sum + item.quantity * item.estimatedUnitPrice, 0),
          status: req.status,
          createdAt: req.createdAt,
          original: req
        }));

        this.allItems = [...this.allItems, ...reqItems];
        this.updateCounts();
        this.filterItems();
      },
      error: (error) => {
        console.error('承認待ち購買依頼の取得に失敗しました', error);
      }
    });
  }

  /** タブ別件数を更新 */
  private updateCounts(): void {
    this.tabCounts.all = this.allItems.length;
    this.tabCounts.requisition = this.allItems.filter(i => i.type === 'requisition').length;
    this.tabCounts.order = this.allItems.filter(i => i.type === 'order').length;
  }

  /** タブでフィルタ */
  filterItems(): void {
    if (this.activeTab === 'all') {
      this.filteredItems = [...this.allItems];
    } else {
      this.filteredItems = this.allItems.filter(i => i.type === this.activeTab);
    }

    // 日時の新しい順にソート
    this.filteredItems.sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  }

  /** タブ切り替え */
  switchTab(tab: 'all' | 'requisition' | 'order'): void {
    this.activeTab = tab;
    this.filterItems();
  }

  /** アイテムの種別ラベルを取得 */
  getTypeLabel(type: string): string {
    return type === 'requisition' ? '購買依頼' : '発注書';
  }

  /** 詳細画面へ遷移 */
  navigateToDetail(item: ApprovalItem): void {
    if (item.type === 'requisition') {
      this.router.navigate(['/procurement/requisitions', item.id]);
    } else {
      this.router.navigate(['/procurement/orders', item.id]);
    }
  }

  /** クイック承認モーダルを開く */
  openApproveModal(item: ApprovalItem, event: Event): void {
    event.stopPropagation();
    this.modalAction = 'approve';
    this.modalTarget = item;
    this.commentText = '';
    this.showCommentModal = true;
  }

  /** クイック却下モーダルを開く */
  openRejectModal(item: ApprovalItem, event: Event): void {
    event.stopPropagation();
    this.modalAction = 'reject';
    this.modalTarget = item;
    this.commentText = '';
    this.showCommentModal = true;
  }

  /** モーダルを閉じる */
  closeModal(): void {
    this.showCommentModal = false;
    this.modalTarget = null;
    this.commentText = '';
  }

  /** モーダルのアクションを実行 */
  executeAction(): void {
    if (!this.modalTarget) return;

    if (this.modalAction === 'reject' && !this.commentText) {
      return;
    }

    this.isProcessing = true;

    if (this.modalTarget.type === 'requisition') {
      this.executeRequisitionAction(this.modalTarget);
    } else {
      this.executeOrderAction(this.modalTarget);
    }
  }

  /** 購買依頼のアクションを実行 */
  private executeRequisitionAction(item: ApprovalItem): void {
    const comment = this.commentText || '承認しました';

    const action$ = this.modalAction === 'approve'
      ? this.requisitionService.approveRequisition(item.id, comment)
      : this.requisitionService.rejectRequisition(item.id, comment);

    action$.pipe(
      takeUntil(this.destroy$),
      finalize(() => {
        this.isProcessing = false;
        this.closeModal();
      })
    ).subscribe({
      next: () => {
        this.loadApprovalQueue();
      },
      error: (error) => {
        console.error('処理に失敗しました', error);
      }
    });
  }

  /** 発注書のアクションを実行 */
  private executeOrderAction(item: ApprovalItem): void {
    const comment = this.commentText || '承認しました';

    const action$ = this.modalAction === 'approve'
      ? this.purchaseOrderService.approveOrder(item.id, comment)
      : this.purchaseOrderService.rejectOrder(item.id, comment);

    action$.pipe(
      takeUntil(this.destroy$),
      finalize(() => {
        this.isProcessing = false;
        this.closeModal();
      })
    ).subscribe({
      next: () => {
        this.loadApprovalQueue();
      },
      error: (error) => {
        console.error('処理に失敗しました', error);
      }
    });
  }
}
