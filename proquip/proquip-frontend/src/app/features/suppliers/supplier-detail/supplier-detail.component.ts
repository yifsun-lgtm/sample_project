import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SupplierService } from '@shared/services/supplier.service';
import { Supplier, SupplierContact, SupplierContract } from '@shared/models/supplier.model';

/**
 * サプライヤー評価履歴のインターフェース
 */
interface RatingHistory {
  id: number;
  evaluationDate: string;
  quality: number;
  delivery: number;
  price: number;
  communication: number;
  overallRating: number;
  evaluatedBy: string;
  comment: string;
}

/**
 * サプライヤー認証情報のインターフェース
 */
interface Certification {
  id: number;
  name: string;
  issuingBody: string;
  certificationNumber: string;
  issueDate: string;
  expiryDate: string;
  status: string;
}

/**
 * サプライヤー詳細コンポーネント
 * タブUIでサプライヤーの詳細情報を表示
 *
 * 技術的負債: 全タブのデータを同時にロード
 */
@Component({
  selector: 'app-supplier-detail',
  templateUrl: './supplier-detail.component.html',
  styleUrls: ['./supplier-detail.component.scss']
})
export class SupplierDetailComponent implements OnInit {

  /** サプライヤーID */
  supplierId!: number;

  /** サプライヤー情報 */
  supplier: Supplier | null = null;

  /** ローディング状態 */
  isLoading = true;

  /** エラーメッセージ */
  errorMessage = '';

  /** アクティブなタブ */
  activeTab = 0;

  /** タブ定義 */
  tabs = [
    { label: '基本情報' },
    { label: '製品' },
    { label: '契約' },
    { label: '評価履歴' },
    { label: '認証' }
  ];

  /** 連絡先一覧 */
  contacts: SupplierContact[] = [];

  /** 取扱製品一覧 */
  supplierProducts: any[] = [];

  /** 契約一覧 */
  contracts: SupplierContract[] = [];

  /** 評価履歴 */
  ratingHistory: RatingHistory[] = [];

  /** 平均評価 */
  averageRating = 0;

  /** 認証一覧 */
  certifications: Certification[] = [];

  /** 削除確認ダイアログ */
  showDeleteConfirm = false;

  /** 評価登録モーダル表示 */
  showRatingModal = false;

  /** 評価保存中 */
  isSavingRating = false;

  /** 新規評価フォーム */
  newQualityScore = 3;
  newDeliveryScore = 3;
  newPriceScore = 3;
  newServiceScore = 3;
  newComments = '';

  /** 評価保存成功メッセージ */
  ratingSuccessMessage = '';

  /** 契約モーダル */
  showContractModal = false;
  isSavingContract = false;
  editingContract: any = null;
  contractForm = { contractNumber: '', title: '', startDate: '', endDate: '', status: 'DRAFT', terms: '' };

  /** 認証モーダル */
  showCertModal = false;
  isSavingCert = false;
  editingCert: any = null;
  certForm = { certType: 'ISO_9001', certNumber: '', issuedDate: '', expiryDate: '', status: 'ACTIVE' };

  /** 製品紐付けモーダル */
  showProductModal = false;
  isSavingProduct = false;
  productForm = { productId: 0, supplierSku: '', unitCost: 0, leadTimeDays: 0, minOrderQty: 1, isPreferred: false };

  /** 成功メッセージ */
  successMessage = '';

  /** 削除確認 */
  pendingDeleteType = '';
  pendingDeleteId = 0;
  showItemDeleteConfirm = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private supplierService: SupplierService
  ) {}

  ngOnInit(): void {
    this.supplierId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAllData();
  }

  /**
   * 全データをロード
   * 技術的負債: アクティブタブに関わらず全タブのデータをロード
   */
  private loadAllData(): void {
    this.isLoading = true;

    this.supplierService.getSupplier(this.supplierId).subscribe(
      (supplier) => {
        this.supplier = supplier;

        // 技術的負債: 全タブのデータを同時ロード
        this.loadContacts();
        this.loadProducts();
        this.loadContracts();
        this.loadRatingHistory();
        this.loadCertifications();

        this.isLoading = false;
      },
      (error) => {
        console.error('サプライヤー詳細取得エラー:', error);
        this.errorMessage = 'サプライヤー情報の取得に失敗しました。';
        this.isLoading = false;
      }
    );
  }

  /**
   * 連絡先一覧をロード
   */
  private loadContacts(): void {
    this.supplierService.getContacts(this.supplierId).subscribe(
      (contacts) => {
        this.contacts = contacts;
      },
      (error) => {
        console.error('連絡先取得エラー:', error);
      }
    );
  }

  /**
   * 取扱製品をロード
   */
  private loadProducts(): void {
    this.supplierService.getSupplierProducts(this.supplierId).subscribe(
      (products) => {
        this.supplierProducts = products.map((p: any) => ({
          spId: p.id,
          id: p.productId,
          sku: p.productSku,
          name: p.productName,
          unitPrice: p.unitCost,
          supplierSku: p.supplierSku,
          leadTimeDays: p.leadTimeDays,
          minOrderQty: p.minOrderQty,
          isPreferred: p.isPreferred
        }));
      },
      (error) => {
        console.error('製品取得エラー:', error);
      }
    );
  }

  /**
   * 契約一覧をロード
   */
  private loadContracts(): void {
    this.supplierService.getContracts(this.supplierId).subscribe(
      (contracts) => {
        this.contracts = contracts;
      },
      (error) => {
        console.error('契約取得エラー:', error);
      }
    );
  }

  /**
   * 評価履歴をロード
   */
  private loadRatingHistory(): void {
    this.supplierService.getRatings(this.supplierId).subscribe(
      (ratings) => {
        this.ratingHistory = ratings.map((r: any) => ({
          id: r.id,
          evaluationDate: r.ratingDate,
          quality: r.qualityScore,
          delivery: r.deliveryScore,
          price: r.priceScore,
          communication: r.serviceScore || 0,
          overallRating: r.overallScore,
          evaluatedBy: r.ratedBy,
          comment: r.comments
        }));

        // 平均評価を計算
        if (this.ratingHistory.length > 0) {
          const sum = this.ratingHistory.reduce((acc, r) => acc + r.overallRating, 0);
          this.averageRating = Math.round((sum / this.ratingHistory.length) * 10) / 10;
        }
      },
      (error) => {
        console.error('評価履歴取得エラー:', error);
      }
    );
  }

  /**
   * 認証情報をロード
   */
  private loadCertifications(): void {
    this.supplierService.getCertifications(this.supplierId).subscribe(
      (certs) => {
        this.certifications = certs.map((c: any) => ({
          id: c.id,
          name: c.certType,
          issuingBody: '',
          certificationNumber: c.certNumber,
          issueDate: c.issuedDate,
          expiryDate: c.expiryDate,
          status: c.status
        }));
      },
      (error) => {
        console.error('認証情報取得エラー:', error);
      }
    );
  }

  /**
   * タブ切り替え
   */
  switchTab(index: number): void {
    this.activeTab = index;
  }

  /**
   * 編集画面へ遷移
   */
  navigateToEdit(): void {
    this.router.navigate(['/suppliers', this.supplierId, 'edit']);
  }

  /**
   * 一覧画面へ戻る
   */
  navigateToList(): void {
    this.router.navigate(['/suppliers']);
  }

  /**
   * 製品詳細へ遷移
   */
  navigateToProduct(productId: number): void {
    this.router.navigate(['/products', productId]);
  }

  /**
   * 削除確認を表示
   */
  confirmDelete(): void {
    this.showDeleteConfirm = true;
  }

  /**
   * サプライヤーを削除
   */
  deleteSupplier(): void {
    this.supplierService.deleteSupplier(this.supplierId).subscribe(
      () => {
        this.navigateToList();
      },
      (error) => {
        console.error('サプライヤー削除エラー:', error);
        this.errorMessage = 'サプライヤーの削除に失敗しました。';
      }
    );
  }

  /**
   * 削除確認をキャンセル
   */
  cancelDelete(): void {
    this.showDeleteConfirm = false;
  }

  /**
   * 日付フォーマット
   */
  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.getFullYear() + '/' + ('0' + (d.getMonth() + 1)).slice(-2) + '/' + ('0' + d.getDate()).slice(-2);
  }

  /**
   * 金額フォーマット
   */
  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + amount.toLocaleString('ja-JP');
  }

  /**
   * 評価を星表示に変換
   */
  getRatingStars(rating: number): string {
    const fullStars = Math.floor(rating);
    const halfStar = rating % 1 >= 0.5 ? 1 : 0;
    const emptyStars = 5 - fullStars - halfStar;
    return '★'.repeat(fullStars) + (halfStar ? '☆' : '') + '☆'.repeat(emptyStars);
  }

  /**
   * 認証ステータスラベル
   */
  getCertStatusLabel(status: string): string {
    const labels: any = {
      'VALID': '有効',
      'EXPIRED': '期限切れ',
      'EXPIRING_SOON': '期限間近',
      'REVOKED': '取消済み'
    };
    return labels[status] || status;
  }

  /**
   * 認証ステータスCSSクラス
   */
  getCertStatusClass(status: string): string {
    const classMap: any = {
      'VALID': 'cert-valid',
      'EXPIRED': 'cert-expired',
      'EXPIRING_SOON': 'cert-expiring',
      'REVOKED': 'cert-revoked'
    };
    return classMap[status] || '';
  }

  /**
   * 契約ステータスラベル
   */
  getContractStatusLabel(status: string): string {
    const labels: any = {
      'ACTIVE': '有効',
      'EXPIRED': '期限切れ',
      'TERMINATED': '解約済み',
      'PENDING': '締結待ち'
    };
    return labels[status] || status;
  }

  openRatingModal(): void {
    this.newQualityScore = 3;
    this.newDeliveryScore = 3;
    this.newPriceScore = 3;
    this.newServiceScore = 3;
    this.newComments = '';
    this.showRatingModal = true;
  }

  closeRatingModal(): void {
    this.showRatingModal = false;
  }

  submitRating(): void {
    this.isSavingRating = true;
    const ratingData = {
      qualityScore: this.newQualityScore,
      deliveryScore: this.newDeliveryScore,
      priceScore: this.newPriceScore,
      serviceScore: this.newServiceScore,
      comments: this.newComments
    };

    this.supplierService.rateSupplier(this.supplierId, ratingData).subscribe(
      () => {
        this.isSavingRating = false;
        this.showRatingModal = false;
        this.loadRatingHistory();
        this.ratingSuccessMessage = '評価を登録しました。';
        setTimeout(() => { this.ratingSuccessMessage = ''; }, 3000);
      },
      (error) => {
        console.error('評価登録エラー:', error);
        this.isSavingRating = false;
        this.errorMessage = '評価の登録に失敗しました。';
        setTimeout(() => { this.errorMessage = ''; }, 3000);
      }
    );
  }

  // --- 契約 CRUD ---

  openContractModal(contract?: any): void {
    this.editingContract = contract || null;
    this.contractForm = contract
      ? { contractNumber: contract.contractNumber, title: contract.title || '', startDate: this.toInputDate(contract.startDate), endDate: this.toInputDate(contract.endDate), status: contract.status, terms: contract.terms || '' }
      : { contractNumber: '', title: '', startDate: '', endDate: '', status: 'DRAFT', terms: '' };
    this.showContractModal = true;
  }

  closeContractModal(): void { this.showContractModal = false; }

  submitContract(): void {
    this.isSavingContract = true;
    const obs = this.editingContract
      ? this.supplierService.updateContract(this.supplierId, this.editingContract.id, this.contractForm)
      : this.supplierService.createContract(this.supplierId, this.contractForm);

    obs.subscribe(
      () => {
        this.isSavingContract = false;
        this.showContractModal = false;
        this.loadContracts();
        this.showSuccess(this.editingContract ? '契約を更新しました。' : '契約を作成しました。');
      },
      (error) => {
        console.error('契約保存エラー:', error);
        this.isSavingContract = false;
        this.showError('契約の保存に失敗しました。');
      }
    );
  }

  confirmDeleteContract(contractId: number): void {
    this.pendingDeleteType = 'contract';
    this.pendingDeleteId = contractId;
    this.showItemDeleteConfirm = true;
  }

  // --- 認証 CRUD ---

  openCertModal(cert?: any): void {
    this.editingCert = cert || null;
    this.certForm = cert
      ? { certType: cert.name || cert.certType, certNumber: cert.certificationNumber || cert.certNumber || '', issuedDate: this.toInputDate(cert.issueDate || cert.issuedDate), expiryDate: this.toInputDate(cert.expiryDate), status: cert.status || 'ACTIVE' }
      : { certType: 'ISO_9001', certNumber: '', issuedDate: '', expiryDate: '', status: 'ACTIVE' };
    this.showCertModal = true;
  }

  closeCertModal(): void { this.showCertModal = false; }

  submitCert(): void {
    this.isSavingCert = true;
    const obs = this.editingCert
      ? this.supplierService.updateCertification(this.supplierId, this.editingCert.id, this.certForm)
      : this.supplierService.createCertification(this.supplierId, this.certForm);

    obs.subscribe(
      () => {
        this.isSavingCert = false;
        this.showCertModal = false;
        this.loadCertifications();
        this.showSuccess(this.editingCert ? '認証を更新しました。' : '認証を登録しました。');
      },
      (error) => {
        console.error('認証保存エラー:', error);
        this.isSavingCert = false;
        this.showError('認証の保存に失敗しました。');
      }
    );
  }

  confirmDeleteCert(certId: number): void {
    this.pendingDeleteType = 'cert';
    this.pendingDeleteId = certId;
    this.showItemDeleteConfirm = true;
  }

  // --- 製品紐付け ---

  openProductModal(): void {
    this.productForm = { productId: 0, supplierSku: '', unitCost: 0, leadTimeDays: 0, minOrderQty: 1, isPreferred: false };
    this.showProductModal = true;
  }

  closeProductModal(): void { this.showProductModal = false; }

  submitProduct(): void {
    this.isSavingProduct = true;
    this.supplierService.addSupplierProduct(this.supplierId, this.productForm).subscribe(
      () => {
        this.isSavingProduct = false;
        this.showProductModal = false;
        this.loadProducts();
        this.showSuccess('製品紐付けを追加しました。');
      },
      (error) => {
        console.error('製品紐付けエラー:', error);
        this.isSavingProduct = false;
        this.showError('製品紐付けに失敗しました。');
      }
    );
  }

  confirmDeleteProduct(spId: number): void {
    this.pendingDeleteType = 'product';
    this.pendingDeleteId = spId;
    this.showItemDeleteConfirm = true;
  }

  // --- 共通削除確認 ---

  executeItemDelete(): void {
    this.showItemDeleteConfirm = false;
    let obs;
    switch (this.pendingDeleteType) {
      case 'contract':
        obs = this.supplierService.deleteContract(this.supplierId, this.pendingDeleteId);
        break;
      case 'cert':
        obs = this.supplierService.deleteCertification(this.supplierId, this.pendingDeleteId);
        break;
      case 'product':
        obs = this.supplierService.removeSupplierProduct(this.supplierId, this.pendingDeleteId);
        break;
      default:
        return;
    }
    obs.subscribe(
      () => {
        if (this.pendingDeleteType === 'contract') this.loadContracts();
        else if (this.pendingDeleteType === 'cert') this.loadCertifications();
        else this.loadProducts();
        this.showSuccess('削除しました。');
      },
      (error: any) => {
        console.error('削除エラー:', error);
        this.showError('削除に失敗しました。');
      }
    );
  }

  cancelItemDelete(): void { this.showItemDeleteConfirm = false; }

  // --- ユーティリティ ---

  private toInputDate(dateStr: any): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.getFullYear() + '-' + ('0' + (d.getMonth() + 1)).slice(-2) + '-' + ('0' + d.getDate()).slice(-2);
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => { this.successMessage = ''; }, 3000);
  }

  private showError(msg: string): void {
    this.errorMessage = msg;
    setTimeout(() => { this.errorMessage = ''; }, 3000);
  }
}
