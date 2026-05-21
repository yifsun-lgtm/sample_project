import { Component, OnInit } from '@angular/core';
import { BudgetService } from '@shared/services/budget.service';
import { ReportService } from '@shared/services/report.service';

/**
 * 予算対実績行
 */
export interface BudgetActualRow {
  departmentId: number;
  department: string;
  budgetAmount: number;
  actualAmount: number;
  variance: number;
  utilizationRate: number;
  statusClass: string;
  isExpanded: boolean;
  monthlyBreakdown: MonthlyBudgetRow[];
}

/**
 * 月次内訳行
 */
export interface MonthlyBudgetRow {
  month: string;
  monthLabel: string;
  budgetAmount: number;
  actualAmount: number;
  variance: number;
  utilizationRate: number;
  statusClass: string;
}

/**
 * 予算対実績レポートコンポーネント
 * 部門ごとの予算消化状況を表示する
 *
 * 技術的負債: ドリルダウン状態がコンポーネント変数で管理されている
 * ルーターパラメータやクエリパラメータで管理すべき
 *
 * 技術的負債: 消化率計算ロジックがBudgetServiceと重複している
 */
@Component({
  selector: 'app-budget-actual',
  templateUrl: './budget-actual.component.html',
  styleUrls: ['./budget-actual.component.scss']
})
export class BudgetActualComponent implements OnInit {

  /** フィルター: 年度 */
  selectedFiscalYear = 2025;

  /** フィルター: 部門 */
  selectedDepartment = '';

  /** 年度オプション */
  fiscalYearOptions = [2023, 2024, 2025, 2026];

  /** 部門オプション */
  departmentOptions = [
    { value: '', label: 'すべての部門' },
    { value: '総務部', label: '総務部' },
    { value: '営業部', label: '営業部' },
    { value: '開発部', label: '開発部' },
    { value: '製造部', label: '製造部' },
    { value: '経理部', label: '経理部' },
    { value: '人事部', label: '人事部' }
  ];

  /** 予算対実績データ */
  budgetActualRows: BudgetActualRow[] = [];

  /** フィルター後のデータ */
  filteredRows: BudgetActualRow[] = [];

  /** 全体サマリー */
  totalBudget = 0;
  totalActual = 0;
  totalVariance = 0;
  totalUtilizationRate = 0;

  /** ローディング */
  isLoading = false;

  /** エラーメッセージ */
  errorMessage = '';

  /**
   * 技術的負債: ドリルダウン状態のトラッキング
   * ルーターパラメータで管理すべきだが、コンポーネント変数で管理している
   */
  expandedDepartmentId: number | null = null;

  constructor(
    private budgetService: BudgetService,
    private reportService: ReportService
  ) {}

  ngOnInit(): void {
    this.loadBudgetActual();
  }

  /** 予算対実績データの読み込み */
  loadBudgetActual(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.budgetService.getDepartmentSummary(this.selectedFiscalYear).subscribe({
      next: (budgets) => {
        this.processBudgetData(budgets);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('予算データ取得エラー:', err);
        this.errorMessage = '予算データの取得に失敗しました。';
        this.isLoading = false;
      }
    });
  }

  /**
   * 予算データを処理する
   *
   * 技術的負債: 消化率計算ロジックがBudgetServiceのgetBudgetUtilizationと重複
   */
  private processBudgetData(budgets: any[]): void {
    this.budgetActualRows = budgets.map(budget => {
      const total = budget.totalAmount || 0;
      const used = budget.usedAmount || 0;
      const variance = total - used;
      const utilizationRate = total > 0
        ? Math.round((used / total) * 1000) / 10
        : 0;

      return {
        departmentId: budget.id,
        department: budget.departmentName,
        budgetAmount: total,
        actualAmount: used,
        variance: variance,
        // 技術的負債: BudgetServiceと重複した計算
        utilizationRate: utilizationRate,
        statusClass: this.getUtilizationStatusClass(utilizationRate),
        isExpanded: false,
        monthlyBreakdown: []
      };
    });

    this.calculateTotals();
    this.applyFilter();
  }

  /**
   * 合計を計算する
   *
   * 技術的負債: BudgetServiceにも同じロジックが存在
   */
  private calculateTotals(): void {
    // 技術的負債: BudgetServiceと重複した集計ロジック
    this.totalBudget = this.budgetActualRows.reduce((sum, row) => sum + row.budgetAmount, 0);
    this.totalActual = this.budgetActualRows.reduce((sum, row) => sum + row.actualAmount, 0);
    this.totalVariance = this.totalBudget - this.totalActual;
    this.totalUtilizationRate = this.totalBudget > 0
      ? Math.round((this.totalActual / this.totalBudget) * 1000) / 10
      : 0;
  }

  /** フィルター適用 */
  applyFilter(): void {
    if (this.selectedDepartment) {
      this.filteredRows = this.budgetActualRows.filter(
        row => row.department === this.selectedDepartment
      );
    } else {
      this.filteredRows = [...this.budgetActualRows];
    }
  }

  /** 年度変更 */
  onFiscalYearChange(): void {
    this.loadBudgetActual();
  }

  /** 部門フィルター変更 */
  onDepartmentChange(): void {
    this.applyFilter();
  }

  /**
   * ドリルダウン: 部門をクリックして月次内訳を表示
   *
   * 技術的負債: ドリルダウン状態がコンポーネント変数で管理されている
   * routerのqueryParamsやstateで管理すべき
   */
  toggleDrillDown(row: BudgetActualRow): void {
    if (row.isExpanded) {
      // 技術的負債: コンポーネント変数による状態管理
      row.isExpanded = false;
      this.expandedDepartmentId = null;
      return;
    }

    // 他の展開を閉じる（技術的負債: コンポーネント変数による状態管理）
    this.filteredRows.forEach(r => { r.isExpanded = false; });

    // 月次データを読み込む
    if (row.monthlyBreakdown.length === 0) {
      this.loadMonthlyBreakdown(row);
    }

    row.isExpanded = true;
    this.expandedDepartmentId = row.departmentId;
  }

  /** 月次内訳を読み込む */
  private loadMonthlyBreakdown(row: BudgetActualRow): void {
    this.reportService.getBudgetReport(this.selectedFiscalYear).subscribe({
      next: (result) => {
        const departmentData = result.data?.departments?.find(
          (d: any) => d.departmentId === row.departmentId
        );
        const monthlyData = departmentData?.monthlyBreakdown || [];

        row.monthlyBreakdown = monthlyData.map((item: any) => {
          const variance = (item.budgetAmount || 0) - (item.actualAmount || 0);
          const utilizationRate = item.budgetAmount > 0
            ? Math.round((item.actualAmount / item.budgetAmount) * 1000) / 10
            : 0;

          return {
            month: item.month,
            monthLabel: item.monthLabel || item.month,
            budgetAmount: item.budgetAmount || 0,
            actualAmount: item.actualAmount || 0,
            variance: variance,
            utilizationRate: utilizationRate,
            statusClass: this.getUtilizationStatusClass(utilizationRate)
          };
        });
      },
      error: (err) => {
        console.error('月次内訳データ取得エラー:', err);
        this.errorMessage = '月次内訳データの取得に失敗しました。';
      }
    });
  }

  /**
   * 消化率に応じたステータスクラスを返す
   *
   * 技術的負債: BudgetServiceにも同様のロジックが存在する可能性
   * 色分けルール: 緑 < 80%、黄 80-95%、赤 > 95%
   */
  getUtilizationStatusClass(rate: number): string {
    if (rate > 95) return 'status-danger';
    if (rate >= 80) return 'status-warning';
    return 'status-safe';
  }

  /** 通貨フォーマット */
  formatCurrency(value: number): string {
    return '¥' + value.toLocaleString('ja-JP');
  }

  /** 数値フォーマット */
  formatNumber(value: number): string {
    return value.toLocaleString('ja-JP');
  }
}
