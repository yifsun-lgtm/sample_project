import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

/**
 * エラーインターセプター
 * HTTPエラーレスポンスをキャッチし、エラー種別に応じた処理を行う
 *
 * 技術的負債 #5: alert()を使用してエラー表示している
 * NotificationServiceによるトースト通知に置き換えるべき
 */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor(
    private router: Router,
    private keycloakService: KeycloakService
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        switch (error.status) {
          case 401:
            // 認証エラー: Keycloakログインへリダイレクト
            console.error('認証エラー: セッションが期限切れです');
            this.keycloakService.login();
            break;

          case 403:
            // アクセス拒否エラー
            // 技術的負債: alert()ではなくNotificationServiceを使用すべき
            alert('アクセスが拒否されました。この操作を行う権限がありません。');
            this.router.navigate(['/dashboard']);
            break;

          case 404:
            console.error('リソースが見つかりません:', request.url);
            break;

          case 409:
            // 競合エラー（楽観的ロック等）
            // 技術的負債: alert()ではなくNotificationServiceを使用すべき
            alert('データが他のユーザーによって更新されています。画面を更新してください。');
            break;

          case 422:
            // バリデーションエラー
            console.error('バリデーションエラー:', error.error);
            break;

          case 500:
            // サーバーエラー
            // 技術的負債: alert()ではなくNotificationServiceを使用すべき
            alert('サーバーエラーが発生しました。しばらく経ってから再度お試しください。');
            console.error('サーバーエラー:', error.error);
            break;

          default:
            // その他のエラー
            // 技術的負債: alert()ではなくNotificationServiceを使用すべき
            alert('通信エラーが発生しました。ネットワーク接続を確認してください。');
            console.error('HTTPエラー:', error.status, error.message);
            break;
        }

        return throwError(() => error);
      })
    );
  }
}
