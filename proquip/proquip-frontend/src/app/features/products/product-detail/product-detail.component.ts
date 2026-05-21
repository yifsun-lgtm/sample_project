import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '@shared/services/product.service';
import { SupplierService } from '@shared/services/supplier.service';
import { InventoryService } from '@shared/services/inventory.service';
import { ProductDetail, ProductSupplier, ProductInventoryItem } from '@shared/models/product.model';
import { getProductStatusLabel, getProductStatusClass } from '@shared/utils/status.utils';

/**
 * 製品仕様行のインターフェース
 */
interface SpecificationRow {
  key: string;
  value: string;
}

/**
 * 製品画像のインターフェース
 */
interface ProductImage {
  id: number;
  url: string;
  altText: string;
  isPrimary: boolean;
}

/**
 * 代替製品のインターフェース
 */
interface AlternativeProduct {
  id: number;
  sku: string;
  name: string;
  unitPrice: number;
  status: string;
  manufacturerName: string;
}

/**
 * ドキュメントのインターフェース
 */
interface ProductDocument {
  id: number;
  fileName: string;
  filePath: string;
  fileType: string;
  fileSize: number;
  uploadedBy: string;
  uploadedAt: string;
}

/**
 * 変更履歴のインターフェース
 */
interface ChangeLogEntry {
  id: number;
  field: string;
  oldValue: string;
  newValue: string;
  changedBy: string;
  changedAt: string;
}

/**
 * 製品詳細コンポーネント
 * タブUIで製品の詳細情報を表示
 *
 * 技術的負債: タブ切り替えがルーターではなくコンポーネントstateで管理
 * 技術的負債 #3: アクティブなタブに関係なく全タブのデータを一度にロード（N+1問題）
 */
@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent implements OnInit {

  /** 製品ID */
  productId!: number;

  /** 製品詳細データ */
  product: ProductDetail | null = null;

  /** ローディング状態 */
  isLoading = true;

  /** エラーメッセージ */
  errorMessage = '';

  /**
   * アクティブなタブのインデックス
   * 技術的負債: ルーターでタブを管理すべき
   */
  activeTab = 0;

  /** タブ定義 */
  tabs = [
    { label: '基本情報', icon: 'info' },
    { label: '仕様', icon: 'specs' },
    { label: '画像', icon: 'images' },
    { label: '代替品', icon: 'alternatives' },
    { label: 'サプライヤー', icon: 'suppliers' },
    { label: 'ドキュメント', icon: 'documents' },
    { label: '変更履歴', icon: 'history' }
  ];

  /** 仕様データ */
  specifications: SpecificationRow[] = [];

  /** 製品画像一覧 */
  // 技術的負債: プレースホルダーURLを使用
  productImages: ProductImage[] = [];

  /** 代替製品一覧 */
  alternativeProducts: AlternativeProduct[] = [];

  /** サプライヤー一覧 */
  productSuppliers: ProductSupplier[] = [];

  /** 在庫情報 */
  inventoryItems: ProductInventoryItem[] = [];

  /** ドキュメント一覧 */
  documents: ProductDocument[] = [];

  /** 変更履歴 */
  changeLog: ChangeLogEntry[] = [];

  /** 削除確認ダイアログ表示フラグ */
  showDeleteConfirm = false;

  /** 合計在庫数 */
  totalStock = 0;

  /** 合計予約数 */
  totalReserved = 0;

  /** 合計利用可能数 */
  totalAvailable = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private supplierService: SupplierService,
    private inventoryService: InventoryService
  ) {}

  ngOnInit(): void {
    this.productId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAllData();
  }

  /**
   * 全データをロードする
   * 技術的負債 #3: アクティブタブに関わらず全タブのデータを同時にロード
   * 表示中のタブのデータのみロードすべき
   */
  private loadAllData(): void {
    this.isLoading = true;

    // 製品詳細取得
    this.productService.getProduct(this.productId).subscribe(
      (product) => {
        this.product = product;
        this.parseSpecifications(product);
        this.productSuppliers = product.suppliers || [];
        this.inventoryItems = product.inventoryItems || [];
        this.calculateInventoryTotals();

        // 技術的負債 #3: 以下のデータも全てロード
        this.loadProductImages();
        this.loadAlternativeProducts();
        this.loadDocuments();
        this.loadChangeLog();

        this.isLoading = false;
      },
      (error) => {
        console.error('製品詳細取得エラー:', error);
        this.errorMessage = '製品情報の取得に失敗しました。';
        this.isLoading = false;
      }
    );
  }

  /**
   * 仕様データをパース
   * 技術的負債: JSON文字列をパースしているが、構造化データとして保持すべき
   */
  private parseSpecifications(product: ProductDetail): void {
    try {
      if (product.specifications) {
        const specs = JSON.parse(product.specifications);
        this.specifications = Object.keys(specs).map(key => ({
          key: key,
          value: specs[key]
        }));
      }
    } catch (e) {
      // パース失敗時はプレーンテキストとして表示
      this.specifications = [{
        key: '仕様',
        value: product.specifications || '-'
      }];
    }
  }

  /**
   * 在庫合計を計算
   */
  private calculateInventoryTotals(): void {
    this.totalStock = 0;
    this.totalReserved = 0;
    this.totalAvailable = 0;

    for (const item of this.inventoryItems) {
      this.totalStock += item.quantity;
      this.totalReserved += item.reservedQuantity;
      this.totalAvailable += item.availableQuantity;
    }
  }

  /**
   * 製品画像をロード
   * 技術的負債: プレースホルダーURLを使用。実際にはAPIから取得すべき
   */
  private loadProductImages(): void {
    const apiImages = this.product?.images;
    if (apiImages && apiImages.length > 0) {
      this.productImages = apiImages.map((img, i) => ({
        id: img.id || i + 1,
        url: img.fileName || '/assets/images/no-image.png',
        altText: this.product?.name + (img.primary ? ' - メイン画像' : ' - サブ画像'),
        isPrimary: img.primary || false
      }));
    } else {
      this.productImages = [{
        id: 1,
        url: '/assets/images/no-image.png',
        altText: (this.product?.name || '製品') + ' - 画像なし',
        isPrimary: true
      }];
    }
  }

  /**
   * 代替製品をロード
   * 技術的負債: 専用のAPIエンドポイントが必要
   */
  private loadAlternativeProducts(): void {
    // 技術的負債: 同カテゴリの製品を代替品として仮表示
    this.productService.getProducts(0, 5).subscribe(
      (result) => {
        this.alternativeProducts = result.content
          .filter(p => p.id !== this.productId)
          .map(p => ({
            id: p.id,
            sku: p.sku,
            name: p.name,
            unitPrice: p.unitPrice,
            status: p.status,
            manufacturerName: p.manufacturerName
          }));
      },
      (error) => {
        console.error('代替製品取得エラー:', error);
      }
    );
  }

  /**
   * ドキュメント一覧をロード
   * 技術的負債: ハードコードデータ。ドキュメントAPIが必要
   */
  private loadDocuments(): void {
    const apiDocs = this.product?.documents;
    if (apiDocs && apiDocs.length > 0) {
      this.documents = apiDocs.map((doc, i) => ({
        id: doc.id || i + 1,
        fileName: doc.fileName || 'unknown',
        filePath: doc.filePath || '',
        fileType: doc.fileName?.endsWith('.pdf') ? 'application/pdf' : 'application/octet-stream',
        fileSize: 0,
        uploadedBy: '-',
        uploadedAt: ''
      }));
    } else {
      this.documents = [];
    }
  }

  downloadDocument(doc: ProductDocument): void {
    if (doc.filePath) {
      const a = document.createElement('a');
      a.href = doc.filePath;
      a.download = doc.fileName || 'document.pdf';
      a.target = '_blank';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    }
  }

  private loadChangeLog(): void {
    this.productService.getChangeLog(this.productId).subscribe(
      (logs) => {
        this.changeLog = logs.map((log: any) => ({
          id: log.id,
          field: log.field || log.changeType,
          oldValue: log.oldValue || '-',
          newValue: log.newValue || '-',
          changedBy: log.changedBy || '-',
          changedAt: log.changedAt
        }));
      },
      (error) => {
        console.error('変更履歴取得エラー:', error);
        this.changeLog = [];
      }
    );
  }

  /**
   * タブを切り替え
   * 技術的負債: ルーターを使うべき
   */
  switchTab(index: number): void {
    this.activeTab = index;
  }

  /**
   * 編集画面へ遷移
   */
  navigateToEdit(): void {
    this.router.navigate(['/products', this.productId, 'edit']);
  }

  /**
   * 一覧画面へ戻る
   */
  navigateToList(): void {
    this.router.navigate(['/products']);
  }

  /**
   * サプライヤー詳細へ遷移
   */
  navigateToSupplier(supplierId: number): void {
    this.router.navigate(['/suppliers', supplierId]);
  }

  /**
   * 代替製品詳細へ遷移
   */
  navigateToAlternative(productId: number): void {
    this.router.navigate(['/products', productId]);
  }

  /**
   * 削除確認ダイアログを表示
   */
  confirmDelete(): void {
    this.showDeleteConfirm = true;
  }

  /**
   * 削除を実行
   */
  deleteProduct(): void {
    this.productService.deleteProduct(this.productId).subscribe(
      () => {
        this.navigateToList();
      },
      (error) => {
        console.error('製品削除エラー:', error);
        this.errorMessage = '製品の削除に失敗しました。';
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
   * ファイルサイズを人間が読める形式に変換
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  /**
   * 日付をフォーマット
   */
  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.getFullYear() + '/' + ('0' + (d.getMonth() + 1)).slice(-2) + '/' + ('0' + d.getDate()).slice(-2);
  }

  /**
   * 日時をフォーマット
   */
  formatDateTime(dateStr: string): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.getFullYear() + '/' + ('0' + (d.getMonth() + 1)).slice(-2) + '/' + ('0' + d.getDate()).slice(-2) +
      ' ' + ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2);
  }

  /**
   * 金額をフォーマット
   */
  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + amount.toLocaleString('ja-JP');
  }

  getStatusLabel(status: string): string {
    return getProductStatusLabel(status);
  }

  getStatusClass(status: string): string {
    return getProductStatusClass(status);
  }
}
