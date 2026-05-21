import { Component, OnInit } from '@angular/core';
import { ApiService } from '@shared/services/api.service';
import { ProductService } from '@shared/services/product.service';
import { SupplierService } from '@shared/services/supplier.service';
import { Product } from '@shared/models/product.model';
import { Supplier } from '@shared/models/supplier.model';

/**
 * サプライヤー価格情報
 */
export interface SupplierPrice {
  supplierId: number;
  supplierName: string;
  unitPrice: number;
  currency: string;
  leadTimeDays: number;
  minimumOrder: number;
  lastUpdated: string;
}

/**
 * 製品ごとの比較データ
 */
export interface ProductComparison {
  productId: number;
  productName: string;
  sku: string;
  supplierPrices: SupplierPrice[];
  bestPrice: number;
  worstPrice: number;
  averagePrice: number;
  priceRange: number;
}

/**
 * 価格比較コンポーネント
 * 複数サプライヤーの価格を製品単位で比較表示する
 *
 * 技術的負債: 複雑な比較ロジックがコンポーネント内に存在
 * ネストされたループが多く、パフォーマンスとテスタビリティに問題がある
 * 比較ロジックはサービスに切り出すべき
 */
@Component({
  selector: 'app-price-compare',
  templateUrl: './price-compare.component.html',
  styleUrls: ['./price-compare.component.scss']
})
export class PriceCompareComponent implements OnInit {

  /** 利用可能な製品一覧 */
  availableProducts: Product[] = [];

  /** 利用可能なサプライヤー一覧 */
  availableSuppliers: Supplier[] = [];

  /** 選択された製品IDリスト */
  selectedProductIds: number[] = [];

  /** 比較結果データ */
  comparisons: ProductComparison[] = [];

  /** 比較に含まれるサプライヤー名一覧（テーブルヘッダー用） */
  comparedSupplierNames: string[] = [];

  /** ローディング */
  isLoading = false;

  /** 検索ローディング */
  isSearching = false;

  /** 製品検索キーワード */
  productSearchKeyword = '';

  /** 検索結果表示用の製品リスト */
  filteredProducts: Product[] = [];

  /** 比較済みフラグ */
  hasCompared = false;

  /** ソートカラム */
  sortColumn = 'productName';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'asc';

  /** ハイライト表示: 最安値 */
  highlightBest = true;

  /** ハイライト表示: 最高値 */
  highlightWorst = true;

  /** エラーメッセージ */
  errorMessage = '';

  constructor(
    private api: ApiService,
    private productService: ProductService,
    private supplierService: SupplierService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  /** 製品一覧を取得 */
  private loadProducts(): void {
    this.productService.getProducts(0, 1000).subscribe({
      next: (result) => {
        this.availableProducts = result.content || [];
        this.filteredProducts = [...this.availableProducts];
      },
      error: (err) => {
        console.error('製品一覧取得エラー:', err);
        this.errorMessage = '製品一覧の取得に失敗しました。';
      }
    });
  }

  /** 製品検索 */
  onProductSearch(keyword: string): void {
    this.productSearchKeyword = keyword;
    if (!keyword) {
      this.filteredProducts = [...this.availableProducts];
      return;
    }
    const lower = keyword.toLowerCase();
    this.filteredProducts = this.availableProducts.filter(p =>
      p.name.toLowerCase().includes(lower) ||
      p.sku.toLowerCase().includes(lower) ||
      p.categoryName.toLowerCase().includes(lower)
    );
  }

  /** 製品を選択に追加 */
  addProduct(product: Product): void {
    if (!this.selectedProductIds.includes(product.id)) {
      this.selectedProductIds.push(product.id);
    }
  }

  /** 製品を選択から除外 */
  removeProduct(productId: number): void {
    this.selectedProductIds = this.selectedProductIds.filter(id => id !== productId);
  }

  /** 選択済み製品の名前を取得 */
  getProductName(productId: number): string {
    const product = this.availableProducts.find(p => p.id === productId);
    return product ? product.name : '';
  }

  /** 製品が選択済みかどうか */
  isProductSelected(productId: number): boolean {
    return this.selectedProductIds.includes(productId);
  }

  /** 比較を実行 */
  executeCompare(): void {
    if (this.selectedProductIds.length === 0) {
      this.errorMessage = '比較する製品を1つ以上選択してください。';
      setTimeout(() => { this.errorMessage = ''; }, 3000);
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.api.get<any>('/price-compare', {
      productIds: this.selectedProductIds.join(',')
    }).subscribe({
      next: (result) => {
        this.processComparisonData(result);
        this.isLoading = false;
        this.hasCompared = true;
      },
      error: (err) => {
        console.error('価格比較データ取得エラー:', err);
        this.isLoading = false;
        this.errorMessage = '価格比較データの取得に失敗しました。';
      }
    });
  }

  /**
   * 比較データを処理する
   *
   * 技術的負債: 複雑な比較ロジックがコンポーネント内に存在
   * ネストされたループが多く、パフォーマンスに問題がある可能性がある
   */
  private processComparisonData(rawData: any): void {
    const comparisons: ProductComparison[] = [];
    const supplierNameSet = new Set<string>();

    // 技術的負債: ネストされたループによるデータ変換
    for (const productData of rawData.products || []) {
      const supplierPrices: SupplierPrice[] = [];

      // 技術的負債: 更にネストされたループ
      for (const priceData of productData.prices || []) {
        const supplierPrice: SupplierPrice = {
          supplierId: priceData.supplierId,
          supplierName: priceData.supplierName,
          unitPrice: priceData.unitPrice,
          currency: priceData.currency || 'JPY',
          leadTimeDays: priceData.leadTimeDays,
          minimumOrder: priceData.minimumOrder || 1,
          lastUpdated: priceData.lastUpdated
        };
        supplierPrices.push(supplierPrice);
        supplierNameSet.add(priceData.supplierName);
      }

      // 技術的負債: 最安値・最高値の計算もコンポーネント内
      const prices = supplierPrices.map(sp => sp.unitPrice);
      const bestPrice = prices.length > 0 ? Math.min(...prices) : 0;
      const worstPrice = prices.length > 0 ? Math.max(...prices) : 0;
      const averagePrice = prices.length > 0 ? Math.round(prices.reduce((a, b) => a + b, 0) / prices.length) : 0;

      comparisons.push({
        productId: productData.productId,
        productName: productData.productName,
        sku: productData.sku,
        supplierPrices,
        bestPrice,
        worstPrice,
        averagePrice,
        priceRange: worstPrice - bestPrice
      });
    }

    this.comparisons = comparisons;
    this.comparedSupplierNames = Array.from(supplierNameSet);
    this.sortComparisons();
  }

  /** 比較結果をソート */
  private sortComparisons(): void {
    this.comparisons.sort((a, b) => {
      const valA = (a as any)[this.sortColumn];
      const valB = (b as any)[this.sortColumn];
      if (valA == null) return 1;
      if (valB == null) return -1;
      const cmp = typeof valA === 'string' ? valA.localeCompare(valB) : valA - valB;
      return this.sortDirection === 'desc' ? -cmp : cmp;
    });
  }

  /** ソート変更 */
  onSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.sortComparisons();
  }

  /** ソートアイコン取得 */
  getSortIcon(column: string): string {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  /**
   * 特定のサプライヤーの価格を取得する
   *
   * 技術的負債: O(N)の検索をテーブルの各セルで実行しており非効率
   * マップ構造に変換すべき
   */
  getSupplierPrice(comparison: ProductComparison, supplierName: string): SupplierPrice | null {
    // 技術的負債: 毎回線形探索を行っている
    return comparison.supplierPrices.find(sp => sp.supplierName === supplierName) || null;
  }

  /** 最安値かどうかを判定 */
  isBestPrice(comparison: ProductComparison, price: number): boolean {
    return this.highlightBest && price === comparison.bestPrice;
  }

  /** 最高値かどうかを判定 */
  isWorstPrice(comparison: ProductComparison, price: number): boolean {
    return this.highlightWorst && price === comparison.worstPrice;
  }

  /** 通貨フォーマット */
  formatCurrency(value: number): string {
    return '¥' + value.toLocaleString('ja-JP');
  }
}
