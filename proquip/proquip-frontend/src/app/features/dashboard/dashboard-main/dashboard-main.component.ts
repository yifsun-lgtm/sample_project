import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DashboardService, DashboardSummary } from '@shared/services/dashboard.service';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { InventoryService } from '@shared/services/inventory.service';
import { BudgetService } from '@shared/services/budget.service';
import { RequisitionService } from '@shared/services/requisition.service';
import { PurchaseOrder } from '@shared/models/purchase-order.model';
import { getOrderStatusLabel, getOrderStatusClass } from '@shared/utils/status.utils';
import { InventoryItem } from '@shared/models/inventory.model';
import { Budget } from '@shared/models/budget.model';

/**
 * メインダッシュボードコンポーネント
 * サマリーカード、チャート、最近の発注、在庫アラートを表示
 *
 * 技術的負債 #5: 複雑なデータ集約ロジックがサービスではなくコンポーネント内にある
 * 技術的負債 #6: パイプを使わず文字列結合で日付フォーマットしている
 * 技術的負債: 複数のsubscriptionがunsubscribeされていない（メモリリーク）
 * 技術的負債: ngOnInitで5つ以上のAPIを並列ではなく直列に呼び出している
 */
@Component({
  selector: 'app-dashboard-main',
  templateUrl: './dashboard-main.component.html',
  styleUrls: ['./dashboard-main.component.scss']
})
export class DashboardMainComponent implements OnInit {

  /** ローディング状態 */
  isLoading = true;

  /** エラーメッセージ */
  errorMessage = '';

  // サマリーカードデータ
  /** 発注件数 */
  totalOrders = 0;
  pendingOrders = 0;
  approvedOrders = 0;

  /** 在庫アラート数 */
  lowStockCount = 0;

  /** 承認待ち件数 */
  pendingApprovals = 0;

  /** 予算消化率 */
  budgetUtilization = 0;
  budgetTotal = 0;
  budgetSpent = 0;

  /** 最近の発注一覧（上位10件） */
  recentOrders: PurchaseOrder[] = [];

  /** 在庫アラート一覧 */
  lowStockItems: InventoryItem[] = [];

  /** 月別支出データ（チャート用） */
  monthlySpendingData: any[] = [];

  /** カテゴリ別支出データ（チャート用） */
  categorySpendingData: any[] = [];

  /** サマリーデータ */
  summary: DashboardSummary | null = null;

  constructor(
    private dashboardService: DashboardService,
    private purchaseOrderService: PurchaseOrderService,
    private inventoryService: InventoryService,
    private budgetService: BudgetService,
    private requisitionService: RequisitionService,
    private router: Router
  ) {}

  /**
   * 初期化処理
   * 技術的負債: APIを直列で呼び出しているため、全データのロードに時間がかかる
   * forkJoinで並列化すべきだが、現状は各APIを順番に呼んでいる
   */
  ngOnInit(): void {
    this.isLoading = true;
    this.loadDashboardSummary();
  }

  /**
   * ダッシュボードサマリーデータを読み込む
   * 技術的負債: 直列呼び出しによるパフォーマンス問題
   */
  private loadDashboardSummary(): void {
    // 技術的負債: 以下の5つのAPI呼び出しはforkJoinで並列化すべき
    // 現状は各コールバック内で次のAPIを呼び出す直列パターン

    // 月別支出データ取得（サマリーとは独立して呼び出し）
    this.dashboardService.getMonthlySpendingTrend().subscribe(
      (data) => {
        this.monthlySpendingData = (data || []).map((item: any) => {
          const [year, month] = item.month.split('-');
          return { month: `${year}年${parseInt(month, 10)}月`, amount: item.amount };
        });
      },
      (error) => {
        console.error('月別支出データ取得エラー:', error);
      }
    );

    // カテゴリ別支出データ取得（サマリーとは独立して呼び出し）
    this.dashboardService.getCategorySpending().subscribe(
      (data) => {
        const items = data || [];
        const totalAmount = items.reduce((sum: number, item: any) => sum + item.totalAmount, 0);
        this.categorySpendingData = items.map((item: any) => ({
          category: item.categoryName,
          percentage: totalAmount > 0 ? Math.round((item.totalAmount / totalAmount) * 100) : 0,
          amount: item.totalAmount
        }));
      },
      (error) => {
        console.error('カテゴリ別支出データ取得エラー:', error);
      }
    );

    // 1. ダッシュボードサマリー取得
    this.dashboardService.getSummary().subscribe(
      (summary) => {
        this.summary = summary;
        this.lowStockCount = summary.lowStockItems;
        this.pendingApprovals = summary.pendingApprovals;

        // 2. 発注書一覧取得（サマリー取得後に直列で呼び出し）
        this.purchaseOrderService.getOrders(0, 100).subscribe(
          (ordersResult) => {
            // 技術的負債 #5: 集約ロジックがコンポーネント内にある
            this.totalOrders = ordersResult.totalElements;
            this.calculateOrderStats(ordersResult.content);
            this.recentOrders = ordersResult.content
              .sort((a, b) => new Date(b.orderDate).getTime() - new Date(a.orderDate).getTime())
              .slice(0, 10);

            // 3. 在庫アラート取得
            this.inventoryService.getLowStockAlerts().subscribe(
              (alerts) => {
                this.lowStockItems = alerts;
                this.lowStockCount = alerts.length;

                // 4. 予算情報取得
                this.budgetService.getBudgets(2026).subscribe(
                  (budgets) => {
                    this.calculateBudgetUtilization(budgets);

                    // 5. 承認待ち件数取得
                    this.requisitionService.getRequisitions(0, 100, 'PENDING_APPROVAL').subscribe(
                      (requisitions) => {
                        this.pendingApprovals = this.pendingApprovals + requisitions.totalElements;
                        this.isLoading = false;
                      },
                      (error) => {
                        console.error('承認待ち取得エラー:', error);
                        this.isLoading = false;
                      }
                    );
                  },
                  (error) => {
                    console.error('予算情報取得エラー:', error);
                    this.isLoading = false;
                  }
                );
              },
              (error) => {
                console.error('在庫アラート取得エラー:', error);
                this.isLoading = false;
              }
            );
          },
          (error) => {
            console.error('発注書一覧取得エラー:', error);
            this.isLoading = false;
          }
        );
      },
      (error) => {
        console.error('ダッシュボードサマリー取得エラー:', error);
        this.errorMessage = 'データの取得に失敗しました。再度お試しください。';
        this.isLoading = false;
      }
    );
  }

  /**
   * 発注統計を計算
   * 技術的負債 #5: この集約ロジックはサーバーサイドまたはサービスに移動すべき
   */
  private calculateOrderStats(orders: PurchaseOrder[]): void {
    this.pendingOrders = 0;
    this.approvedOrders = 0;

    for (let i = 0; i < orders.length; i++) {
      const order = orders[i];
      if (order.status === 'PENDING' || order.status === 'PENDING_APPROVAL') {
        this.pendingOrders++;
      } else if (order.status === 'APPROVED') {
        this.approvedOrders++;
      }
    }
  }

  /**
   * 予算消化率を計算
   * 技術的負債 #5: 集約ロジックがコンポーネント内にある
   */
  private calculateBudgetUtilization(budgets: Budget[]): void {
    let totalBudget = 0;
    let totalSpent = 0;

    for (const budget of budgets) {
      totalBudget += budget.totalAmount || 0;
      totalSpent += budget.usedAmount || 0;
    }

    this.budgetTotal = totalBudget;
    this.budgetSpent = totalSpent;
    this.budgetUtilization = totalBudget > 0
      ? Math.round((totalSpent / totalBudget) * 100)
      : 0;
  }

  /**
   * 日付をフォーマットする
   * 技術的負債 #6: JapaneseDatePipeを使うべきだが、文字列結合で実装
   */
  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '-';
    const year = d.getFullYear();
    const month = ('0' + (d.getMonth() + 1)).slice(-2);
    const day = ('0' + d.getDate()).slice(-2);
    return year + '年' + month + '月' + day + '日';
  }

  /**
   * 金額をフォーマットする
   * 技術的負債 #6: CurrencyJpPipeを使うべきだが、文字列結合で実装
   */
  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + amount.toLocaleString('ja-JP');
  }

  getStatusLabel(status: string): string {
    return getOrderStatusLabel(status);
  }

  getStatusClass(status: string): string {
    return getOrderStatusClass(status);
  }

  /**
   * チャートの棒グラフの高さを計算（プレースホルダー）
   * 技術的負債: 実際のチャートライブラリを導入すべき
   */
  getBarHeight(amount: number): number {
    const maxAmount = Math.max(...this.monthlySpendingData.map((d: any) => d.amount));
    return maxAmount > 0 ? (amount / maxAmount) * 200 : 0;
  }

  /** 発注詳細画面へ遷移 */
  navigateToOrder(order: PurchaseOrder): void {
    this.router.navigate(['/procurement', 'orders', order.id]);
  }

  /** 在庫管理画面へ遷移 */
  navigateToInventory(): void {
    this.router.navigate(['/inventory']);
  }

  /** 製品詳細画面へ遷移 */
  navigateToProduct(productId: number): void {
    this.router.navigate(['/products', productId]);
  }

  /** タスク画面へ遷移 */
  navigateToTasks(): void {
    this.router.navigate(['/dashboard', 'tasks']);
  }

  /** データを再読み込み */
  refresh(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.loadDashboardSummary();
  }
}
