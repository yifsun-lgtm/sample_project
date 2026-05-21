import { Component, ContentChild, EventEmitter, Input, OnChanges, Output, SimpleChanges, TemplateRef } from '@angular/core';

/**
 * テーブルカラム定義
 */
export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
  width?: string;
  type?: 'text' | 'number' | 'date' | 'currency' | 'status';
}

/**
 * ページ変更イベント
 */
export interface PageChangeEvent {
  page: number;
  pageSize: number;
}

/**
 * ソート変更イベント
 */
export interface SortChangeEvent {
  column: string;
  direction: 'asc' | 'desc';
}

/**
 * 汎用データテーブルコンポーネント
 * ページネーション、ソート、行クリック機能を内蔵
 *
 * 技術的負債 #5: ソートやフィルタリングのビジネスロジックがコンポーネント内にある
 * サーバーサイドで処理すべき
 */
@Component({
  selector: 'app-data-table',
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.scss']
})
export class DataTableComponent implements OnChanges {

  /** テーブルカラム定義 */
  @Input() columns: TableColumn[] = [];

  /** テーブルデータ */
  @Input() data: any[] = [];

  /** データの総件数 */
  @Input() totalCount = 0;

  /** ページサイズ */
  @Input() pageSize = 20;

  /** 現在のページ番号（1始まり） */
  @Input() currentPage = 1;

  /** 現在のソートカラム */
  @Input() sortColumn = '';

  /** ソート方向 */
  @Input() sortDirection: 'asc' | 'desc' = 'asc';

  /** ページ変更イベント */
  @Output() pageChange = new EventEmitter<PageChangeEvent>();

  /** ソート変更イベント */
  @Output() sortChange = new EventEmitter<SortChangeEvent>();

  /** 行クリックイベント */
  @Output() rowClick = new EventEmitter<any>();

  /** セルカスタムテンプレート */
  @ContentChild('cellTemplate', { static: false }) cellTemplate?: TemplateRef<any>;

  /** 行アクション用テンプレート */
  @ContentChild('rowActions', { static: false }) rowActionsTemplate?: TemplateRef<any>;

  /** 総ページ数 */
  totalPages = 0;

  /** 表示用ページ番号配列 */
  displayPages: number[] = [];

  /** 表示用データ（技術的負債: クライアントサイドでのソート・フィルタ） */
  displayData: any[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    // ページ数を計算
    this.totalPages = Math.ceil(this.totalCount / this.pageSize) || 1;
    this.calculateDisplayPages();

    // 技術的負債: クライアントサイドでソートしている（サーバーサイドで行うべき）
    if (changes['data'] || changes['sortColumn'] || changes['sortDirection']) {
      this.sortData();
    }
  }

  /** 表示用ページ番号を計算 */
  private calculateDisplayPages(): void {
    const pages: number[] = [];
    const maxDisplay = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxDisplay / 2));
    const end = Math.min(this.totalPages, start + maxDisplay - 1);

    if (end - start < maxDisplay - 1) {
      start = Math.max(1, end - maxDisplay + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    this.displayPages = pages;
  }

  /**
   * 技術的負債: クライアントサイドでのソート処理
   * 本来はサーバーサイドAPIに委譲すべき
   */
  private sortData(): void {
    if (!this.sortColumn || !this.data) {
      this.displayData = [...this.data];
      return;
    }

    this.displayData = [...this.data].sort((a, b) => {
      const valA = a[this.sortColumn];
      const valB = b[this.sortColumn];

      if (valA == null) return 1;
      if (valB == null) return -1;

      let comparison = 0;
      if (typeof valA === 'string') {
        comparison = valA.localeCompare(valB);
      } else {
        comparison = valA - valB;
      }

      return this.sortDirection === 'desc' ? -comparison : comparison;
    });
  }

  /** ソート変更ハンドラ */
  onSort(column: TableColumn): void {
    if (!column.sortable) return;

    if (this.sortColumn === column.key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column.key;
      this.sortDirection = 'asc';
    }

    this.sortChange.emit({
      column: this.sortColumn,
      direction: this.sortDirection
    });

    // 技術的負債: ローカルソートも実行してしまっている
    this.sortData();
  }

  /** ページ変更ハンドラ */
  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) return;

    this.currentPage = page;
    this.calculateDisplayPages();
    this.pageChange.emit({ page: this.currentPage, pageSize: this.pageSize });
  }

  /** 行クリックハンドラ */
  onRowClick(row: any): void {
    this.rowClick.emit(row);
  }

  /** ソートアイコンを取得 */
  getSortIcon(column: TableColumn): string {
    if (this.sortColumn !== column.key) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }
}
