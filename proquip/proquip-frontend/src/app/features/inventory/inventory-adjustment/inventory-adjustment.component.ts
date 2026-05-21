import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { InventoryItem, Warehouse } from '@shared/models/inventory.model';

interface AdjustmentLine {
  itemId: number;
  productName: string;
  productSku: string;
  currentQuantity: number;
  adjustmentQuantity: number;
  reason: string;
}

@Component({
  selector: 'app-inventory-adjustment',
  templateUrl: './inventory-adjustment.component.html',
  styleUrls: ['./inventory-adjustment.component.scss']
})
export class InventoryAdjustmentComponent implements OnInit, OnDestroy {

  warehouses: Warehouse[] = [];
  selectedWarehouseId: number | null = null;
  adjustmentLines: AdjustmentLine[] = [];
  isLoading = false;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  adjustmentReasons = [
    '破損',
    '紛失',
    '数え間違い',
    '未記録の出荷',
    '未記録の入荷',
    'その他'
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private inventoryService: InventoryService,
    private warehouseService: WarehouseService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadWarehouses(): void {
    this.warehouseService.getAllWarehouses().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (warehouses) => {
        this.warehouses = warehouses;
      },
      error: (error) => {
        console.error('倉庫一覧の取得に失敗しました', error);
      }
    });
  }

  onWarehouseSelect(): void {
    if (!this.selectedWarehouseId) {
      this.adjustmentLines = [];
      return;
    }

    this.isLoading = true;
    this.clearMessages();

    this.inventoryService.getStockCheckList(this.selectedWarehouseId).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (items: InventoryItem[]) => {
        this.adjustmentLines = items.map(item => ({
          itemId: item.id,
          productName: item.productName,
          productSku: item.productSku,
          currentQuantity: item.quantity,
          adjustmentQuantity: 0,
          reason: ''
        }));
      },
      error: () => {
        this.errorMessage = '在庫データの取得に失敗しました';
      }
    });
  }

  submitAdjustments(): void {
    const linesToSubmit = this.adjustmentLines.filter(l => l.adjustmentQuantity !== 0);
    if (linesToSubmit.length === 0) return;

    this.isSubmitting = true;
    this.clearMessages();

    const results = linesToSubmit.map(line => ({
      itemId: line.itemId,
      actualQuantity: line.currentQuantity + line.adjustmentQuantity
    }));

    this.inventoryService.saveStockCheckResults(results).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: () => {
        this.successMessage = '在庫調整を保存しました';
        this.adjustmentLines = [];
        this.selectedWarehouseId = null;
      },
      error: () => {
        this.errorMessage = '在庫調整の保存に失敗しました';
      }
    });
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
