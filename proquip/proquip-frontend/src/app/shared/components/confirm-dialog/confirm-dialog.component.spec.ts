import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfirmDialogComponent } from './confirm-dialog.component';

/**
 * 確認ダイアログコンポーネントのテスト
 *
 * 技術的負債 #13: テストカバレッジが不十分
 * 確認・キャンセルのイベントエミット、カスタムテキスト表示のテストが未実装
 */
describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ConfirmDialogComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default title and message', () => {
    expect(component.title).toBe('確認');
    expect(component.message).toBe('この操作を実行してもよろしいですか？');
    expect(component.confirmText).toBe('確認');
    expect(component.cancelText).toBe('キャンセル');
  });

  xit('should emit confirmed true on confirm', () => {
    // TODO: 確認ボタン押下時のイベントエミットテスト
    // spyOn(component.confirmed, 'emit');
    // component.visible = true;
    // fixture.detectChanges();
    // component.onConfirm();
    // expect(component.confirmed.emit).toHaveBeenCalledWith(true);
  });

  xit('should emit confirmed false on cancel', () => {
    // TODO: キャンセルボタン押下時のイベントエミットテスト
    // spyOn(component.confirmed, 'emit');
    // component.visible = true;
    // fixture.detectChanges();
    // component.onCancel();
    // expect(component.confirmed.emit).toHaveBeenCalledWith(false);
  });
});
