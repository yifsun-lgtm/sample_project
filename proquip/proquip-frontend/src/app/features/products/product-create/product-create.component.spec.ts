import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { ProductCreateComponent } from './product-create.component';
import { ProductService } from '@shared/services/product.service';

/**
 * 製品新規登録コンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * ウィザードの全ステップ遷移テスト、SKU重複チェック、送信テストが未実装
 *
 * 技術的負債 #17: SKUバリデーションのフロント/バック不整合に対応したテストがない
 */
describe('ProductCreateComponent', () => {
  let component: ProductCreateComponent;
  let fixture: ComponentFixture<ProductCreateComponent>;

  /** 製品サービスのモック */
  const mockProductService = {
    getCategories: jasmine.createSpy('getCategories').and.returnValue(of([
      { id: 1, name: '電子部品', description: '', parentId: null, productCount: 10 },
      { id: 2, name: '機械部品', description: '', parentId: null, productCount: 5 }
    ])),
    checkSkuExists: jasmine.createSpy('checkSkuExists').and.returnValue(of(false)),
    createProduct: jasmine.createSpy('createProduct').and.returnValue(of({
      id: 100,
      sku: 'AB-1234',
      name: '新規テスト製品'
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ReactiveFormsModule
      ],
      declarations: [ProductCreateComponent],
      providers: [
        { provide: ProductService, useValue: mockProductService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form', () => {
    // 基本情報フォームが初期化されていることを確認
    expect(component.basicForm).toBeTruthy();
    expect(component.pricingForm).toBeTruthy();
    expect(component.specForm).toBeTruthy();

    // 初期ステップが0であること
    expect(component.currentStep).toBe(0);

    // 基本情報フォームのデフォルト値
    expect(component.basicForm.get('status')?.value).toBe('ACTIVE');
  });

  xit('should validate SKU format', () => {
    // TODO: SKUバリデーションのテスト
    // 技術的負債 #17: フロント側のパターン ^[A-Z0-9]{2,4}-\d{3,8}$ のテスト
    // component.basicForm.patchValue({ sku: 'invalid' });
    // expect(component.basicForm.get('sku')?.valid).toBeFalse();
    //
    // component.basicForm.patchValue({ sku: 'AB-1234' });
    // expect(component.basicForm.get('sku')?.valid).toBeTrue();
    //
    // // バックエンドと不整合: 3桁の数字は通るが、バックエンドでは4桁以上が必須
    // component.basicForm.patchValue({ sku: 'AB-123' });
    // expect(component.basicForm.get('sku')?.valid).toBeTrue(); // フロント的にはvalid
  });

  // it('should submit form', () => {
  //   // TODO: フォーム送信のテスト
  //   // 全ステップの入力が完了した状態でsubmit()を呼び出し、
  //   // createProductが正しいパラメータで呼ばれることを確認
  //   // component.basicForm.patchValue({
  //   //   name: '新規テスト製品',
  //   //   sku: 'AB-1234',
  //   //   categoryId: 1,
  //   //   manufacturerId: 1,
  //   //   status: 'ACTIVE'
  //   // });
  //   // component.pricingForm.patchValue({
  //   //   unitPrice: 10000,
  //   //   unit: '個',
  //   //   minimumOrderQuantity: 1,
  //   //   leadTimeDays: 7
  //   // });
  //   // component.submit();
  //   // expect(mockProductService.createProduct).toHaveBeenCalled();
  // });
});
