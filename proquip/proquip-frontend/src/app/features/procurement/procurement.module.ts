import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { ProcurementRoutingModule } from './procurement-routing.module';

// コンポーネント
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
 * 調達管理モジュール
 * 購買依頼、発注書、承認、入荷、返品の機能を提供する
 */
@NgModule({
  declarations: [
    RequisitionListComponent,
    RequisitionCreateComponent,
    RequisitionDetailComponent,
    OrderListComponent,
    OrderCreateComponent,
    OrderDetailComponent,
    ApprovalQueueComponent,
    GoodsReceiptComponent,
    ReturnManagementComponent
  ],
  imports: [
    SharedModule,
    ProcurementRoutingModule
  ]
})
export class ProcurementModule { }
