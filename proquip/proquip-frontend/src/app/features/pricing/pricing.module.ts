import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { PricingRoutingModule } from './pricing-routing.module';
import { PriceListManagementComponent } from './price-list-management/price-list-management.component';
import { PriceEditComponent } from './price-edit/price-edit.component';
import { PriceCompareComponent } from './price-compare/price-compare.component';

/**
 * 価格管理モジュール
 * 価格リスト管理、価格編集、価格比較機能を提供する
 */
@NgModule({
  declarations: [
    PriceListManagementComponent,
    PriceEditComponent,
    PriceCompareComponent
  ],
  imports: [
    SharedModule,
    PricingRoutingModule
  ]
})
export class PricingModule { }
