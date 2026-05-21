import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { PriceEditComponent } from './price-edit.component';
import { ApiService } from '@shared/services/api.service';

/**
 * 価格編集コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 価格変更、割引率適用、一括更新のテストが未実装
 */
describe('PriceEditComponent', () => {
  let component: PriceEditComponent;
  let fixture: ComponentFixture<PriceEditComponent>;

  /** APIサービスのモック */
  const mockApiService = {
    get: jasmine.createSpy('get').and.returnValue(of({
      id: 1, name: 'テスト価格リスト', currency: 'JPY',
      effectiveFrom: '2026-01-01', effectiveTo: '2026-12-31', status: 'ACTIVE',
      items: [
        {
          id: 10, productId: 1, productName: 'テスト製品A', sku: 'AB-1234',
          standardUnitPrice: 5000, listPrice: 4500, discountRate: 10,
          taxRate: 10, taxIncludedPrice: 4950
        }
      ]
    })),
    put: jasmine.createSpy('put').and.returnValue(of({ success: true }))
  };

  /** ActivatedRouteのモック */
  const mockActivatedRoute = {
    params: of({ id: '1' }),
    snapshot: { paramMap: { get: () => '1' } }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [PriceEditComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PriceEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  xit('should update item price', () => {
    // TODO: 価格変更のテスト
    // component.updateItemPrice(0, 4000);
    // expect(component.priceItems[0].listPrice).toBe(4000);
    // expect(component.priceItems[0].isModified).toBeTrue();
  });

  xit('should apply bulk discount', () => {
    // TODO: 一括割引適用のテスト
    // component.applyBulkDiscount(15);
    // expect(component.priceItems[0].discountRate).toBe(15);
  });
});
