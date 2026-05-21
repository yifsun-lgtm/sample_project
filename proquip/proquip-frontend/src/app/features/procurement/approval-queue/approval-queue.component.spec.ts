import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { ApprovalQueueComponent } from './approval-queue.component';
import { PurchaseOrderService } from '@shared/services/purchase-order.service';
import { RequisitionService } from '@shared/services/requisition.service';

/**
 * 承認待ちキューコンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 承認・却下操作、フィルタリング、一括承認のテストが未実装
 */
describe('ApprovalQueueComponent', () => {
  let component: ApprovalQueueComponent;
  let fixture: ComponentFixture<ApprovalQueueComponent>;

  /** 発注サービスのモック */
  const mockPurchaseOrderService = {
    getPendingApprovals: jasmine.createSpy('getPendingApprovals').and.returnValue(of({
      content: [
        {
          id: 1, orderNumber: 'PO20260501001', status: 'PENDING_APPROVAL',
          totalAmount: 150000, supplierName: 'テストサプライヤー'
        }
      ],
      totalElements: 1, totalPages: 1, size: 20, number: 0,
      first: true, last: true, empty: false
    })),
    approveOrder: jasmine.createSpy('approveOrder').and.returnValue(of({ status: 'APPROVED' })),
    rejectOrder: jasmine.createSpy('rejectOrder').and.returnValue(of({ status: 'REJECTED' }))
  };

  /** 購買依頼サービスのモック */
  const mockRequisitionService = {
    getPendingApprovals: jasmine.createSpy('getPendingApprovals').and.returnValue(of({
      content: [],
      totalElements: 0, totalPages: 0, size: 20, number: 0,
      first: true, last: true, empty: true
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [ApprovalQueueComponent],
      providers: [
        { provide: PurchaseOrderService, useValue: mockPurchaseOrderService },
        { provide: RequisitionService, useValue: mockRequisitionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApprovalQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should approve an order', () => {
    // TODO: 承認操作のテスト
    // component.approve(1, 'order', '承認します');
    // expect(mockPurchaseOrderService.approveOrder).toHaveBeenCalledWith(1, '承認します');
  });

  xit('should reject an order with reason', () => {
    // TODO: 却下操作のテスト（理由必須）
    // component.reject(1, 'order', '予算超過のため却下');
    // expect(mockPurchaseOrderService.rejectOrder).toHaveBeenCalledWith(1, '予算超過のため却下');
  });
});
