import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { SharedModule } from '@shared/shared.module';
import { AdminRoutingModule } from './admin-routing.module';
import { UserManagementComponent } from './user-management/user-management.component';
import { RolePermissionComponent } from './role-permission/role-permission.component';
import { SystemConfigComponent } from './system-config/system-config.component';
import { AuditLogComponent } from './audit-log/audit-log.component';
import { BudgetManagementComponent } from './budget-management/budget-management.component';
import { MasterDataComponent } from './master-data/master-data.component';
import { DelegationComponent } from './delegation/delegation.component';
import { DepartmentManagementComponent } from './department-management/department-management.component';
import { NotificationSettingsComponent } from './notification-settings/notification-settings.component';

/**
 * 管理者モジュール
 * ユーザー管理、ロール・権限、システム設定、監査ログを提供する
 *
 * 技術的負債: HttpClientModuleをAppModuleでもインポート済みだが、ここでも重複インポート
 */
@NgModule({
  declarations: [
    UserManagementComponent,
    RolePermissionComponent,
    SystemConfigComponent,
    AuditLogComponent,
    BudgetManagementComponent,
    MasterDataComponent,
    DelegationComponent,
    DepartmentManagementComponent,
    NotificationSettingsComponent
  ],
  imports: [
    SharedModule,
    // 技術的負債: AppModuleと重複インポート
    HttpClientModule,
    AdminRoutingModule
  ]
})
export class AdminModule { }
