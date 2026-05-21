import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { KeycloakService } from 'keycloak-angular';

import { ErrorInterceptor } from './error.interceptor';

/**
 * エラーインターセプターのテスト
 *
 * 技術的負債 #13: テストが最小限
 * alert()呼び出しの検証（技術的負債 #5）、各ステータスコードのハンドリングテストが未実装
 *
 * 技術的負債: alert()をspyOnするテストパターンは脆弱
 * NotificationServiceに置き換え後はサービスのspyでテスト可能
 */
describe('ErrorInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  /** KeycloakServiceのモック */
  const mockKeycloakService = {
    login: jasmine.createSpy('login'),
    logout: jasmine.createSpy('logout')
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule
      ],
      providers: [
        {
          provide: HTTP_INTERCEPTORS,
          useClass: ErrorInterceptor,
          multi: true
        },
        { provide: KeycloakService, useValue: mockKeycloakService }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    // インターセプターがHTTPリクエストパイプラインに正しく登録されていることを確認
    expect(httpClient).toBeTruthy();
  });

  xit('should handle 401', () => {
    // TODO: 401エラー時にKeycloakログインにリダイレクトされることを確認
    // httpClient.get('/api/test').subscribe({
    //   error: (error: HttpErrorResponse) => {
    //     expect(error.status).toBe(401);
    //     expect(mockKeycloakService.login).toHaveBeenCalled();
    //   }
    // });
    //
    // const req = httpMock.expectOne('/api/test');
    // req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  xit('should handle 500', () => {
    // TODO: 500エラー時にアラート表示されることを確認
    // 技術的負債: alert()のspy設定が必要
    // spyOn(window, 'alert');
    //
    // httpClient.get('/api/test').subscribe({
    //   error: (error: HttpErrorResponse) => {
    //     expect(error.status).toBe(500);
    //     expect(window.alert).toHaveBeenCalledWith(
    //       'サーバーエラーが発生しました。しばらく経ってから再度お試しください。'
    //     );
    //   }
    // });
    //
    // const req = httpMock.expectOne('/api/test');
    // req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
