import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleChange, SimpleChanges } from '@angular/core';

import { DataTableComponent, TableColumn, PageChangeEvent, SortChangeEvent } from './data-table.component';

/**
 * データテーブルコンポーネントのテスト
 *
 * 技術的負債 #13: ソート機能のテストがskipされている
 * クライアントサイドソート（技術的負債 #5）の挙動テストが必要
 */
describe('DataTableComponent', () => {
  let component: DataTableComponent;
  let fixture: ComponentFixture<DataTableComponent>;

  /** テスト用のカラム定義 */
  const testColumns: TableColumn[] = [
    { key: 'id', label: 'ID', sortable: true, width: '60px', type: 'number' },
    { key: 'name', label: '名前', sortable: true, type: 'text' },
    { key: 'price', label: '価格', sortable: true, width: '120px', type: 'currency' },
    { key: 'status', label: 'ステータス', sortable: false, width: '100px', type: 'status' }
  ];

  /** テスト用のデータ */
  const testData = [
    { id: 1, name: 'テスト製品A', price: 10000, status: 'ACTIVE' },
    { id: 2, name: 'テスト製品B', price: 20000, status: 'INACTIVE' },
    { id: 3, name: 'テスト製品C', price: 15000, status: 'ACTIVE' },
    { id: 4, name: 'テスト製品D', price: 8000, status: 'PENDING' },
    { id: 5, name: 'テスト製品E', price: 30000, status: 'ACTIVE' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DataTableComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(DataTableComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render table headers', () => {
    // カラム定義を設定
    component.columns = testColumns;
    component.data = testData;
    component.totalCount = testData.length;
    component.pageSize = 20;
    component.currentPage = 1;

    // ngOnChangesを手動で発火
    const changes: SimpleChanges = {
      data: new SimpleChange(null, testData, true),
      columns: new SimpleChange(null, testColumns, true)
    };
    component.ngOnChanges(changes);

    fixture.detectChanges();

    // カラムヘッダーが定義数分だけ存在すること
    expect(component.columns.length).toBe(4);
    expect(component.columns[0].label).toBe('ID');
    expect(component.columns[1].label).toBe('名前');
    expect(component.columns[2].label).toBe('価格');
    expect(component.columns[3].label).toBe('ステータス');
  });

  it('should emit page change', () => {
    // ページ変更イベントのテスト
    component.columns = testColumns;
    component.data = testData;
    component.totalCount = 100;
    component.pageSize = 20;
    component.currentPage = 1;

    const changes: SimpleChanges = {
      data: new SimpleChange(null, testData, true)
    };
    component.ngOnChanges(changes);

    spyOn(component.pageChange, 'emit');

    // 2ページ目に遷移
    component.goToPage(2);

    expect(component.pageChange.emit).toHaveBeenCalledWith(
      jasmine.objectContaining({ page: 2, pageSize: 20 })
    );
  });

  xit('should sort columns', () => {
    // TODO: ソート機能のテスト
    // 技術的負債: クライアントサイドソートとサーバーサイドソートの両方が動作する
    // ため、テストが複雑になる
    //
    // component.columns = testColumns;
    // component.data = [...testData];
    // component.totalCount = testData.length;
    //
    // const changes: SimpleChanges = {
    //   data: new SimpleChange(null, testData, true)
    // };
    // component.ngOnChanges(changes);
    //
    // spyOn(component.sortChange, 'emit');
    //
    // // 名前カラムでソート
    // component.onSort(testColumns[1]);
    // expect(component.sortColumn).toBe('name');
    // expect(component.sortDirection).toBe('asc');
    // expect(component.sortChange.emit).toHaveBeenCalledWith(
    //   jasmine.objectContaining({ column: 'name', direction: 'asc' })
    // );
    //
    // // 同じカラムで再度ソート（降順に切り替わる）
    // component.onSort(testColumns[1]);
    // expect(component.sortDirection).toBe('desc');
    //
    // // sortableでないカラムではソートされないこと
    // component.onSort(testColumns[3]); // status, sortable: false
    // expect(component.sortColumn).toBe('name'); // 変更されない
  });
});
