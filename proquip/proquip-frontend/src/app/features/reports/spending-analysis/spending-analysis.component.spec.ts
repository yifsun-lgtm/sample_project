import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { SpendingAnalysisComponent } from './spending-analysis.component';
import { ReportService } from '@shared/services/report.service';
import { ApiService } from '@shared/services/api.service';

/**
 * 支出分析コンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限
 * レポート生成、CSV/Excel出力、期間選択のテストが未実装
 */
describe('SpendingAnalysisComponent', () => {
  let component: SpendingAnalysisComponent;
  let fixture: ComponentFixture<SpendingAnalysisComponent>;

  /** レポートサービスのモック */
  const mockReportService = {
    getProcurementSummary: jasmine.createSpy('getProcurementSummary').and.returnValue(of({
      reportId: 'RPT-001',
      reportName: '調達サマリー',
      generatedAt: '2026-05-15T10:00:00',
      data: {
        totalSpending: 50000000,
        orderCount: 150,
        averageOrderValue: 333333
      }
    })),
    exportCsv: jasmine.createSpy('exportCsv').and.returnValue(of(new Blob())),
    exportExcel: jasmine.createSpy('exportExcel').and.returnValue(of(new Blob()))
  };

  /** APIサービスのモック */
  const mockApiService = {
    get: jasmine.createSpy('get').and.returnValue(of({}))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SpendingAnalysisComponent],
      providers: [
        { provide: ReportService, useValue: mockReportService },
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SpendingAnalysisComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should generate report', () => {
    // TODO: 期間を指定してレポート生成が正しく行われることを確認
    // fixture.detectChanges();
    // expect(mockReportService.getProcurementSummary).toHaveBeenCalled();
  });
});
