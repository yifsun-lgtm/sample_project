import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { StockTransferCreateComponent } from './stock-transfer-create.component';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';
import { ProductService } from '@shared/services/product.service';

/**
 * 在庫移動作成コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 倉庫選択、数量バリデーション、送信処理のテストが未実装
 *
 * 技術的負債: 製品検索がdebounceなし（コンポーネント側の負債を反映）
 */
describe('StockTransferCreateComponent', () => {
  let component: StockTransferCreateComponent;
  let fixture: ComponentFixture<StockTransferCreateComponent>;

  /** 在庫サービスのモック */
  const mockInventoryService = {
    createTransfer: jasmine.createSpy('createTransfer').and.returnValue(of({
      id: 1, transferNumber: 'TRF20260515001', status: 'REQUESTED'
    }))
  };

  /** 倉庫サービスのモック */
  const mockWarehouseService = {
    getWarehouses: jasmine.createSpy('getWarehouses').and.returnValue(of([
      { id: 1, code: 'WH-001', name: '東京倉庫', active: true },
      { id: 2, code: 'WH-002', name: '大阪倉庫', active: true }
    ]))
  };

  /** 製品サービスのモック */
  const mockProductService = {
    searchProducts: jasmine.createSpy('searchProducts').and.returnValue(of({
      content: [{ id: 1, sku: 'AB-1234', name: 'テスト製品A' }],
      totalElements: 1, totalPages: 1, size: 10, number: 0,
      first: true, last: true, empty: false
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, ReactiveFormsModule],
      declarations: [StockTransferCreateComponent],
      providers: [
        { provide: InventoryService, useValue: mockInventoryService },
        { provide: WarehouseService, useValue: mockWarehouseService },
        { provide: ProductService, useValue: mockProductService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StockTransferCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should prevent transfer to same warehouse', () => {
    // TODO: 同一倉庫への移動禁止バリデーションのテスト
    // component.transferForm.patchValue({
    //   sourceWarehouseId: 1,
    //   destinationWarehouseId: 1
    // });
    // expect(component.transferForm.valid).toBeFalse();
  });

  xit('should validate quantity does not exceed available stock', () => {
    // TODO: 移動数量が在庫数を超えないバリデーションのテスト
    // component.transferForm.patchValue({
    //   sourceWarehouseId: 1,
    //   productId: 1,
    //   quantity: 9999
    // });
    // expect(component.hasStockError()).toBeTrue();
  });
});
