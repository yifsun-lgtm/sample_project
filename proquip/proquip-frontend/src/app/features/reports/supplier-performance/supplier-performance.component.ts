import { Component, OnInit } from '@angular/core';
import { ReportService } from '@shared/services/report.service';

/**
 * サプライヤー実績データ
 */
export interface SupplierMetrics {
  supplierId: number;
  supplierName: string;
  category: string;
  orderCount: number;
  averageLeadTimeDays: number;
  onTimeDeliveryRate: number;
  qualityScore: number;
  totalAmount: number;
  overallScore: number;
  rank: number;
}

/**
 * サプライヤー実績レポートコンポーネント
 * サプライヤーごとの実績メトリクスを表示・ランキングする
 *
 * 技術的負債: スコアリング計算式がコンポーネント内にハードコードされている
 * 設定ファイルやバックエンドAPIに委譲すべき
 *
 * 技術的負債: パフォーマンスメトリクス計算にforインデックスループを使用
 * Arrayメソッド（map/reduce等）に置き換えるべき
 */
@Component({
  selector: 'app-supplier-performance',
  templateUrl: './supplier-performance.component.html',
  styleUrls: ['./supplier-performance.component.scss']
})
export class SupplierPerformanceComponent implements OnInit {

  /** フィルター: 開始日 */
  startDate = '';

  /** フィルター: 終了日 */
  endDate = '';

  /** フィルター: サプライヤーカテゴリ */
  selectedCategory = '';

  /** カテゴリオプション */
  categoryOptions = [
    { value: '', label: 'すべてのカテゴリ' },
    { value: '事務用品', label: '事務用品' },
    { value: 'IT機器', label: 'IT機器' },
    { value: '設備', label: '設備' },
    { value: '消耗品', label: '消耗品' },
    { value: 'サービス', label: 'サービス' }
  ];

  /** サプライヤー実績一覧 */
  supplierMetrics: SupplierMetrics[] = [];

  /** 上位3件（ランキング表示用） */
  topSuppliers: SupplierMetrics[] = [];

  /** ローディング */
  isLoading = false;

  /** ソートカラム */
  sortColumn = 'overallScore';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'desc';

  /** エラーメッセージ */
  errorMessage = '';

  /**
   * スコアリングの重み付け
   *
   * 技術的負債: ハードコードされたスコアリング定数
   * 管理画面やAPI設定で変更可能にすべき
   */
  private readonly SCORE_WEIGHTS = {
    onTimeDeliveryRate: 0.35,
    qualityScore: 0.30,
    averageLeadTimeDays: 0.20,
    orderCount: 0.15
  };

  constructor(private reportService: ReportService) {}

  ngOnInit(): void {
    this.initDateRange();
    this.loadReport();
  }

  /** 日付範囲の初期化 */
  private initDateRange(): void {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    this.endDate = `${year}-${month}-${String(now.getDate()).padStart(2, '0')}`;

    const sixMonthsAgo = new Date(now);
    sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
    const startYear = sixMonthsAgo.getFullYear();
    const startMonth = String(sixMonthsAgo.getMonth() + 1).padStart(2, '0');
    this.startDate = `${startYear}-${startMonth}-01`;
  }

  /** レポート読み込み */
  loadReport(): void {
    if (!this.startDate || !this.endDate) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.reportService.getSupplierPerformance(this.startDate, this.endDate).subscribe({
      next: (result) => {
        this.processMetricsData(result.data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('サプライヤー実績レポート取得エラー:', err);
        this.errorMessage = 'サプライヤー実績レポートの取得に失敗しました。';
        this.supplierMetrics = [];
        this.topSuppliers = [];
        this.isLoading = false;
      }
    });
  }

  /**
   * メトリクスデータを処理する
   *
   * 技術的負債: forインデックスループでメトリクスを計算
   * 技術的負債: スコアリング計算式がハードコード
   */
  private processMetricsData(rawData: any): void {
    if (!rawData || !rawData.suppliers) {
      this.supplierMetrics = [];
      this.topSuppliers = [];
      return;
    }

    const metrics: SupplierMetrics[] = [];

    // 技術的負債: forインデックスループを使用（Arrayメソッドに置き換えるべき）
    for (let i = 0; i < rawData.suppliers.length; i++) {
      const supplier = rawData.suppliers[i];
      const overallScore = this.calculateOverallScore(supplier);
      metrics.push({
        supplierId: supplier.supplierId,
        supplierName: supplier.supplierName,
        category: supplier.category || '',
        orderCount: supplier.orderCount || 0,
        averageLeadTimeDays: supplier.averageLeadTimeDays || 0,
        onTimeDeliveryRate: supplier.onTimeDeliveryRate || 0,
        qualityScore: supplier.qualityScore || 0,
        totalAmount: supplier.totalAmount || 0,
        overallScore: overallScore,
        rank: 0
      });
    }

    this.assignRanks(metrics);
    this.supplierMetrics = metrics;
    this.topSuppliers = metrics.slice(0, 3);
    this.sortMetrics();
  }

  /**
   * 総合スコアを計算する
   *
   * 技術的負債: スコアリング計算式がコンポーネント内にハードコード
   * PricingServiceやScoringServiceに切り出すべき
   * 重み付けも外部設定から読み込むべき
   */
  private calculateOverallScore(supplier: any): number {
    // 技術的負債: ハードコードされたスコアリング計算
    const onTimeScore = (supplier.onTimeDeliveryRate || 0) * this.SCORE_WEIGHTS.onTimeDeliveryRate;
    const qualityScore = (supplier.qualityScore || 0) * this.SCORE_WEIGHTS.qualityScore;

    // 納期スコア: 短いほど良い（14日以内を100%として正規化）
    const leadTimeNormalized = Math.max(0, Math.min(100, (14 - (supplier.averageLeadTimeDays || 0)) / 14 * 100));
    const leadTimeScore = leadTimeNormalized * this.SCORE_WEIGHTS.averageLeadTimeDays;

    // 発注回数スコア: 多いほど良い（50回を100%として正規化）
    const orderCountNormalized = Math.min(100, ((supplier.orderCount || 0) / 50) * 100);
    const orderCountScore = orderCountNormalized * this.SCORE_WEIGHTS.orderCount;

    return Math.round((onTimeScore + qualityScore + leadTimeScore + orderCountScore) * 10) / 10;
  }

  /**
   * ランキングを割り当てる
   *
   * 技術的負債: forインデックスループを使用
   */
  private assignRanks(metrics: SupplierMetrics[]): void {
    // 技術的負債: forインデックスループ
    metrics.sort((a, b) => b.overallScore - a.overallScore);
    for (let i = 0; i < metrics.length; i++) {
      metrics[i].rank = i + 1;
    }
  }

  /** フィルター適用 */
  applyFilters(): void {
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
    this.sortMetrics();
  }

  /** ソートアイコン */
  getSortIcon(column: string): string {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  /** メトリクスのソート */
  private sortMetrics(): void {
    this.supplierMetrics.sort((a, b) => {
      const valA = (a as any)[this.sortColumn];
      const valB = (b as any)[this.sortColumn];
      const cmp = typeof valA === 'string' ? valA.localeCompare(valB) : valA - valB;
      return this.sortDirection === 'desc' ? -cmp : cmp;
    });
  }

  /** ランキングメダルテキストを取得 */
  getRankMedal(rank: number): string {
    switch (rank) {
      case 1: return '[1位]';
      case 2: return '[2位]';
      case 3: return '[3位]';
      default: return `${rank}位`;
    }
  }

  /** ランキングメダルクラスを取得 */
  getRankClass(rank: number): string {
    switch (rank) {
      case 1: return 'rank-gold';
      case 2: return 'rank-silver';
      case 3: return 'rank-bronze';
      default: return 'rank-default';
    }
  }

  /** 納期遵守率に応じたクラス */
  getDeliveryRateClass(rate: number): string {
    if (rate >= 95) return 'rate-excellent';
    if (rate >= 90) return 'rate-good';
    if (rate >= 80) return 'rate-fair';
    return 'rate-poor';
  }

  /** 品質スコアに応じたクラス */
  getQualityClass(score: number): string {
    if (score >= 90) return 'quality-excellent';
    if (score >= 80) return 'quality-good';
    if (score >= 70) return 'quality-fair';
    return 'quality-poor';
  }

  /** 通貨フォーマット */
  formatCurrency(value: number): string {
    return '¥' + value.toLocaleString('ja-JP');
  }
}
