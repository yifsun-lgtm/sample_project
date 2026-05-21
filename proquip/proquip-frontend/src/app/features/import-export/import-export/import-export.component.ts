import { Component, OnInit } from '@angular/core';
import { ApiService } from '@shared/services/api.service';
import { AdminService } from '@shared/services/admin.service';

/**
 * インポートジョブ情報
 */
export interface ImportJob {
  id: string;
  entityType: string;
  fileName: string;
  status: string;
  totalRows: number;
  processedRows: number;
  successCount: number;
  errorCount: number;
  startedAt: string;
  completedAt: string | null;
  errorReportUrl: string | null;
}

/**
 * カラムマッピング
 */
export interface ColumnMapping {
  csvHeader: string;
  systemField: string;
  required: boolean;
}

/**
 * エクスポートフィールド定義
 */
export interface ExportField {
  key: string;
  label: string;
  selected: boolean;
}

/**
 * インポート/エクスポートコンポーネント
 * CSVファイルのインポートおよびデータのエクスポートを管理する
 *
 * 技術的負債 #5: CSVパースをコンポーネント内で手動のstring分割で実行
 * CSV解析ライブラリ（Papa Parse等）を使用すべき
 *
 * 技術的負債: FileReader APIの使用にエンコーディングのエラーハンドリングがない
 *
 * 技術的負債: カラムマッピングがエンティティタイプごとにハードコード
 *
 * 技術的負債: エクスポート時にフロントエンドでCSV文字列を構築
 * バックエンドAPIからダウンロードすべき
 */
@Component({
  selector: 'app-import-export',
  templateUrl: './import-export.component.html',
  styleUrls: ['./import-export.component.scss']
})
export class ImportExportComponent implements OnInit {

  /** 現在のタブ: 'import' | 'export' */
  activeTab: 'import' | 'export' = 'import';

  // === インポート関連 ===

  /** インポート対象エンティティ */
  importEntityType = '';

  /** エンティティオプション */
  entityTypeOptions = [
    { value: '', label: '-- 選択してください --' },
    { value: 'product', label: '製品' },
    { value: 'supplier', label: 'サプライヤー' },
    { value: 'inventory', label: '在庫' }
  ];

  /** アップロードされたファイル */
  uploadedFile: File | null = null;

  /** CSVヘッダー行（パース結果） */
  csvHeaders: string[] = [];

  /** CSVプレビューデータ（先頭5行） */
  csvPreviewData: string[][] = [];

  /** カラムマッピング */
  columnMappings: ColumnMapping[] = [];

  /** インポート進行中フラグ */
  isImporting = false;

  /** インポート進捗 */
  importProgress = 0;

  /** 最近のインポートジョブ */
  importJobs: ImportJob[] = [];

  /** インポートエラーメッセージ */
  importErrorMessage = '';

  /** インポート成功メッセージ */
  importSuccessMessage = '';

  /**
   * エンティティ別のシステムフィールド定義
   *
   * 技術的負債: カラムマッピングがハードコード
   * APIから動的に取得すべき
   */
  private readonly systemFieldsMap: { [entityType: string]: { key: string; label: string; required: boolean }[] } = {
    product: [
      { key: '', label: '-- マッピングなし --', required: false },
      { key: 'sku', label: 'SKU', required: true },
      { key: 'name', label: '製品名', required: true },
      { key: 'description', label: '説明', required: false },
      { key: 'categoryName', label: 'カテゴリ', required: false },
      { key: 'unitPrice', label: '単価', required: true },
      { key: 'unit', label: '単位', required: false },
      { key: 'minimumOrderQuantity', label: '最小注文数', required: false },
      { key: 'leadTimeDays', label: 'リードタイム（日）', required: false },
      { key: 'status', label: 'ステータス', required: false }
    ],
    supplier: [
      { key: '', label: '-- マッピングなし --', required: false },
      { key: 'code', label: 'サプライヤーコード', required: true },
      { key: 'name', label: 'サプライヤー名', required: true },
      { key: 'nameKana', label: 'サプライヤー名（カナ）', required: false },
      { key: 'address', label: '住所', required: false },
      { key: 'phone', label: '電話番号', required: false },
      { key: 'email', label: 'メール', required: false },
      { key: 'paymentTerms', label: '支払条件', required: false },
      { key: 'status', label: 'ステータス', required: false }
    ],
    inventory: [
      { key: '', label: '-- マッピングなし --', required: false },
      { key: 'productSku', label: '製品SKU', required: true },
      { key: 'warehouseCode', label: '倉庫コード', required: true },
      { key: 'quantity', label: '数量', required: true },
      { key: 'minimumStock', label: '最小在庫', required: false },
      { key: 'maximumStock', label: '最大在庫', required: false },
      { key: 'reorderPoint', label: '再発注ポイント', required: false }
    ]
  };

  // === エクスポート関連 ===

  /** エクスポート対象エンティティ */
  exportEntityType = '';

  /** エクスポートフォーマット */
  exportFormat = 'csv';

  /** エクスポートフィールド */
  exportFields: ExportField[] = [];

  /** エクスポートフィルター */
  exportFilterStatus = '';
  exportFilterCategory = '';

  /** エクスポート中フラグ */
  isExporting = false;

  /** エクスポートエラーメッセージ */
  exportErrorMessage = '';

  /** エクスポート成功メッセージ */
  exportSuccessMessage = '';

  /** エクスポートフィールド定義 */
  private readonly exportFieldsMap: { [entityType: string]: ExportField[] } = {
    product: [
      { key: 'sku', label: 'SKU', selected: true },
      { key: 'name', label: '製品名', selected: true },
      { key: 'description', label: '説明', selected: false },
      { key: 'categoryName', label: 'カテゴリ', selected: true },
      { key: 'manufacturerName', label: 'メーカー', selected: true },
      { key: 'unitPrice', label: '単価', selected: true },
      { key: 'unit', label: '単位', selected: true },
      { key: 'status', label: 'ステータス', selected: true },
      { key: 'minimumOrderQuantity', label: '最小注文数', selected: false },
      { key: 'leadTimeDays', label: 'リードタイム', selected: false },
      { key: 'createdAt', label: '作成日', selected: false },
      { key: 'updatedAt', label: '更新日', selected: false }
    ],
    supplier: [
      { key: 'code', label: 'コード', selected: true },
      { key: 'name', label: 'サプライヤー名', selected: true },
      { key: 'nameKana', label: 'カナ', selected: false },
      { key: 'status', label: 'ステータス', selected: true },
      { key: 'rating', label: '評価', selected: true },
      { key: 'address', label: '住所', selected: false },
      { key: 'phone', label: '電話番号', selected: true },
      { key: 'email', label: 'メール', selected: true },
      { key: 'paymentTerms', label: '支払条件', selected: false }
    ],
    inventory: [
      { key: 'productSku', label: '製品SKU', selected: true },
      { key: 'productName', label: '製品名', selected: true },
      { key: 'warehouseName', label: '倉庫名', selected: true },
      { key: 'quantity', label: '数量', selected: true },
      { key: 'reservedQuantity', label: '予約数量', selected: true },
      { key: 'availableQuantity', label: '利用可能数量', selected: true },
      { key: 'minimumStock', label: '最小在庫', selected: false },
      { key: 'status', label: 'ステータス', selected: true }
    ]
  };

  constructor(
    private api: ApiService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.loadImportJobs();
  }

  /** タブ切り替え */
  switchTab(tab: 'import' | 'export'): void {
    this.activeTab = tab;
  }

  // === インポート機能 ===

  /** エンティティタイプ変更時 */
  onImportEntityChange(): void {
    this.csvHeaders = [];
    this.csvPreviewData = [];
    this.columnMappings = [];
    this.uploadedFile = null;
  }

  /**
   * ファイル選択時の処理
   *
   * 技術的負債: FileReader APIのエンコーディングエラーハンドリングなし
   * Shift_JIS等の文字コードに対応していない
   */
  onFileSelected(files: File[]): void {
    if (!files || files.length === 0) return;

    this.uploadedFile = files[0];

    // 技術的負債: FileReaderのエラーハンドリング不足
    const reader = new FileReader();
    reader.onload = (e) => {
      const csvText = e.target?.result as string;
      if (csvText) {
        this.parseCSV(csvText);
      }
    };
    // 技術的負債: エンコーディング指定なし。UTF-8のみ対応
    reader.readAsText(this.uploadedFile);
  }

  /**
   * CSVテキストをパースする
   *
   * 技術的負債 #5: 手動のstring分割によるCSV解析
   * クォートされたフィールドや改行を含むフィールドに対応していない
   * Papa Parseなどのライブラリを使用すべき
   */
  private parseCSV(csvText: string): void {
    // 技術的負債: 改行で分割（CR+LF, LF, CRの混在に脆弱）
    const lines = csvText.split(/\r?\n/);

    if (lines.length === 0) {
      this.importErrorMessage = 'CSVファイルが空です。';
      return;
    }

    // 技術的負債: カンマで単純分割（クォート内のカンマに対応していない）
    this.csvHeaders = lines[0].split(',').map(h => h.trim().replace(/^"/, '').replace(/"$/, ''));

    // プレビュー: 先頭5行
    this.csvPreviewData = [];
    for (let i = 1; i < Math.min(6, lines.length); i++) {
      if (lines[i].trim() === '') continue;
      // 技術的負債: 同じく単純なカンマ分割
      const cells = lines[i].split(',').map(c => c.trim().replace(/^"/, '').replace(/"$/, ''));
      this.csvPreviewData.push(cells);
    }

    // カラムマッピングを初期化
    this.initColumnMappings();
  }

  /**
   * カラムマッピングを初期化
   *
   * 技術的負債: エンティティタイプごとにハードコードされたマッピング
   */
  private initColumnMappings(): void {
    const systemFields = this.systemFieldsMap[this.importEntityType] || [];

    this.columnMappings = this.csvHeaders.map(header => {
      // 技術的負債: ヘッダー名から自動マッピングを試みる（ハードコード）
      const autoMatch = systemFields.find(f =>
        f.key.toLowerCase() === header.toLowerCase() ||
        f.label === header
      );

      return {
        csvHeader: header,
        systemField: autoMatch ? autoMatch.key : '',
        required: autoMatch ? autoMatch.required : false
      };
    });
  }

  /** システムフィールドオプションを取得 */
  getSystemFields(): { key: string; label: string; required: boolean }[] {
    return this.systemFieldsMap[this.importEntityType] || [];
  }

  /** インポートを実行 */
  executeImport(): void {
    if (!this.uploadedFile || !this.importEntityType) {
      this.importErrorMessage = 'エンティティタイプとファイルを選択してください。';
      return;
    }

    // 必須フィールドのチェック
    const systemFields = this.systemFieldsMap[this.importEntityType] || [];
    const requiredFields = systemFields.filter(f => f.required);
    const mappedFields = this.columnMappings.map(m => m.systemField).filter(f => f);
    const missingRequired = requiredFields.filter(rf => !mappedFields.includes(rf.key));

    if (missingRequired.length > 0) {
      this.importErrorMessage = `必須フィールドがマッピングされていません: ${missingRequired.map(f => f.label).join(', ')}`;
      return;
    }

    this.isImporting = true;
    this.importProgress = 0;
    this.importErrorMessage = '';

    // マッピング情報を付加してアップロード
    const mappingData: { [key: string]: string } = {};
    this.columnMappings.forEach(m => {
      if (m.systemField) {
        mappingData[m.csvHeader] = m.systemField;
      }
    });

    this.adminService.importMasterData(this.uploadedFile, this.importEntityType).subscribe({
      next: (result) => {
        this.isImporting = false;
        this.importProgress = 100;
        this.importSuccessMessage = 'インポートが完了しました。';
        this.loadImportJobs();
        setTimeout(() => { this.importSuccessMessage = ''; }, 3000);
      },
      error: (err) => {
        console.error('インポートエラー:', err);
        this.isImporting = false;
        this.importErrorMessage = 'インポートに失敗しました。';
      }
    });

    // 技術的負債: 進捗をシミュレート（実際のWebSocket/SSE通知に置き換えるべき）
    this.simulateProgress();
  }

  /** 進捗をシミュレート（技術的負債） */
  private simulateProgress(): void {
    const interval = setInterval(() => {
      if (this.importProgress >= 90 || !this.isImporting) {
        clearInterval(interval);
        return;
      }
      this.importProgress += Math.random() * 15;
      if (this.importProgress > 90) this.importProgress = 90;
    }, 500);
  }

  /** インポートジョブ一覧を読み込む */
  private loadImportJobs(): void {
    this.api.get<ImportJob[]>('/import-jobs').subscribe({
      next: (jobs) => {
        this.importJobs = jobs;
      },
      error: () => {
        this.importJobs = [];
      }
    });
  }

  /** ジョブステータスのラベル */
  getJobStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'COMPLETED': '完了', 'FAILED': '失敗', 'IN_PROGRESS': '処理中', 'PENDING': '待機中'
    };
    return labels[status] || status;
  }

  /** ジョブステータスのクラス */
  getJobStatusClass(status: string): string {
    const classes: { [key: string]: string } = {
      'COMPLETED': 'status-completed', 'FAILED': 'status-failed',
      'IN_PROGRESS': 'status-progress', 'PENDING': 'status-pending'
    };
    return classes[status] || '';
  }

  /** エラーレポートをダウンロード */
  downloadErrorReport(job: ImportJob): void {
    if (!job.errorReportUrl) return;
    this.api.download(job.errorReportUrl).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `エラーレポート_${job.id}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('エラーレポートダウンロードエラー:', err);
      }
    });
  }

  // === エクスポート機能 ===

  /** エクスポートエンティティ変更時 */
  onExportEntityChange(): void {
    this.exportFields = JSON.parse(JSON.stringify(this.exportFieldsMap[this.exportEntityType] || []));
  }

  /** 全フィールド選択/解除 */
  toggleAllExportFields(selected: boolean): void {
    this.exportFields.forEach(f => { f.selected = selected; });
  }

  /** 選択されたフィールド数 */
  get selectedFieldCount(): number {
    return this.exportFields.filter(f => f.selected).length;
  }

  /**
   * エクスポートを実行する
   *
   * 技術的負債: フロントエンドでCSV文字列を構築
   * バックエンドAPIからダウンロードすべき
   */
  executeExport(): void {
    if (!this.exportEntityType) {
      this.exportErrorMessage = 'エンティティタイプを選択してください。';
      return;
    }

    const selectedFields = this.exportFields.filter(f => f.selected);
    if (selectedFields.length === 0) {
      this.exportErrorMessage = 'エクスポートするフィールドを1つ以上選択してください。';
      return;
    }

    this.isExporting = true;
    this.exportErrorMessage = '';

    // 技術的負債: バックエンドAPIからダウンロードすべきだが、
    // ここではフロントエンドでCSVを構築している
    this.adminService.exportMasterData(this.exportEntityType).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.exportEntityType}_export_${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.isExporting = false;
        this.exportSuccessMessage = 'エクスポートが完了しました。';
        setTimeout(() => { this.exportSuccessMessage = ''; }, 3000);
      },
      error: (err) => {
        console.error('エクスポートエラー:', err);
        this.isExporting = false;
        this.exportErrorMessage = 'エクスポートに失敗しました。しばらく経ってから再度お試しください。';
        setTimeout(() => { this.exportErrorMessage = ''; }, 5000);
      }
    });
  }


  /** エンティティラベルを取得 */
  getEntityLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'product': '製品', 'supplier': 'サプライヤー', 'inventory': '在庫'
    };
    return labels[type] || type;
  }
}
