import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ImportExportComponent } from './import-export.component';
import { ApiService } from '@shared/services/api.service';
import { AdminService } from '@shared/services/admin.service';

/**
 * インポート・エクスポートコンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * ファイルアップロード、カラムマッピング、インポートジョブ進捗のテストが未実装
 *
 * 技術的負債: ファイルサイズ制限チェックがフロントエンドにない（コンポーネント側の負債）
 */
describe('ImportExportComponent', () => {
  let component: ImportExportComponent;
  let fixture: ComponentFixture<ImportExportComponent>;

  /** APIサービスのモック */
  const mockApiService = {
    get: jasmine.createSpy('get').and.returnValue(of([])),
    post: jasmine.createSpy('post').and.returnValue(of({
      jobId: 'job-001', status: 'RUNNING', progress: 0
    }))
  };

  /** 管理サービスのモック */
  const mockAdminService = {
    getImportJobs: jasmine.createSpy('getImportJobs').and.returnValue(of([
      {
        id: 'job-001', entityType: 'PRODUCT', fileName: 'products.csv',
        status: 'COMPLETED', totalRows: 100, processedRows: 100,
        successCount: 98, errorCount: 2, startedAt: '2026-05-15T10:00:00',
        completedAt: '2026-05-15T10:01:30', errorReportUrl: null
      }
    ])),
    getExportTemplates: jasmine.createSpy('getExportTemplates').and.returnValue(of([
      { entityType: 'PRODUCT', label: '製品マスタ' },
      { entityType: 'SUPPLIER', label: '仕入先マスタ' }
    ]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ImportExportComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: AdminService, useValue: mockAdminService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ImportExportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load import job history', () => {
    // インポートジョブ履歴が読み込まれていることを確認
    expect(mockAdminService.getImportJobs).toHaveBeenCalled();
  });

  xit('should validate file type before upload', () => {
    // TODO: ファイルタイプバリデーションのテスト
    // CSV以外のファイルはエラーとなること
    // const invalidFile = new File(['data'], 'test.txt', { type: 'text/plain' });
    // component.onFileSelected({ target: { files: [invalidFile] } } as any);
    // expect(component.fileError).toBeTruthy();
  });

  xit('should track import progress', () => {
    // TODO: インポート進捗追跡のテスト
    // component.startImport('PRODUCT', mockFile);
    // expect(component.currentJob).toBeTruthy();
    // expect(component.currentJob.status).toBe('RUNNING');
  });
});
