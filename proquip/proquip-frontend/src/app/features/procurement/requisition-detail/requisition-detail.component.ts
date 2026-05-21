import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { RequisitionService } from '@shared/services/requisition.service';
import { Requisition, RequisitionItem } from '@shared/models/purchase-order.model';
import { getOrderStatusLabel, getOrderStatusColor } from '@shared/utils/status.utils';

/**
 * 購買依頼詳細コンポーネント
 * 依頼内容、明細、承認ステータス、アクションボタンを表示する
 *
 * 技術的負債: 承認ステータスの色マッピングがハードコードされている
 */
@Component({
  selector: 'app-requisition-detail',
  templateUrl: './requisition-detail.component.html',
  styleUrls: ['./requisition-detail.component.scss']
})
export class RequisitionDetailComponent implements OnInit, OnDestroy {

  /** 購買依頼 */
  requisition: Requisition | null = null;

  /** 読み込み中フラグ */
  isLoading = true;

  /** アクション実行中フラグ */
  isProcessing = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 合計金額 */
  totalAmount = 0;

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private requisitionService: RequisitionService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadRequisition(id);
    } else {
      this.errorMessage = '購買依頼IDが無効です';
      this.isLoading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 購買依頼を読み込む */
  private loadRequisition(id: number): void {
    this.isLoading = true;
    this.requisitionService.getRequisition(id).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (requisition) => {
        this.requisition = requisition;
        this.calculateTotal();
      },
      error: (error) => {
        console.error('購買依頼の取得に失敗しました', error);
        this.errorMessage = '購買依頼の取得に失敗しました';
      }
    });
  }

  /** 合計金額を計算 */
  private calculateTotal(): void {
    if (!this.requisition?.items) {
      this.totalAmount = 0;
      return;
    }
    this.totalAmount = this.requisition.items.reduce((sum, item) => {
      return sum + (item.quantity * item.estimatedUnitPrice);
    }, 0);
  }

  getStatusColor(status: string): string {
    return getOrderStatusColor(status);
  }

  getStatusLabel(status: string): string {
    return getOrderStatusLabel(status);
  }

  /** 緊急度のラベルを取得 */
  getPriorityLabel(priority: string): string {
    const labels: { [key: string]: string } = {
      'LOW': '低',
      'NORMAL': '通常',
      'HIGH': '高',
      'URGENT': '緊急'
    };
    return labels[priority] || priority;
  }

  /** 緊急度の色を取得 */
  getPriorityColor(priority: string): string {
    const colors: { [key: string]: string } = {
      'LOW': '#78909c',
      'NORMAL': '#1976d2',
      'HIGH': '#f57c00',
      'URGENT': '#e53935'
    };
    return colors[priority] || '#9e9e9e';
  }

  /** 明細行の小計を計算 */
  getItemSubtotal(item: RequisitionItem): number {
    return item.quantity * item.estimatedUnitPrice;
  }

  /** アクション: 承認申請 */
  submitForApproval(): void {
    if (!this.requisition) return;
    this.isProcessing = true;
    this.clearMessages();

    this.requisitionService.submitForApproval(this.requisition.id).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (updated) => {
        this.requisition = updated;
        this.successMessage = '承認申請を提出しました';
      },
      error: (error) => {
        console.error('承認申請の提出に失敗しました', error);
        this.errorMessage = '承認申請の提出に失敗しました';
      }
    });
  }

  /** アクション: 承認 */
  approveRequisition(): void {
    if (!this.requisition) return;
    this.isProcessing = true;
    this.clearMessages();

    this.requisitionService.approveRequisition(this.requisition.id, '承認します').pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (updated) => {
        this.requisition = updated;
        this.successMessage = '購買依頼を承認しました';
      },
      error: (error) => {
        console.error('承認処理に失敗しました', error);
        this.errorMessage = '承認処理に失敗しました';
      }
    });
  }

  /** アクション: 却下 */
  rejectRequisition(): void {
    if (!this.requisition) return;
    this.isProcessing = true;
    this.clearMessages();

    this.requisitionService.rejectRequisition(this.requisition.id, '要件を確認してください').pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (updated) => {
        this.requisition = updated;
        this.successMessage = '購買依頼を却下しました';
      },
      error: (error) => {
        console.error('却下処理に失敗しました', error);
        this.errorMessage = '却下処理に失敗しました';
      }
    });
  }

  /** アクション: 発注書に変換 */
  convertToOrder(): void {
    if (!this.requisition) return;
    this.isProcessing = true;
    this.clearMessages();

    this.requisitionService.convertToOrder(this.requisition.id).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isProcessing = false)
    ).subscribe({
      next: (result) => {
        this.successMessage = '発注書に変換しました';
        // 発注書詳細画面へ遷移
        if (result && result.orderId) {
          this.router.navigate(['/procurement/orders', result.orderId]);
        }
      },
      error: (error) => {
        console.error('発注書への変換に失敗しました', error);
        this.errorMessage = '発注書への変換に失敗しました';
      }
    });
  }

  /** メッセージをクリア */
  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  /** 一覧画面へ戻る */
  goBack(): void {
    this.router.navigate(['/procurement/requisitions']);
  }

  /** アクションボタン表示条件 */
  canSubmit(): boolean {
    return this.requisition?.status === 'DRAFT';
  }

  canApprove(): boolean {
    return this.requisition?.status === 'SUBMITTED' || this.requisition?.status === 'PENDING_APPROVAL';
  }

  canReject(): boolean {
    return this.requisition?.status === 'SUBMITTED' || this.requisition?.status === 'PENDING_APPROVAL';
  }

  canConvert(): boolean {
    return this.requisition?.status === 'APPROVED';
  }
}
