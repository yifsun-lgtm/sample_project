import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { ProductService } from '@shared/services/product.service';
import { Warehouse, InventoryItem, StockTransfer } from '@shared/models/inventory.model';
import { Product } from '@shared/models/product.model';

/**
 * 在庫移動作成コンポーネント
 * 倉庫間の在庫移動を作成する
 *
 * 技術的負債: 製品検索がキーストロークごとにAPIコールを発行する（debounceなし）
 */
@Component({
  selector: 'app-stock-transfer-create',
  templateUrl: './stock-transfer-create.component.html',
  styleUrls: ['./stock-transfer-create.component.scss']
})
export class StockTransferCreateComponent implements OnInit, OnDestroy {

  /** 移動フォーム */
  transferForm!: FormGroup;

  /** 倉庫一覧 */
  warehouses: Warehouse[] = [];

  /** 製品検索キーワード */
  productSearchKeyword = '';

  /** 製品検索結果 */
  productSearchResults: Product[] = [];

  /** 検索ドロップダウン表示 */
  showProductDropdown = false;

  /** 選択済み製品 */
  selectedProduct: Product | null = null;

  /** 移動元倉庫の在庫数量 */
  availableQuantity: number | null = null;

  /** 送信中フラグ */
  isSubmitting = false;

  /** 読み込み中フラグ */
  isLoading = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private inventoryService: InventoryService,
    private warehouseService: WarehouseService,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadWarehouses();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** フォーム初期化 */
  private initForm(): void {
    this.transferForm = this.fb.group({
      sourceWarehouseId: [null, [Validators.required]],
      destinationWarehouseId: [null, [Validators.required]],
      productId: [null, [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]],
      notes: ['', [Validators.maxLength(500)]]
    });
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
   * 製品を検索する
   *
   * 技術的負債: キーストロークごとにAPIコールが発生する
   * debounceTime を使用して、入力が安定してからAPIを呼ぶべき
   * 高頻度のAPI呼び出しがサーバーに負荷をかける可能性がある
   */
  onProductSearch(keyword: string): void {
    this.productSearchKeyword = keyword;
    this.showProductDropdown = true;

    if (!keyword || keyword.length < 1) {
      this.productSearchResults = [];
      return;
    }

    // 技術的負債: debounce なしで毎回APIコール
    this.productService.searchProducts(keyword, 0, 15).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        this.productSearchResults = result.content || [];
      },
      error: (error) => {
        console.error('製品検索に失敗しました', error);
        this.productSearchResults = [];
      }
    });
  }

  /** 製品を選択 */
  selectProduct(product: Product): void {
    this.selectedProduct = product;
    this.productSearchKeyword = `${product.name} (${product.sku})`;
    this.showProductDropdown = false;
    this.transferForm.patchValue({ productId: product.id });

    // 移動元倉庫が選択済みの場合、在庫数量を取得
    this.updateAvailableQuantity();
  }

  /** ドロップダウンを非表示 */
  hideProductDropdown(): void {
    setTimeout(() => {
      this.showProductDropdown = false;
    }, 200);
  }

  /** 移動元倉庫変更時 */
  onSourceWarehouseChange(): void {
    this.updateAvailableQuantity();
    this.validateWarehouses();
  }

  /** 移動先倉庫変更時 */
  onDestinationWarehouseChange(): void {
    this.validateWarehouses();
  }

  /** 移動元倉庫の在庫数量を取得 */
  private updateAvailableQuantity(): void {
    const sourceWarehouseId = this.transferForm.get('sourceWarehouseId')?.value;
    const productId = this.transferForm.get('productId')?.value;

    if (!sourceWarehouseId || !productId) {
      this.availableQuantity = null;
      return;
    }

    // 技術的負債: debounceなしのAPI呼び出し
    this.inventoryService.getProductInventorySummary(productId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (items) => {
        const warehouseItem = items.find(i => i.warehouseId === sourceWarehouseId);
        this.availableQuantity = warehouseItem ? warehouseItem.availableQuantity : 0;

        // 数量バリデーション更新
        const quantityControl = this.transferForm.get('quantity');
        if (quantityControl && this.availableQuantity !== null) {
          if (quantityControl.value > this.availableQuantity) {
            quantityControl.setValue(this.availableQuantity);
          }
        }
      },
      error: (error) => {
        console.error('在庫数量の取得に失敗しました', error);
        this.availableQuantity = null;
      }
    });
  }

  /** 倉庫バリデーション（移動元と移動先が同じでないか） */
  private validateWarehouses(): void {
    const source = this.transferForm.get('sourceWarehouseId')?.value;
    const dest = this.transferForm.get('destinationWarehouseId')?.value;

    if (source && dest && source === dest) {
      this.errorMessage = '移動元と移動先に同じ倉庫は選択できません';
    } else {
      this.errorMessage = '';
    }
  }

  /** フォームバリデーション */
  isFormValid(): boolean {
    if (this.transferForm.invalid) return false;
    if (this.errorMessage) return false;

    const source = this.transferForm.get('sourceWarehouseId')?.value;
    const dest = this.transferForm.get('destinationWarehouseId')?.value;
    if (source === dest) return false;

    const quantity = this.transferForm.get('quantity')?.value;
    if (this.availableQuantity !== null && quantity > this.availableQuantity) return false;

    return true;
  }

  /** フォーム送信 */
  onSubmit(): void {
    if (!this.isFormValid()) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    const formValue = this.transferForm.value;
    const transferData: Partial<StockTransfer> = {
      sourceWarehouseId: formValue.sourceWarehouseId,
      destinationWarehouseId: formValue.destinationWarehouseId,
      productId: formValue.productId,
      productName: this.selectedProduct?.name || '',
      quantity: formValue.quantity,
      notes: formValue.notes
    };

    this.inventoryService.createTransfer(transferData).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: () => {
        this.router.navigate(['/inventory/transfers']);
      },
      error: (error) => {
        console.error('在庫移動の作成に失敗しました', error);
        this.errorMessage = '在庫移動の作成に失敗しました。入力内容を確認してください。';
      }
    });
  }

  /** キャンセル */
  onCancel(): void {
    this.router.navigate(['/inventory/transfers']);
  }

  /** フィールドのエラーチェック */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.transferForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /** 移動先として選択可能な倉庫（移動元を除く） */
  getDestinationWarehouses(): Warehouse[] {
    const sourceId = this.transferForm.get('sourceWarehouseId')?.value;
    return this.warehouses.filter(w => w.id !== sourceId);
  }
}
