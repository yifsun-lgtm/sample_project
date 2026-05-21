import { NgModule } from '@angular/core';
import { SharedModule } from '@shared/shared.module';
import { ImportExportRoutingModule } from './import-export-routing.module';
import { ImportExportComponent } from './import-export/import-export.component';

/**
 * インポート/エクスポートモジュール
 * データのCSVインポートおよびエクスポート機能を提供する
 */
@NgModule({
  declarations: [
    ImportExportComponent
  ],
  imports: [
    SharedModule,
    ImportExportRoutingModule
  ]
})
export class ImportExportModule { }
