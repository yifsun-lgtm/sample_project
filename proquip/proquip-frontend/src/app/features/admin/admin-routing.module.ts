import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
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
 * 管理者ルーティング設定
 */
const routes: Routes = [
  { path: '', redirectTo: 'users', pathMatch: 'full' },
  { path: 'users', component: UserManagementComponent, data: { title: 'ユーザー管理' } },
  { path: 'roles', component: RolePermissionComponent, data: { title: 'ロール・権限管理' } },
  { path: 'config', component: SystemConfigComponent, data: { title: 'システム設定' } },
  { path: 'settings', component: SystemConfigComponent, data: { title: 'システム設定' } },
  { path: 'audit-log', component: AuditLogComponent, data: { title: '監査ログ' } },
  { path: 'budgets', component: BudgetManagementComponent, data: { title: '予算管理' } },
  { path: 'master-data', component: MasterDataComponent, data: { title: 'マスタデータ管理' } },
  { path: 'delegation', component: DelegationComponent, data: { title: '承認委譲管理' } },
  { path: 'departments', component: DepartmentManagementComponent, data: { title: '部門管理' } },
  { path: 'notification-settings', component: NotificationSettingsComponent, data: { title: '通知設定' } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
