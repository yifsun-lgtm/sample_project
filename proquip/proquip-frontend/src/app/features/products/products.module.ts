import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { ProductsRoutingModule } from './products-routing.module';

// コンポーネント
import { ProductListComponent } from './product-list/product-list.component';
import { ProductDetailComponent } from './product-detail/product-detail.component';
import { ProductCreateComponent } from './product-create/product-create.component';
import { ProductEditComponent } from './product-edit/product-edit.component';
import { CategoryManagementComponent } from './category-management/category-management.component';
import { BundleManagementComponent } from './bundle-management/bundle-management.component';

/**
 * 製品カタログモジュール
 * 製品の一覧・詳細・作成・編集・カテゴリ管理・バンドル管理を提供
 */
@NgModule({
  declarations: [
    ProductListComponent,
    ProductDetailComponent,
    ProductCreateComponent,
    ProductEditComponent,
    CategoryManagementComponent,
    BundleManagementComponent
  ],
  imports: [
    SharedModule,
    ProductsRoutingModule
  ]
})
export class ProductsModule { }
