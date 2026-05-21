import { Injectable } from '@angular/core';

/**
 * 通知メッセージの型定義
 */
export interface NotificationMessage {
  id: number;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  timestamp: Date;
}

/**
 * 通知サービス
 * トースト通知やアラートメッセージを管理する
 *
 * 技術的負債: メッセージをプレーンな配列で管理しており、自動非表示機能がない
 * タイムアウト管理が手動で、メモリリークの可能性がある
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  /** 通知メッセージの一覧（技術的負債: BehaviorSubjectにすべき） */
  messages: NotificationMessage[] = [];

  /** メッセージIDのカウンター */
  private nextId = 0;

  /** タイムアウト管理用のMap（技術的負債: クリーンアップが不完全） */
  private timeouts: { [key: number]: any } = {};

  /** 成功メッセージを表示 */
  success(message: string): void {
    this.addMessage('success', message);
  }

  /** エラーメッセージを表示 */
  error(message: string): void {
    this.addMessage('error', message);
  }

  /** 警告メッセージを表示 */
  warning(message: string): void {
    this.addMessage('warning', message);
  }

  /** 情報メッセージを表示 */
  info(message: string): void {
    this.addMessage('info', message);
  }

  /** 指定IDの通知を削除 */
  dismiss(id: number): void {
    this.messages = this.messages.filter(m => m.id !== id);
    // 技術的負債: タイムアウトのクリアが漏れている場合がある
    if (this.timeouts[id]) {
      clearTimeout(this.timeouts[id]);
      delete this.timeouts[id];
    }
  }

  /** 全通知をクリア */
  clearAll(): void {
    this.messages = [];
    // 技術的負債: 残存タイムアウトを全てクリアすべきだが、一部漏れる可能性がある
    Object.keys(this.timeouts).forEach(key => {
      clearTimeout(this.timeouts[Number(key)]);
    });
    this.timeouts = {};
  }

  /** メッセージを追加して自動非表示タイマーを設定 */
  private addMessage(type: NotificationMessage['type'], message: string): void {
    const id = this.nextId++;
    const notification: NotificationMessage = {
      id,
      type,
      message,
      timestamp: new Date()
    };
    this.messages.push(notification);

    // 技術的負債: 固定5秒で自動非表示。設定可能にすべき。
    // エラーメッセージは自動非表示しない方が良い場合もある
    this.timeouts[id] = setTimeout(() => {
      this.dismiss(id);
    }, 5000);
  }
}
