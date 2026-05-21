import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SupplierService } from '@shared/services/supplier.service';
import { Supplier, SupplierContract } from '@shared/models/supplier.model';

/**
 * サプライヤー比較メトリクスのインターフェース
 */
interface SupplierMetrics {
  supplier: Supplier;
  averageRating: number;
  qualityRating: number;
  deliveryRating: number;
  priceRating: number;
  communicationRating: number;
  activeProductCount: number;
  activeContractCount: number;
  totalContractValue: number;
  averageLeadTimeDays: number;
  onTimeDeliveryRate: number;
  defectRate: number;
  contracts: SupplierContract[];
}

/**
 * 比較基準のインターフェース
 */
interface ComparisonCriteria {
  key: string;
  label: string;
  type: 'number' | 'percentage' | 'currency' | 'rating' | 'text';
  higherIsBetter: boolean;
}

/**
 * サプライヤー比較コンポーネント
 * 最大3社のサプライヤーを並べて比較
 *
 * 技術的負債: 複雑な比較ロジックがコンポーネント内にある
 * 技術的負債: 比較基準がハードコード
 */
@Component({
  selector: 'app-supplier-compare',
  templateUrl: './supplier-compare.component.html',
  styleUrls: ['./supplier-compare.component.scss']
})
export class SupplierCompareComponent implements OnInit {

  /** 比較対象のサプライヤーID */
  supplierIds: number[] = [];

  /** 比較メトリクスデータ */
  metrics: SupplierMetrics[] = [];

  /** ローディング状態 */
  isLoading = true;

  /** エラーメッセージ */
  errorMessage = '';

  /**
   * 比較基準定義
   * 技術的負債: ハードコードされた比較基準
   */
  comparisonCriteria: ComparisonCriteria[] = [
    { key: 'averageRating', label: '総合評価', type: 'rating', higherIsBetter: true },
    { key: 'qualityRating', label: '品質評価', type: 'rating', higherIsBetter: true },
    { key: 'deliveryRating', label: '納期評価', type: 'rating', higherIsBetter: true },
    { key: 'priceRating', label: '価格評価', type: 'rating', higherIsBetter: true },
    { key: 'communicationRating', label: 'コミュニケーション', type: 'rating', higherIsBetter: true },
    { key: 'activeProductCount', label: '取扱製品数', type: 'number', higherIsBetter: true },
    { key: 'activeContractCount', label: '有効契約数', type: 'number', higherIsBetter: true },
    { key: 'totalContractValue', label: '契約総額', type: 'currency', higherIsBetter: false },
    { key: 'averageLeadTimeDays', label: '平均リードタイム', type: 'number', higherIsBetter: false },
    { key: 'onTimeDeliveryRate', label: '納期遵守率', type: 'percentage', higherIsBetter: true },
    { key: 'defectRate', label: '不良率', type: 'percentage', higherIsBetter: false }
  ];

  /** サプライヤー追加用: 検索キーワード */
  addSearchKeyword = '';

  /** サプライヤー追加用: 検索結果 */
  searchResults: Supplier[] = [];

  /** 全サプライヤーリスト（検索用） */
  allSuppliers: Supplier[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private supplierService: SupplierService
  ) {}

  ngOnInit(): void {
    // クエリパラメータからサプライヤーIDを取得
    const idsParam = this.route.snapshot.queryParamMap.get('ids');
    if (idsParam) {
      this.supplierIds = idsParam.split(',').map(id => Number(id)).filter(id => !isNaN(id));
    }

    this.loadAllSuppliers();

    if (this.supplierIds.length >= 2) {
      this.loadComparisonData();
    } else {
      this.isLoading = false;
    }
  }

  /**
   * 全サプライヤーをロード（追加検索用）
   */
  private loadAllSuppliers(): void {
    this.supplierService.getSuppliers(0, 100).subscribe(
      (result) => {
        this.allSuppliers = result.content;
      },
      (error) => {
        console.error('サプライヤー一覧取得エラー:', error);
      }
    );
  }

  /**
   * 比較データをロード
   */
  private loadComparisonData(): void {
    this.isLoading = true;
    this.metrics = [];
    this.errorMessage = '';

    this.supplierService.compareSuppliers(this.supplierIds).subscribe(
      (results: any[]) => {
        this.metrics = results.map(r => this.mapMetrics(r));
        this.isLoading = false;
      },
      (error) => {
        console.error('比較データ取得エラー:', error);
        this.errorMessage = '比較データの取得に失敗しました。';
        this.metrics = [];
        this.isLoading = false;
      }
    );
  }

  /**
   * APIレスポンスをメトリクスにマッピング
   */
  private mapMetrics(data: any): SupplierMetrics {
    return {
      supplier: data.supplier,
      averageRating: data.averageRating || 0,
      qualityRating: data.qualityRating || 0,
      deliveryRating: data.deliveryRating || 0,
      priceRating: data.priceRating || 0,
      communicationRating: data.communicationRating || 0,
      activeProductCount: data.activeProductCount || 0,
      activeContractCount: data.activeContractCount || 0,
      totalContractValue: data.totalContractValue || 0,
      averageLeadTimeDays: data.averageLeadTimeDays || 0,
      onTimeDeliveryRate: data.onTimeDeliveryRate || 0,
      defectRate: data.defectRate || 0,
      contracts: data.contracts || []
    };
  }

  /**
   * メトリクス値を取得
   */
  getMetricValue(metric: SupplierMetrics, key: string): any {
    return (metric as any)[key];
  }

  /**
   * メトリクス値をフォーマット
   */
  formatMetricValue(value: any, type: string): string {
    switch (type) {
      case 'rating':
        return value != null ? value.toFixed(1) : '-';
      case 'percentage':
        return value != null ? value.toFixed(1) + '%' : '-';
      case 'currency':
        return value != null ? '¥' + value.toLocaleString('ja-JP') : '-';
      case 'number':
        return value != null ? value.toString() : '-';
      default:
        return value?.toString() || '-';
    }
  }

  /**
   * 最良の値かどうかを判定
   * 技術的負債: 比較ロジックがコンポーネント内に直接実装
   */
  isBestValue(criteria: ComparisonCriteria, metricIndex: number): boolean {
    if (this.metrics.length < 2) return false;

    const values = this.metrics.map(m => this.getMetricValue(m, criteria.key));
    const currentValue = values[metricIndex];

    if (currentValue == null) return false;

    if (criteria.higherIsBetter) {
      return currentValue === Math.max(...values.filter((v: any) => v != null));
    } else {
      return currentValue === Math.min(...values.filter((v: any) => v != null));
    }
  }

  /**
   * 評価バーの幅を計算（0-5のスケールを0-100%に変換）
   */
  getRatingBarWidth(value: number): number {
    return Math.min(100, (value / 5) * 100);
  }

  /**
   * サプライヤー検索
   */
  searchSuppliers(): void {
    if (!this.addSearchKeyword) {
      this.searchResults = [];
      return;
    }

    const keyword = this.addSearchKeyword.toLowerCase();
    this.searchResults = this.allSuppliers.filter(s =>
      !this.supplierIds.includes(s.id) &&
      (s.name.toLowerCase().includes(keyword) || s.code.toLowerCase().includes(keyword))
    ).slice(0, 5);
  }

  /**
   * サプライヤーを比較に追加
   */
  addSupplier(supplier: Supplier): void {
    if (this.supplierIds.length >= 3) return;

    this.supplierIds.push(supplier.id);
    this.addSearchKeyword = '';
    this.searchResults = [];

    // URLを更新
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ids: this.supplierIds.join(',') },
      queryParamsHandling: 'merge'
    });

    this.loadComparisonData();
  }

  /**
   * サプライヤーを比較から除去
   */
  removeSupplier(index: number): void {
    if (this.metrics.length <= 2) return;

    this.supplierIds.splice(index, 1);
    this.metrics.splice(index, 1);

    // URLを更新
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ids: this.supplierIds.join(',') },
      queryParamsHandling: 'merge'
    });
  }

  /**
   * サプライヤー詳細へ遷移
   */
  navigateToSupplier(supplierId: number): void {
    this.router.navigate(['/suppliers', supplierId]);
  }

  /**
   * 一覧へ戻る
   */
  navigateToList(): void {
    this.router.navigate(['/suppliers']);
  }

  /**
   * 評価を星表示に変換
   */
  getRatingStars(rating: number): string {
    const fullStars = Math.floor(rating);
    const halfStar = rating % 1 >= 0.5 ? 1 : 0;
    const emptyStars = 5 - fullStars - halfStar;
    return '★'.repeat(fullStars) + (halfStar ? '☆' : '') + '☆'.repeat(emptyStars);
  }

  /**
   * ステータスラベル
   */
  getStatusLabel(status: string): string {
    const labels: any = {
      'ACTIVE': '取引中',
      'INACTIVE': '取引停止',
      'PENDING': '審査中',
      'BLOCKED': 'ブロック済み'
    };
    return labels[status] || status;
  }
}
