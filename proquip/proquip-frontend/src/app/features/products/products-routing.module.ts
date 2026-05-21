import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { ProductCreateComponent } from './product-create/product-create.component';
import { ProductEditComponent } from './product-edit/product-edit.component';
import { CategoryManagementComponent } from './category-management/category-management.component';
import { BundleManagementComponent } from './bundle-management/bundle-management.component';

/**
 * 製品カタログルーティング設定
 */
const routes: Routes = [
  {
    path: '',
    component: ProductListComponent,
    data: { title: '製品一覧' }
  },
  {
    path: 'new',
    component: ProductCreateComponent,
    data: { title: '製品登録' }
  },
  {
    path: 'categories',
    component: CategoryManagementComponent,
    data: { title: 'カテゴリ管理' }
  },
  {
    path: 'bundles',
    component: BundleManagementComponent,
    data: { title: 'バンドル管理' }
  },
  {
    path: ':id',
    component: ProductDetailComponent,
    data: { title: '製品詳細' }
  },
  {
    path: ':id/edit',
    component: ProductEditComponent,
    data: { title: '製品編集' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProductsRoutingModule { }
