import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SupplierService } from '@shared/services/supplier.service';
import { Supplier, SupplierContact } from '@shared/models/supplier.model';

/**
 * サプライヤー編集コンポーネント
 *
 * 技術的負債 #2: supplier-createからの大量コピペ
 * 本来は共通フォームコンポーネントに切り出すべき
 */
@Component({
  selector: 'app-supplier-edit',
  templateUrl: './supplier-edit.component.html',
  styleUrls: ['./supplier-edit.component.scss']
})
export class SupplierEditComponent implements OnInit {

  /** サプライヤーID */
  supplierId!: number;

  /** 元のサプライヤーデータ */
  supplier: Supplier | null = null;

  /** ローディング状態 */
  isLoading = true;

  /** 送信中フラグ */
  isSubmitting = false;

  /** エラーメッセージ */
  errorMessage = '';

  /** 成功メッセージ */
  successMessage = '';

  /** メインフォーム */
  supplierForm!: FormGroup;

  /** ステータスオプション */
  // 技術的負債 #2: supplier-createと同じ定義（コピペ）
  statusOptions = [
    { value: 'ACTIVE', label: '取引中' },
    { value: 'INACTIVE', label: '取引停止' },
    { value: 'PENDING', label: '審査中' },
    { value: 'BLOCKED', label: 'ブロック済み' }
  ];

  /** 支払条件オプション */
  // 技術的負債 #2: supplier-createと同じ定義（コピペ）
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
    private route: ActivatedRoute,
    private router: Router,
    private supplierService: SupplierService
  ) {}

  ngOnInit(): void {
    this.supplierId = Number(this.route.snapshot.paramMap.get('id'));
    this.initializeForm();
    this.loadSupplier();
  }

  /**
   * フォームを初期化
   * 技術的負債 #2: supplier-createのinitializeFormとほぼ同一
   */
  private initializeForm(): void {
    this.supplierForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^[A-Z]{2,4}-\d{3,6}$/)]],
      name: ['', [Validators.required, Validators.maxLength(200)]],
      nameKana: ['', [Validators.maxLength(200)]],
      status: ['ACTIVE', [Validators.required]],
      email: ['', [Validators.email]],
      phone: ['', [Validators.pattern(/^[\d\-\+\(\)]+$/)]],
      website: [''],
      address: ['', [Validators.maxLength(500)]],
      paymentTerms: ['NET_30'],
      notes: ['', [Validators.maxLength(2000)]],
      contacts: this.fb.array([])
    });
  }

  /** 連絡先FormArrayを取得 */
  get contactsArray(): FormArray {
    return this.supplierForm.get('contacts') as FormArray;
  }

  /**
   * サプライヤーデータをロードしてフォームにセット
   */
  private loadSupplier(): void {
    this.isLoading = true;

    this.supplierService.getSupplier(this.supplierId).subscribe(
      (supplier) => {
        this.supplier = supplier;
        this.populateForm(supplier);

        // 連絡先をロード
        this.supplierService.getContacts(this.supplierId).subscribe(
          (contacts) => {
            this.populateContacts(contacts);
            this.isLoading = false;
          },
          (error) => {
            console.error('連絡先取得エラー:', error);
            this.addContact(); // エラー時はデフォルト1行追加
            this.isLoading = false;
          }
        );
      },
      (error) => {
        console.error('サプライヤー取得エラー:', error);
        this.errorMessage = 'サプライヤーデータの取得に失敗しました。';
        this.isLoading = false;
      }
    );
  }

  /**
   * フォームにデータをセット
   */
  private populateForm(supplier: Supplier): void {
    this.supplierForm.patchValue({
      code: supplier.code,
      name: supplier.name,
      nameKana: supplier.nameKana,
      status: supplier.status,
      email: supplier.email,
      phone: supplier.phone,
      website: supplier.website,
      address: supplier.address,
      paymentTerms: supplier.paymentTerms,
      notes: supplier.notes
    });
  }

  /**
   * 連絡先データをフォームにセット
   */
  private populateContacts(contacts: SupplierContact[]): void {
    // FormArrayをクリア
    while (this.contactsArray.length > 0) {
      this.contactsArray.removeAt(0);
    }

    if (contacts.length > 0) {
      contacts.forEach(contact => {
        const contactGroup = this.fb.group({
          id: [contact.id],
          name: [contact.name, [Validators.required, Validators.maxLength(100)]],
          department: [contact.department, [Validators.maxLength(100)]],
          position: [contact.position, [Validators.maxLength(100)]],
          phone: [contact.phone, [Validators.pattern(/^[\d\-\+\(\)]+$/)]],
          email: [contact.email, [Validators.email]],
          isPrimary: [contact.isPrimary]
        });
        this.contactsArray.push(contactGroup);
      });
    } else {
      this.addContact();
    }
  }

  /**
   * 連絡先を追加
   * 技術的負債 #2: supplier-createと同じコード
   */
  addContact(): void {
    const contact = this.fb.group({
      id: [null],
      name: ['', [Validators.required, Validators.maxLength(100)]],
      department: ['', [Validators.maxLength(100)]],
      position: ['', [Validators.maxLength(100)]],
      phone: ['', [Validators.pattern(/^[\d\-\+\(\)]+$/)]],
      email: ['', [Validators.email]],
      isPrimary: [this.contactsArray.length === 0]
    });
    this.contactsArray.push(contact);
  }

  /**
   * 連絡先を削除
   * 技術的負債 #2: supplier-createと同じコード
   */
  removeContact(index: number): void {
    if (this.contactsArray.length > 1) {
      const wasPrimary = this.contactsArray.at(index).get('isPrimary')?.value;
      this.contactsArray.removeAt(index);
      if (wasPrimary && this.contactsArray.length > 0) {
        this.contactsArray.at(0).get('isPrimary')?.setValue(true);
      }
    }
  }

  /**
   * 主担当を切り替え
   * 技術的負債 #2: supplier-createと同じコード
   */
  setPrimaryContact(index: number): void {
    this.contactsArray.controls.forEach((control, i) => {
      control.get('isPrimary')?.setValue(i === index);
    });
  }

  /**
   * コードエラーメッセージ
   * 技術的負債 #2: supplier-createと同じコード
   */
  getCodeErrorMessage(): string {
    const codeControl = this.supplierForm.get('code');
    if (!codeControl?.touched) return '';
    if (codeControl?.errors?.['required']) return 'サプライヤーコードは必須です';
    if (codeControl?.errors?.['pattern']) return 'コード形式が不正です（例: SUP-001）';
    return '';
  }

  /**
   * 更新を実行
   */
  submit(): void {
    this.supplierForm.markAllAsTouched();

    if (this.supplierForm.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const formData = this.supplierForm.value;

    this.supplierService.updateSupplier(this.supplierId, formData).subscribe(
      () => {
        this.isSubmitting = false;
        this.successMessage = 'サプライヤー情報が更新されました。';
        setTimeout(() => {
          this.router.navigate(['/suppliers', this.supplierId]);
        }, 1500);
      },
      (error) => {
        console.error('サプライヤー更新エラー:', error);
        this.isSubmitting = false;
        this.errorMessage = 'サプライヤーの更新に失敗しました。入力内容を確認してください。';
      }
    );
  }

  /**
   * キャンセルして詳細画面へ戻る
   */
  cancel(): void {
    this.router.navigate(['/suppliers', this.supplierId]);
  }
}
