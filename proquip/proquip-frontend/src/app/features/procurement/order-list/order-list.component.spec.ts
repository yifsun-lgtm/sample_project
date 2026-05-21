import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { OrderListComponent } from './order-list.component';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { SupplierService } from '@shared/services/supplier.service';

/**
 * 発注書一覧コンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限
 * フィルタリング、URLクエリパラメータ同期、ソートのテストが未実装
 */
describe('OrderListComponent', () => {
  let component: OrderListComponent;
  let fixture: ComponentFixture<OrderListComponent>;

  /** 発注サービスのモック */
  const mockPurchaseOrderService = {
    getOrders: jasmine.createSpy('getOrders').and.returnValue(of({
      content: [
        {
          id: 1,
          orderNumber: 'PO20260501001',
          supplierId: 1,
          supplierName: 'テストサプライヤー',
          status: 'APPROVED',
          orderDate: '2026-05-01',
          totalAmount: 500000,
          currency: 'JPY',
          items: [],
          approvalSteps: []
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false
    }))
  };

  const mockSupplierService = {
    getSuppliers: jasmine.createSpy('getSuppliers').and.returnValue(of({
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

  const mockActivatedRoute = {
    queryParams: of({}),
    snapshot: {
      queryParams: {}
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [OrderListComponent],
      providers: [
        { provide: PurchaseOrderService, useValue: mockPurchaseOrderService },
        { provide: SupplierService, useValue: mockSupplierService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should filter orders', () => {
    // TODO: ステータスフィルタ、サプライヤーフィルタ、日付範囲フィルタのテスト
    // fixture.detectChanges();
    // component.selectedStatus = 'APPROVED';
    // component.onFilterChange();
    // expect(mockPurchaseOrderService.getOrders).toHaveBeenCalled();
  });
});
