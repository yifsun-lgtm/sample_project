import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuditLogComponent } from './audit-log.component';
import { AdminService } from '@shared/services/admin.service';

/**
 * 監査ログコンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限
 * フィルタリング（エンティティタイプ、ユーザー、日時範囲）、エクスポートのテストが未実装
 * 大量データ時のパフォーマンステストも必要（クライアントサイドフィルタリングの技術的負債）
 */
describe('AuditLogComponent', () => {
  let component: AuditLogComponent;
  let fixture: ComponentFixture<AuditLogComponent>;

  /** 管理者サービスのモック */
  const mockAdminService = {
    getAuditLogs: jasmine.createSpy('getAuditLogs').and.returnValue(of({
      content: [
        {
          id: 1,
          action: 'CREATE',
          entityType: 'PurchaseOrder',
          entityId: '1',
          username: 'yamada.taro',
          timestamp: '2026-05-10T14:30:00',
          details: '{"orderNumber": "PO20260510001"}',
          ipAddress: '192.168.1.100'
        },
        {
          id: 2,
          action: 'UPDATE',
          entityType: 'Product',
          entityId: '42',
          username: 'tanaka.hanako',
          timestamp: '2026-05-10T15:00:00',
          details: '{"field": "unitPrice", "oldValue": "12000", "newValue": "12500"}',
          ipAddress: '192.168.1.101'
        }
      ],
      totalElements: 2,
      totalPages: 1,
      size: 50,
      number: 0,
      first: true,
      last: true,
      empty: false
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AuditLogComponent],
      providers: [
        { provide: AdminService, useValue: mockAdminService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should filter logs', () => {
    // TODO: エンティティタイプやユーザー名によるフィルタリングのテスト
    // fixture.detectChanges();
    // expect(mockAdminService.getAuditLogs).toHaveBeenCalled();
    //
    // // エンティティタイプでフィルタ
    // component.selectedEntityType = 'PurchaseOrder';
    // component.onFilterChange();
    // expect(mockAdminService.getAuditLogs).toHaveBeenCalledWith(0, 50, 'PurchaseOrder');
  });
});
