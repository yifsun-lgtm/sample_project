import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { ProductListComponent } from './product-list.component';
import { ProductService } from '@shared/services/product.service';
import { SharedModule } from '@shared/shared.module';

/**
 * 製品一覧コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * フィルタリング、ページネーション、CSV出力のテストが未実装
 * SharedModuleのインポートによりDataTableComponent等を一括読み込みしているが、
 * 本来は個別にモックすべき
 */
describe('ProductListComponent', () => {
  let component: ProductListComponent;
  let fixture: ComponentFixture<ProductListComponent>;

  /** 製品サービスのモック */
  const mockProductService = {
    getProducts: jasmine.createSpy('getProducts').and.returnValue(of({
      content: [
        {
          id: 1,
          sku: 'AB-1234',
          name: 'テスト製品A',
          categoryName: '電子部品',
          manufacturerName: 'テストメーカー',
          unitPrice: 12500,
          status: 'ACTIVE',
          stockQuantity: 100
        },
        {
          id: 2,
          sku: 'CD-5678',
          name: 'テスト製品B',
          categoryName: '機械部品',
          manufacturerName: 'テストメーカー2',
          unitPrice: 8900,
          status: 'ACTIVE',
          stockQuantity: 50
        }
      ],
      totalElements: 2,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false
    })),
    getCategories: jasmine.createSpy('getCategories').and.returnValue(of([
      { id: 1, name: '電子部品', description: '', parentId: null, productCount: 10 },
      { id: 2, name: '機械部品', description: '', parentId: null, productCount: 5 }
    ])),
    searchProducts: jasmine.createSpy('searchProducts').and.returnValue(of({
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
      declarations: [ProductListComponent],
      providers: [
        { provide: ProductService, useValue: mockProductService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load products on init', () => {
    // ngOnInitでloadProducts()が呼ばれることを確認
    fixture.detectChanges();
    expect(mockProductService.getProducts).toHaveBeenCalled();
    expect(component.products.length).toBeGreaterThan(0);
  });

  xit('should filter products', () => {
    // TODO: カテゴリ、メーカー、ステータスのフィルタテスト
    // component.selectedCategoryId = 1;
    // component.onFilterChange();
    // expect(mockProductService.getProducts).toHaveBeenCalled();
  });

  xit('should paginate', () => {
    // TODO: ページネーションのテスト
    // component.onPageChange({ page: 2, pageSize: 20 });
    // expect(mockProductService.getProducts).toHaveBeenCalledWith(1, 20, undefined);
  });
});
