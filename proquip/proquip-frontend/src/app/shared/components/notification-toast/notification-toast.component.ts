import { Component } from '@angular/core';
import { NotificationService, NotificationMessage } from '@core/services/notification.service';

/**
 * トースト通知表示コンポーネント
 * NotificationServiceのメッセージをトースト形式で表示
 */
@Component({
  selector: 'app-notification-toast',
  templateUrl: './notification-toast.component.html',
  styleUrls: ['./notification-toast.component.scss']
})
export class NotificationToastComponent {

  constructor(public notificationService: NotificationService) {}

  /** 通知メッセージ一覧を取得 */
  get messages(): NotificationMessage[] {
    return this.notificationService.messages;
  }

  /** 個別の通知を閉じる */
  onDismiss(id: number): void {
    this.notificationService.dismiss(id);
  }

  /** トースト種別に応じたアイコンを返す */
  getIcon(type: string): string {
    switch (type) {
      case 'success': return '&#10004;';
      case 'error': return '&#10006;';
      case 'warning': return '&#9888;';
      case 'info': return '&#8505;';
      default: return '';
    }
  }
}
