import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { GoodsReceiptComponent } from './goods-receipt.component';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';

/**
 * 入荷検収コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 受領数量の入力、品質チェック、部分入荷のテストが未実装
 *
 * 技術的負債: ローディング状態の管理がない（コンポーネント側の負債を反映）
 */
describe('GoodsReceiptComponent', () => {
  let component: GoodsReceiptComponent;
  let fixture: ComponentFixture<GoodsReceiptComponent>;

  /** 発注サービスのモック */
  const mockPurchaseOrderService = {
    getOrder: jasmine.createSpy('getOrder').and.returnValue(of({
      id: 1, orderNumber: 'PO20260501001', status: 'ORDERED',
      supplierName: 'テストサプライヤー',
      items: [
        {
          id: 10, productName: 'テスト製品A', productSku: 'AB-1234',
          quantity: 100, receivedQuantity: 0, unitPrice: 5000
        }
      ]
    })),
    receiveGoods: jasmine.createSpy('receiveGoods').and.returnValue(of({ success: true }))
  };

  /** ActivatedRouteのモック */
  const mockActivatedRoute = {
    params: of({ id: '1' }),
    snapshot: { paramMap: { get: () => '1' } }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [GoodsReceiptComponent],
      providers: [
        { provide: PurchaseOrderService, useValue: mockPurchaseOrderService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GoodsReceiptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should submit goods receipt', () => {
    // TODO: 入荷検収の送信テスト
    // component.receiptLines[0].receivedQuantity = 100;
    // component.receiptLines[0].isAccepted = true;
    // component.submitReceipt();
    // expect(mockPurchaseOrderService.receiveGoods).toHaveBeenCalled();
  });

  xit('should validate received quantity does not exceed ordered', () => {
    // TODO: 受領数量バリデーションのテスト
    // component.receiptLines[0].receivedQuantity = 200; // 発注数100を超過
    // expect(component.hasValidationErrors()).toBeTrue();
  });
});
