import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { InventoryService } from '@shared/services/inventory.service';
import { ProductService } from '@shared/services/product.service';
import { InventoryItem } from '@shared/models/inventory.model';
import { ProductDetail, ProductInventoryItem } from '@shared/models/product.model';
import { ApiService } from '@shared/services/api.service';

/** 在庫取引履歴 */
interface InventoryTransaction {
  id: number;
  transactionDate: string;
  transactionType: string;
  quantity: number;
  notes: string;
  performedBy: number;
  productId: number;
  warehouseId: number;
}

/**
 * 在庫詳細コンポーネント
 * 特定製品の全倉庫在庫、取引履歴、発注点設定を表示する
 */
@Component({
  selector: 'app-inventory-detail',
  templateUrl: './inventory-detail.component.html',
  styleUrls: ['./inventory-detail.component.scss']
})
export class InventoryDetailComponent implements OnInit, OnDestroy {

  /** 在庫アイテム */
  inventoryItem: InventoryItem | null = null;

  /** 製品詳細 */
  productDetail: ProductDetail | null = null;

  /** 全倉庫の在庫状況 */
  warehouseStocks: ProductInventoryItem[] = [];

  /** 取引履歴 */
  transactions: InventoryTransaction[] = [];

  /** 読み込み中フラグ */
  isLoading = true;

  /** エラーメッセージ */
  errorMessage = '';

  /** 発注点設定（編集用） */
  editReorderPoint: number | null = null;

  /** 最低在庫設定（編集用） */
  editMinimumStock: number | null = null;

  /** 最大在庫設定（編集用） */
  editMaximumStock: number | null = null;

  /** 設定編集モード */
  isEditingSettings = false;

  /** 設定保存中フラグ */
  isSavingSettings = false;

  /** 在庫合計 */
  totalQuantity = 0;

  /** 全倉庫有効在庫合計 */
  totalAvailable = 0;

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private inventoryService: InventoryService,
    private productService: ProductService,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadInventoryDetail(id);
    } else {
      this.errorMessage = '在庫IDが無効です';
      this.isLoading = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 在庫詳細を読み込む */
  private loadInventoryDetail(id: number): void {
    this.isLoading = true;

    this.inventoryService.getInventoryItem(id).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (item) => {
        this.inventoryItem = item;
        this.editReorderPoint = item.reorderPoint;
        this.editMinimumStock = item.minimumStock;
        this.editMaximumStock = item.maximumStock;

        // 製品詳細と全倉庫在庫を取得
        this.loadProductDetail(item.productId);
        this.loadWarehouseStocks(item.productId);
        this.loadTransactionHistory(item.productId);
      },
      error: (error) => {
        console.error('在庫詳細の取得に失敗しました', error);
        this.errorMessage = '在庫詳細の取得に失敗しました';
        this.isLoading = false;
      }
    });
  }

  /** 製品詳細を読み込む */
  private loadProductDetail(productId: number): void {
    this.productService.getProduct(productId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (product) => {
        this.productDetail = product;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('製品詳細の取得に失敗しました', error);
        this.isLoading = false;
      }
    });
  }

  /** 全倉庫の在庫状況を読み込む */
  private loadWarehouseStocks(productId: number): void {
    this.inventoryService.getProductInventorySummary(productId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (items) => {
        this.warehouseStocks = items.map(item => ({
          warehouseId: item.warehouseId,
          warehouseName: item.warehouseName,
          quantity: item.quantity || 0,
          reservedQuantity: item.reservedQuantity || 0,
          availableQuantity: (item.quantity || 0) - (item.reservedQuantity || 0)
        }));
        this.totalQuantity = this.warehouseStocks.reduce((s, w) => s + w.quantity, 0);
        this.totalAvailable = this.warehouseStocks.reduce((s, w) => s + w.availableQuantity, 0);
      },
      error: (error) => {
        console.error('倉庫別在庫の取得に失敗しました', error);
      }
    });
  }

  /** 取引履歴を読み込む */
  private loadTransactionHistory(productId: number): void {
    this.apiService.get<InventoryTransaction[]>(`/inventory/transactions`, {
      productId,
      page: 0,
      size: 20
    }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (transactions) => {
        this.transactions = transactions || [];
      },
      error: (error) => {
        console.error('取引履歴の取得に失敗しました', error);
      }
    });
  }

  /** 取引種別ラベルを取得 */
  getTransactionTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'IN': '入荷',
      'OUT': '出荷',
      'RECEIPT': '入荷',
      'ISSUE': '出荷',
      'TRANSFER': '移動',
      'TRANSFER_IN': '移動入庫',
      'TRANSFER_OUT': '移動出庫',
      'ADJUST': '調整',
      'ADJUSTMENT': '調整',
      'RETURN': '返品',
      'COUNT': '棚卸し'
    };
    return labels[type] || type;
  }

  /** 取引種別色を取得 */
  getTransactionTypeColor(type: string): string {
    const colors: { [key: string]: string } = {
      'IN': '#43a047',
      'OUT': '#e53935',
      'RECEIPT': '#43a047',
      'ISSUE': '#e53935',
      'TRANSFER': '#1976d2',
      'TRANSFER_IN': '#1976d2',
      'TRANSFER_OUT': '#f57c00',
      'ADJUST': '#7b1fa2',
      'ADJUSTMENT': '#7b1fa2',
      'RETURN': '#ff9800',
      'COUNT': '#78909c'
    };
    return colors[type] || '#9e9e9e';
  }

  /** 設定編集モードを切り替え */
  toggleEditSettings(): void {
    this.isEditingSettings = !this.isEditingSettings;
    if (!this.isEditingSettings && this.inventoryItem) {
      // 編集キャンセル時は元の値に戻す
      this.editReorderPoint = this.inventoryItem.reorderPoint;
      this.editMinimumStock = this.inventoryItem.minimumStock;
      this.editMaximumStock = this.inventoryItem.maximumStock;
    }
  }

  /** 設定を保存 */
  saveSettings(): void {
    if (!this.inventoryItem) return;

    this.isSavingSettings = true;

    this.apiService.put(`/inventory/${this.inventoryItem.id}/settings`, {
      reorderPoint: this.editReorderPoint,
      minimumStock: this.editMinimumStock,
      maximumStock: this.editMaximumStock
    }).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSavingSettings = false)
    ).subscribe({
      next: () => {
        if (this.inventoryItem) {
          this.inventoryItem.reorderPoint = this.editReorderPoint!;
          this.inventoryItem.minimumStock = this.editMinimumStock!;
          this.inventoryItem.maximumStock = this.editMaximumStock!;
        }
        this.isEditingSettings = false;
      },
      error: (error) => {
        console.error('設定の保存に失敗しました', error);
      }
    });
  }

  /** 一覧画面へ戻る */
  goBack(): void {
    this.router.navigate(['/inventory']);
  }
}
