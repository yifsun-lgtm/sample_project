import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';

import { UserManagementComponent } from './user-management.component';
import { UserService } from '@shared/services/user.service';

/**
 * ユーザー管理コンポーネントのテスト
 *
 * 技術的負債 #13: テストが最小限（should createのみ）
 * ユーザー検索、ロール変更、プロファイル編集のテストが未実装
 */
describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;

  /** ユーザーサービスのモック */
  const mockUserService = {
    getUsers: jasmine.createSpy('getUsers').and.returnValue(of({
      content: [
        {
          id: 'user-001',
          username: 'yamada.taro',
          firstName: '太郎',
          lastName: '山田',
          email: 'yamada@example.com',
          department: '調達部',
          roles: [{ id: 'role-1', name: 'PROCUREMENT_USER', description: '調達ユーザー', permissions: [] }],
          enabled: true,
          createdAt: '2025-01-15T09:00:00'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false
    })),
    getRoles: jasmine.createSpy('getRoles').and.returnValue(of([
      { id: 'role-1', name: 'ADMIN', description: '管理者', permissions: [] },
      { id: 'role-2', name: 'PROCUREMENT_USER', description: '調達ユーザー', permissions: [] },
      { id: 'role-3', name: 'VIEWER', description: '閲覧者', permissions: [] }
    ]))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      declarations: [UserManagementComponent],
      providers: [
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
