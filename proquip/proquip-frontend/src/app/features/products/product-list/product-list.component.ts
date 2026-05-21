import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProductService } from '@shared/services/product.service';
import { Product, Category } from '@shared/models/product.model';
import { PageResult } from '@shared/models/common.model';
import { getProductStatusLabel } from '@shared/utils/status.utils';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';

/**
 * 製品一覧コンポーネント
 * データテーブルによる製品の一覧表示、フィルタリング、ページネーション、CSV出力
 *
 * 技術的負債 #5: フィルタリング/ソートロジックが一部コンポーネント内に、一部サービスに委譲
 * 技術的負債: 複数箇所でany型を使用
 */
@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit {

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'sku', label: 'SKU', sortable: true, width: '120px', type: 'text' },
    { key: 'name', label: '製品名', sortable: true, type: 'text' },
    { key: 'categoryName', label: 'カテゴリ', sortable: true, width: '140px', type: 'text' },
    { key: 'manufacturerName', label: 'メーカー', sortable: true, width: '140px', type: 'text' },
    { key: 'unitPrice', label: '単価', sortable: true, width: '120px', type: 'currency' },
    { key: 'status', label: 'ステータス', sortable: true, width: '100px', type: 'status' },
    { key: 'stockQuantity', label: '在庫数', sortable: false, width: '80px', type: 'number' }
  ];

  /** 製品データ */
  products: Product[] = [];

  /** 全件数 */
  totalCount = 0;

  /** 現在のページ */
  currentPage = 1;

  /** ページサイズ */
  pageSize = 20;

  /** ソートカラム */
  sortColumn = 'name';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'asc';

  /** ローディング状態 */
  isLoading = false;

  /** フィルター: キーワード */
  searchKeyword = '';

  /** フィルター: カテゴリID */
  selectedCategoryId: any = '';

  /** フィルター: メーカー名 */
  selectedManufacturer: any = '';

  /** フィルター: ステータス */
  selectedStatus: any = '';

  /** カテゴリ一覧（フィルタ用） */
  categories: Category[] = [];

  /** メーカー一覧（フィルタ用） */
  // 技術的負債: any型を使用、メーカーリストをAPIから取得すべき
  manufacturers: any[] = [];

  /** ステータスオプション */
  statusOptions = [
    { value: '', label: 'すべて' },
    ...['ACTIVE', 'INACTIVE', 'DISCONTINUED', 'PENDING']
      .map(s => ({ value: s, label: getProductStatusLabel(s) }))
  ];

  /** CSV出力中フラグ */
  isExporting = false;

  constructor(
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadManufacturers();
    this.loadProducts();
  }

  /**
   * カテゴリ一覧を読み込む
   */
  private loadCategories(): void {
    this.productService.getCategories().subscribe(
      (categories) => {
        this.categories = categories;
      },
      (error) => {
        console.error('カテゴリ取得エラー:', error);
      }
    );
  }

  /**
   * 製品一覧を読み込む（サーバーサイドフィルタリング）
   */
  loadProducts(): void {
    this.isLoading = true;
    const page = this.currentPage - 1;
    const sort = this.sortColumn ? `${this.sortColumn},${this.sortDirection}` : undefined;
    const filters: any = {};
    if (this.searchKeyword) filters.keyword = this.searchKeyword;
    if (this.selectedCategoryId) filters.categoryId = Number(this.selectedCategoryId);
    if (this.selectedManufacturer) filters.manufacturerId = Number(this.selectedManufacturer);
    if (this.selectedStatus) filters.status = this.selectedStatus;

    this.productService.getProducts(page, this.pageSize, sort, filters).subscribe(
      (result: PageResult<Product>) => {
        this.products = result.content;
        this.totalCount = result.totalElements;
        this.isLoading = false;
      },
      (error) => {
        console.error('製品一覧取得エラー:', error);
        this.isLoading = false;
      }
    );
  }

  /**
   * メーカー一覧をAPIから取得
   */
  private loadManufacturers(): void {
    this.productService.getProducts(0, 1000).subscribe(
      (result: PageResult<Product>) => {
        const manufacturerMap = new Map<number, string>();
        result.content.forEach((p: any) => {
          if (p.manufacturerId && p.manufacturerName) {
            manufacturerMap.set(p.manufacturerId, p.manufacturerName);
          }
        });
        this.manufacturers = Array.from(manufacturerMap.entries())
          .sort((a, b) => a[1].localeCompare(b[1]))
          .map(([id, name]) => ({ value: id, label: name }));
      }
    );
  }

  /**
   * キーワード検索を実行
   */
  onSearch(keyword: string): void {
    this.searchKeyword = keyword;
    this.currentPage = 1;
    this.loadProducts();
  }

  /**
   * フィルター変更ハンドラ
   */
  onFilterChange(): void {
    this.currentPage = 1;
    this.loadProducts();
  }

  /**
   * ページ変更ハンドラ
   */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  /**
   * ソート変更ハンドラ
   */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadProducts();
  }

  /**
   * 行クリック: 製品詳細へ遷移
   */
  onRowClick(product: any): void {
    this.router.navigate(['/products', product.id]);
  }

  /**
   * 新規登録画面へ遷移
   */
  navigateToCreate(): void {
    this.router.navigate(['/products', 'new']);
  }

  /**
   * カテゴリ管理画面へ遷移
   */
  navigateToCategories(): void {
    this.router.navigate(['/products', 'categories']);
  }

  /**
   * バンドル管理画面へ遷移
   */
  navigateToBundles(): void {
    this.router.navigate(['/products', 'bundles']);
  }

  /**
   * CSV出力
   * 技術的負債: CSVフォーマットのロジックがコンポーネント内にべた書き
   */
  exportCsv(): void {
    this.isExporting = true;

    // 技術的負債: 全件取得してCSV生成（大量データ時にメモリ問題の可能性）
    this.productService.getProducts(0, 10000).subscribe(
      (result: PageResult<Product>) => {
        const headers = ['SKU', '製品名', 'カテゴリ', 'メーカー', '単価', 'ステータス'];
        const rows = result.content.map((p: any) => [
          p.sku,
          p.name,
          p.categoryName,
          p.manufacturerName,
          p.unitPrice,
          p.status
        ]);

        let csv = '\uFEFF'; // BOMを追加（Excelで文字化け防止）
        csv += headers.join(',') + '\n';
        rows.forEach((row: any) => {
          csv += row.map((cell: any) => {
            const str = String(cell || '');
            // カンマを含む場合はダブルクォートで囲む
            if (str.includes(',') || str.includes('"') || str.includes('\n')) {
              return '"' + str.replace(/"/g, '""') + '"';
            }
            return str;
          }).join(',') + '\n';
        });

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', 'products_' + new Date().toISOString().slice(0, 10) + '.csv');
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        this.isExporting = false;
      },
      (error) => {
        console.error('CSV出力エラー:', error);
        this.isExporting = false;
      }
    );
  }

  /**
   * フィルターをリセット
   */
  resetFilters(): void {
    this.searchKeyword = '';
    this.selectedCategoryId = '';
    this.selectedManufacturer = '';
    this.selectedStatus = '';
    this.currentPage = 1;
    this.loadProducts();
  }
}
