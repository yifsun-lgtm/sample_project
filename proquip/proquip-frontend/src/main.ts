import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';

// アプリケーションのブートストラップ
platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error('アプリケーションの起動に失敗しました:', err));
