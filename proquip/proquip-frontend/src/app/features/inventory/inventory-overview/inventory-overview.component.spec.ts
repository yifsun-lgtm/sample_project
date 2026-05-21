import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { InventoryOverviewComponent } from './inventory-overview.component';
import { InventoryService } from '@shared/services/inventory.service';
import { WarehouseService } from '@shared/services/warehouse.service';

/**
 * 在庫一覧コンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限
 * サマリーカード表示、倉庫フィルタ、在庫ステータス色ロジックのテストが未実装
 */
describe('InventoryOverviewComponent', () => {
  let component: InventoryOverviewComponent;
  let fixture: ComponentFixture<InventoryOverviewComponent>;

  /** 在庫サービスのモック */
  const mockInventoryService = {
    getInventoryItems: jasmine.createSpy('getInventoryItems').and.returnValue(of({
      content: [
        {
          id: 1,
          productId: 1,
          productName: 'テスト製品A',
          productSku: 'AB-1234',
          warehouseId: 1,
          warehouseName: '東京倉庫',
          quantity: 100,
          reservedQuantity: 10,
          availableQuantity: 90,
          minimumStock: 20,
          maximumStock: 500,
          reorderPoint: 30,
          status: 'IN_STOCK'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false
    })),
    getLowStockAlerts: jasmine.createSpy('getLowStockAlerts').and.returnValue(of([]))
  };

  /** 倉庫サービスのモック */
  const mockWarehouseService = {
    getWarehouses: jasmine.createSpy('getWarehouses').and.returnValue(of([
      {
        id: 1,
        code: 'WH-TKY',
        name: '東京倉庫',
        status: 'ACTIVE'
      }
    ]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [InventoryOverviewComponent],
      providers: [
        { provide: InventoryService, useValue: mockInventoryService },
        { provide: WarehouseService, useValue: mockWarehouseService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InventoryOverviewComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should load inventory data', () => {
    // TODO: ngOnInit後に在庫データが正しくロードされることを確認
    // fixture.detectChanges();
    // expect(mockInventoryService.getInventoryItems).toHaveBeenCalled();
    // expect(component.inventoryItems.length).toBeGreaterThan(0);
  });
});
