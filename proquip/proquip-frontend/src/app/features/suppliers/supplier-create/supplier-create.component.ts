import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SupplierService } from '@shared/services/supplier.service';

/**
 * サプライヤー新規登録コンポーネント
 * リアクティブフォームによるサプライヤー情報の登録
 * 複数連絡先の追加が可能
 */
@Component({
  selector: 'app-supplier-create',
  templateUrl: './supplier-create.component.html',
  styleUrls: ['./supplier-create.component.scss']
})
export class SupplierCreateComponent implements OnInit {

  /** メインフォーム */
  supplierForm!: FormGroup;

  /** 送信中フラグ */
  isSubmitting = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** ステータスオプション */
  statusOptions = [
    { value: 'ACTIVE', label: '取引中' },
    { value: 'INACTIVE', label: '取引停止' },
    { value: 'PENDING', label: '審査中' }
  ];

  /** 支払条件オプション */
  paymentTermsOptions = [
    { value: 'NET_30', label: '月末締め翌月末払い' },
    { value: 'NET_60', label: '月末締め翌々月末払い' },
    { value: 'NET_15', label: '月末締め翌月15日払い' },
    { value: 'PREPAID', label: '前払い' },
    { value: 'COD', label: '代金引換' },
    { value: 'OTHER', label: 'その他' }
  ];

  constructor(
    private fb: FormBuilder,
    private supplierService: SupplierService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  /**
   * フォームを初期化
   */
  private initializeForm(): void {
    this.supplierForm = this.fb.group({
      // 基本情報
      code: ['', [Validators.required, Validators.pattern(/^[A-Z]{2,4}-\d{3,6}$/)]],
      name: ['', [Validators.required, Validators.maxLength(200)]],
      nameKana: ['', [Validators.maxLength(200)]],
      status: ['PENDING', [Validators.required]],

      // 連絡先情報
      email: ['', [Validators.email]],
      phone: ['', [Validators.pattern(/^[\d\-\+\(\)]+$/)]],
      website: [''],

      // 住所
      address: ['', [Validators.maxLength(500)]],

      // 取引条件
      paymentTerms: ['NET_30'],

      // 備考
      notes: ['', [Validators.maxLength(2000)]],

      // 担当者リスト（複数追加可能）
      contacts: this.fb.array([])
    });

    // デフォルトで1人の担当者を追加
    this.addContact();
  }

  /** 連絡先FormArrayを取得 */
  get contactsArray(): FormArray {
    return this.supplierForm.get('contacts') as FormArray;
  }

  /**
   * 連絡先を追加
   */
  addContact(): void {
    const contact = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      department: ['', [Validators.maxLength(100)]],
      position: ['', [Validators.maxLength(100)]],
      phone: ['', [Validators.pattern(/^[\d\-\+\(\)]+$/)]],
      email: ['', [Validators.email]],
      isPrimary: [this.contactsArray.length === 0] // 最初の担当者を主担当に
    });
    this.contactsArray.push(contact);
  }

  /**
   * 連絡先を削除
   */
  removeContact(index: number): void {
    if (this.contactsArray.length > 1) {
      const wasPrimary = this.contactsArray.at(index).get('isPrimary')?.value;
      this.contactsArray.removeAt(index);

      // 主担当が削除された場合、最初の担当者を主担当にする
      if (wasPrimary && this.contactsArray.length > 0) {
        this.contactsArray.at(0).get('isPrimary')?.setValue(true);
      }
    }
  }

  /**
   * 主担当を切り替え
   */
  setPrimaryContact(index: number): void {
    this.contactsArray.controls.forEach((control, i) => {
      control.get('isPrimary')?.setValue(i === index);
    });
  }

  /**
   * コードエラーメッセージ
   */
  getCodeErrorMessage(): string {
    const codeControl = this.supplierForm.get('code');
    if (!codeControl?.touched) return '';
    if (codeControl?.errors?.['required']) return 'サプライヤーコードは必須です';
    if (codeControl?.errors?.['pattern']) return 'コード形式が不正です（例: SUP-001）';
    return '';
  }

  /**
   * サプライヤーを登録
   */
  submit(): void {
    this.supplierForm.markAllAsTouched();

    if (this.supplierForm.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const formData = this.supplierForm.value;

    this.supplierService.createSupplier(formData).subscribe(
      (created) => {
        this.isSubmitting = false;
        this.successMessage = 'サプライヤーが正常に登録されました。';
        setTimeout(() => {
          this.router.navigate(['/suppliers', created.id]);
        }, 1500);
      },
      (error) => {
        console.error('サプライヤー登録エラー:', error);
        this.isSubmitting = false;
        this.errorMessage = 'サプライヤーの登録に失敗しました。入力内容を確認してください。';
      }
    );
  }

  /**
   * キャンセルして一覧へ戻る
   */
  cancel(): void {
    this.router.navigate(['/suppliers']);
  }
}
