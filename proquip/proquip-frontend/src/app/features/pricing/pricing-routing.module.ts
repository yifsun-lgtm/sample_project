import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PriceListManagementComponent } from './price-list-management/price-list-management.component';
import { PriceEditComponent } from './price-edit/price-edit.component';
import { PriceCompareComponent } from './price-compare/price-compare.component';

/**
 * 価格管理ルーティング設定
 */
const routes: Routes = [
  { path: '', component: PriceListManagementComponent, data: { title: '価格リスト管理' } },
  { path: ':id/edit', component: PriceEditComponent, data: { title: '価格編集' } },
  { path: 'compare', component: PriceCompareComponent, data: { title: '価格比較' } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PricingRoutingModule { }
