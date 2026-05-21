import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { WarehousesRoutingModule } from './warehouses-routing.module';

// コンポーネント
import { WarehouseListComponent } from './warehouse-list/warehouse-list.component';
import { WarehouseDetailComponent } from './warehouse-detail/warehouse-detail.component';
import { WarehouseLayoutComponent } from './warehouse-layout/warehouse-layout.component';

/**
 * 倉庫管理モジュール
 * 倉庫一覧、詳細、レイアウト表示の機能を提供する
 */
@NgModule({
  declarations: [
    WarehouseListComponent,
    WarehouseDetailComponent,
    WarehouseLayoutComponent
  ],
  imports: [
    SharedModule,
    WarehousesRoutingModule
  ]
})
export class WarehousesModule { }
