import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// コンポーネント
import { DataTableComponent } from './components/data-table/data-table.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { SearchBoxComponent } from './components/search-box/search-box.component';
import { StatusBadgeComponent } from './components/status-badge/status-badge.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { PageHeaderComponent } from './components/page-header/page-header.component';
import { FormFieldComponent } from './components/form-field/form-field.component';
import { FileUploadComponent } from './components/file-upload/file-upload.component';
import { NotificationToastComponent } from './components/notification-toast/notification-toast.component';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';

// パイプ
import { JapaneseDatePipe } from './pipes/japanese-date.pipe';
import { CurrencyJpPipe } from './pipes/currency-jp.pipe';
import { TruncatePipe } from './pipes/truncate.pipe';
import { StatusLabelPipe } from './pipes/status-label.pipe';
import { OrderNumberPipe } from './pipes/order-number.pipe';

// ディレクティブ
import { HasRoleDirective } from './directives/has-role.directive';
import { AutoFocusDirective } from './directives/auto-focus.directive';
import { ClickOutsideDirective } from './directives/click-outside.directive';

/**
 * 共有モジュール
 * アプリケーション全体で再利用するコンポーネント、パイプ、ディレクティブを提供
 */
@NgModule({
  declarations: [
    // コンポーネント
    DataTableComponent,
    ConfirmDialogComponent,
    SearchBoxComponent,
    StatusBadgeComponent,
    LoadingSpinnerComponent,
    PageHeaderComponent,
    FormFieldComponent,
    FileUploadComponent,
    NotificationToastComponent,
    EmptyStateComponent,
    // パイプ
    JapaneseDatePipe,
    CurrencyJpPipe,
    TruncatePipe,
    StatusLabelPipe,
    OrderNumberPipe,
    // ディレクティブ
    HasRoleDirective,
    AutoFocusDirective,
    ClickOutsideDirective
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule
  ],
  exports: [
    // Angularモジュール再エクスポート
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    // コンポーネント
    DataTableComponent,
    ConfirmDialogComponent,
    SearchBoxComponent,
    StatusBadgeComponent,
    LoadingSpinnerComponent,
    PageHeaderComponent,
    FormFieldComponent,
    FileUploadComponent,
    NotificationToastComponent,
    EmptyStateComponent,
    // パイプ
    JapaneseDatePipe,
    CurrencyJpPipe,
    TruncatePipe,
    StatusLabelPipe,
    OrderNumberPipe,
    // ディレクティブ
    HasRoleDirective,
    AutoFocusDirective,
    ClickOutsideDirective
  ]
})
export class SharedModule { }
