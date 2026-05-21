import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { PurchaseOrderService } from './purchase-order.service';
import { ApiService } from './api.service';

/**
 * 発注サービスのテスト
 *
 * 技術的負債 #13: テストが最小限
 * 承認ワークフロー（承認・却下・キャンセル）、入荷処理、
 * 明細操作のテストが全て未実装
 */
describe('PurchaseOrderService', () => {
  let service: PurchaseOrderService;

  /** ApiServiceのモック */
  const mockApiService = {
    get: jasmine.createSpy('get').and.returnValue(of({
      content: [
        {
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
          notes: '',
          createdBy: '山田太郎',
          approvedBy: '田中部長',
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
    })),
    post: jasmine.createSpy('post').and.returnValue(of({})),
    put: jasmine.createSpy('put').and.returnValue(of({})),
    delete: jasmine.createSpy('delete').and.returnValue(of(void 0))
  };

  beforeEach(() => {
    mockApiService.get.calls.reset();
    mockApiService.post.calls.reset();
    mockApiService.put.calls.reset();
    mockApiService.delete.calls.reset();

    TestBed.configureTestingModule({
      providers: [
        PurchaseOrderService,
        { provide: ApiService, useValue: mockApiService }
      ]
    });
    service = TestBed.inject(PurchaseOrderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call GET /api/purchase-orders', () => {
    // 発注書一覧取得のテスト
    service.getOrders(0, 20).subscribe(result => {
      expect(result.content.length).toBe(1);
      expect(result.content[0].orderNumber).toBe('PO20260501001');
      expect(result.content[0].totalAmount).toBe(550000);
    });

    expect(mockApiService.get).toHaveBeenCalledWith(
      '/purchase-orders',
      jasmine.objectContaining({ page: 0, size: 20 })
    );
  });
});
