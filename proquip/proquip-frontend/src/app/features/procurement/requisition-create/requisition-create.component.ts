import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, Observable } from 'rxjs';
import { takeUntil, debounceTime, switchMap, finalize } from 'rxjs/operators';
import { RequisitionService } from '@shared/services/requisition.service';
import { ProductService } from '@shared/services/product.service';
import { Product } from '@shared/models/product.model';
import { Requisition } from '@shared/models/purchase-order.model';
import { SelectOption } from '@shared/models/common.model';

/**
 * 購買依頼作成コンポーネント
 *
 * 技術的負債 #5: 合計金額の計算、明細行のバリデーションがすべてコンポーネント内にある
 * 本来はサービス層やユーティリティに分離すべき
 *
 * 技術的負債 #17: maxAmount バリデータが 1,000,000 に設定されている
 * バックエンド側の上限は 999,999.99 であり不整合がある
 */
@Component({
  selector: 'app-requisition-create',
  templateUrl: './requisition-create.component.html',
  styleUrls: ['./requisition-create.component.scss']
})
export class RequisitionCreateComponent implements OnInit, OnDestroy {

  /** 購買依頼フォーム */
  requisitionForm!: FormGroup;

  /** 送信中フラグ */
  isSubmitting = false;

  /** 合計金額 */
  totalAmount = 0;

  /** 製品検索結果（オートコンプリート用） */
  productSuggestions: Product[] = [];

  /** 製品検索表示フラグ（行インデックス別） */
  showSuggestions: { [key: number]: boolean } = {};

  /** 緊急度選択肢 */
  urgencyOptions: SelectOption[] = [
    { value: 'LOW', label: '低' },
    { value: 'NORMAL', label: '通常' },
    { value: 'HIGH', label: '高' },
    { value: 'URGENT', label: '緊急' }
  ];

  /** 部門選択肢 */
  departmentOptions: SelectOption[] = [
    { value: '総務部', label: '総務部' },
    { value: '営業部', label: '営業部' },
    { value: '技術部', label: '技術部' },
    { value: '製造部', label: '製造部' },
    { value: '品質管理部', label: '品質管理部' },
    { value: '物流部', label: '物流部' },
    { value: '経理部', label: '経理部' }
  ];

  /** エラーメッセージ */
  errorMessage = '';

  /** コンポーネント破棄用Subject */
  private destroy$ = new Subject<void>();

  /** 製品検索用Subject */
  private searchSubject$ = new Subject<string>();

  constructor(
    private fb: FormBuilder,
    private requisitionService: RequisitionService,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
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
    this.requisitionForm = this.fb.group({
      department: ['', [Validators.required]],
      justification: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      urgency: ['NORMAL', [Validators.required]],
      requiredDate: ['', [Validators.required]],
      items: this.fb.array([], [Validators.required])
    });
  }

  /** 製品検索のセットアップ */
  private setupProductSearch(): void {
    this.searchSubject$.pipe(
      debounceTime(300),
      switchMap((keyword: string) => {
        if (!keyword || keyword.length < 2) {
          return new Observable<any>(subscriber => {
            subscriber.next({ content: [] });
            subscriber.complete();
          });
        }
        return this.productService.searchProducts(keyword, 0, 10);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (result) => {
        this.productSuggestions = result.content || [];
      },
      error: (error) => {
        console.error('製品検索に失敗しました', error);
        this.productSuggestions = [];
      }
    });
  }

  /** 明細行のFormArrayを取得 */
  get items(): FormArray {
    return this.requisitionForm.get('items') as FormArray;
  }

  /** 明細行を追加 */
  addItem(): void {
    const itemGroup = this.fb.group({
      productId: [null, [Validators.required]],
      productName: ['', [Validators.required]],
      productSearch: [''],
      quantity: [1, [Validators.required, Validators.min(1), Validators.max(99999)]],
      estimatedUnitPrice: [0, [Validators.required, Validators.min(0)]],
      notes: ['']
    });

    this.items.push(itemGroup);
    this.recalculateTotal();
  }

  /** 明細行を削除 */
  removeItem(index: number): void {
    if (this.items.length <= 1) return;
    this.items.removeAt(index);
    this.recalculateTotal();
  }

  /** 製品を検索 */
  onProductSearch(keyword: string, index: number): void {
    this.showSuggestions[index] = true;
    this.searchSubject$.next(keyword);
  }

  /** 製品を選択 */
  selectProduct(product: Product, index: number): void {
    const item = this.items.at(index);
    item.patchValue({
      productId: product.id,
      productName: product.name,
      productSearch: product.name,
      estimatedUnitPrice: product.unitPrice
    });
    this.showSuggestions[index] = false;
    this.recalculateTotal();
  }

  /** 製品サジェストを非表示 */
  hideSuggestions(index: number): void {
    // 選択操作のためにわずかに遅延させる
    setTimeout(() => {
      this.showSuggestions[index] = false;
    }, 200);
  }

  /**
   * 合計金額を再計算する
   *
   * 技術的負債 #5: この計算ロジックはコンポーネントに直接記述されている
   * 専用の計算サービスまたはユーティリティに分離すべき
   */
  recalculateTotal(): void {
    let total = 0;
    for (let i = 0; i < this.items.length; i++) {
      const item = this.items.at(i);
      const quantity = item.get('quantity')?.value || 0;
      const unitPrice = item.get('estimatedUnitPrice')?.value || 0;
      total += quantity * unitPrice;
    }
    this.totalAmount = total;

    // 技術的負債 #17: フロントエンド上限 1,000,000 vs バックエンド上限 999,999.99
    this.validateTotalAmount();
  }

  /**
   * 合計金額バリデーション
   *
   * 技術的負債 #17: maxAmountバリデータが 1,000,000 に設定されているが、
   * バックエンド側の制限は 999,999.99 である
   * この不整合により、999,999.99 < 金額 <= 1,000,000 の範囲で
   * フロントエンドではOKだがバックエンドでエラーになるケースがある
   */
  private validateTotalAmount(): void {
    // 技術的負債 #17: 上限値がバックエンドと不整合
    const MAX_AMOUNT = 1000000;
    if (this.totalAmount > MAX_AMOUNT) {
      this.errorMessage = `合計金額は${MAX_AMOUNT.toLocaleString()}円以下にしてください`;
    } else {
      this.errorMessage = '';
    }
  }

  /**
   * 明細行のバリデーション
   *
   * 技術的負債 #5: バリデーションロジックがコンポーネント内に散在している
   */
  validateItems(): boolean {
    let isValid = true;

    for (let i = 0; i < this.items.length; i++) {
      const item = this.items.at(i);

      // 製品が選択されているか
      if (!item.get('productId')?.value) {
        isValid = false;
      }

      // 数量チェック
      const quantity = item.get('quantity')?.value;
      if (!quantity || quantity < 1) {
        isValid = false;
      }

      // 単価チェック
      const unitPrice = item.get('estimatedUnitPrice')?.value;
      if (unitPrice == null || unitPrice < 0) {
        isValid = false;
      }

      // 小計チェック（個別上限）
      const itemTotal = (quantity || 0) * (unitPrice || 0);
      if (itemTotal > 500000) {
        // 技術的負債 #5: ハードコードされた個別上限値
        isValid = false;
      }
    }

    // 明細行の合計金額チェック
    // 技術的負債 #17: フロントエンド上限がバックエンドと不一致
    if (this.totalAmount > 1000000) {
      isValid = false;
    }

    return isValid;
  }

  /**
   * 明細行の小計を計算する
   *
   * 技術的負債 #5: コンポーネント内のビジネスロジック
   */
  getItemSubtotal(index: number): number {
    const item = this.items.at(index);
    const quantity = item.get('quantity')?.value || 0;
    const unitPrice = item.get('estimatedUnitPrice')?.value || 0;
    return quantity * unitPrice;
  }

  /** フォーム送信 */
  onSubmit(): void {
    if (this.requisitionForm.invalid || !this.validateItems()) {
      this.markFormAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const formValue = this.requisitionForm.value;
    const requisitionData: Partial<Requisition> = {
      department: formValue.department,
      justification: formValue.justification,
      priority: formValue.urgency,
      requiredDate: formValue.requiredDate,
      items: formValue.items.map((item: any) => ({
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity,
        estimatedUnitPrice: item.estimatedUnitPrice,
        notes: item.notes
      }))
    };

    this.requisitionService.createRequisition(requisitionData).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: (created) => {
        this.router.navigate(['/procurement/requisitions', created.id]);
      },
      error: (error) => {
        console.error('購買依頼の作成に失敗しました', error);
        this.errorMessage = '購買依頼の作成に失敗しました。入力内容を確認してください。';
      }
    });
  }

  /** 下書き保存 */
  saveDraft(): void {
    const formValue = this.requisitionForm.value;
    const draftData: Partial<Requisition> = {
      department: formValue.department,
      justification: formValue.justification,
      priority: formValue.urgency,
      requiredDate: formValue.requiredDate,
      status: 'DRAFT',
      items: formValue.items.map((item: any) => ({
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity,
        estimatedUnitPrice: item.estimatedUnitPrice,
        notes: item.notes
      }))
    };

    this.isSubmitting = true;
    this.requisitionService.createRequisition(draftData).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: (created) => {
        this.router.navigate(['/procurement/requisitions', created.id]);
      },
      error: (error) => {
        console.error('下書き保存に失敗しました', error);
        this.errorMessage = '下書き保存に失敗しました。';
      }
    });
  }

  /** キャンセル */
  onCancel(): void {
    this.router.navigate(['/procurement/requisitions']);
  }

  /** 全フォームフィールドをtouchedにする */
  private markFormAsTouched(): void {
    Object.keys(this.requisitionForm.controls).forEach(key => {
      const control = this.requisitionForm.get(key);
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
    const field = this.requisitionForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /** 明細行フィールドのエラーチェック */
  isItemFieldInvalid(index: number, fieldName: string): boolean {
    const item = this.items.at(index);
    const field = item.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /** 数量変更ハンドラ */
  onQuantityChange(): void {
    this.recalculateTotal();
  }

  /** 単価変更ハンドラ */
  onPriceChange(): void {
    this.recalculateTotal();
  }
}
