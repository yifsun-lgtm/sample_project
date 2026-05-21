import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { BudgetActualComponent } from './budget-actual.component';
import { BudgetService } from '@shared/services/budget.service';
import { ReportService } from '@shared/services/report.service';

/**
 * 予算対実績コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 部門ごとの展開・折りたたみ、月次内訳表示のテストが未実装
 */
describe('BudgetActualComponent', () => {
  let component: BudgetActualComponent;
  let fixture: ComponentFixture<BudgetActualComponent>;

  /** 予算サービスのモック */
  const mockBudgetService = {
    getBudgets: jasmine.createSpy('getBudgets').and.returnValue(of([
      {
        id: 1, departmentName: '製造部', fiscalYear: 2026,
        totalAmount: 10000000, usedAmount: 6500000, remainingAmount: 3500000
      },
      {
        id: 2, departmentName: '営業部', fiscalYear: 2026,
        totalAmount: 5000000, usedAmount: 3200000, remainingAmount: 1800000
      }
    ]))
  };

  /** レポートサービスのモック */
  const mockReportService = {
    getBudgetVsActual: jasmine.createSpy('getBudgetVsActual').and.returnValue(of({
      fiscalYear: 2026,
      totalBudget: 15000000,
      totalActual: 9700000,
      overallUtilizationRate: 64.67,
      departments: []
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BudgetActualComponent],
      providers: [
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: ReportService, useValue: mockReportService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BudgetActualComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should toggle department row expansion', () => {
    // TODO: 部門行の展開・折りたたみテスト
    // component.toggleRow(0);
    // expect(component.rows[0].isExpanded).toBeTrue();
    // component.toggleRow(0);
    // expect(component.rows[0].isExpanded).toBeFalse();
  });

  xit('should display correct utilization rate status class', () => {
    // TODO: 消化率に応じたステータスクラスのテスト
    // 80%以上: 'danger', 60-80%: 'warning', 60%未満: 'normal'
    // expect(component.getStatusClass(85)).toBe('danger');
    // expect(component.getStatusClass(70)).toBe('warning');
    // expect(component.getStatusClass(50)).toBe('normal');
  });
});
