import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, AbstractControl } from '@angular/forms';
import { ProductService } from '@shared/services/product.service';
import { Product } from '@shared/models/product.model';

/**
 * バンドル定義のインターフェース
 */
interface ProductBundle {
  id: number;
  name: string;
  description: string;
  products: BundleItem[];
  totalPrice: number;
  discountPercentage: number;
  bundlePrice: number;
  status: string;
  createdAt: string;
}

/**
 * バンドル内製品のインターフェース
 */
interface BundleItem {
  productId: number;
  productName: string;
  productSku: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

/**
 * バンドル管理コンポーネント
 * 製品バンドルのCRUD、構成製品の管理、価格計算
 *
 * 技術的負債: インライン価格計算がバックエンドのロジックを重複
 */
@Component({
  selector: 'app-bundle-management',
  templateUrl: './bundle-management.component.html',
  styleUrls: ['./bundle-management.component.scss']
})
export class BundleManagementComponent implements OnInit {

  /** バンドル一覧 */
  bundles: ProductBundle[] = [];

  /** 製品一覧（バンドルに追加用） */
  availableProducts: Product[] = [];

  /** ローディング状態 */
  isLoading = true;

  /** バンドル編集フォーム */
  bundleForm!: FormGroup;

  /** 編集モード */
  editMode: 'create' | 'edit' | null = null;

  /** 編集中のバンドルID */
  editingBundleId: number | null = null;

  /** 製品検索キーワード */
  productSearchKeyword = '';

  /** 検索結果 */
  searchResults: Product[] = [];

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** 送信中フラグ */
  isSubmitting = false;

  /** 計算されたバンドル価格情報 */
  calculatedTotalPrice = 0;
  calculatedDiscountAmount = 0;
  calculatedBundlePrice = 0;

  /** 削除確認 */
  showDeleteConfirm = false;
  deletingBundle: ProductBundle | null = null;

  constructor(
    private fb: FormBuilder,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadBundles();
    this.loadProducts();
  }

  /**
   * フォームを初期化
   */
  private initializeForm(): void {
    this.bundleForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', [Validators.maxLength(1000)]],
      discountPercentage: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      status: ['ACTIVE', [Validators.required]],
      items: this.fb.array([])
    });

    // 割引率変更時に価格を再計算
    this.bundleForm.get('discountPercentage')?.valueChanges.subscribe(() => {
      this.recalculatePrices();
    });
  }

  /** バンドルアイテムのFormArrayを取得 */
  get bundleItems(): FormArray {
    return this.bundleForm.get('items') as FormArray;
  }

  /**
   * バンドル一覧をロード
   */
  private loadBundles(): void {
    this.isLoading = true;

    this.productService.getBundles().subscribe(
      (bundles) => {
        this.bundles = bundles;
        this.isLoading = false;
      },
      (error) => {
        console.error('バンドル取得エラー:', error);
        this.errorMessage = 'バンドルの取得に失敗しました。';
        this.isLoading = false;
      }
    );
  }

  /**
   * 製品一覧をロード
   */
  private loadProducts(): void {
    this.productService.getProducts(0, 100).subscribe(
      (result) => {
        this.availableProducts = result.content;
      },
      (error) => {
        console.error('製品一覧取得エラー:', error);
      }
    );
  }

  /**
   * 製品を検索
   */
  searchProducts(): void {
    if (!this.productSearchKeyword) {
      this.searchResults = [];
      return;
    }

    const keyword = this.productSearchKeyword.toLowerCase();
    this.searchResults = this.availableProducts.filter(p =>
      p.name.toLowerCase().includes(keyword) ||
      p.sku.toLowerCase().includes(keyword)
    ).slice(0, 10);
  }

  /**
   * バンドルに製品を追加
   */
  addProductToBundle(product: Product): void {
    // 既に追加されている場合は数量を増やす
    const existingIndex = this.bundleItems.controls.findIndex(
      (control: AbstractControl) => control.get('productId')?.value === product.id
    );

    if (existingIndex >= 0) {
      const quantityControl = this.bundleItems.at(existingIndex).get('quantity');
      if (quantityControl) {
        quantityControl.setValue(quantityControl.value + 1);
      }
    } else {
      const item = this.fb.group({
        productId: [product.id],
        productName: [product.name],
        productSku: [product.sku],
        unitPrice: [product.unitPrice],
        quantity: [1, [Validators.required, Validators.min(1)]]
      });

      // 数量変更時に価格を再計算
      item.get('quantity')?.valueChanges.subscribe(() => {
        this.recalculatePrices();
      });

      this.bundleItems.push(item);
    }

    this.recalculatePrices();
    this.productSearchKeyword = '';
    this.searchResults = [];
  }

  /**
   * バンドルから製品を削除
   */
  removeProductFromBundle(index: number): void {
    this.bundleItems.removeAt(index);
    this.recalculatePrices();
  }

  /**
   * バンドル価格を再計算
   * 技術的負債: バックエンドの価格計算ロジックと重複
   */
  recalculatePrices(): void {
    let totalPrice = 0;

    this.bundleItems.controls.forEach((control: AbstractControl) => {
      const unitPrice = control.get('unitPrice')?.value || 0;
      const quantity = control.get('quantity')?.value || 0;
      totalPrice += unitPrice * quantity;
    });

    const discountPercentage = this.bundleForm.get('discountPercentage')?.value || 0;
    const discountAmount = Math.floor(totalPrice * (discountPercentage / 100));
    const bundlePrice = totalPrice - discountAmount;

    this.calculatedTotalPrice = totalPrice;
    this.calculatedDiscountAmount = discountAmount;
    this.calculatedBundlePrice = bundlePrice;
  }

  /**
   * 新規作成モードを開始
   */
  startCreate(): void {
    this.editMode = 'create';
    this.editingBundleId = null;
    this.bundleForm.reset({
      name: '',
      description: '',
      discountPercentage: 0,
      status: 'ACTIVE'
    });
    // FormArrayをクリア
    while (this.bundleItems.length > 0) {
      this.bundleItems.removeAt(0);
    }
    this.calculatedTotalPrice = 0;
    this.calculatedDiscountAmount = 0;
    this.calculatedBundlePrice = 0;
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * 編集モードを開始
   */
  startEdit(bundle: ProductBundle): void {
    this.editMode = 'edit';
    this.editingBundleId = bundle.id;
    this.bundleForm.patchValue({
      name: bundle.name,
      description: bundle.description,
      discountPercentage: bundle.discountPercentage,
      status: bundle.status
    });

    // FormArrayをクリアして再構築
    while (this.bundleItems.length > 0) {
      this.bundleItems.removeAt(0);
    }

    bundle.products.forEach(item => {
      const itemGroup = this.fb.group({
        productId: [item.productId],
        productName: [item.productName],
        productSku: [item.productSku],
        unitPrice: [item.unitPrice],
        quantity: [item.quantity, [Validators.required, Validators.min(1)]]
      });

      itemGroup.get('quantity')?.valueChanges.subscribe(() => {
        this.recalculatePrices();
      });

      this.bundleItems.push(itemGroup);
    });

    this.recalculatePrices();
    this.errorMessage = '';
    this.successMessage = '';
  }

  /**
   * 編集をキャンセル
   */
  cancelEdit(): void {
    this.editMode = null;
    this.editingBundleId = null;
  }

  /**
   * バンドルを保存
   */
  saveBundle(): void {
    this.bundleForm.markAllAsTouched();
    if (this.bundleForm.invalid || this.bundleItems.length === 0 || this.isSubmitting) {
      if (this.bundleItems.length === 0) {
        this.errorMessage = 'バンドルには少なくとも1つの製品を追加してください。';
      }
      return;
    }

    this.isSubmitting = true;

    const apiData = {
      name: this.bundleForm.get('name')?.value,
      description: this.bundleForm.get('description')?.value || '',
      discountPercentage: this.bundleForm.get('discountPercentage')?.value,
      status: this.bundleForm.get('status')?.value,
      items: this.bundleItems.controls.map((control: AbstractControl) => ({
        productId: control.get('productId')?.value,
        quantity: control.get('quantity')?.value
      }))
    };

    const request$ = this.editMode === 'create'
      ? this.productService.createBundle(apiData)
      : this.productService.updateBundle(this.editingBundleId!, apiData);

    request$.subscribe(
      () => {
        this.successMessage = this.editMode === 'create' ? 'バンドルを作成しました。' : 'バンドルを更新しました。';
        this.isSubmitting = false;
        this.cancelEdit();
        this.loadBundles();
      },
      (error) => {
        console.error('バンドル保存エラー:', error);
        this.errorMessage = 'バンドルの保存に失敗しました。';
        this.isSubmitting = false;
      }
    );
  }

  /**
   * 削除確認を表示
   */
  confirmDelete(bundle: ProductBundle): void {
    this.deletingBundle = bundle;
    this.showDeleteConfirm = true;
  }

  /**
   * バンドルを削除
   */
  deleteBundle(): void {
    if (!this.deletingBundle) return;

    const id = this.deletingBundle.id;
    this.productService.deleteBundle(id).subscribe(
      () => {
        this.successMessage = 'バンドルを削除しました。';
        this.showDeleteConfirm = false;
        this.deletingBundle = null;
        this.loadBundles();
      },
      (error) => {
        console.error('バンドル削除エラー:', error);
        this.errorMessage = 'バンドルの削除に失敗しました。';
        this.showDeleteConfirm = false;
        this.deletingBundle = null;
      }
    );
  }

  /**
   * 削除確認をキャンセル
   */
  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deletingBundle = null;
  }

  /**
   * 金額フォーマット
   */
  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + amount.toLocaleString('ja-JP');
  }

  /**
   * ステータスラベル
   */
  getStatusLabel(status: string): string {
    const labels: any = {
      'ACTIVE': '有効',
      'INACTIVE': '無効',
      'DRAFT': '下書き'
    };
    return labels[status] || status;
  }
}
