import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

/**
 * 認証インターセプター
 * 外部APIリクエストにBearerトークンを自動付与する
 * /assets/ と /auth/ へのリクエストはスキップ
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  /** トークン付与をスキップするURLパターン */
  private readonly excludedUrls = ['/assets/', '/auth/'];

  constructor(private keycloakService: KeycloakService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // 除外URLへのリクエストはトークン付与をスキップ
    const isExcluded = this.excludedUrls.some(url => request.url.includes(url));
    if (isExcluded) {
      return next.handle(request);
    }

    // Keycloakからトークンを取得してヘッダーに付与
    return from(this.keycloakService.getToken()).pipe(
      switchMap(token => {
        if (token) {
          const authRequest = request.clone({
            setHeaders: {
              Authorization: `Bearer ${token}`
            }
          });
          return next.handle(authRequest);
        }
        return next.handle(request);
      })
    );
  }
}
