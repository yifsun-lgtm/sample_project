// 開発環境設定
export const environment = {
  production: false,
  apiUrl: '/api',
  keycloak: {
    url: '/auth',
    realm: 'proquip',
    clientId: 'proquip-web'
  },
  // デバッグログの有効化
  enableDebugLog: true,
  // ページネーションのデフォルトサイズ
  defaultPageSize: 20,
  // 通知の自動非表示時間（ミリ秒）
  notificationTimeout: 5000
};
