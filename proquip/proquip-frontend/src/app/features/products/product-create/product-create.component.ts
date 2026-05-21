import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '@shared/services/product.service';
import { Category } from '@shared/models/product.model';
import { forkJoin, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';

/**
 * 製品新規登録コンポーネント（ウィザード形式）
 * 4ステップのウィザードで製品を登録
 *
 * 技術的負債: ウィザードのステート管理が全てコンポーネント内にあり、サービスに分離されていない
 * 技術的負債 #17: SKUバリデーションの正規表現がバックエンドと異なる
 * 技術的負債: 未保存変更ガードが実装されていない
 */
@Component({
  selector: 'app-product-create',
  templateUrl: './product-create.component.html',
  styleUrls: ['./product-create.component.scss']
})
export class ProductCreateComponent implements OnInit {

  /** 現在のウィザードステップ（0始まり） */
  currentStep = 0;

  /** ウィザードステップ定義 */
  steps = [
    { title: '基本情報', description: '製品名、SKU、カテゴリ' },
    { title: '価格・在庫', description: '価格、単位、発注点' },
    { title: '仕様', description: '製品仕様の詳細' },
    { title: '画像・資料', description: '画像・ドキュメント' },
    { title: '確認', description: '入力内容の最終確認' }
  ];

  /** 基本情報フォーム */
  basicForm!: FormGroup;

  /** 価格・在庫フォーム */
  pricingForm!: FormGroup;

  /** 仕様フォーム */
  specForm!: FormGroup;

  /** カテゴリ一覧 */
  categories: Category[] = [];

  /** メーカー一覧 */
  // 技術的負債: any型を使用
  manufacturers: any[] = [];

  /** ステータスオプション */
  statusOptions = [
    { value: 'ACTIVE', label: '有効' },
    { value: 'INACTIVE', label: '無効' },
    { value: 'PENDING', label: '保留' }
  ];

  /** SKU重複チェック結果 */
  skuExists = false;
  skuCheckLoading = false;

  /** 送信中フラグ */
  isSubmitting = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** ステージング中の画像ファイル */
  pendingImages: { file: File; preview: string; isPrimary: boolean }[] = [];

  /** ステージング中のドキュメントファイル */
  pendingDocuments: { file: File; docType: string }[] = [];

  /** ドキュメント種別オプション */
  docTypeOptions = [
    { value: 'DATASHEET', label: '仕様書' },
    { value: 'BROCHURE', label: 'カタログ' },
    { value: 'MANUAL', label: 'マニュアル' },
    { value: 'DRAWING', label: '図面' },
    { value: 'OTHER', label: 'その他' }
  ];

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForms();
    this.loadCategories();
    this.loadManufacturers();
  }

  /**
   * フォームを初期化
   * 技術的負債: ウィザード状態管理がコンポーネント内のネストされたオブジェクトで管理されている
   */
  private initializeForms(): void {
    // ステップ1: 基本情報
    this.basicForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      // 技術的負債 #17: SKUバリデーション正規表現がバックエンドと異なる
      // バックエンド: ^[A-Z]{2,4}-\d{4,8}$
      // フロントエンド（ここ）: ^[A-Z0-9]{2,4}-\d{3,8}$（数字始まりも許可、3桁も許可）
      sku: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{2,4}-\d{3,8}$/)]],
      description: ['', [Validators.maxLength(2000)]],
      categoryId: [null, [Validators.required]],
      manufacturerId: [null, [Validators.required]],
      status: ['ACTIVE', [Validators.required]]
    });

    // ステップ2: 価格・在庫
    this.pricingForm = this.fb.group({
      unitPrice: [null, [Validators.required, Validators.min(0)]],
      unit: ['個', [Validators.required]],
      minimumOrderQuantity: [1, [Validators.required, Validators.min(1)]],
      leadTimeDays: [7, [Validators.required, Validators.min(1)]],
      weight: [null, [Validators.min(0)]],
      dimensions: ['']
    });

    // ステップ3: 仕様（動的行）
    this.specForm = this.fb.group({
      specifications: this.fb.array([]),
      notes: ['']
    });

    // デフォルトで1行追加
    this.addSpecificationRow();
  }

  /**
   * カテゴリ一覧をロード
   */
  private loadCategories(): void {
    this.productService.getCategories().subscribe(
      (categories) => {
        this.categories = categories;
      },
      (error) => {
        console.error('カテゴリ取得エラー:', error);
      }
    );
  }

  private loadManufacturers(): void {
    this.productService.getManufacturers().subscribe(
      (data) => { this.manufacturers = data; },
      (error) => { console.error('メーカー取得エラー:', error); }
    );
  }

  /** 仕様フォームの配列を取得 */
  get specificationRows(): FormArray {
    return this.specForm.get('specifications') as FormArray;
  }

  /**
   * 仕様行を追加
   */
  addSpecificationRow(): void {
    const row = this.fb.group({
      key: ['', [Validators.required]],
      value: ['', [Validators.required]]
    });
    this.specificationRows.push(row);
  }

  /**
   * 仕様行を削除
   */
  removeSpecificationRow(index: number): void {
    if (this.specificationRows.length > 1) {
      this.specificationRows.removeAt(index);
    }
  }

  /**
   * SKU重複チェック
   * 技術的負債 #17: フロントのバリデーションルールがバックエンドと不一致
   */
  checkSkuDuplicate(): void {
    const sku = this.basicForm.get('sku')?.value;
    if (!sku || this.basicForm.get('sku')?.invalid) {
      return;
    }

    this.skuCheckLoading = true;
    this.productService.checkSkuExists(sku).subscribe(
      (exists) => {
        this.skuExists = exists;
        this.skuCheckLoading = false;
      },
      (error) => {
        console.error('SKU重複チェックエラー:', error);
        this.skuCheckLoading = false;
      }
    );
  }

  /**
   * 次のステップへ進む
   */
  nextStep(): void {
    if (this.validateCurrentStep()) {
      this.currentStep++;
    }
  }

  /**
   * 前のステップへ戻る
   */
  prevStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  /**
   * 特定のステップへジャンプ
   */
  goToStep(step: number): void {
    // 完了済みステップまたは現在のステップの前のみ遷移可能
    if (step <= this.currentStep) {
      this.currentStep = step;
    }
  }

  /**
   * 現在のステップのバリデーション
   */
  private validateCurrentStep(): boolean {
    switch (this.currentStep) {
      case 0:
        this.basicForm.markAllAsTouched();
        return this.basicForm.valid && !this.skuExists;
      case 1:
        this.pricingForm.markAllAsTouched();
        return this.pricingForm.valid;
      case 2:
        this.specForm.markAllAsTouched();
        // 仕様は空行でも許可（optionalの場合）
        return true;
      default:
        return true;
    }
  }

  /**
   * ステップが完了しているかチェック
   */
  isStepComplete(step: number): boolean {
    switch (step) {
      case 0:
        return this.basicForm.valid && !this.skuExists;
      case 1:
        return this.pricingForm.valid;
      case 2:
        return true; // 仕様はオプション
      default:
        return false;
    }
  }

  /**
   * フォームのコントロールを取得（テンプレート用ヘルパー）
   */
  getControl(form: FormGroup, name: string): AbstractControl | null {
    return form.get(name);
  }

  /**
   * SKUエラーメッセージを生成
   */
  getSkuErrorMessage(): string {
    const skuControl = this.basicForm.get('sku');
    if (!skuControl?.touched) return '';
    if (skuControl?.errors?.['required']) return 'SKUは必須です';
    if (skuControl?.errors?.['pattern']) return 'SKU形式が不正です（例: AB-12345）';
    if (this.skuExists) return 'このSKUは既に使用されています';
    return '';
  }

  /**
   * カテゴリ名を取得
   */
  getCategoryName(id: number): string {
    const category = this.categories.find(c => c.id === Number(id));
    return category ? category.name : '-';
  }

  /**
   * メーカー名を取得
   */
  getManufacturerName(id: number): string {
    const manufacturer = this.manufacturers.find((m: any) => m.id === Number(id));
    return manufacturer ? manufacturer.name : '-';
  }

  /**
   * ステータスラベルを取得
   */
  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(o => o.value === status);
    return option ? option.label : status;
  }

  /**
   * 確認画面用: 仕様データを整形
   */
  getSpecificationsForReview(): { key: string; value: string }[] {
    return this.specificationRows.controls
      .map((control: AbstractControl) => ({
        key: control.get('key')?.value || '',
        value: control.get('value')?.value || ''
      }))
      .filter(spec => spec.key && spec.value);
  }

  /**
   * 金額フォーマット
   */
  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + amount.toLocaleString('ja-JP');
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    for (let i = 0; i < input.files.length; i++) {
      const file = input.files[i];
      const reader = new FileReader();
      reader.onload = (e) => {
        this.pendingImages.push({
          file,
          preview: e.target?.result as string,
          isPrimary: this.pendingImages.length === 0
        });
      };
      reader.readAsDataURL(file);
    }
    input.value = '';
  }

  removeImage(index: number): void {
    const wasPrimary = this.pendingImages[index].isPrimary;
    this.pendingImages.splice(index, 1);
    if (wasPrimary && this.pendingImages.length > 0) {
      this.pendingImages[0].isPrimary = true;
    }
  }

  setPrimaryImage(index: number): void {
    this.pendingImages.forEach((img, i) => img.isPrimary = i === index);
  }

  onDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    for (let i = 0; i < input.files.length; i++) {
      this.pendingDocuments.push({ file: input.files[i], docType: 'DATASHEET' });
    }
    input.value = '';
  }

  removeDocument(index: number): void {
    this.pendingDocuments.splice(index, 1);
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  submit(): void {
    if (this.isSubmitting) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    const specs: any = {};
    this.getSpecificationsForReview().forEach(spec => {
      specs[spec.key] = spec.value;
    });

    const productData = {
      ...this.basicForm.value,
      ...this.pricingForm.value,
      specifications: JSON.stringify(specs),
      notes: this.specForm.get('notes')?.value || ''
    };

    this.productService.createProduct(productData).pipe(
      switchMap((created) => {
        const uploads: any[] = [];

        this.pendingImages.forEach(img => {
          uploads.push(
            this.productService.uploadImage(created.id, img.file, img.isPrimary).pipe(
              catchError(err => { console.error('画像アップロードエラー:', err); return of(null); })
            )
          );
        });

        this.pendingDocuments.forEach(doc => {
          uploads.push(
            this.productService.uploadDocument(created.id, doc.file, doc.docType).pipe(
              catchError(err => { console.error('ドキュメントアップロードエラー:', err); return of(null); })
            )
          );
        });

        if (uploads.length > 0) {
          return forkJoin(uploads).pipe(
            switchMap(() => of(created))
          );
        }
        return of(created);
      })
    ).subscribe(
      (created) => {
        this.isSubmitting = false;
        this.successMessage = '製品が正常に登録されました。';
        setTimeout(() => {
          this.router.navigate(['/products', created.id]);
        }, 2000);
      },
      (error) => {
        console.error('製品登録エラー:', error);
        this.isSubmitting = false;
        this.errorMessage = '製品の登録に失敗しました。入力内容を確認してください。';
      }
    );
  }

  /**
   * キャンセルして一覧へ戻る
   * 技術的負債: 未保存変更の確認ダイアログが未実装
   */
  cancel(): void {
    // 技術的負債: CanDeactivateガードを実装すべき
    this.router.navigate(['/products']);
  }
}
