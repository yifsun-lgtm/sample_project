import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * ローディングサービス
 * アプリケーション全体のローディング状態を管理する
 */
@Injectable({
  providedIn: 'root'
})
export class LoadingService {

  /** ローディング状態を保持するBehaviorSubject */
  private loadingSubject = new BehaviorSubject<boolean>(false);

  /** ローディング状態のObservable */
  loading$: Observable<boolean> = this.loadingSubject.asObservable();

  /** ローディング表示 */
  show(): void {
    this.loadingSubject.next(true);
  }

  /** ローディング非表示 */
  hide(): void {
    this.loadingSubject.next(false);
  }
}
