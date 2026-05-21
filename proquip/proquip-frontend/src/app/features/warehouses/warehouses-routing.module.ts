import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { WarehouseListComponent } from './warehouse-list/warehouse-list.component';
import { WarehouseDetailComponent } from './warehouse-detail/warehouse-detail.component';
import { WarehouseLayoutComponent } from './warehouse-layout/warehouse-layout.component';

/**
 * 倉庫管理ルーティング設定
 */
const routes: Routes = [
  {
    path: '',
    component: WarehouseListComponent,
    data: { title: '倉庫一覧' }
  },
  {
    path: ':id/layout',
    component: WarehouseLayoutComponent,
    data: { title: '倉庫レイアウト' }
  },
  {
    path: ':id',
    component: WarehouseDetailComponent,
    data: { title: '倉庫詳細' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class WarehousesRoutingModule { }
