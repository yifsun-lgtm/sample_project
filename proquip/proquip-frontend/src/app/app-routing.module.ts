import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '@core/auth/auth.guard';

/**
 * アプリケーションのルーティング設定
 * 全てのルートは遅延読み込み（Lazy Loading）で構成
 * 認証ガードにより未認証ユーザーはKeycloakログインへリダイレクト
 */
const routes: Routes = [
  {
    path: 'dashboard',
    loadChildren: () =>
      import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
    canActivate: [AuthGuard],
    data: { title: 'ダッシュボード' }
  },
  {
    path: 'products',
    loadChildren: () =>
      import('./features/products/products.module').then(m => m.ProductsModule),
    canActivate: [AuthGuard],
    data: { title: '製品カタログ' }
  },
  {
    path: 'suppliers',
    loadChildren: () =>
      import('./features/suppliers/suppliers.module').then(m => m.SuppliersModule),
    canActivate: [AuthGuard],
    data: { title: 'サプライヤー' }
  },
  {
    path: 'procurement',
    loadChildren: () =>
      import('./features/procurement/procurement.module').then(m => m.ProcurementModule),
    canActivate: [AuthGuard],
    data: { title: '調達管理' }
  },
  {
    path: 'inventory',
    loadChildren: () =>
      import('./features/inventory/inventory.module').then(m => m.InventoryModule),
    canActivate: [AuthGuard],
    data: { title: '在庫管理' }
  },
  {
    path: 'warehouses',
    loadChildren: () =>
      import('./features/warehouses/warehouses.module').then(m => m.WarehousesModule),
    canActivate: [AuthGuard],
    data: { title: '倉庫管理' }
  },
  {
    path: 'pricing',
    loadChildren: () =>
      import('./features/pricing/pricing.module').then(m => m.PricingModule),
    canActivate: [AuthGuard],
    data: { title: '価格管理' }
  },
  {
    path: 'reports',
    loadChildren: () =>
      import('./features/reports/reports.module').then(m => m.ReportsModule),
    canActivate: [AuthGuard],
    data: { title: 'レポート' }
  },
  {
    path: 'admin',
    loadChildren: () =>
      import('./features/admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard],
    data: { title: '管理者設定', roles: ['ADMIN'] }
  },
  {
    path: 'import-export',
    loadChildren: () =>
      import('./features/import-export/import-export.module').then(m => m.ImportExportModule),
    canActivate: [AuthGuard],
    data: { title: 'インポート/エクスポート' }
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
