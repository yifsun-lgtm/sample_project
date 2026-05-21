import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ProductService } from './product.service';
import { ApiService } from './api.service';

/**
 * 製品サービスのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * エラーハンドリング、検索機能、カテゴリ取得のテストが未実装
 */
describe('ProductService', () => {
  let service: ProductService;

  /** ApiServiceのモック */
  const mockApiService = {
    get: jasmine.createSpy('get').and.returnValue(of({
      content: [
        {
          id: 1,
          sku: 'AB-1234',
          name: 'テスト製品A',
          description: 'テスト説明',
          categoryId: 1,
          categoryName: '電子部品',
          manufacturerId: 1,
          manufacturerName: 'テストメーカー',
          unitPrice: 12500,
          unit: '個',
          status: 'ACTIVE',
          minimumOrderQuantity: 1,
          leadTimeDays: 7
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
    post: jasmine.createSpy('post').and.returnValue(of({})),
    put: jasmine.createSpy('put').and.returnValue(of({})),
    delete: jasmine.createSpy('delete').and.returnValue(of(void 0))
  };

  beforeEach(() => {
    // 各テストでspyをリセット
    mockApiService.get.calls.reset();
    mockApiService.post.calls.reset();
    mockApiService.put.calls.reset();
    mockApiService.delete.calls.reset();

    TestBed.configureTestingModule({
      providers: [
        ProductService,
        { provide: ApiService, useValue: mockApiService }
      ]
    });
    service = TestBed.inject(ProductService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call GET /api/products', () => {
    // 製品一覧取得のテスト
    service.getProducts(0, 20).subscribe(result => {
      expect(result.content.length).toBe(1);
      expect(result.content[0].sku).toBe('AB-1234');
    });

    expect(mockApiService.get).toHaveBeenCalledWith(
      '/products',
      jasmine.objectContaining({ page: 0, size: 20 })
    );
  });

  xit('should handle error', () => {
    // TODO: APIエラー時のハンドリングテスト
    // 技術的負債: ApiServiceのエラーハンドリングが不一致（GET/PUTはcatchError、POST/DELETEはなし）
    // mockApiService.get.and.returnValue(throwError(() => new Error('API Error')));
    // service.getProducts(0, 20).subscribe({
    //   error: (err) => {
    //     expect(err.message).toBe('API Error');
    //   }
    // });
  });
});
