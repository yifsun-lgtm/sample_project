import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormFieldComponent } from './form-field.component';

/**
 * フォームフィールドラッパーコンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 必須マーク表示、エラーメッセージ表示、ヒントテキスト表示のテストが未実装
 */
describe('FormFieldComponent', () => {
  let component: FormFieldComponent;
  let fixture: ComponentFixture<FormFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [FormFieldComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(FormFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default empty values', () => {
    expect(component.label).toBe('');
    expect(component.required).toBeFalse();
    expect(component.errorMessage).toBe('');
    expect(component.hint).toBe('');
  });

  xit('should display required marker when required is true', () => {
    // TODO: 必須マーク表示のテスト
    // component.required = true;
    // component.label = 'テストラベル';
    // fixture.detectChanges();
    // const compiled = fixture.nativeElement as HTMLElement;
    // expect(compiled.querySelector('.required-marker')).toBeTruthy();
  });

  xit('should display error message when provided', () => {
    // TODO: エラーメッセージ表示のテスト
    // component.errorMessage = '入力が必要です';
    // fixture.detectChanges();
    // const compiled = fixture.nativeElement as HTMLElement;
    // expect(compiled.querySelector('.error-message')?.textContent).toContain('入力が必要です');
  });
});
