import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';

import { SearchBoxComponent } from './search-box.component';

/**
 * 検索ボックスコンポーネントのテスト
 *
 * 技術的負債 #13: デバウンスのテストがfakeAsyncとハードコードタイミングに依存
 * コンポーネント側の技術的負債（setTimeoutベースのデバウンス）に起因する
 * RxJSのdebounceTimeに置き換えればtestSchedulerで安定してテストできる
 */
describe('SearchBoxComponent', () => {
  let component: SearchBoxComponent;
  let fixture: ComponentFixture<SearchBoxComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [SearchBoxComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SearchBoxComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit search on input', fakeAsync(() => {
    // 技術的負債: setTimeoutベースのデバウンスのため、
    // ハードコードされたタイミング（300ms）に依存したテスト
    // デバウンス時間が変更されるとこのテストも壊れる

    spyOn(component.searchChange, 'emit');

    // 検索文字列を入力
    component.searchTerm = 'テスト検索';
    component.onInputChange();

    // デバウンス時間（300ms）経過前はemitされない
    tick(100);
    expect(component.searchChange.emit).not.toHaveBeenCalled();

    // デバウンス時間経過後にemitされる
    // 技術的負債: 300msはコンポーネントのデフォルト値に依存するハードコード
    tick(200);
    expect(component.searchChange.emit).toHaveBeenCalledWith('テスト検索');
  }));
});
