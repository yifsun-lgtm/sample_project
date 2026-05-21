import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { DashboardRoutingModule } from './dashboard-routing.module';

// コンポーネント
import { DashboardMainComponent } from './dashboard-main/dashboard-main.component';
import { MyTasksComponent } from './my-tasks/my-tasks.component';

/**
 * ダッシュボードモジュール
 * メインダッシュボードおよびタスク管理画面を提供
 */
@NgModule({
  declarations: [
    DashboardMainComponent,
    MyTasksComponent
  ],
  imports: [
    SharedModule,
    DashboardRoutingModule
  ]
})
export class DashboardModule { }
