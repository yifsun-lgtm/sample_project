import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * ファイルアップロードコンポーネント
 * CSV/Excelファイルのインポート用
 *
 * 技術的負債: ファイルサイズ制限なし、ファイル種別バリデーションなし
 * 大容量ファイルがアップロードされるとブラウザがフリーズする可能性がある
 */
@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss']
})
export class FileUploadComponent {

  /** 受け入れるファイル形式 */
  @Input() accept = '.csv,.xlsx,.xls';

  /** ラベルテキスト */
  @Input() label = 'ファイルを選択';

  /** 複数ファイル選択可否 */
  @Input() multiple = false;

  /** ファイル選択イベント */
  @Output() fileSelected = new EventEmitter<File[]>();

  /** 選択されたファイル名 */
  selectedFileName = '';

  /** ドラッグオーバー状態 */
  isDragOver = false;

  /** ファイル選択ハンドラ */
  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFiles(input.files);
    }
  }

  /** ドラッグオーバー */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  /** ドラッグリーブ */
  onDragLeave(): void {
    this.isDragOver = false;
  }

  /** ドロップ */
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    if (event.dataTransfer?.files) {
      // 技術的負債: ドロップ時のファイル種別チェックなし
      this.handleFiles(event.dataTransfer.files);
    }
  }

  /**
   * ファイル処理
   * 技術的負債: ファイルサイズの上限チェックがない
   * 技術的負債: ファイル種別の厳密な検証がない
   */
  private handleFiles(files: FileList): void {
    const fileArray = Array.from(files);
    this.selectedFileName = fileArray.map(f => f.name).join(', ');
    this.fileSelected.emit(fileArray);
  }

  /** ファイル選択をクリア */
  clearFile(): void {
    this.selectedFileName = '';
  }
}
