// 本番環境設定
export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    url: '/auth',
    realm: 'proquip',
    clientId: 'proquip-web'
  },
  // 本番環境ではデバッグログを無効化
  enableDebugLog: false,
  // ページネーションのデフォルトサイズ
  defaultPageSize: 20,
  // 通知の自動非表示時間（ミリ秒）
  notificationTimeout: 5000
};
