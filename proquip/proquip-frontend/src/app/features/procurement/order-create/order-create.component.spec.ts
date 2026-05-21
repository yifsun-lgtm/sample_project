import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { OrderCreateComponent } from './order-create.component';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { SupplierService } from '@shared/services/supplier.service';
import { ProductService } from '@shared/services/product.service';

/**
 * 発注書作成コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 金額計算（技術的負債 #5のビジネスロジック）のテストが未実装
 * 金額上限バリデーション（技術的負債 #17）のテストが未実装
 *
 * 技術的負債: サプライヤー検索、製品サジェスト、下書き保存のテストが欠落
 */
describe('OrderCreateComponent', () => {
  let component: OrderCreateComponent;
  let fixture: ComponentFixture<OrderCreateComponent>;

  /** 発注サービスのモック */
  const mockPurchaseOrderService = {
    createOrder: jasmine.createSpy('createOrder').and.returnValue(of({
      id: 1,
      orderNumber: 'PO20260515001',
      status: 'DRAFT',
      totalAmount: 0,
      items: []
    })),
    submitForApproval: jasmine.createSpy('submitForApproval').and.returnValue(of({
      id: 1,
      status: 'PENDING_APPROVAL'
    }))
  };

  /** サプライヤーサービスのモック */
  const mockSupplierService = {
    getSuppliers: jasmine.createSpy('getSuppliers').and.returnValue(of({
      content: [
        {
          id: 1,
          code: 'SUP-001',
          name: 'テストサプライヤー株式会社',
          nameKana: 'テストサプライヤー',
          status: 'ACTIVE',
          rating: 4.0,
          email: 'test@example.com',
          phone: '03-1234-5678'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 200,
      number: 0,
      first: true,
      last: true,
      empty: false
    }))
  };

  /** 製品サービスのモック */
  const mockProductService = {
    searchProducts: jasmine.createSpy('searchProducts').and.returnValue(of({
      content: [
        {
          id: 1,
          sku: 'AB-1234',
          name: 'テスト製品A',
          unitPrice: 5000,
          unit: '個'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: false
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ReactiveFormsModule
      ],
      declarations: [OrderCreateComponent],
      providers: [
        { provide: PurchaseOrderService, useValue: mockPurchaseOrderService },
        { provide: SupplierService, useValue: mockSupplierService },
        { provide: ProductService, useValue: mockProductService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with empty items', () => {
    // フォームが初期化されていることを確認
    expect(component.orderForm).toBeTruthy();
    // 初期明細行が1行追加されていること
    expect(component.items.length).toBe(1);
    // 初期値の確認
    expect(component.subtotal).toBe(0);
    expect(component.totalAmount).toBe(0);
    expect(component.taxRate).toBe(0.10);
  });

  xit('should calculate totals', () => {
    // TODO: 金額計算のテスト（技術的負債 #5 のビジネスロジック）
    // const firstItem = component.items.at(0);
    // firstItem.patchValue({ quantity: 10, unitPrice: 5000 });
    // component.recalculateAmounts();
    // expect(component.subtotal).toBe(50000);
    // expect(component.taxAmount).toBe(5000);
    // expect(component.totalAmount).toBe(55000);
  });

  xit('should validate amounts', () => {
    // TODO: 金額上限バリデーションのテスト（技術的負債 #17）
    // フロント上限: 10,000,000 / バックエンド上限: 9,999,999.99
    // const firstItem = component.items.at(0);
    // firstItem.patchValue({ quantity: 1000, unitPrice: 10001 });
    // component.recalculateAmounts();
    // expect(component.errorMessage).toContain('上限');
  });

  // it('should submit order', () => {
  //   // TODO: 発注書作成のテスト
  //   // サプライヤーと明細行を設定した状態でonSubmit()を呼び出し、
  //   // createOrderが正しいパラメータで呼ばれることを確認
  //   //
  //   // component.orderForm.patchValue({
  //   //   supplierId: 1,
  //   //   supplierName: 'テストサプライヤー',
  //   //   deliveryAddress: '東京都千代田区',
  //   //   expectedDeliveryDate: '2026-06-15'
  //   // });
  //   // const item = component.items.at(0);
  //   // item.patchValue({
  //   //   productId: 1,
  //   //   productName: 'テスト製品',
  //   //   quantity: 10,
  //   //   unitPrice: 5000,
  //   //   unit: '個'
  //   // });
  //   // component.onSubmit();
  //   // expect(mockPurchaseOrderService.createOrder).toHaveBeenCalled();
  // });
});
