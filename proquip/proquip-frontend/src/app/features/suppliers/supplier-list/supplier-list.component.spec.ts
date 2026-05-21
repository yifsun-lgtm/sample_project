import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { SupplierListComponent } from './supplier-list.component';
import { SupplierService } from '@shared/services/supplier.service';

/**
 * サプライヤー一覧コンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限
 * 検索、フィルタリング、比較選択機能のテストが未実装
 */
describe('SupplierListComponent', () => {
  let component: SupplierListComponent;
  let fixture: ComponentFixture<SupplierListComponent>;

  /** サプライヤーサービスのモック */
  const mockSupplierService = {
    getSuppliers: jasmine.createSpy('getSuppliers').and.returnValue(of({
      content: [
        {
          id: 1,
          code: 'SUP-001',
          name: 'テストサプライヤー株式会社',
          nameKana: 'テストサプライヤー',
          status: 'ACTIVE',
          rating: 4.5,
          address: '東京都千代田区',
          phone: '03-1234-5678',
          email: 'test@supplier.co.jp',
          website: 'https://supplier.co.jp',
          paymentTerms: '月末締め翌月末払い',
          notes: ''
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [SupplierListComponent],
      providers: [
        { provide: SupplierService, useValue: mockSupplierService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SupplierListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should search suppliers', () => {
    // TODO: キーワード検索のテスト
    // component.onSearch('テスト');
    // expect(component.searchKeyword).toBe('テスト');
    // expect(mockSupplierService.getSuppliers).toHaveBeenCalled();
  });
});
