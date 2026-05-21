import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { RequisitionCreateComponent } from './requisition-create.component';
import { RequisitionService } from '@shared/services/requisition.service';
import { ProductService } from '@shared/services/product.service';

/**
 * 購買依頼作成コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 明細行の追加・削除、金額計算（技術的負債 #5）のテストが未実装
 * maxAmount バリデーション（技術的負債 #17）のテストが未実装
 *
 * 技術的負債: 製品サジェスト、依頼理由のバリデーション、下書き保存のテストが欠落
 */
describe('RequisitionCreateComponent', () => {
  let component: RequisitionCreateComponent;
  let fixture: ComponentFixture<RequisitionCreateComponent>;

  /** 購買依頼サービスのモック */
  const mockRequisitionService = {
    createRequisition: jasmine.createSpy('createRequisition').and.returnValue(of({
      id: 1,
      requisitionNumber: 'REQ20260515001',
      status: 'DRAFT',
      totalAmount: 0,
      items: []
    })),
    submitForApproval: jasmine.createSpy('submitForApproval').and.returnValue(of({
      id: 1,
      status: 'PENDING_APPROVAL'
    }))
  };

  /** 製品サービスのモック */
  const mockProductService = {
    searchProducts: jasmine.createSpy('searchProducts').and.returnValue(of({
      content: [
        { id: 1, sku: 'AB-1234', name: 'テスト製品A', unitPrice: 5000, unit: '個' }
      ],
      totalElements: 1, totalPages: 1, size: 10, number: 0,
      first: true, last: true, empty: false
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ReactiveFormsModule
      ],
      declarations: [RequisitionCreateComponent],
      providers: [
        { provide: RequisitionService, useValue: mockRequisitionService },
        { provide: ProductService, useValue: mockProductService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RequisitionCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with empty values', () => {
    // フォームが初期化されていることを確認
    expect(component.requisitionForm).toBeTruthy();
  });

  xit('should add and remove item lines', () => {
    // TODO: 明細行の追加・削除のテスト（技術的負債 #5 のビジネスロジック）
    // component.addItem();
    // expect(component.items.length).toBe(2);
    // component.removeItem(1);
    // expect(component.items.length).toBe(1);
  });

  xit('should calculate total amount', () => {
    // TODO: 合計金額計算のテスト（技術的負債 #5）
    // const item = component.items.at(0);
    // item.patchValue({ quantity: 5, estimatedUnitPrice: 3000 });
    // component.recalculate();
    // expect(component.totalAmount).toBe(15000);
  });
});
