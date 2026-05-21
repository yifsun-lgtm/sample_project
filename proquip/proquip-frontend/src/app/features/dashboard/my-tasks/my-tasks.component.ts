import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { RequisitionService } from '@shared/services/requisition.service';
import { PurchaseOrder, Requisition } from '@shared/models/purchase-order.model';

/**
 * 承認タスクの統合インターフェース
 */
interface ApprovalTask {
  id: number;
  type: 'requisition' | 'purchase_order';
  typeLabel: string;
  referenceNumber: string;
  requester: string;
  department: string;
  amount: number;
  date: string;
  priority: string;
  status: string;
  originalData: any;
}

/**
 * マイタスクコンポーネント
 * 現在のユーザーに割り当てられた承認待ちタスクを表示
 * タブ切り替えでフィルタリング可能
 */
@Component({
  selector: 'app-my-tasks',
  templateUrl: './my-tasks.component.html',
  styleUrls: ['./my-tasks.component.scss']
})
export class MyTasksComponent implements OnInit, OnDestroy {

  /** タブ定義 */
  tabs = [
    { key: 'all', label: 'すべて' },
    { key: 'requisition', label: '購買依頼承認' },
    { key: 'purchase_order', label: '発注承認' }
  ];

  /** 選択中のタブ */
  activeTab = 'all';

  /** 全タスクリスト */
  allTasks: ApprovalTask[] = [];

  /** フィルタ後のタスクリスト */
  filteredTasks: ApprovalTask[] = [];

  /** ローディング状態 */
  isLoading = true;

  /** エラーメッセージ */
  errorMessage = '';

  /** 承認処理中のタスクID */
  processingTaskId: number | null = null;

  /** 承認コメント用（インライン承認/却下） */
  commentText: { [key: number]: string } = {};

  /** コメント入力表示フラグ */
  showCommentInput: { [key: number]: string } = {};

  /** サブスクリプション管理 */
  private subscriptions: Subscription[] = [];

  /** タブ別件数 */
  tabCounts: { [key: string]: number } = {
    all: 0,
    requisition: 0,
    purchase_order: 0
  };

  constructor(
    private purchaseOrderService: PurchaseOrderService,
    private requisitionService: RequisitionService
  ) {}

  ngOnInit(): void {
    this.loadTasks();
  }

  ngOnDestroy(): void {
    // サブスクリプションを解除してメモリリークを防止
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * タスクデータを読み込む
   */
  private loadTasks(): void {
    this.isLoading = true;
    this.allTasks = [];

    // 購買依頼の承認待ち取得
    const reqSub = this.requisitionService.getRequisitions(0, 100, 'PENDING_APPROVAL').subscribe(
      (result) => {
        const requisitionTasks = result.content.map(req => this.mapRequisitionToTask(req));
        this.allTasks = [...this.allTasks, ...requisitionTasks];

        // 発注書の承認待ち取得
        const poSub = this.purchaseOrderService.getPendingApprovals(0, 100).subscribe(
          (poResult) => {
            const orderTasks = poResult.content.map(order => this.mapOrderToTask(order));
            this.allTasks = [...this.allTasks, ...orderTasks];
            this.sortTasks();
            this.updateTabCounts();
            this.filterTasks();
            this.isLoading = false;
          },
          (error) => {
            console.error('発注承認一覧の取得に失敗:', error);
            this.sortTasks();
            this.updateTabCounts();
            this.filterTasks();
            this.isLoading = false;
          }
        );
        this.subscriptions.push(poSub);
      },
      (error) => {
        console.error('購買依頼一覧の取得に失敗:', error);
        this.errorMessage = 'タスクの取得に失敗しました。';
        this.isLoading = false;
      }
    );
    this.subscriptions.push(reqSub);
  }

  /**
   * 購買依頼をタスクオブジェクトに変換
   */
  private mapRequisitionToTask(req: Requisition): ApprovalTask {
    const totalAmount = req.items.reduce((sum, item) => {
      return sum + (item.estimatedUnitPrice * item.quantity);
    }, 0);

    return {
      id: req.id,
      type: 'requisition',
      typeLabel: '購買依頼',
      referenceNumber: req.requisitionNumber,
      requester: req.requestedBy,
      department: req.department,
      amount: totalAmount,
      date: req.createdAt,
      priority: req.priority,
      status: req.status,
      originalData: req
    };
  }

  /**
   * 発注書をタスクオブジェクトに変換
   */
  private mapOrderToTask(order: PurchaseOrder): ApprovalTask {
    return {
      id: order.id,
      type: 'purchase_order',
      typeLabel: '発注承認',
      referenceNumber: order.orderNumber,
      requester: order.createdBy,
      department: '-',
      amount: order.totalAmount,
      date: order.createdAt,
      priority: 'NORMAL',
      status: order.status,
      originalData: order
    };
  }

  /**
   * タスクを日付の新しい順にソート
   */
  private sortTasks(): void {
    this.allTasks.sort((a, b) => {
      // 優先度でソート（HIGH → NORMAL → LOW）
      const priorityOrder: any = { 'HIGH': 0, 'URGENT': 0, 'NORMAL': 1, 'LOW': 2 };
      const pA = priorityOrder[a.priority] ?? 1;
      const pB = priorityOrder[b.priority] ?? 1;
      if (pA !== pB) return pA - pB;

      // 日付の新しい順
      return new Date(b.date).getTime() - new Date(a.date).getTime();
    });
  }

  /**
   * タブ別件数を更新
   */
  private updateTabCounts(): void {
    this.tabCounts = {
      all: this.allTasks.length,
      requisition: this.allTasks.filter(t => t.type === 'requisition').length,
      purchase_order: this.allTasks.filter(t => t.type === 'purchase_order').length
    };
  }

  /**
   * タブ切り替え
   */
  selectTab(tabKey: string): void {
    this.activeTab = tabKey;
    this.filterTasks();
  }

  /**
   * タスクをフィルタリング
   */
  private filterTasks(): void {
    if (this.activeTab === 'all') {
      this.filteredTasks = [...this.allTasks];
    } else {
      this.filteredTasks = this.allTasks.filter(t => t.type === this.activeTab);
    }
  }

  /**
   * クイック承認
   */
  approveTask(task: ApprovalTask): void {
    const comment = this.commentText[task.id] || '';
    this.processingTaskId = task.id;

    if (task.type === 'requisition') {
      const sub = this.requisitionService.approveRequisition(task.id, comment).subscribe(
        () => {
          this.removeCompletedTask(task);
          this.processingTaskId = null;
        },
        (error) => {
          console.error('承認処理に失敗:', error);
          this.processingTaskId = null;
        }
      );
      this.subscriptions.push(sub);
    } else {
      const sub = this.purchaseOrderService.approveOrder(task.id, comment).subscribe(
        () => {
          this.removeCompletedTask(task);
          this.processingTaskId = null;
        },
        (error) => {
          console.error('承認処理に失敗:', error);
          this.processingTaskId = null;
        }
      );
      this.subscriptions.push(sub);
    }
  }

  /**
   * 却下（コメント入力を表示）
   */
  showRejectInput(task: ApprovalTask): void {
    this.showCommentInput[task.id] = 'reject';
    this.commentText[task.id] = '';
  }

  /**
   * 承認（コメント入力を表示）
   */
  showApproveInput(task: ApprovalTask): void {
    this.showCommentInput[task.id] = 'approve';
    this.commentText[task.id] = '';
  }

  /**
   * コメント入力をキャンセル
   */
  cancelAction(task: ApprovalTask): void {
    delete this.showCommentInput[task.id];
    delete this.commentText[task.id];
  }

  /**
   * 却下を実行
   */
  rejectTask(task: ApprovalTask): void {
    const comment = this.commentText[task.id] || '';
    if (!comment) {
      return; // 却下にはコメント必須
    }

    this.processingTaskId = task.id;

    if (task.type === 'requisition') {
      const sub = this.requisitionService.rejectRequisition(task.id, comment).subscribe(
        () => {
          this.removeCompletedTask(task);
          this.processingTaskId = null;
        },
        (error) => {
          console.error('却下処理に失敗:', error);
          this.processingTaskId = null;
        }
      );
      this.subscriptions.push(sub);
    } else {
      const sub = this.purchaseOrderService.rejectOrder(task.id, comment).subscribe(
        () => {
          this.removeCompletedTask(task);
          this.processingTaskId = null;
        },
        (error) => {
          console.error('却下処理に失敗:', error);
          this.processingTaskId = null;
        }
      );
      this.subscriptions.push(sub);
    }
  }

  /**
   * 処理済みタスクをリストから除去
   */
  private removeCompletedTask(task: ApprovalTask): void {
    this.allTasks = this.allTasks.filter(t => !(t.id === task.id && t.type === task.type));
    this.updateTabCounts();
    this.filterTasks();
    delete this.showCommentInput[task.id];
    delete this.commentText[task.id];
  }

  /**
   * 優先度に応じたCSSクラスを返す
   */
  getPriorityClass(priority: string): string {
    const classMap: any = {
      'URGENT': 'priority-urgent',
      'HIGH': 'priority-high',
      'NORMAL': 'priority-normal',
      'LOW': 'priority-low'
    };
    return classMap[priority] || 'priority-normal';
  }

  /**
   * 優先度ラベルを返す
   */
  getPriorityLabel(priority: string): string {
    const labels: any = {
      'URGENT': '至急',
      'HIGH': '高',
      'NORMAL': '通常',
      'LOW': '低'
    };
    return labels[priority] || priority;
  }

  /**
   * 金額フォーマット
   */
  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + amount.toLocaleString('ja-JP');
  }

  /**
   * 日付フォーマット
   */
  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    const month = d.getMonth() + 1;
    const day = d.getDate();
    return month + '/' + day;
  }
}
