import { Component, OnInit } from '@angular/core';
import { ReportService } from '@shared/services/report.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { Warehouse } from '@shared/models/inventory.model';

/**
 * 在庫評価アイテム
 */
export interface ValuationItem {
  productId: number;
  productName: string;
  sku: string;
  categoryName: string;
  quantity: number;
  unitPrice: number;
  totalValue: number;
  percentage: number;
  isHighValue: boolean;
}

/**
 * 在庫評価サマリー
 */
export interface ValuationSummary {
  totalItems: number;
  totalValue: number;
  averageValuePerItem: number;
  highValueItemCount: number;
}

/**
 * 在庫評価レポートコンポーネント
 * 倉庫ごとの在庫金額を評価し表示する
 */
@Component({
  selector: 'app-inventory-valuation',
  templateUrl: './inventory-valuation.component.html',
  styleUrls: ['./inventory-valuation.component.scss']
})
export class InventoryValuationComponent implements OnInit {

  /** 倉庫一覧 */
  warehouses: Warehouse[] = [];

  /** 選択中の倉庫ID */
  selectedWarehouseId: number | null = null;

  /** 在庫評価サマリー */
  summary: ValuationSummary | null = null;

  /** 在庫評価アイテム一覧 */
  valuationItems: ValuationItem[] = [];

  /** フィルター済みアイテム */
  filteredItems: ValuationItem[] = [];

  /** ローディング */
  isLoading = false;

  /** エクスポート中 */
  isExporting = false;

  /** 検索キーワード */
  searchKeyword = '';

  /** ソートカラム */
  sortColumn = 'totalValue';

  /** ソート方向 */
  sortDirection: 'asc' | 'desc' = 'desc';

  /** 高額アイテムのみ表示フラグ */
  showHighValueOnly = false;

  /** 高額アイテム閾値（上位N%） */
  private readonly HIGH_VALUE_THRESHOLD_PERCENT = 20;

  /** エラーメッセージ */
  errorMessage = '';

  constructor(
    private reportService: ReportService,
    private warehouseService: WarehouseService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
  }

  /** 倉庫一覧を読み込む */
  private loadWarehouses(): void {
    this.warehouseService.getAllWarehouses().subscribe({
      next: (warehouses) => {
        this.warehouses = warehouses;
        if (warehouses.length > 0) {
          this.selectedWarehouseId = warehouses[0].id;
          this.loadValuation();
        }
      },
      error: (err) => {
        console.error('倉庫一覧取得エラー:', err);
        this.errorMessage = '倉庫一覧の取得に失敗しました。';
        this.warehouses = [];
      }
    });
  }

  /** 在庫評価データを読み込む */
  loadValuation(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.reportService.getInventoryValuation(this.selectedWarehouseId || undefined).subscribe({
      next: (result) => {
        this.processValuationData(result.data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('在庫評価データ取得エラー:', err);
        this.errorMessage = '在庫評価データの取得に失敗しました。';
        this.valuationItems = [];
        this.filteredItems = [];
        this.summary = null;
        this.isLoading = false;
      }
    });
  }

  /** 在庫評価データを処理 */
  private processValuationData(rawData: any): void {
    if (!rawData || !rawData.items) {
      this.valuationItems = [];
      this.filteredItems = [];
      this.summary = null;
      return;
    }

    const items: ValuationItem[] = rawData.items.map((item: any) => ({
      productId: item.productId,
      productName: item.productName,
      sku: item.sku,
      categoryName: item.categoryName || '',
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      totalValue: item.quantity * item.unitPrice,
      percentage: 0,
      isHighValue: false
    }));

    const totalValue = items.reduce((sum: number, item: ValuationItem) => sum + item.totalValue, 0);

    // 構成比の計算と高額アイテムのマーク
    items.forEach((item: ValuationItem) => {
      item.percentage = totalValue > 0 ? Math.round((item.totalValue / totalValue) * 1000) / 10 : 0;
    });

    // 高額アイテムの判定（上位20%の金額に該当するもの）
    const sortedByValue = [...items].sort((a, b) => b.totalValue - a.totalValue);
    let cumulativePercentage = 0;
    for (const item of sortedByValue) {
      cumulativePercentage += item.percentage;
      item.isHighValue = cumulativePercentage <= this.HIGH_VALUE_THRESHOLD_PERCENT * 5;
    }

    this.valuationItems = items;
    this.summary = {
      totalItems: items.length,
      totalValue: totalValue,
      averageValuePerItem: items.length > 0 ? Math.round(totalValue / items.length) : 0,
      highValueItemCount: items.filter((i: ValuationItem) => i.isHighValue).length
    };

    this.applyFilters();
  }

  /** 倉庫変更時 */
  onWarehouseChange(): void {
    this.loadValuation();
  }

  /** フィルター適用 */
  applyFilters(): void {
    let items = [...this.valuationItems];

    // 検索フィルター
    if (this.searchKeyword) {
      const keyword = this.searchKeyword.toLowerCase();
      items = items.filter(item =>
        item.productName.toLowerCase().includes(keyword) ||
        item.sku.toLowerCase().includes(keyword) ||
        item.categoryName.toLowerCase().includes(keyword)
      );
    }

    // 高額アイテムフィルター
    if (this.showHighValueOnly) {
      items = items.filter(item => item.isHighValue);
    }

    // ソート
    items.sort((a, b) => {
      const valA = (a as any)[this.sortColumn];
      const valB = (b as any)[this.sortColumn];
      const cmp = typeof valA === 'string' ? valA.localeCompare(valB) : valA - valB;
      return this.sortDirection === 'desc' ? -cmp : cmp;
    });

    this.filteredItems = items;
  }

  /** 検索変更 */
  onSearchChange(keyword: string): void {
    this.searchKeyword = keyword;
    this.applyFilters();
  }

  /** ソート変更 */
  onSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'desc';
    }
    this.applyFilters();
  }

  /** ソートアイコン */
  getSortIcon(column: string): string {
    if (this.sortColumn !== column) return '';
    return this.sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  /** CSVエクスポート */
  exportCsv(): void {
    this.isExporting = true;
    this.reportService.exportCsv('inventory-valuation', {
      warehouseId: this.selectedWarehouseId || ''
    }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `在庫評価_${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
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

  /** 通貨フォーマット */
  formatCurrency(value: number): string {
    return '¥' + value.toLocaleString('ja-JP');
  }

  /** 数値フォーマット */
  formatNumber(value: number): string {
    return value.toLocaleString('ja-JP');
  }
}
