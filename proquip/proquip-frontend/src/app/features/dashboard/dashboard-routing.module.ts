import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardMainComponent } from './dashboard-main/dashboard-main.component';
import { MyTasksComponent } from './my-tasks/my-tasks.component';

/**
 * ダッシュボードルーティング設定
 */
const routes: Routes = [
  {
    path: '',
    component: DashboardMainComponent,
    data: { title: 'ダッシュボード' }
  },
  {
    path: 'tasks',
    component: MyTasksComponent,
    data: { title: 'マイタスク' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DashboardRoutingModule { }
