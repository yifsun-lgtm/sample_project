import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { OrderDetailComponent } from './order-detail.component';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';

/**
 * 発注書詳細コンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限
 * ステータスごとのアクションボタン表示テスト、承認・却下ワークフローのテストが未実装
 *
 * 技術的負債 #14: ステータス文字列リテラルに依存するテストが書かれていない
 */
describe('OrderDetailComponent', () => {
  let component: OrderDetailComponent;
  let fixture: ComponentFixture<OrderDetailComponent>;

  /** ActivatedRouteのモック */
  const mockActivatedRoute = {
    snapshot: {
      paramMap: {
        get: jasmine.createSpy('get').and.returnValue('1')
      }
    }
  };

  /** 発注サービスのモック */
  const mockPurchaseOrderService = {
    getOrder: jasmine.createSpy('getOrder').and.returnValue(of({
      id: 1,
      orderNumber: 'PO20260501001',
      supplierId: 1,
      supplierName: 'テストサプライヤー株式会社',
      status: 'APPROVED',
      orderDate: '2026-05-01',
      expectedDeliveryDate: '2026-05-15',
      actualDeliveryDate: null,
      totalAmount: 550000,
      currency: 'JPY',
      notes: 'テスト発注',
      createdBy: '山田太郎',
      approvedBy: '田中部長',
      approvedAt: '2026-05-02T10:00:00',
      items: [
        {
          id: 1,
          purchaseOrderId: 1,
          productId: 1,
          productName: 'テスト製品A',
          productSku: 'AB-1234',
          quantity: 100,
          unitPrice: 5000,
          totalPrice: 500000,
          receivedQuantity: 0,
          unit: '個',
          notes: ''
        }
      ],
      approvalSteps: [],
      createdAt: '2026-05-01T09:00:00',
      updatedAt: '2026-05-02T10:00:00'
    })),
    approveOrder: jasmine.createSpy('approveOrder').and.returnValue(of({})),
    rejectOrder: jasmine.createSpy('rejectOrder').and.returnValue(of({})),
    cancelOrder: jasmine.createSpy('cancelOrder').and.returnValue(of({})),
    receiveGoods: jasmine.createSpy('receiveGoods').and.returnValue(of({}))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [OrderDetailComponent],
      providers: [
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: PurchaseOrderService, useValue: mockPurchaseOrderService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should display order details', () => {
    // TODO: 発注書詳細データが正しく表示されることを確認
    // fixture.detectChanges();
    // expect(component.order).toBeTruthy();
    // expect(component.order?.orderNumber).toBe('PO20260501001');
    // expect(component.order?.totalAmount).toBe(550000);
    //
    // const compiled = fixture.nativeElement as HTMLElement;
    // expect(compiled.textContent).toContain('PO20260501001');
    // expect(compiled.textContent).toContain('テストサプライヤー株式会社');
  });
});
