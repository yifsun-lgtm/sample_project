import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SpendingAnalysisComponent } from './spending-analysis/spending-analysis.component';
import { InventoryValuationComponent } from './inventory-valuation/inventory-valuation.component';
import { SupplierPerformanceComponent } from './supplier-performance/supplier-performance.component';
import { BudgetActualComponent } from './budget-actual/budget-actual.component';

/**
 * レポートルーティング設定
 */
const routes: Routes = [
  { path: '', redirectTo: 'spending', pathMatch: 'full' },
  { path: 'spending', component: SpendingAnalysisComponent, data: { title: '支出分析' } },
  { path: 'inventory-valuation', component: InventoryValuationComponent, data: { title: '在庫評価' } },
  { path: 'supplier-performance', component: SupplierPerformanceComponent, data: { title: 'サプライヤー実績' } },
  { path: 'budget-actual', component: BudgetActualComponent, data: { title: '予算対実績' } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ReportsRoutingModule { }
