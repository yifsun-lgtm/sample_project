import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { WarehouseService } from '@shared/services/warehouse.service';
import { InventoryService } from '@shared/services/inventory.service';
import { ApiService } from '@shared/services/api.service';
import { Warehouse, InventoryItem } from '@shared/models/inventory.model';

/** ゾーン表示データ */
interface ZoneDisplay {
  id: number;
  name: string;
  type: string;
  row: number;
  col: number;
  rowSpan: number;
  colSpan: number;
  capacity: number;
  currentOccupancy: number;
  utilizationPercent: number;
  itemCount: number;
  items: InventoryItem[];
}

/**
 * 倉庫レイアウトコンポーネント
 * 倉庫ゾーンのグリッドベースのビジュアルレイアウトを表示する
 *
 * 技術的負債: レイアウト全体がHTMLにハードコードされている
 * 本来はデータベースからゾーン配置データを取得してレンダリングすべき
 */
@Component({
  selector: 'app-warehouse-layout',
  templateUrl: './warehouse-layout.component.html',
  styleUrls: ['./warehouse-layout.component.scss']
})
export class WarehouseLayoutComponent implements OnInit, OnDestroy {

  /** 倉庫情報 */
  warehouse: Warehouse | null = null;

  /** ゾーン一覧 */
  zones: ZoneDisplay[] = [];

  /** 選択中のゾーン */
  selectedZone: ZoneDisplay | null = null;

  /** 選択中ゾーンの在庫アイテム */
  selectedZoneItems: InventoryItem[] = [];

  /** 読み込み中フラグ */
  isLoading = true;

  /** ゾーンアイテム読み込み中 */
  isLoadingItems = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** グリッド行数 */
  gridRows = 4;

  /** グリッド列数 */
  gridCols = 6;

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private warehouseService: WarehouseService,
    private inventoryService: InventoryService,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadWarehouse(id);
    } else {
      this.errorMessage = '倉庫IDが無効です';
      this.isLoading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 倉庫情報を読み込む */
  private loadWarehouse(id: number): void {
    this.isLoading = true;

    this.warehouseService.getWarehouse(id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (warehouse) => {
        this.warehouse = warehouse;
        this.loadZones(id);
      },
      error: (error) => {
        console.error('倉庫の取得に失敗しました', error);
        this.errorMessage = '倉庫の取得に失敗しました';
        this.isLoading = false;
      }
    });
  }

  /**
   * ゾーン一覧を読み込む
   *
   * 技術的負債: ゾーンのグリッド配置（row, col, span）はAPIから取得すべきだが、
   * 現在はフロントエンドでハードコードしている
   */
  private loadZones(warehouseId: number): void {
    this.apiService.get<any[]>(`/warehouses/${warehouseId}/zones`).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (rawZones) => {
        // 技術的負債: ゾーンのグリッド配置をハードコード
        this.zones = this.mapZonesToGrid(rawZones || []);
      },
      error: (error) => {
        console.error('ゾーン一覧の取得に失敗しました', error);
        // フォールバック: デフォルトレイアウト
        this.zones = this.getDefaultLayout();
      }
    });
  }

  /**
   * ゾーンデータをグリッド配置にマッピング
   *
   * 技術的負債: グリッド配置がハードコードされている
   * データベースに配置情報を持たせてデータドリブンにすべき
   */
  private mapZonesToGrid(rawZones: any[]): ZoneDisplay[] {
    // 技術的負債: ハードコードされたレイアウトマッピング
    const layoutMap: { [key: string]: { row: number; col: number; rowSpan: number; colSpan: number } } = {
      'RECEIVING': { row: 1, col: 1, rowSpan: 1, colSpan: 2 },
      'QUALITY': { row: 1, col: 3, rowSpan: 1, colSpan: 1 },
      'STAGING': { row: 1, col: 4, rowSpan: 1, colSpan: 1 },
      'SHIPPING': { row: 1, col: 5, rowSpan: 1, colSpan: 2 },
      'STORAGE': { row: 2, col: 1, rowSpan: 2, colSpan: 4 },
      'COLD_STORAGE': { row: 2, col: 5, rowSpan: 1, colSpan: 2 },
      'HAZARDOUS': { row: 3, col: 5, rowSpan: 1, colSpan: 2 },
      'RETURNS': { row: 4, col: 1, rowSpan: 1, colSpan: 3 }
    };

    return rawZones.map((zone, index) => {
      const layout = layoutMap[zone.type] || {
        row: Math.floor(index / this.gridCols) + 1,
        col: (index % this.gridCols) + 1,
        rowSpan: 1,
        colSpan: 1
      };

      const utilization = zone.capacity > 0
        ? Math.round((zone.currentOccupancy / zone.capacity) * 100)
        : 0;

      return {
        id: zone.id,
        name: zone.name,
        type: zone.type,
        row: layout.row,
        col: layout.col,
        rowSpan: layout.rowSpan,
        colSpan: layout.colSpan,
        capacity: zone.capacity,
        currentOccupancy: zone.currentOccupancy,
        utilizationPercent: utilization,
        itemCount: zone.itemCount || 0,
        items: []
      };
    });
  }

  /**
   * デフォルトレイアウト（フォールバック用）
   *
   * 技術的負債: 全体がハードコードされたデフォルトレイアウト
   */
  private getDefaultLayout(): ZoneDisplay[] {
    return [
      { id: 1, name: '入荷エリア', type: 'RECEIVING', row: 1, col: 1, rowSpan: 1, colSpan: 2, capacity: 100, currentOccupancy: 35, utilizationPercent: 35, itemCount: 12, items: [] },
      { id: 2, name: '品質検査', type: 'QUALITY', row: 1, col: 3, rowSpan: 1, colSpan: 1, capacity: 50, currentOccupancy: 10, utilizationPercent: 20, itemCount: 5, items: [] },
      { id: 3, name: 'ステージング', type: 'STAGING', row: 1, col: 4, rowSpan: 1, colSpan: 1, capacity: 80, currentOccupancy: 45, utilizationPercent: 56, itemCount: 8, items: [] },
      { id: 4, name: '出荷エリア', type: 'SHIPPING', row: 1, col: 5, rowSpan: 1, colSpan: 2, capacity: 120, currentOccupancy: 60, utilizationPercent: 50, itemCount: 15, items: [] },
      { id: 5, name: 'メイン保管エリア', type: 'STORAGE', row: 2, col: 1, rowSpan: 2, colSpan: 4, capacity: 500, currentOccupancy: 380, utilizationPercent: 76, itemCount: 150, items: [] },
      { id: 6, name: '冷蔵保管', type: 'COLD_STORAGE', row: 2, col: 5, rowSpan: 1, colSpan: 2, capacity: 80, currentOccupancy: 72, utilizationPercent: 90, itemCount: 20, items: [] },
      { id: 7, name: '危険物保管', type: 'HAZARDOUS', row: 3, col: 5, rowSpan: 1, colSpan: 2, capacity: 40, currentOccupancy: 15, utilizationPercent: 38, itemCount: 6, items: [] },
      { id: 8, name: '返品エリア', type: 'RETURNS', row: 4, col: 1, rowSpan: 1, colSpan: 3, capacity: 60, currentOccupancy: 22, utilizationPercent: 37, itemCount: 9, items: [] }
    ];
  }

  /** ゾーンをクリック */
  onZoneClick(zone: ZoneDisplay): void {
    this.selectedZone = zone;
    this.loadZoneItems(zone);
  }

  /** ゾーン内の在庫アイテムを読み込む */
  private loadZoneItems(zone: ZoneDisplay): void {
    this.isLoadingItems = true;
    this.selectedZoneItems = [];

    this.apiService.get<InventoryItem[]>(`/warehouses/${this.warehouse?.id}/zones/${zone.id}/items`).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoadingItems = false)
    ).subscribe({
      next: (items) => {
        this.selectedZoneItems = items || [];
      },
      error: (error) => {
        console.error('ゾーンアイテムの取得に失敗しました', error);
      }
    });
  }

  /** ゾーン選択を解除 */
  clearSelection(): void {
    this.selectedZone = null;
    this.selectedZoneItems = [];
  }

  /** 使用率に基づく色を取得 */
  getOccupancyColor(percent: number): string {
    if (percent >= 90) return '#e53935';
    if (percent >= 75) return '#f57c00';
    if (percent >= 50) return '#fdd835';
    return '#43a047';
  }

  /** 使用率に基づく背景色を取得 */
  getOccupancyBgColor(percent: number): string {
    if (percent >= 90) return '#ffebee';
    if (percent >= 75) return '#fff3e0';
    if (percent >= 50) return '#fffde7';
    return '#e8f5e9';
  }

  /** ゾーン種別ラベルを取得 */
  getZoneTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'STORAGE': '保管エリア',
      'GENERAL': '一般保管',
      'BULK': 'バルク保管',
      'RECEIVING': '入荷エリア',
      'SHIPPING': '出荷エリア',
      'STAGING': 'ステージング',
      'COLD_STORAGE': '冷蔵保管',
      'HAZARDOUS': '危険物保管',
      'RETURNS': '返品エリア',
      'QUALITY': '品質検査',
      'PICKING': 'ピッキング'
    };
    return labels[type] || type;
  }

  /** 詳細画面へ戻る */
  goBack(): void {
    if (this.warehouse) {
      this.router.navigate(['/warehouses', this.warehouse.id]);
    } else {
      this.router.navigate(['/warehouses']);
    }
  }
}
