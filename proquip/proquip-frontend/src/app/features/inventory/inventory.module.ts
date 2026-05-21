import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { InventoryRoutingModule } from './inventory-routing.module';

// コンポーネント
import { InventoryOverviewComponent } from './inventory-overview/inventory-overview.component';
import { InventoryDetailComponent } from './inventory-detail/inventory-detail.component';
import { StockTransferCreateComponent } from './stock-transfer-create/stock-transfer-create.component';
import { StockTransferListComponent } from './stock-transfer-list/stock-transfer-list.component';
import { InventoryCountComponent } from './inventory-count/inventory-count.component';
import { TransactionHistoryComponent } from './transaction-history/transaction-history.component';
import { InventoryAdjustmentComponent } from './inventory-adjustment/inventory-adjustment.component';

/**
 * 在庫管理モジュール
 * 在庫一覧、詳細、移動、棚卸し、取引履歴の機能を提供する
 */
@NgModule({
  declarations: [
    InventoryOverviewComponent,
    InventoryDetailComponent,
    StockTransferCreateComponent,
    StockTransferListComponent,
    InventoryCountComponent,
    InventoryAdjustmentComponent,
    TransactionHistoryComponent
  ],
  imports: [
    SharedModule,
    InventoryRoutingModule
  ]
})
export class InventoryModule { }
