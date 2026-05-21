import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { InventoryOverviewComponent } from './inventory-overview/inventory-overview.component';
import { InventoryDetailComponent } from './inventory-detail/inventory-detail.component';
import { StockTransferCreateComponent } from './stock-transfer-create/stock-transfer-create.component';
import { StockTransferListComponent } from './stock-transfer-list/stock-transfer-list.component';
import { InventoryCountComponent } from './inventory-count/inventory-count.component';
import { TransactionHistoryComponent } from './transaction-history/transaction-history.component';
import { InventoryAdjustmentComponent } from './inventory-adjustment/inventory-adjustment.component';

/**
 * 在庫管理ルーティング設定
 */
const routes: Routes = [
  {
    path: '',
    component: InventoryOverviewComponent,
    data: { title: '在庫一覧' }
  },
  {
    path: 'transfer/new',
    component: StockTransferCreateComponent,
    data: { title: '在庫移動作成' }
  },
  {
    path: 'transfers/new',
    component: StockTransferCreateComponent,
    data: { title: '在庫移動作成' }
  },
  {
    path: 'transfers',
    component: StockTransferListComponent,
    data: { title: '在庫移動一覧' }
  },
  {
    path: 'adjustments',
    component: InventoryAdjustmentComponent,
    data: { title: '在庫調整' }
  },
  {
    path: 'stocktaking',
    component: InventoryCountComponent,
    data: { title: '棚卸し' }
  },
  {
    path: 'count',
    component: InventoryCountComponent,
    data: { title: '棚卸し' }
  },
  {
    path: 'transactions',
    component: TransactionHistoryComponent,
    data: { title: '取引履歴' }
  },
  {
    path: ':id',
    component: InventoryDetailComponent,
    data: { title: '在庫詳細' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class InventoryRoutingModule { }
