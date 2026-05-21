import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { InventoryItem, Warehouse } from '@shared/models/inventory.model';

/** 棚卸し明細行 */
interface CountLine {
  itemId: number;
  productName: string;
  productSku: string;
  expectedQuantity: number;
  actualQuantity: number | null;
  discrepancy: number;
  adjustmentReason: string;
  isConfirmed: boolean;
}

/**
 * 棚卸し（サイクルカウント）コンポーネント
 * 倉庫を選択し、全在庫の実数カウントと差異の確認を行う
 *
 * 技術的負債: 倉庫全体の在庫を一度にロードする（ページネーションなし）
 * 在庫品目が多い倉庫ではパフォーマンス問題が発生する
 */
@Component({
  selector: 'app-inventory-count',
  templateUrl: './inventory-count.component.html',
  styleUrls: ['./inventory-count.component.scss']
})
export class InventoryCountComponent implements OnInit, OnDestroy {

  /** 倉庫一覧 */
  warehouses: Warehouse[] = [];

  /** 選択中の倉庫ID */
  selectedWarehouseId: number | null = null;

  /** 選択中の倉庫 */
  selectedWarehouse: Warehouse | null = null;

  /** 棚卸し明細行 */
  countLines: CountLine[] = [];

  /** 読み込み中フラグ */
  isLoading = false;

  /** 送信中フラグ */
  isSubmitting = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 差異ありフィルタ */
  showDiscrepancyOnly = false;

  /** 棚卸し日 */
  countDate: string = '';

  /** 確認済み件数 */
  confirmedCount = 0;

  /** 差異あり件数 */
  discrepancyCount = 0;

  /** 調整理由選択肢 */
  adjustmentReasons = [
    '破損',
    '紛失',
    '数え間違い（前回）',
    '未記録の出荷',
    '未記録の入荷',
    '盗難疑い',
    'その他'
  ];

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private inventoryService: InventoryService,
    private warehouseService: WarehouseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const today = new Date();
    this.countDate = today.toISOString().split('T')[0];
    this.loadWarehouses();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** 倉庫一覧を読み込む */
  private loadWarehouses(): void {
    this.warehouseService.getAllWarehouses().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (warehouses) => {
        this.warehouses = warehouses.filter((w: any) => w.active !== false);
      },
      error: (error) => {
        console.error('倉庫一覧の取得に失敗しました', error);
      }
    });
  }

  /**
   * 倉庫を選択して在庫を読み込む
   *
   * 技術的負債: 倉庫全体の在庫をページネーションなしで一度にロード
   * 在庫品目が数千件ある大規模倉庫では、レスポンスが遅くなりメモリを圧迫する
   */
  onWarehouseSelect(): void {
    if (!this.selectedWarehouseId) {
      this.countLines = [];
      this.selectedWarehouse = null;
      return;
    }

    this.selectedWarehouse = this.warehouses.find(w => w.id === this.selectedWarehouseId) || null;
    this.isLoading = true;
    this.clearMessages();

    // 技術的負債: 全在庫を一度にロード（ページネーションなし）
    this.inventoryService.getStockCheckList(this.selectedWarehouseId).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (items: InventoryItem[]) => {
        this.countLines = items.map(item => ({
          itemId: item.id,
          productName: item.productName,
          productSku: item.productSku,
          expectedQuantity: item.quantity,
          actualQuantity: null,
          discrepancy: 0,
          adjustmentReason: '',
          isConfirmed: false
        }));
        this.updateCounts();
      },
      error: (error) => {
        console.error('棚卸し在庫の取得に失敗しました', error);
        this.errorMessage = '在庫データの取得に失敗しました';
      }
    });
  }

  /** 実数入力時の差異計算 */
  onActualQuantityChange(line: CountLine): void {
    if (line.actualQuantity !== null) {
      line.discrepancy = line.actualQuantity - line.expectedQuantity;
    } else {
      line.discrepancy = 0;
    }
    this.updateCounts();
  }

  /** 行を確認済みにする */
  confirmLine(line: CountLine): void {
    if (line.actualQuantity === null) return;
    line.isConfirmed = true;
    this.updateCounts();
  }

  /** 確認を取り消す */
  unconfirmLine(line: CountLine): void {
    line.isConfirmed = false;
    this.updateCounts();
  }

  /** カウントを更新 */
  private updateCounts(): void {
    this.confirmedCount = this.countLines.filter(l => l.isConfirmed).length;
    this.discrepancyCount = this.countLines.filter(l =>
      l.actualQuantity !== null && l.discrepancy !== 0
    ).length;
  }

  /** 差異フィルタで表示する明細行 */
  getDisplayLines(): CountLine[] {
    if (this.showDiscrepancyOnly) {
      return this.countLines.filter(l =>
        l.actualQuantity !== null && l.discrepancy !== 0
      );
    }
    return this.countLines;
  }

  /** 全て実在庫=理論在庫で埋める */
  fillExpected(): void {
    this.countLines.forEach(line => {
      if (line.actualQuantity === null) {
        line.actualQuantity = line.expectedQuantity;
        line.discrepancy = 0;
      }
    });
    this.updateCounts();
  }

  /** 実在庫をクリア */
  clearActual(): void {
    this.countLines.forEach(line => {
      line.actualQuantity = null;
      line.discrepancy = 0;
      line.isConfirmed = false;
      line.adjustmentReason = '';
    });
    this.updateCounts();
  }

  /** 送信バリデーション */
  isSubmitValid(): boolean {
    // 全ての行に実在庫が入力されているか
    const allCounted = this.countLines.every(l => l.actualQuantity !== null);
    if (!allCounted) return false;

    // 差異がある行に全て理由が入力されているか
    const discrepancyLines = this.countLines.filter(l => l.discrepancy !== 0);
    const allReasoned = discrepancyLines.every(l => l.adjustmentReason !== '');
    if (!allReasoned) return false;

    return true;
  }

  /** 棚卸し結果を送信 */
  submitCount(): void {
    if (!this.isSubmitValid()) return;

    this.isSubmitting = true;
    this.clearMessages();

    const results = this.countLines.map(line => ({
      itemId: line.itemId,
      actualQuantity: line.actualQuantity!
    }));

    this.inventoryService.saveStockCheckResults(results).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: () => {
        this.successMessage = '棚卸し結果を保存しました';
        this.countLines = [];
        this.selectedWarehouseId = null;
        this.selectedWarehouse = null;
      },
      error: (error) => {
        console.error('棚卸し結果の保存に失敗しました', error);
        this.errorMessage = '棚卸し結果の保存に失敗しました';
      }
    });
  }

  /** メッセージクリア */
  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  /** 進捗率を取得 */
  getProgressPercent(): number {
    if (this.countLines.length === 0) return 0;
    const counted = this.countLines.filter(l => l.actualQuantity !== null).length;
    return Math.round((counted / this.countLines.length) * 100);
  }

  /** 差異の合計を取得 */
  getTotalDiscrepancy(): number {
    return this.countLines.reduce((sum, l) => sum + Math.abs(l.discrepancy), 0);
  }
}
