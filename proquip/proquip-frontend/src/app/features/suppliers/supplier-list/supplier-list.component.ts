import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SupplierService } from '@shared/services/supplier.service';
import { Supplier } from '@shared/models/supplier.model';
import { PageResult } from '@shared/models/common.model';
import { TableColumn, PageChangeEvent, SortChangeEvent } from '@shared/components/data-table/data-table.component';

/**
 * サプライヤー一覧コンポーネント
 * データテーブルによるサプライヤーの一覧表示、フィルタリング、ページネーション
 */
@Component({
  selector: 'app-supplier-list',
  templateUrl: './supplier-list.component.html',
  styleUrls: ['./supplier-list.component.scss']
})
export class SupplierListComponent implements OnInit {

  /** テーブルカラム定義 */
  columns: TableColumn[] = [
    { key: 'code', label: 'コード', sortable: true, width: '100px', type: 'text' },
    { key: 'name', label: '会社名', sortable: true, type: 'text' },
    { key: 'status', label: 'ステータス', sortable: true, width: '100px', type: 'status' },
    { key: 'rating', label: '評価', sortable: true, width: '80px', type: 'number' },
    { key: 'email', label: 'メールアドレス', sortable: false, width: '200px', type: 'text' },
    { key: 'phone', label: '電話番号', sortable: false, width: '140px', type: 'text' }
  ];

  /** サプライヤーデータ */
  suppliers: Supplier[] = [];

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

  /** フィルター: ステータス */
  selectedStatus = '';

  /** フィルター: 評価範囲（最低） */
  minRating: number | null = null;

  /** フィルター: 評価範囲（最高） */
  maxRating: number | null = null;

  /** ステータスオプション */
  statusOptions = [
    { value: '', label: 'すべて' },
    { value: 'ACTIVE', label: '取引中' },
    { value: 'INACTIVE', label: '取引停止' },
    { value: 'PENDING', label: '審査中' },
    { value: 'BLOCKED', label: 'ブロック済み' }
  ];

  /** 評価フィルターオプション */
  ratingOptions = [
    { value: null, label: '指定なし' },
    { value: 1, label: '1以上' },
    { value: 2, label: '2以上' },
    { value: 3, label: '3以上' },
    { value: 4, label: '4以上' },
    { value: 5, label: '5' }
  ];

  /** 比較用の選択済みサプライヤー */
  selectedForCompare: number[] = [];

  constructor(
    private supplierService: SupplierService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadSuppliers();
  }

  /**
   * サプライヤー一覧を読み込む
   */
  loadSuppliers(): void {
    this.isLoading = true;
    const page = this.currentPage - 1;

    this.supplierService.getSuppliers(page, this.pageSize).subscribe(
      (result: PageResult<Supplier>) => {
        let filtered = result.content;

        // クライアントサイドフィルタリング
        if (this.searchKeyword) {
          const keyword = this.searchKeyword.toLowerCase();
          filtered = filtered.filter(s =>
            s.name.toLowerCase().includes(keyword) ||
            s.code.toLowerCase().includes(keyword) ||
            s.email.toLowerCase().includes(keyword)
          );
        }

        if (this.selectedStatus) {
          filtered = filtered.filter(s => s.status === this.selectedStatus);
        }

        if (this.minRating !== null) {
          filtered = filtered.filter(s => s.rating >= (this.minRating as number));
        }

        this.suppliers = filtered;
        this.totalCount = result.totalElements;
        this.isLoading = false;
      },
      (error) => {
        console.error('サプライヤー一覧取得エラー:', error);
        this.isLoading = false;
      }
    );
  }

  /**
   * キーワード検索
   */
  onSearch(keyword: string): void {
    this.searchKeyword = keyword;
    this.currentPage = 1;
    this.loadSuppliers();
  }

  /**
   * フィルター変更
   */
  onFilterChange(): void {
    this.currentPage = 1;
    this.loadSuppliers();
  }

  /**
   * ページ変更
   */
  onPageChange(event: PageChangeEvent): void {
    this.currentPage = event.page;
    this.pageSize = event.pageSize;
    this.loadSuppliers();
  }

  /**
   * ソート変更
   */
  onSortChange(event: SortChangeEvent): void {
    this.sortColumn = event.column;
    this.sortDirection = event.direction;
    this.loadSuppliers();
  }

  /**
   * 行クリック: サプライヤー詳細へ遷移
   */
  onRowClick(supplier: any): void {
    this.router.navigate(['/suppliers', supplier.id]);
  }

  /**
   * 新規登録画面へ遷移
   */
  navigateToCreate(): void {
    this.router.navigate(['/suppliers', 'new']);
  }

  /**
   * 比較画面へ遷移
   */
  navigateToCompare(): void {
    if (this.selectedForCompare.length >= 2) {
      const ids = this.selectedForCompare.join(',');
      this.router.navigate(['/suppliers', 'compare'], {
        queryParams: { ids }
      });
    }
  }

  /**
   * 比較用選択の切り替え
   */
  toggleCompareSelection(supplierId: number, event: Event): void {
    event.stopPropagation();

    const index = this.selectedForCompare.indexOf(supplierId);
    if (index >= 0) {
      this.selectedForCompare.splice(index, 1);
    } else if (this.selectedForCompare.length < 3) {
      this.selectedForCompare.push(supplierId);
    }
  }

  /**
   * 比較用に選択されているかチェック
   */
  isSelectedForCompare(supplierId: number): boolean {
    return this.selectedForCompare.includes(supplierId);
  }

  /**
   * フィルターをリセット
   */
  resetFilters(): void {
    this.searchKeyword = '';
    this.selectedStatus = '';
    this.minRating = null;
    this.maxRating = null;
    this.currentPage = 1;
    this.loadSuppliers();
  }

  /**
   * 評価を星表示用に変換
   */
  getRatingStars(rating: number): string {
    const fullStars = Math.floor(rating);
    const halfStar = rating % 1 >= 0.5 ? 1 : 0;
    const emptyStars = 5 - fullStars - halfStar;
    return '★'.repeat(fullStars) + (halfStar ? '☆' : '') + '☆'.repeat(emptyStars);
  }
}
