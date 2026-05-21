import { Component, OnInit } from '@angular/core';
import { MasterDataService } from '@shared/services/master-data.service';

@Component({
  selector: 'app-master-data',
  templateUrl: './master-data.component.html',
  styleUrls: ['./master-data.component.scss']
})
export class MasterDataComponent implements OnInit {

  activeTab: 'manufacturers' | 'units' | 'currencies' | 'taxRates' = 'manufacturers';

  manufacturers: any[] = [];
  units: any[] = [];
  currencies: any[] = [];
  taxRates: any[] = [];

  isLoading = false;
  errorMessage = '';
  successMessage = '';

  showEditModal = false;
  isEditing = false;
  editItem: any = {};
  isSaving = false;

  constructor(private masterDataService: MasterDataService) {}

  ngOnInit(): void {
    this.loadData();
  }

  switchTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
    this.clearMessages();
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.clearMessages();

    switch (this.activeTab) {
      case 'manufacturers':
        this.masterDataService.getManufacturers().subscribe({
          next: (data) => { this.manufacturers = data; this.isLoading = false; },
          error: () => { this.errorMessage = 'メーカー一覧の取得に失敗しました。'; this.isLoading = false; }
        });
        break;
      case 'units':
        this.masterDataService.getUnits().subscribe({
          next: (data) => { this.units = data; this.isLoading = false; },
          error: () => { this.errorMessage = '単位一覧の取得に失敗しました。'; this.isLoading = false; }
        });
        break;
      case 'currencies':
        this.masterDataService.getCurrencies().subscribe({
          next: (data) => { this.currencies = data; this.isLoading = false; },
          error: () => { this.errorMessage = '通貨一覧の取得に失敗しました。'; this.isLoading = false; }
        });
        break;
      case 'taxRates':
        this.masterDataService.getTaxRates().subscribe({
          next: (data) => { this.taxRates = data; this.isLoading = false; },
          error: () => { this.errorMessage = '税率一覧の取得に失敗しました。'; this.isLoading = false; }
        });
        break;
    }
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editItem = {};
    this.showEditModal = true;
  }

  openEditModal(item: any): void {
    this.isEditing = true;
    this.editItem = { ...item };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editItem = {};
  }

  save(): void {
    this.isSaving = true;
    this.clearMessages();

    let obs;
    switch (this.activeTab) {
      case 'manufacturers':
        obs = this.isEditing
          ? this.masterDataService.updateManufacturer(this.editItem.id!, this.editItem)
          : this.masterDataService.createManufacturer(this.editItem);
        break;
      case 'units':
        obs = this.isEditing
          ? this.masterDataService.updateUnit(this.editItem.id!, this.editItem)
          : this.masterDataService.createUnit(this.editItem);
        break;
      case 'currencies':
        obs = this.isEditing
          ? this.masterDataService.updateCurrency(this.editItem.id!, this.editItem)
          : this.masterDataService.createCurrency(this.editItem);
        break;
      case 'taxRates':
        obs = this.isEditing
          ? this.masterDataService.updateTaxRate(this.editItem.id!, this.editItem)
          : this.masterDataService.createTaxRate(this.editItem);
        break;
    }

    obs.subscribe({
      next: () => {
        this.successMessage = this.isEditing ? '更新しました。' : '作成しました。';
        this.isSaving = false;
        this.closeEditModal();
        this.loadData();
      },
      error: (err: any) => {
        console.error('保存エラー:', err);
        this.errorMessage = '保存に失敗しました。';
        this.isSaving = false;
      }
    });
  }

  deleteItem(item: any): void {
    if (!confirm(`「${item['name'] || item['code']}」を削除しますか？`)) return;
    this.clearMessages();

    let obs;
    switch (this.activeTab) {
      case 'manufacturers':
        obs = this.masterDataService.deleteManufacturer(item.id!);
        break;
      case 'units':
        obs = this.masterDataService.deleteUnit(item.id!);
        break;
      default:
        return;
    }

    obs.subscribe({
      next: () => {
        this.successMessage = '削除しました。';
        this.loadData();
      },
      error: () => { this.errorMessage = '削除に失敗しました。関連データが存在する可能性があります。'; }
    });
  }

  getTabLabel(): string {
    switch (this.activeTab) {
      case 'manufacturers': return 'メーカー';
      case 'units': return '単位';
      case 'currencies': return '通貨';
      case 'taxRates': return '税率';
    }
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
