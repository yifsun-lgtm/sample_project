import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, AbstractControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '@shared/services/product.service';
import { ProductDetail, Category } from '@shared/models/product.model';

interface ExistingImage {
  id: number;
  url: string;
  isPrimary: boolean;
}

interface ExistingDocument {
  id: number;
  fileName: string;
  filePath: string;
}

/**
 * 製品編集コンポーネント
 * 既存製品情報を編集するフォーム
 *
 * 技術的負債 #2: product-createコンポーネントからの大量コピペ
 * 本来は共通フォームコンポーネントに切り出すか、createとeditを統合すべき
 * 技術的負債: 未保存変更ガードが未実装
 */
@Component({
  selector: 'app-product-edit',
  templateUrl: './product-edit.component.html',
  styleUrls: ['./product-edit.component.scss']
})
export class ProductEditComponent implements OnInit {

  /** 製品ID */
  productId!: number;

  /** 元の製品データ */
  product: ProductDetail | null = null;

  /** ローディング状態 */
  isLoading = true;

  /** 送信中フラグ */
  isSubmitting = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** メインフォーム */
  editForm!: FormGroup;

  /** 仕様フォーム（動的行） */
  specForm!: FormGroup;

  /** カテゴリ一覧 */
  categories: Category[] = [];

  /** メーカー一覧 */
  // 技術的負債 #2: product-createと同じハードコードリスト（コピペ）
  manufacturers: any[] = [];

  /** ステータスオプション */
  // 技術的負債 #2: product-createと同じ定義（コピペ）
  statusOptions = [
    { value: 'ACTIVE', label: '有効' },
    { value: 'INACTIVE', label: '無効' },
    { value: 'DISCONTINUED', label: '廃番' },
    { value: 'PENDING', label: '保留' }
  ];

  /** SKU重複チェック結果 */
  skuExists = false;
  skuCheckLoading = false;

  /** 元のSKU（変更チェック用） */
  originalSku = '';

  /** 既存の画像一覧 */
  existingImages: ExistingImage[] = [];

  /** 既存のドキュメント一覧 */
  existingDocuments: ExistingDocument[] = [];

  /** 画像アップロード中 */
  imageUploading = false;

  /** ドキュメントアップロード中 */
  docUploading = false;

  /** ドキュメント種別オプション */
  docTypeOptions = [
    { value: 'DATASHEET', label: '仕様書' },
    { value: 'BROCHURE', label: 'カタログ' },
    { value: 'MANUAL', label: 'マニュアル' },
    { value: 'DRAWING', label: '図面' },
    { value: 'OTHER', label: 'その他' }
  ];

  /** 新規ドキュメント種別 */
  newDocType = 'DATASHEET';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    this.productId = Number(this.route.snapshot.paramMap.get('id'));
    this.initializeForm();
    this.loadCategories();
    this.loadManufacturers();
    this.loadProduct();
  }

  /**
   * フォームを初期化
   * 技術的負債 #2: product-createのinitializeFormsとほぼ同一のコード
   */
  private initializeForm(): void {
    this.editForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(200)]],
      // 技術的負債 #17: SKU正規表現がバックエンドと不一致（createと同じ問題）
      sku: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{2,4}-\d{3,8}$/)]],
      description: ['', [Validators.maxLength(2000)]],
      categoryId: [null, [Validators.required]],
      manufacturerId: [null, [Validators.required]],
      status: ['ACTIVE', [Validators.required]],
      unitPrice: [null, [Validators.required, Validators.min(0)]],
      unit: ['個', [Validators.required]],
      minimumOrderQuantity: [1, [Validators.required, Validators.min(1)]],
      leadTimeDays: [7, [Validators.required, Validators.min(1)]],
      weight: [null, [Validators.min(0)]],
      dimensions: [''],
      notes: ['']
    });

    this.specForm = this.fb.group({
      specifications: this.fb.array([])
    });
  }

  /**
   * カテゴリ一覧をロード
   * 技術的負債 #2: product-createと同じコード
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

  /**
   * 製品データをロードしてフォームにセット
   */
  private loadProduct(): void {
    this.isLoading = true;
    this.productService.getProduct(this.productId).subscribe(
      (product) => {
        this.product = product;
        this.originalSku = product.sku;
        this.populateForm(product);
        this.isLoading = false;
      },
      (error) => {
        console.error('製品データ取得エラー:', error);
        this.errorMessage = '製品データの取得に失敗しました。';
        this.isLoading = false;
      }
    );
  }

  /**
   * フォームに既存データをセット
   */
  private populateForm(product: ProductDetail): void {
    this.editForm.patchValue({
      name: product.name,
      sku: product.sku,
      description: product.description,
      categoryId: product.categoryId,
      manufacturerId: product.manufacturerId,
      status: product.status,
      unitPrice: product.unitPrice,
      unit: product.unit,
      minimumOrderQuantity: product.minimumOrderQuantity,
      leadTimeDays: product.leadTimeDays,
      weight: product.weight,
      dimensions: product.dimensions,
      notes: product.notes
    });

    // 仕様データをフォーム行に展開
    this.populateSpecifications(product.specifications);

    // 画像・ドキュメントを読み込み
    if (product.images) {
      this.existingImages = product.images.map((img: any) => ({
        id: img.id,
        url: img.fileName || '/assets/images/no-image.png',
        isPrimary: img.primary || false
      }));
    }
    if (product.documents) {
      this.existingDocuments = product.documents.map((doc: any) => ({
        id: doc.id,
        fileName: doc.fileName || 'unknown',
        filePath: doc.filePath || ''
      }));
    }
  }

  /**
   * 仕様データをパースしてフォーム行を生成
   */
  private populateSpecifications(specJson: string): void {
    const specArray = this.specForm.get('specifications') as FormArray;
    specArray.clear();

    try {
      if (specJson) {
        const specs = JSON.parse(specJson);
        Object.keys(specs).forEach(key => {
          specArray.push(this.fb.group({
            key: [key, [Validators.required]],
            value: [specs[key], [Validators.required]]
          }));
        });
      }
    } catch (e) {
      // パース失敗時は空行を1つ追加
    }

    // 行がない場合は1行追加
    if (specArray.length === 0) {
      this.addSpecificationRow();
    }
  }

  /** 仕様行の配列を取得 */
  get specificationRows(): FormArray {
    return this.specForm.get('specifications') as FormArray;
  }

  /**
   * 仕様行を追加
   * 技術的負債 #2: product-createと同じコード
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
   * 技術的負債 #2: product-createと同じコード
   */
  removeSpecificationRow(index: number): void {
    if (this.specificationRows.length > 1) {
      this.specificationRows.removeAt(index);
    }
  }

  /**
   * SKU重複チェック
   * 技術的負債 #2: product-createと同じコード（SKU未変更時はスキップのロジック追加）
   */
  checkSkuDuplicate(): void {
    const sku = this.editForm.get('sku')?.value;
    if (!sku || this.editForm.get('sku')?.invalid) return;

    // SKUが変更されていない場合はチェック不要
    if (sku === this.originalSku) {
      this.skuExists = false;
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
   * SKUエラーメッセージを生成
   * 技術的負債 #2: product-createと同じコード
   */
  getSkuErrorMessage(): string {
    const skuControl = this.editForm.get('sku');
    if (!skuControl?.touched) return '';
    if (skuControl?.errors?.['required']) return 'SKUは必須です';
    if (skuControl?.errors?.['pattern']) return 'SKU形式が不正です（例: AB-12345）';
    if (this.skuExists) return 'このSKUは既に使用されています';
    return '';
  }

  /**
   * 金額フォーマット
   */
  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + amount.toLocaleString('ja-JP');
  }

  /**
   * 更新を実行
   */
  submit(): void {
    this.editForm.markAllAsTouched();

    if (this.editForm.invalid || this.skuExists || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    // 仕様データをJSON文字列に変換
    const specs: any = {};
    this.specificationRows.controls.forEach((control: AbstractControl) => {
      const key = control.get('key')?.value;
      const value = control.get('value')?.value;
      if (key && value) {
        specs[key] = value;
      }
    });

    const productData = {
      ...this.editForm.value,
      specifications: JSON.stringify(specs)
    };

    this.productService.updateProduct(this.productId, productData).subscribe(
      () => {
        this.isSubmitting = false;
        this.successMessage = '製品情報が更新されました。';
        setTimeout(() => {
          this.router.navigate(['/products', this.productId]);
        }, 1500);
      },
      (error) => {
        console.error('製品更新エラー:', error);
        this.isSubmitting = false;
        this.errorMessage = '製品の更新に失敗しました。入力内容を確認してください。';
      }
    );
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    this.imageUploading = true;
    const file = input.files[0];
    const isPrimary = this.existingImages.length === 0;

    this.productService.uploadImage(this.productId, file, isPrimary).subscribe(
      (result) => {
        this.existingImages.push({
          id: result.id,
          url: result.url,
          isPrimary: result.isPrimary
        });
        this.imageUploading = false;
      },
      (error) => {
        console.error('画像アップロードエラー:', error);
        this.errorMessage = '画像のアップロードに失敗しました。';
        this.imageUploading = false;
      }
    );
    input.value = '';
  }

  deleteImage(index: number): void {
    const img = this.existingImages[index];
    this.productService.deleteImage(this.productId, img.id).subscribe(
      () => {
        this.existingImages.splice(index, 1);
      },
      (error) => {
        console.error('画像削除エラー:', error);
        this.errorMessage = '画像の削除に失敗しました。';
      }
    );
  }

  onDocumentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    this.docUploading = true;
    const file = input.files[0];

    this.productService.uploadDocument(this.productId, file, this.newDocType).subscribe(
      (result) => {
        this.existingDocuments.push({
          id: result.id,
          fileName: result.fileName,
          filePath: result.filePath
        });
        this.docUploading = false;
      },
      (error) => {
        console.error('ドキュメントアップロードエラー:', error);
        this.errorMessage = 'ドキュメントのアップロードに失敗しました。';
        this.docUploading = false;
      }
    );
    input.value = '';
  }

  deleteDocument(index: number): void {
    const doc = this.existingDocuments[index];
    this.productService.deleteDocument(this.productId, doc.id).subscribe(
      () => {
        this.existingDocuments.splice(index, 1);
      },
      (error) => {
        console.error('ドキュメント削除エラー:', error);
        this.errorMessage = 'ドキュメントの削除に失敗しました。';
      }
    );
  }

  /**
   * キャンセルして詳細画面へ戻る
   */
  cancel(): void {
    this.router.navigate(['/products', this.productId]);
  }
}
