import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SupplierListComponent } from './supplier-list/supplier-list.component';
import { SupplierDetailComponent } from './supplier-detail/supplier-detail.component';
import { SupplierCreateComponent } from './supplier-create/supplier-create.component';
import { SupplierEditComponent } from './supplier-edit/supplier-edit.component';
import { SupplierCompareComponent } from './supplier-compare/supplier-compare.component';

/**
 * サプライヤー管理ルーティング設定
 */
const routes: Routes = [
  {
    path: '',
    component: SupplierListComponent,
    data: { title: 'サプライヤー一覧' }
  },
  {
    path: 'new',
    component: SupplierCreateComponent,
    data: { title: 'サプライヤー登録' }
  },
  {
    path: 'compare',
    component: SupplierCompareComponent,
    data: { title: 'サプライヤー比較' }
  },
  {
    path: ':id',
    component: SupplierDetailComponent,
    data: { title: 'サプライヤー詳細' }
  },
  {
    path: ':id/edit',
    component: SupplierEditComponent,
    data: { title: 'サプライヤー編集' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SuppliersRoutingModule { }
