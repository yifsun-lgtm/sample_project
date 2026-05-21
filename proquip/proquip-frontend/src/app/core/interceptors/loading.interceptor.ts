import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '@core/services/loading.service';

/**
 * ローディングインターセプター
 * HTTPリクエストの開始・終了に連動してローディング状態を管理する
 */
@Injectable()
export class LoadingInterceptor implements HttpInterceptor {

  /** 実行中のリクエスト数 */
  private activeRequests = 0;

  constructor(private loadingService: LoadingService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // リクエスト開始時にカウントアップ
    this.activeRequests++;
    if (this.activeRequests === 1) {
      this.loadingService.show();
    }

    return next.handle(request).pipe(
      finalize(() => {
        // リクエスト完了時にカウントダウン
        this.activeRequests--;
        if (this.activeRequests === 0) {
          this.loadingService.hide();
        }
      })
    );
  }
}
