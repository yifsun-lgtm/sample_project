import { TestBed } from '@angular/core/testing';
import { KeycloakService } from 'keycloak-angular';

import { AuthService } from './auth.service';

/**
 * 認証サービスのテスト
 *
 * 技術的負債 #13: テストが最小限
 * ロールのObservable化（技術的負債）に伴い、リアクティブなテストが必要
 * Keycloak連携部分のテストが未実装
 */
describe('AuthService', () => {
  let service: AuthService;

  /** KeycloakServiceのモック */
  const mockKeycloakService = {
    getUserRoles: jasmine.createSpy('getUserRoles').and.returnValue(['ADMIN', 'PROCUREMENT_USER']),
    isLoggedIn: jasmine.createSpy('isLoggedIn').and.returnValue(true),
    getUsername: jasmine.createSpy('getUsername').and.returnValue('yamada.taro'),
    getToken: jasmine.createSpy('getToken').and.returnValue(Promise.resolve('mock-token')),
    loadUserProfile: jasmine.createSpy('loadUserProfile').and.returnValue(Promise.resolve({
      id: 'user-001',
      username: 'yamada.taro',
      firstName: '太郎',
      lastName: '山田',
      email: 'yamada@example.com'
    })),
    login: jasmine.createSpy('login'),
    logout: jasmine.createSpy('logout')
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: KeycloakService, useValue: mockKeycloakService }
      ]
    });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  xit('should return user roles', () => {
    // TODO: ロール取得のテスト
    // 技術的負債: ロールがプレーン配列であるため、キャッシュの挙動もテストが必要
    // const roles = service.getRoles();
    // expect(roles).toContain('ADMIN');
    // expect(roles).toContain('PROCUREMENT_USER');
    // expect(roles.length).toBe(2);
  });

  xit('should check role', () => {
    // TODO: ロールチェックのテスト
    // expect(service.hasRole('ADMIN')).toBeTrue();
    // expect(service.hasRole('NONEXISTENT_ROLE')).toBeFalse();
  });
});
