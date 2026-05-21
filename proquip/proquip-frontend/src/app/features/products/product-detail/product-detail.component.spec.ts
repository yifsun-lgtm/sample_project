import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ProductDetailComponent } from './product-detail.component';
import { ProductService } from '@shared/services/product.service';
import { SupplierService } from '@shared/services/supplier.service';
import { InventoryService } from '@shared/services/inventory.service';

/**
 * 製品詳細コンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限
 * タブ切り替え、各タブのデータ表示、削除確認ダイアログのテストが未実装
 */
describe('ProductDetailComponent', () => {
  let component: ProductDetailComponent;
  let fixture: ComponentFixture<ProductDetailComponent>;

  /** ActivatedRouteのモック */
  const mockActivatedRoute = {
    snapshot: {
      paramMap: {
        get: jasmine.createSpy('get').and.returnValue('1')
      }
    }
  };

  /** 製品サービスのモック */
  const mockProductService = {
    getProduct: jasmine.createSpy('getProduct').and.returnValue(of({
      id: 1,
      sku: 'AB-1234',
      name: 'テスト製品',
      description: 'テスト説明',
      categoryId: 1,
      categoryName: '電子部品',
      manufacturerId: 1,
      manufacturerName: 'テストメーカー',
      unitPrice: 12500,
      unit: '個',
      status: 'ACTIVE',
      specifications: '{}',
      suppliers: [],
      inventoryItems: [],
      priceHistory: []
    })),
    getProducts: jasmine.createSpy('getProducts').and.returnValue(of({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: true
    })),
    deleteProduct: jasmine.createSpy('deleteProduct').and.returnValue(of(void 0))
  };

  const mockSupplierService = {};

  const mockInventoryService = {};

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [ProductDetailComponent],
      providers: [
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        { provide: ProductService, useValue: mockProductService },
        { provide: SupplierService, useValue: mockSupplierService },
        { provide: InventoryService, useValue: mockInventoryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should load product detail', () => {
    // TODO: ngOnInit後に製品詳細が正しく表示されることを確認
    // fixture.detectChanges();
    // expect(mockProductService.getProduct).toHaveBeenCalledWith(1);
    // expect(component.product).toBeTruthy();
    // expect(component.product?.name).toBe('テスト製品');
  });
});
