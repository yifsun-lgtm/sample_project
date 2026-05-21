/**
 * 通知モデル定義
 */

/** 通知 */
export interface Notification {
  id: number;
  type: 'ORDER_CREATED' | 'ORDER_APPROVED' | 'ORDER_REJECTED' | 'LOW_STOCK' | 'DELIVERY_OVERDUE' | 'SYSTEM' | 'INFO';
  title: string;
  message: string;
  entityType: string;
  entityId: string;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
  actionUrl: string | null;
}

/** 通知件数サマリー */
export interface NotificationCount {
  total: number;
  unread: number;
  byType: {
    type: string;
    count: number;
  }[];
}
