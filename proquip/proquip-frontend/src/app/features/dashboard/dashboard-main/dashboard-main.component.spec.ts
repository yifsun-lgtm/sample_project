import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { DashboardMainComponent } from './dashboard-main.component';
import { DashboardService } from '@shared/services/dashboard.service';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { InventoryService } from '@shared/services/inventory.service';
import { BudgetService } from '@shared/services/budget.service';
import { RequisitionService } from '@shared/services/requisition.service';

/**
 * ダッシュボードメインコンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限（should createのみ実質稼働）
 * 各サマリーカードの表示テスト、エラーハンドリングのテストが未実装
 */
describe('DashboardMainComponent', () => {
  let component: DashboardMainComponent;
  let fixture: ComponentFixture<DashboardMainComponent>;

  /** モックサービス定義 */
  const mockDashboardService = {
    getSummary: jasmine.createSpy('getSummary').and.returnValue(of({
      totalProducts: 100,
      totalSuppliers: 50,
      pendingOrders: 5,
      lowStockItems: 3,
      monthlySpending: 1500000,
      budgetRemaining: 5000000,
      recentOrders: [],
      stockAlerts: [],
      pendingApprovals: []
    }))
  };

  const mockPurchaseOrderService = {
    getOrders: jasmine.createSpy('getOrders').and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: true
    }))
  };

  const mockInventoryService = {
    getLowStockAlerts: jasmine.createSpy('getLowStockAlerts').and.returnValue(of([]))
  };

  const mockBudgetService = {
    getBudgets: jasmine.createSpy('getBudgets').and.returnValue(of([]))
  };

  const mockRequisitionService = {
    getRequisitions: jasmine.createSpy('getRequisitions').and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: true
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [DashboardMainComponent],
      providers: [
        { provide: DashboardService, useValue: mockDashboardService },
        { provide: PurchaseOrderService, useValue: mockPurchaseOrderService },
        { provide: InventoryService, useValue: mockInventoryService },
        { provide: BudgetService, useValue: mockBudgetService },
        { provide: RequisitionService, useValue: mockRequisitionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardMainComponent);
    component = fixture.componentInstance;
    // 注意: detectChangesを呼ぶとngOnInitが発火し、直列API呼び出しが走る
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should load dashboard data', () => {
    // TODO: ngOnInit後にサマリーデータが正しく設定されることを確認
    // fixture.detectChanges();
    // expect(mockDashboardService.getSummary).toHaveBeenCalled();
    // expect(component.summary).toBeTruthy();
  });

  xit('should display summary cards', () => {
    // TODO: サマリーカード（発注件数、在庫アラート、承認待ち、予算消化率）の表示テスト
    // fixture.detectChanges();
    // const compiled = fixture.nativeElement as HTMLElement;
    // expect(compiled.querySelectorAll('.summary-card').length).toBe(4);
  });
});
