import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, switchMap, finalize } from 'rxjs/operators';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { getOrderStatusLabel } from '@shared/utils/status.utils';
import { SupplierService } from '@shared/services/supplier.service';
import { ProductService } from '@shared/services/product.service';
import { PurchaseOrder, PurchaseOrderItem } from '@shared/models/purchase-order.model';
import { Supplier } from '@shared/models/supplier.model';
import { Product } from '@shared/models/product.model';
import { PageResult, SelectOption } from '@shared/models/common.model';

/**
 * 発注書作成コンポーネント — KEY TECH DEBT FILE
 *
 * 技術的負債 #5: ~200行のビジネスロジック（価格計算、税計算、バリデーション）が
 * コンポーネント内に直接実装されている。専用サービスに分離すべき。
 *
 * 技術的負債 #14: ステータス文字列がリテラルとして使用されている。
 * 定数やenumに置き換えるべき。
 *
 * 技術的負債 #17: 金額上限がバックエンドと不整合。
 * フロント: 10,000,000 / バックエンド: 9,999,999.99
 *
 * 技術的負債: var を一部で使用（const/let にすべき）
 * 技術的負債: ネストされた subscribe() が適切にクリーンアップされていない
 */
@Component({
  selector: 'app-order-create',
  templateUrl: './order-create.component.html',
  styleUrls: ['./order-create.component.scss']
})
export class OrderCreateComponent implements OnInit, OnDestroy {

  /** 発注書フォーム */
  orderForm!: FormGroup;

  /** 送信中フラグ */
  isSubmitting = false;

  /** サプライヤー一覧 */
  suppliers: Supplier[] = [];

  /** サプライヤー検索結果 */
  filteredSuppliers: Supplier[] = [];

  /** サプライヤー検索キーワード */
  supplierSearchKeyword = '';

  /** サプライヤードロップダウン表示 */
  showSupplierDropdown = false;

  /** 選択済みサプライヤー */
  selectedSupplier: Supplier | null = null;

  /** 製品検索結果 */
  productSuggestions: Product[] = [];

  /** 製品サジェスト表示フラグ（行インデックス別） */
  showProductSuggestions: { [key: number]: boolean } = {};

  /** 小計 */
  subtotal = 0;

  /** 消費税額 */
  taxAmount = 0;

  /** 値引き額 */
  discountAmount = 0;

  /** 合計金額 */
  totalAmount = 0;

  /** 消費税率 */
  taxRate = 0.10;

  /** エラーメッセージ */
  errorMessage = '';

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  /** サプライヤー検索用Subject */
  private supplierSearch$ = new Subject<string>();

  /** 製品検索用Subject */
  private productSearch$ = new Subject<{ keyword: string; index: number }>();

  constructor(
    private fb: FormBuilder,
    private purchaseOrderService: PurchaseOrderService,
    private supplierService: SupplierService,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadSuppliers();
    this.setupSupplierSearch();
    this.setupProductSearch();
    // 初期明細行を1行追加
    this.addItem();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** フォーム初期化 */
  private initForm(): void {
    this.orderForm = this.fb.group({
      supplierId: [null, [Validators.required]],
      supplierName: [''],
      items: this.fb.array([], [Validators.required]),
      deliveryAddress: ['', [Validators.required, Validators.maxLength(200)]],
      expectedDeliveryDate: ['', [Validators.required]],
      notes: ['', [Validators.maxLength(1000)]],
      discountPercent: [0, [Validators.min(0), Validators.max(100)]]
    });
  }

  /** サプライヤー一覧を読み込む */
  private loadSuppliers(): void {
    // 技術的負債: ネストされたsubscribe — switchMapやcombineLatestを使うべき
    this.supplierService.getSuppliers(0, 200).subscribe({
      next: (result) => {
        this.suppliers = result.content || [];
        this.filteredSuppliers = [...this.suppliers];
      },
      error: (error) => {
        console.error('サプライヤー一覧の取得に失敗しました', error);
      }
    });
  }

  /** サプライヤー検索のセットアップ */
  private setupSupplierSearch(): void {
    this.supplierSearch$.pipe(
      debounceTime(200),
      takeUntil(this.destroy$)
    ).subscribe((keyword) => {
      if (!keyword) {
        this.filteredSuppliers = [...this.suppliers];
      } else {
        // 技術的負債: var を使用（const/let にすべき）
        var lowerKeyword = keyword.toLowerCase();
        this.filteredSuppliers = this.suppliers.filter(s =>
          s.name.toLowerCase().includes(lowerKeyword) ||
          s.code.toLowerCase().includes(lowerKeyword) ||
          (s.nameKana && s.nameKana.toLowerCase().includes(lowerKeyword))
        );
      }
    });
  }

  /** 製品検索のセットアップ */
  private setupProductSearch(): void {
    this.productSearch$.pipe(
      debounceTime(300),
      switchMap(({ keyword, index }) => {
        if (!keyword || keyword.length < 2) {
          this.productSuggestions = [];
          return [];
        }
        return this.productService.searchProducts(keyword, 0, 10);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result: any) => {
        if (result && result.content) {
          this.productSuggestions = result.content;
        }
      },
      error: (error) => {
        console.error('製品検索に失敗しました', error);
      }
    });
  }

  /** 明細行のFormArrayを取得 */
  get items(): FormArray {
    return this.orderForm.get('items') as FormArray;
  }

  /** 明細行を追加 */
  addItem(): void {
    const itemGroup = this.fb.group({
      productId: [null, [Validators.required]],
      productName: [''],
      productSku: [''],
      productSearch: [''],
      quantity: [1, [Validators.required, Validators.min(1), Validators.max(99999)]],
      unitPrice: [0, [Validators.required, Validators.min(0)]],
      discountPercent: [0, [Validators.min(0), Validators.max(100)]],
      unit: ['個'],
      notes: ['']
    });

    this.items.push(itemGroup);
    this.recalculateAmounts();
  }

  /** 明細行を削除 */
  removeItem(index: number): void {
    if (this.items.length <= 1) return;
    this.items.removeAt(index);
    this.recalculateAmounts();
  }

  /** サプライヤー検索入力 */
  onSupplierSearch(keyword: string): void {
    this.supplierSearchKeyword = keyword;
    this.showSupplierDropdown = true;
    this.supplierSearch$.next(keyword);
  }

  /** サプライヤーを選択 */
  selectSupplier(supplier: Supplier): void {
    this.selectedSupplier = supplier;
    this.supplierSearchKeyword = supplier.name;
    this.showSupplierDropdown = false;
    this.orderForm.patchValue({
      supplierId: supplier.id,
      supplierName: supplier.name
    });
  }

  /** サプライヤードロップダウンを非表示 */
  hideSupplierDropdown(): void {
    setTimeout(() => {
      this.showSupplierDropdown = false;
    }, 200);
  }

  /** 製品検索入力 */
  onProductSearch(keyword: string, index: number): void {
    this.showProductSuggestions[index] = true;
    this.productSearch$.next({ keyword, index });
  }

  /** 製品を選択 */
  selectProduct(product: Product, index: number): void {
    const item = this.items.at(index);
    item.patchValue({
      productId: product.id,
      productName: product.name,
      productSku: product.sku,
      productSearch: `${product.name} (${product.sku})`,
      unitPrice: product.unitPrice,
      unit: product.unit
    });
    this.showProductSuggestions[index] = false;
    this.recalculateAmounts();
  }

  /** 製品サジェストを非表示 */
  hideProductSuggestions(index: number): void {
    setTimeout(() => {
      this.showProductSuggestions[index] = false;
    }, 200);
  }

  /**
   * 金額を再計算する
   *
   * 技術的負債 #5: 価格計算ロジック（~50行）がコンポーネント内にある
   * PriceCalculationServiceなどに切り出すべき
   */
  recalculateAmounts(): void {
    // 技術的負債: var を使用
    var rawSubtotal = 0;

    for (let i = 0; i < this.items.length; i++) {
      const item = this.items.at(i);
      const quantity = item.get('quantity')?.value || 0;
      const unitPrice = item.get('unitPrice')?.value || 0;
      const itemDiscount = item.get('discountPercent')?.value || 0;

      // 明細行の合計（値引き適用後）
      // 技術的負債 #5: 計算ロジックがインラインで記述されている
      var itemTotal = quantity * unitPrice;
      if (itemDiscount > 0) {
        itemTotal = itemTotal * (1 - itemDiscount / 100);
      }
      rawSubtotal += itemTotal;
    }

    this.subtotal = Math.round(rawSubtotal);

    // 全体値引き
    const globalDiscountPercent = this.orderForm.get('discountPercent')?.value || 0;
    if (globalDiscountPercent > 0) {
      this.discountAmount = Math.round(this.subtotal * globalDiscountPercent / 100);
    } else {
      this.discountAmount = 0;
    }

    // 技術的負債 #5: 税計算ロジック
    var taxableAmount = this.subtotal - this.discountAmount;
    this.taxAmount = Math.round(taxableAmount * this.taxRate);

    // 合計
    this.totalAmount = taxableAmount + this.taxAmount;

    // 技術的負債 #17: 金額上限チェック（バックエンドと不整合）
    this.validateAmounts();
  }

  /**
   * 金額バリデーション
   *
   * 技術的負債 #17: フロントエンドの上限値 10,000,000 に対して
   * バックエンドは 9,999,999.99 のため、端数のケースで不整合が発生する
   */
  private validateAmounts(): void {
    // 技術的負債 #17: バックエンドとの不整合
    // 技術的負債: var を使用
    var MAX_ORDER_AMOUNT = 10000000;
    var MAX_ITEM_AMOUNT = 5000000;

    if (this.totalAmount > MAX_ORDER_AMOUNT) {
      this.errorMessage = `合計金額は${MAX_ORDER_AMOUNT.toLocaleString()}円以下にしてください`;
      return;
    }

    // 明細行ごとの上限チェック
    for (let i = 0; i < this.items.length; i++) {
      const item = this.items.at(i);
      const quantity = item.get('quantity')?.value || 0;
      const unitPrice = item.get('unitPrice')?.value || 0;
      var lineTotal = quantity * unitPrice;

      if (lineTotal > MAX_ITEM_AMOUNT) {
        this.errorMessage = `明細行 ${i + 1} の金額が上限（${MAX_ITEM_AMOUNT.toLocaleString()}円）を超えています`;
        return;
      }
    }

    this.errorMessage = '';
  }

  /**
   * 明細行の小計を計算する
   *
   * 技術的負債 #5: コンポーネント内のビジネスロジック
   */
  getItemSubtotal(index: number): number {
    const item = this.items.at(index);
    const quantity = item.get('quantity')?.value || 0;
    const unitPrice = item.get('unitPrice')?.value || 0;
    const discountPercent = item.get('discountPercent')?.value || 0;

    // 技術的負債: var を使用
    var total = quantity * unitPrice;
    if (discountPercent > 0) {
      total = total * (1 - discountPercent / 100);
    }
    return Math.round(total);
  }

  /**
   * 明細行の値引き額を計算する
   *
   * 技術的負債 #5: コンポーネント内のビジネスロジック
   */
  getItemDiscountAmount(index: number): number {
    const item = this.items.at(index);
    const quantity = item.get('quantity')?.value || 0;
    const unitPrice = item.get('unitPrice')?.value || 0;
    const discountPercent = item.get('discountPercent')?.value || 0;

    if (discountPercent <= 0) return 0;
    return Math.round(quantity * unitPrice * discountPercent / 100);
  }

  getStatusLabel(status: string): string {
    return getOrderStatusLabel(status);
  }

  /**
   * 全体バリデーション
   *
   * 技術的負債 #5: バリデーションロジックがコンポーネントに集中している
   */
  validateOrder(): boolean {
    // 技術的負債: var を使用
    var isValid = true;

    // サプライヤーチェック
    if (!this.orderForm.get('supplierId')?.value) {
      isValid = false;
    }

    // 明細行チェック
    if (this.items.length === 0) {
      isValid = false;
    }

    for (let i = 0; i < this.items.length; i++) {
      const item = this.items.at(i);

      if (!item.get('productId')?.value) {
        isValid = false;
      }

      // 技術的負債: var を使用
      var qty = item.get('quantity')?.value;
      if (!qty || qty < 1) {
        isValid = false;
      }

      var price = item.get('unitPrice')?.value;
      if (price == null || price < 0) {
        isValid = false;
      }
    }

    // 配送情報チェック
    if (!this.orderForm.get('deliveryAddress')?.value) {
      isValid = false;
    }
    if (!this.orderForm.get('expectedDeliveryDate')?.value) {
      isValid = false;
    }

    // 技術的負債 #17: 金額上限チェック（バックエンドと不整合の可能性）
    if (this.totalAmount > 10000000) {
      isValid = false;
    }

    return isValid;
  }

  /** フォーム送信 */
  onSubmit(): void {
    if (this.orderForm.invalid || !this.validateOrder()) {
      this.markFormAsTouched();
      return;
    }

    if (this.errorMessage) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    const formValue = this.orderForm.value;

    // 技術的負債 #14: ステータスがリテラル文字列
    const orderData: Partial<PurchaseOrder> = {
      supplierId: formValue.supplierId,
      supplierName: formValue.supplierName,
      status: 'DRAFT',
      expectedDeliveryDate: formValue.expectedDeliveryDate,
      totalAmount: this.totalAmount,
      currency: 'JPY',
      notes: formValue.notes,
      items: formValue.items.map((item: any, index: number) => ({
        productId: item.productId,
        productName: item.productName,
        productSku: item.productSku,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        totalPrice: this.getItemSubtotal(index),
        unit: item.unit,
        notes: item.notes
      }))
    };

    // 技術的負債: ネストされたsubscribe
    this.purchaseOrderService.createOrder(orderData).subscribe({
      next: (created) => {
        this.isSubmitting = false;
        // 技術的負債: ネストされたsubscribe — 作成後に自動で承認申請
        if (formValue.autoSubmit) {
          this.purchaseOrderService.submitForApproval(created.id).subscribe({
            next: () => {
              this.router.navigate(['/procurement/orders', created.id]);
            },
            error: (err) => {
              console.error('承認申請の自動送信に失敗しました', err);
              // 発注書は作成済みなので詳細画面へ遷移
              this.router.navigate(['/procurement/orders', created.id]);
            }
          });
        } else {
          this.router.navigate(['/procurement/orders', created.id]);
        }
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('発注書の作成に失敗しました', error);
        // 技術的負債 #14: エラーメッセージにステータスリテラルが混在
        if (error.status === 400) {
          this.errorMessage = '入力内容にエラーがあります。金額や数量を確認してください。';
        } else if (error.status === 409) {
          this.errorMessage = 'この発注書は既に作成されています。';
        } else {
          this.errorMessage = '発注書の作成に失敗しました。しばらく待ってから再度お試しください。';
        }
      }
    });
  }

  /** 下書き保存 */
  saveDraft(): void {
    this.isSubmitting = true;
    const formValue = this.orderForm.value;

    // 技術的負債 #14: ステータスリテラル
    const draftData: Partial<PurchaseOrder> = {
      supplierId: formValue.supplierId,
      supplierName: formValue.supplierName || '',
      status: 'DRAFT',
      expectedDeliveryDate: formValue.expectedDeliveryDate || '',
      totalAmount: this.totalAmount,
      currency: 'JPY',
      notes: formValue.notes || '',
      items: formValue.items
        .filter((item: any) => item.productId)
        .map((item: any, index: number) => ({
          productId: item.productId,
          productName: item.productName,
          productSku: item.productSku,
          quantity: item.quantity || 1,
          unitPrice: item.unitPrice || 0,
          totalPrice: this.getItemSubtotal(index),
          unit: item.unit || '個',
          notes: item.notes || ''
        }))
    };

    this.purchaseOrderService.createOrder(draftData).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: (created) => {
        this.router.navigate(['/procurement/orders', created.id]);
      },
      error: (error) => {
        console.error('下書き保存に失敗しました', error);
        this.errorMessage = '下書き保存に失敗しました。';
      }
    });
  }

  /** キャンセル */
  onCancel(): void {
    this.router.navigate(['/procurement/orders']);
  }

  /** 数量・単価・値引き変更ハンドラ */
  onAmountChange(): void {
    this.recalculateAmounts();
  }

  /** 全体値引き変更ハンドラ */
  onGlobalDiscountChange(): void {
    this.recalculateAmounts();
  }

  /** 全フォームフィールドをtouchedにする */
  private markFormAsTouched(): void {
    Object.keys(this.orderForm.controls).forEach(key => {
      const control = this.orderForm.get(key);
      if (control instanceof FormArray) {
        control.controls.forEach((group: AbstractControl) => {
          if (group instanceof FormGroup) {
            Object.keys(group.controls).forEach(childKey => {
              group.get(childKey)?.markAsTouched();
            });
          }
        });
      } else {
        control?.markAsTouched();
      }
    });
  }

  /** フィールドのエラーチェック */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.orderForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /** 明細行フィールドのエラーチェック */
  isItemFieldInvalid(index: number, fieldName: string): boolean {
    const item = this.items.at(index);
    const field = item?.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /** 消費税率変更ハンドラ */
  onTaxRateChange(rate: number): void {
    this.taxRate = rate;
    this.recalculateAmounts();
  }

  /**
   * 納品予定日の最小値（本日）を取得
   *
   * 技術的負債 #5: 日付バリデーションのヘルパーがコンポーネント内にある
   */
  getMinDeliveryDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
