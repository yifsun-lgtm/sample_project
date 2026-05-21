import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ImportExportComponent } from './import-export/import-export.component';

/**
 * インポート/エクスポートルーティング設定
 */
const routes: Routes = [
  { path: '', component: ImportExportComponent, data: { title: 'インポート/エクスポート' } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ImportExportRoutingModule { }
