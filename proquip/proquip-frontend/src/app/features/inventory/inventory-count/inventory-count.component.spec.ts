import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { InventoryCountComponent } from './inventory-count.component';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';

/**
 * 棚卸しコンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 差異の計算、調整理由の入力、確定処理のテストが未実装
 *
 * 技術的負債: 倉庫全体の在庫を一度にロードする（コンポーネント側の負債を反映）
 */
describe('InventoryCountComponent', () => {
  let component: InventoryCountComponent;
  let fixture: ComponentFixture<InventoryCountComponent>;

  /** 在庫サービスのモック */
  const mockInventoryService = {
    getItemsByWarehouse: jasmine.createSpy('getItemsByWarehouse').and.returnValue(of([
      { id: 1, productName: 'テスト製品A', productSku: 'AB-1234', quantity: 100 },
      { id: 2, productName: 'テスト製品B', productSku: 'CD-5678', quantity: 50 }
    ])),
    submitCount: jasmine.createSpy('submitCount').and.returnValue(of({ success: true }))
  };

  /** 倉庫サービスのモック */
  const mockWarehouseService = {
    getWarehouses: jasmine.createSpy('getWarehouses').and.returnValue(of([
      { id: 1, code: 'WH-001', name: '東京倉庫', active: true },
      { id: 2, code: 'WH-002', name: '大阪倉庫', active: true }
    ]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [InventoryCountComponent],
      providers: [
        { provide: InventoryService, useValue: mockInventoryService },
        { provide: WarehouseService, useValue: mockWarehouseService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InventoryCountComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should calculate discrepancy', () => {
    // TODO: 差異計算のテスト
    // component.countLines[0].actualQuantity = 95;
    // component.calculateDiscrepancy(0);
    // expect(component.countLines[0].discrepancy).toBe(-5);
  });

  xit('should require adjustment reason for discrepancies', () => {
    // TODO: 差異がある場合に調整理由が必須であるテスト
    // component.countLines[0].actualQuantity = 90;
    // component.countLines[0].adjustmentReason = '';
    // expect(component.canSubmit()).toBeFalse();
  });
});
