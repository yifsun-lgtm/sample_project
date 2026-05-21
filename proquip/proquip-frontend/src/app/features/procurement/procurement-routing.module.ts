import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RequisitionListComponent } from './requisition-list/requisition-list.component';
import { RequisitionCreateComponent } from './requisition-create/requisition-create.component';
import { RequisitionDetailComponent } from './requisition-detail/requisition-detail.component';
import { OrderListComponent } from './order-list/order-list.component';
import { OrderCreateComponent } from './order-create/order-create.component';
import { OrderDetailComponent } from './order-detail/order-detail.component';
import { ApprovalQueueComponent } from './approval-queue/approval-queue.component';
import { GoodsReceiptComponent } from './goods-receipt/goods-receipt.component';
import { ReturnManagementComponent } from './return-management/return-management.component';

/**
 * 調達管理ルーティング設定
 */
const routes: Routes = [
  {
    path: 'requisitions',
    component: RequisitionListComponent,
    data: { title: '購買依頼一覧' }
  },
  {
    path: 'requisitions/new',
    component: RequisitionCreateComponent,
    data: { title: '購買依頼作成' }
  },
  {
    path: 'requisitions/:id',
    component: RequisitionDetailComponent,
    data: { title: '購買依頼詳細' }
  },
  {
    path: 'orders',
    component: OrderListComponent,
    data: { title: '発注書一覧' }
  },
  {
    path: 'orders/new',
    component: OrderCreateComponent,
    data: { title: '発注書作成' }
  },
  {
    path: 'orders/:id',
    component: OrderDetailComponent,
    data: { title: '発注書詳細' }
  },
  {
    path: 'approvals',
    component: ApprovalQueueComponent,
    data: { title: '承認待ちキュー' }
  },
  {
    path: 'goods-receipt',
    component: GoodsReceiptComponent,
    data: { title: '入荷検収' }
  },
  {
    path: 'returns',
    component: ReturnManagementComponent,
    data: { title: '返品管理' }
  },
  {
    path: '',
    redirectTo: 'requisitions',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProcurementRoutingModule { }
