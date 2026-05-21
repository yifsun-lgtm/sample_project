import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { ReportsRoutingModule } from './reports-routing.module';
import { SpendingAnalysisComponent } from './spending-analysis/spending-analysis.component';
import { InventoryValuationComponent } from './inventory-valuation/inventory-valuation.component';
import { SupplierPerformanceComponent } from './supplier-performance/supplier-performance.component';
import { BudgetActualComponent } from './budget-actual/budget-actual.component';

/**
 * レポートモジュール
 * 支出分析、在庫評価、サプライヤー実績、予算対実績レポートを提供する
 */
@NgModule({
  declarations: [
    SpendingAnalysisComponent,
    InventoryValuationComponent,
    SupplierPerformanceComponent,
    BudgetActualComponent
  ],
  imports: [
    SharedModule,
    ReportsRoutingModule
  ]
})
export class ReportsModule { }
