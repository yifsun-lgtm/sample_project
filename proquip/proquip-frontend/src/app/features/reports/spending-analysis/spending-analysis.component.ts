import { Component, OnInit } from '@angular/core';
import { ReportService } from '@shared/services/report.service';
import { ApiService } from '@shared/services/api.service';

/**
 * 支出サマリー
 */
export interface SpendingSummary {
  totalSpending: number;
  orderCount: number;
  averageOrderValue: number;
  topCategory: string;
  topCategorySpending: number;
  periodLabel: string;
}

/**
 * 支出明細行
 */
export interface SpendingBreakdownItem {
  name: string;
  type: string;
  orderCount: number;
  totalAmount: number;
  percentage: number;
}

/**
 * 月次トレンドデータ
 */
export interface MonthlyTrend {
  month: string;
  label: string;
  amount: number;
  percentage: number;
}

/**
 * 支出分析コンポーネント
 * 期間別・部門別・カテゴリ別・サプライヤー別の支出分析を表示する
 *
 * 技術的負債: レポートデータの変換処理がコンポーネント内に約100行存在
 * サービスに切り出してテスト可能にすべき
 *
 * 技術的負債 #6: 日付フォーマットにパイプを使わず手動でフォーマットしている
 */
@Component({
  selector: 'app-spending-analysis',
  templateUrl: './spending-analysis.component.html',
  styleUrls: ['./spending-analysis.component.scss']
})
export class SpendingAnalysisComponent implements OnInit {

  /** フィルター: 開始日 */
  startDate = '';

  /** フィルター: 終了日 */
  endDate = '';

  /** フィルター: 部門 */
  selectedDepartment = '';

  /** フィルター: カテゴリ */
  selectedCategory = '';

  /** フィルター: サプライヤー */
  selectedSupplier = '';

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

  /** カテゴリオプション */
  categoryOptions = [
    { value: '', label: 'すべてのカテゴリ' },
    { value: '事務用品', label: '事務用品' },
    { value: 'IT機器', label: 'IT機器' },
    { value: '設備', label: '設備' },
    { value: '消耗品', label: '消耗品' },
    { value: 'サービス', label: 'サービス' }
  ];

  /** サプライヤーオプション */
  supplierOptions = [
    { value: '', label: 'すべてのサプライヤー' },
    { value: 'オフィスデポ', label: 'オフィスデポ' },
    { value: 'アスクル', label: 'アスクル' },
    { value: 'カウネット', label: 'カウネット' },
    { value: 'たのめーる', label: 'たのめーる' }
  ];

  /** サマリーデータ */
  summary: SpendingSummary | null = null;

  /** 内訳データ */
  breakdownItems: SpendingBreakdownItem[] = [];

  /** 表示グループ: 'category' | 'supplier' | 'department' */
  breakdownGroupBy = 'category';

  /** 月次トレンドデータ */
  monthlyTrends: MonthlyTrend[] = [];

  /** ローディング */
  isLoading = false;

  /** エクスポート中フラグ */
  isExporting = false;

  /** ソートカラム */
  sortColumn = 'totalAmount';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'desc';

  /** エラーメッセージ */
  errorMessage = '';

  constructor(
    private reportService: ReportService,
    private api: ApiService
  ) {}

  ngOnInit(): void {
    this.initDateRange();
    this.loadReport();
  }

  /**
   * 日付範囲の初期化
   *
   * 技術的負債 #6: パイプを使わず手動でフォーマットしている
   */
  private initDateRange(): void {
    const now = new Date();
    // 技術的負債 #6: 手動で日付フォーマット
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    this.endDate = `${year}-${month}-${String(now.getDate()).padStart(2, '0')}`;

    // 3ヶ月前
    const threeMonthsAgo = new Date(now);
    threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
    const startYear = threeMonthsAgo.getFullYear();
    const startMonth = String(threeMonthsAgo.getMonth() + 1).padStart(2, '0');
    this.startDate = `${startYear}-${startMonth}-01`;
  }

  /** レポートを読み込む */
  loadReport(): void {
    if (!this.startDate || !this.endDate) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.reportService.getProcurementSummary(this.startDate, this.endDate).subscribe({
      next: (result) => {
        this.transformReportData(result.data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('支出分析レポート取得エラー:', err);
        this.errorMessage = '支出分析レポートの取得に失敗しました。';
        this.isLoading = false;
      }
    });
  }

  /**
   * レポートデータを変換する
   *
   * 技術的負債: データ変換処理がコンポーネント内に約100行存在
   * ReportTransformerServiceなどに切り出すべき
   */
  private transformReportData(rawData: any): void {
    // 技術的負債: データ変換ロジックがコンポーネント内
    if (!rawData) {
      this.errorMessage = '支出分析レポートのデータが空です。';
      return;
    }

    // サマリー変換
    this.summary = {
      totalSpending: rawData.totalSpending || 0,
      orderCount: rawData.orderCount || 0,
      averageOrderValue: rawData.averageOrderValue || 0,
      topCategory: rawData.topCategory || '',
      topCategorySpending: rawData.topCategorySpending || 0,
      // 技術的負債 #6: 手動で日付をフォーマット
      periodLabel: this.formatDateRange(this.startDate, this.endDate)
    };

    // 内訳データ変換
    this.transformBreakdownData(rawData);

    // 月次トレンド変換
    this.transformMonthlyTrends(rawData.monthlyTrends || []);
  }

  /**
   * 内訳データの変換
   *
   * 技術的負債: ビジネスロジックがコンポーネント内
   */
  private transformBreakdownData(rawData: any): void {
    const dataKey = this.breakdownGroupBy === 'category' ? 'categoryBreakdown'
      : this.breakdownGroupBy === 'supplier' ? 'supplierBreakdown'
      : 'departmentBreakdown';

    const items = rawData[dataKey] || [];
    const total = items.reduce((sum: number, item: any) => sum + (item.totalAmount || 0), 0);

    // 技術的負債: 変換ロジックがコンポーネント内
    this.breakdownItems = items.map((item: any) => ({
      name: item.name || '',
      type: this.breakdownGroupBy,
      orderCount: item.orderCount || 0,
      totalAmount: item.totalAmount || 0,
      percentage: total > 0 ? Math.round((item.totalAmount / total) * 1000) / 10 : 0
    }));

    this.sortBreakdownItems();
  }

  /**
   * 月次トレンドの変換
   *
   * 技術的負債 #6: 月ラベルを手動フォーマット
   */
  private transformMonthlyTrends(rawTrends: any[]): void {
    if (!rawTrends || rawTrends.length === 0) {
      this.monthlyTrends = [];
      return;
    }

    const maxAmount = Math.max(...rawTrends.map((t: any) => t.amount || 0));

    this.monthlyTrends = rawTrends.map((trend: any) => ({
      month: trend.month,
      // 技術的負債 #6: 手動で月名をフォーマット
      label: this.formatMonthLabel(trend.month),
      amount: trend.amount || 0,
      percentage: maxAmount > 0 ? Math.round((trend.amount / maxAmount) * 100) : 0
    }));
  }

  /**
   * 日付範囲の表示フォーマット
   *
   * 技術的負債 #6: JapaneseDatePipeを使うべき
   */
  private formatDateRange(start: string, end: string): string {
    // 技術的負債: パイプの代わりに手動フォーマット
    const s = new Date(start);
    const e = new Date(end);
    const sy = s.getFullYear();
    const sm = s.getMonth() + 1;
    const sd = s.getDate();
    const ey = e.getFullYear();
    const em = e.getMonth() + 1;
    const ed = e.getDate();
    return `${sy}年${sm}月${sd}日 〜 ${ey}年${em}月${ed}日`;
  }

  /**
   * 月ラベルのフォーマット
   *
   * 技術的負債 #6: パイプを使わない手動フォーマット
   */
  private formatMonthLabel(monthStr: string): string {
    // 技術的負債: YYYY-MM形式を手動パース
    const parts = monthStr.split('-');
    if (parts.length >= 2) {
      return `${parts[0]}年${parseInt(parts[1], 10)}月`;
    }
    return monthStr;
  }

  /** フィルターの適用 */
  applyFilters(): void {
    this.loadReport();
  }

  /** フィルターのリセット */
  resetFilters(): void {
    this.selectedDepartment = '';
    this.selectedCategory = '';
    this.selectedSupplier = '';
    this.initDateRange();
    this.loadReport();
  }

  /** グループ変更 */
  onGroupByChange(): void {
    this.loadReport();
  }

  /** ソート変更 */
  onSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'desc';
    }
    this.sortBreakdownItems();
  }

  /** ソートアイコン取得 */
  getSortIcon(column: string): string {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  /** 内訳データのソート */
  private sortBreakdownItems(): void {
    this.breakdownItems.sort((a, b) => {
      const valA = (a as any)[this.sortColumn];
      const valB = (b as any)[this.sortColumn];
      const cmp = typeof valA === 'string' ? valA.localeCompare(valB) : valA - valB;
      return this.sortDirection === 'desc' ? -cmp : cmp;
    });
  }

  /** CSV エクスポート */
  exportCsv(): void {
    this.isExporting = true;
    this.reportService.exportCsv('spending', {
      startDate: this.startDate,
      endDate: this.endDate,
      department: this.selectedDepartment,
      category: this.selectedCategory,
      supplier: this.selectedSupplier
    }).subscribe({
      next: (blob) => {
        this.downloadBlob(blob, `支出分析_${this.startDate}_${this.endDate}.csv`);
        this.isExporting = false;
      },
      error: (err) => {
        console.error('CSVエクスポートエラー:', err);
        this.errorMessage = 'CSVエクスポートに失敗しました。';
        this.isExporting = false;
        setTimeout(() => { this.errorMessage = ''; }, 3000);
      }
    });
  }

  /** Blobデータのダウンロード */
  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
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
