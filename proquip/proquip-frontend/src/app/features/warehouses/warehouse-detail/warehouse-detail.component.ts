import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { WarehouseService } from '@shared/services/warehouse.service';
import { InventoryService } from '@shared/services/inventory.service';
import { ApiService } from '@shared/services/api.service';
import { Warehouse, InventoryItem } from '@shared/models/inventory.model';
import { PageResult } from '@shared/models/common.model';

/** 倉庫ゾーン */
interface WarehouseZone {
  id: number;
  code: string;
  name: string;
  type: string;
  capacity: number;
  currentOccupancy: number;
  itemCount: number;
  description: string;
}

/** 保管ロケーション */
interface StorageLocationItem {
  id: number;
  code: string;
  aisle: string;
  rack: string;
  shelf: string;
  bin: string;
  locationType: string;
}

/**
 * 倉庫詳細コンポーネント
 * 倉庫情報、ゾーン一覧、在庫アイテム、稼働率チャートを表示する
 */
@Component({
  selector: 'app-warehouse-detail',
  templateUrl: './warehouse-detail.component.html',
  styleUrls: ['./warehouse-detail.component.scss']
})
export class WarehouseDetailComponent implements OnInit, OnDestroy {

  /** 倉庫詳細 */
  warehouse: Warehouse | null = null;

  /** ゾーン一覧 */
  zones: WarehouseZone[] = [];

  /** 倉庫内在庫アイテム */
  inventoryItems: InventoryItem[] = [];

  /** 在庫全件数 */
  inventoryTotalCount = 0;

  /** 在庫ページ */
  inventoryPage = 1;

  /** 読み込み中フラグ */
  isLoading = true;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 編集モーダル */
  showEditModal = false;
  isSaving = false;
  editForm = { name: '', address: '', capacity: 0 };

  /** 削除確認 */
  showDeleteConfirm = false;

  /** ゾーン作成/編集モーダル */
  showZoneModal = false;
  isZoneEdit = false;
  zoneForm = { code: '', name: '', zoneType: 'GENERAL' };
  editingZoneId: number | null = null;

  /** ゾーン削除確認 */
  showZoneDeleteConfirm = false;
  deletingZoneId: number | null = null;

  /** ロケーション表示用 */
  expandedZoneId: number | null = null;
  zoneLocations: StorageLocationItem[] = [];
  isLoadingLocations = false;

  /** ロケーション作成/編集モーダル */
  showLocationModal = false;
  isLocationEdit = false;
  locationForm = { code: '', aisle: '', rack: '', shelf: '', bin: '', locationType: 'SHELF' };
  editingLocationId: number | null = null;

  /** ロケーション削除確認 */
  showLocationDeleteConfirm = false;
  deletingLocationId: number | null = null;

  /** ゾーンタイプ選択肢 */
  zoneTypeOptions = [
    { value: 'GENERAL', label: '一般保管' },
    { value: 'RECEIVING', label: '入荷エリア' },
    { value: 'SHIPPING', label: '出荷エリア' },
    { value: 'PICKING', label: 'ピッキング' },
    { value: 'BULK', label: 'バルク保管' },
    { value: 'COLD', label: '冷蔵保管' },
    { value: 'HAZARDOUS', label: '危険物保管' },
    { value: 'QUARANTINE', label: '検疫エリア' }
  ];

  /** ロケーションタイプ選択肢 */
  locationTypeOptions = [
    { value: 'SHELF', label: '棚' },
    { value: 'RACK', label: 'ラック' },
    { value: 'PALLET', label: 'パレット' },
    { value: 'BIN', label: 'ビン' },
    { value: 'FLOOR', label: '床置き' },
    { value: 'HANGING', label: '吊り下げ' }
  ];

  /** 全体使用率 */
  utilizationPercent = 0;

  /** ゾーン種別ラベル */
  private zoneTypeLabels: { [key: string]: string } = {
    'GENERAL': '一般保管',
    'RECEIVING': '入荷エリア',
    'SHIPPING': '出荷エリア',
    'PICKING': 'ピッキング',
    'BULK': 'バルク保管',
    'COLD': '冷蔵保管',
    'HAZARDOUS': '危険物保管',
    'QUARANTINE': '検疫エリア'
  };

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
      this.loadWarehouseDetail(id);
    } else {
      this.errorMessage = '倉庫IDが無効です';
      this.isLoading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 倉庫詳細を読み込む */
  loadWarehouseDetail(id: number): void {
    this.isLoading = true;

    this.warehouseService.getWarehouse(id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (warehouse) => {
        this.warehouse = warehouse;
        this.utilizationPercent = warehouse.capacity > 0
          ? Math.round((warehouse.currentOccupancy / warehouse.capacity) * 100)
          : 0;

        this.loadZones(id);
        this.loadInventoryItems(id);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('倉庫詳細の取得に失敗しました', error);
        this.errorMessage = '倉庫詳細の取得に失敗しました';
        this.isLoading = false;
      }
    });
  }

  /** ゾーン一覧を読み込む */
  loadZones(warehouseId?: number): void {
    const id = warehouseId || this.warehouse?.id;
    if (!id) return;
    this.warehouseService.getZones(id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (zones) => {
        this.zones = zones || [];
      },
      error: (error) => {
        console.error('ゾーン一覧の取得に失敗しました', error);
      }
    });
  }

  /** 倉庫内在庫アイテムを読み込む */
  loadInventoryItems(warehouseId?: number): void {
    const id = warehouseId || this.warehouse?.id;
    if (!id) return;

    this.inventoryService.getInventoryItems(this.inventoryPage - 1, 10, id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result: PageResult<InventoryItem>) => {
        this.inventoryItems = result.content || [];
        this.inventoryTotalCount = result.totalElements;
      },
      error: (error) => {
        console.error('在庫アイテムの取得に失敗しました', error);
      }
    });
  }

  /** ゾーン種別ラベルを取得 */
  getZoneTypeLabel(type: string): string {
    return this.zoneTypeLabels[type] || type;
  }

  /** ゾーン使用率を計算 */
  getZoneUtilization(zone: WarehouseZone): number {
    if (zone.capacity === 0) return 0;
    return Math.round((zone.currentOccupancy / zone.capacity) * 100);
  }

  /** ゾーン使用率の色を取得 */
  getZoneColor(zone: WarehouseZone): string {
    const percent = this.getZoneUtilization(zone);
    if (percent >= 90) return '#e53935';
    if (percent >= 75) return '#f57c00';
    if (percent >= 50) return '#fdd835';
    return '#43a047';
  }

  /** 全体使用率の色を取得 */
  getUtilizationColor(): string {
    if (this.utilizationPercent >= 90) return '#e53935';
    if (this.utilizationPercent >= 75) return '#f57c00';
    if (this.utilizationPercent >= 50) return '#fdd835';
    return '#43a047';
  }

  /** ステータスラベルを取得 */
  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'ACTIVE': '稼働中',
      'MAINTENANCE': 'メンテナンス中',
      'CLOSED': '閉鎖',
      'PLANNED': '計画中'
    };
    return labels[status] || status;
  }

  /** レイアウト画面へ遷移 */
  navigateToLayout(): void {
    if (this.warehouse) {
      this.router.navigate(['/warehouses', this.warehouse.id, 'layout']);
    }
  }

  /** 在庫ページ変更 */
  onInventoryPageChange(event: any): void {
    this.inventoryPage = event.page;
    this.loadInventoryItems();
  }

  // --- ゾーンCRUD ---

  openZoneCreateModal(): void {
    this.isZoneEdit = false;
    this.editingZoneId = null;
    this.zoneForm = { code: '', name: '', zoneType: 'GENERAL' };
    this.showZoneModal = true;
  }

  openZoneEditModal(zone: WarehouseZone): void {
    this.isZoneEdit = true;
    this.editingZoneId = zone.id;
    this.zoneForm = { code: zone.code || '', name: zone.name, zoneType: zone.type || 'GENERAL' };
    this.showZoneModal = true;
  }

  closeZoneModal(): void {
    this.showZoneModal = false;
  }

  submitZone(): void {
    if (!this.warehouse) return;
    this.isSaving = true;
    const payload = { code: this.zoneForm.code, name: this.zoneForm.name, zoneType: this.zoneForm.zoneType };

    const obs = this.isZoneEdit && this.editingZoneId
      ? this.warehouseService.updateZone(this.warehouse.id, this.editingZoneId, payload)
      : this.warehouseService.createZone(this.warehouse.id, payload);

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.isSaving = false;
        this.showZoneModal = false;
        this.loadZones();
        this.successMessage = this.isZoneEdit ? 'ゾーンを更新しました。' : 'ゾーンを作成しました。';
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (error) => {
        console.error('ゾーン保存エラー:', error);
        this.isSaving = false;
        this.errorMessage = error.error?.error || 'ゾーンの保存に失敗しました。';
        setTimeout(() => { this.errorMessage = ''; }, 5000);
      }
    });
  }

  confirmZoneDelete(zoneId: number): void {
    this.deletingZoneId = zoneId;
    this.showZoneDeleteConfirm = true;
  }

  cancelZoneDelete(): void {
    this.showZoneDeleteConfirm = false;
    this.deletingZoneId = null;
  }

  deleteZone(): void {
    if (!this.warehouse || !this.deletingZoneId) return;
    this.warehouseService.deleteZone(this.warehouse.id, this.deletingZoneId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.showZoneDeleteConfirm = false;
        this.deletingZoneId = null;
        this.loadZones();
        this.successMessage = 'ゾーンを削除しました。';
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (error) => {
        console.error('ゾーン削除エラー:', error);
        this.showZoneDeleteConfirm = false;
        this.errorMessage = error.error?.error || 'ゾーンの削除に失敗しました。';
        setTimeout(() => { this.errorMessage = ''; }, 5000);
      }
    });
  }

  // --- ロケーション管理 ---

  toggleZoneLocations(zone: WarehouseZone): void {
    if (this.expandedZoneId === zone.id) {
      this.expandedZoneId = null;
      this.zoneLocations = [];
      return;
    }
    this.expandedZoneId = zone.id;
    this.loadLocations(zone.id);
  }

  loadLocations(zoneId: number): void {
    if (!this.warehouse) return;
    this.isLoadingLocations = true;
    this.warehouseService.getLocations(this.warehouse.id, zoneId).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoadingLocations = false)
    ).subscribe({
      next: (locations) => {
        this.zoneLocations = locations || [];
      },
      error: (error) => {
        console.error('ロケーション一覧の取得に失敗しました', error);
      }
    });
  }

  openLocationCreateModal(): void {
    this.isLocationEdit = false;
    this.editingLocationId = null;
    this.locationForm = { code: '', aisle: '', rack: '', shelf: '', bin: '', locationType: 'SHELF' };
    this.showLocationModal = true;
  }

  openLocationEditModal(loc: StorageLocationItem): void {
    this.isLocationEdit = true;
    this.editingLocationId = loc.id;
    this.locationForm = {
      code: loc.code,
      aisle: loc.aisle || '',
      rack: loc.rack || '',
      shelf: loc.shelf || '',
      bin: loc.bin || '',
      locationType: loc.locationType || 'SHELF'
    };
    this.showLocationModal = true;
  }

  closeLocationModal(): void {
    this.showLocationModal = false;
  }

  submitLocation(): void {
    if (!this.warehouse || !this.expandedZoneId) return;
    this.isSaving = true;

    const obs = this.isLocationEdit && this.editingLocationId
      ? this.warehouseService.updateLocation(this.warehouse.id, this.expandedZoneId, this.editingLocationId, this.locationForm)
      : this.warehouseService.createLocation(this.warehouse.id, this.expandedZoneId, this.locationForm);

    obs.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.isSaving = false;
        this.showLocationModal = false;
        this.loadLocations(this.expandedZoneId!);
        this.successMessage = this.isLocationEdit ? 'ロケーションを更新しました。' : 'ロケーションを作成しました。';
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (error) => {
        console.error('ロケーション保存エラー:', error);
        this.isSaving = false;
        this.errorMessage = error.error?.error || 'ロケーションの保存に失敗しました。';
        setTimeout(() => { this.errorMessage = ''; }, 5000);
      }
    });
  }

  confirmLocationDelete(locId: number): void {
    this.deletingLocationId = locId;
    this.showLocationDeleteConfirm = true;
  }

  cancelLocationDelete(): void {
    this.showLocationDeleteConfirm = false;
    this.deletingLocationId = null;
  }

  deleteLocation(): void {
    if (!this.warehouse || !this.expandedZoneId || !this.deletingLocationId) return;
    this.warehouseService.deleteLocation(this.warehouse.id, this.expandedZoneId, this.deletingLocationId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.showLocationDeleteConfirm = false;
        this.deletingLocationId = null;
        this.loadLocations(this.expandedZoneId!);
        this.successMessage = 'ロケーションを削除しました。';
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (error) => {
        console.error('ロケーション削除エラー:', error);
        this.showLocationDeleteConfirm = false;
        this.errorMessage = error.error?.error || 'ロケーションの削除に失敗しました。';
        setTimeout(() => { this.errorMessage = ''; }, 5000);
      }
    });
  }

  getLocationTypeLabel(type: string): string {
    const opt = this.locationTypeOptions.find(o => o.value === type);
    return opt ? opt.label : type;
  }

  /** 一覧画面へ戻る */
  goBack(): void {
    this.router.navigate(['/warehouses']);
  }

  openEditModal(): void {
    if (!this.warehouse) return;
    this.editForm = {
      name: this.warehouse.name || '',
      address: this.warehouse.address || '',
      capacity: this.warehouse.capacity || 0
    };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
  }

  submitEdit(): void {
    if (!this.warehouse) return;
    this.isSaving = true;
    this.warehouseService.updateWarehouse(this.warehouse.id, this.editForm).subscribe({
      next: (updated) => {
        this.isSaving = false;
        this.showEditModal = false;
        this.loadWarehouseDetail(this.warehouse!.id);
        this.successMessage = '倉庫を更新しました。';
        setTimeout(() => { this.successMessage = ''; }, 3000);
      },
      error: (error) => {
        console.error('倉庫更新エラー:', error);
        this.isSaving = false;
      }
    });
  }

  confirmDelete(): void {
    this.showDeleteConfirm = true;
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
  }

  deleteWarehouse(): void {
    if (!this.warehouse) return;
    this.warehouseService.deleteWarehouse(this.warehouse.id).subscribe({
      next: () => {
        this.goBack();
      },
      error: (error) => {
        console.error('倉庫削除エラー:', error);
        this.showDeleteConfirm = false;
        this.errorMessage = error.error?.error || '倉庫の削除に失敗しました。';
        setTimeout(() => { this.errorMessage = ''; }, 5000);
      }
    });
  }
}
