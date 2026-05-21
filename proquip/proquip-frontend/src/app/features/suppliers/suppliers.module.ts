import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { SuppliersRoutingModule } from './suppliers-routing.module';

// コンポーネント
import { SupplierListComponent } from './supplier-list/supplier-list.component';
import { SupplierDetailComponent } from './supplier-detail/supplier-detail.component';
import { SupplierCreateComponent } from './supplier-create/supplier-create.component';
import { SupplierEditComponent } from './supplier-edit/supplier-edit.component';
import { SupplierCompareComponent } from './supplier-compare/supplier-compare.component';

/**
 * サプライヤー管理モジュール
 * サプライヤーの一覧・詳細・作成・編集・比較を提供
 */
@NgModule({
  declarations: [
    SupplierListComponent,
    SupplierDetailComponent,
    SupplierCreateComponent,
    SupplierEditComponent,
    SupplierCompareComponent
  ],
  imports: [
    SharedModule,
    SuppliersRoutingModule
  ]
})
export class SuppliersModule { }
