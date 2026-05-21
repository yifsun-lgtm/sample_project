import { Component, OnInit } from '@angular/core';
import { DelegationService } from '@shared/services/delegation.service';

@Component({
  selector: 'app-delegation',
  templateUrl: './delegation.component.html',
  styleUrls: ['./delegation.component.scss']
})
export class DelegationComponent implements OnInit {

  delegations: any[] = [];
  users: any[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  showEditModal = false;
  isEditing = false;
  editItem: any = {};
  isSaving = false;

  scopeOptions = [
    { value: 'APPROVAL', label: '承認' },
    { value: 'PURCHASE', label: '購買' },
    { value: 'FULL', label: '全権限' }
  ];

  constructor(private delegationService: DelegationService) {}

  ngOnInit(): void {
    this.loadData();
    this.loadUsers();
  }

  loadData(): void {
    this.isLoading = true;
    this.clearMessages();
    this.delegationService.getDelegations().subscribe({
      next: (data) => { this.delegations = data; this.isLoading = false; },
      error: () => { this.errorMessage = '委譲ルール一覧の取得に失敗しました。'; this.isLoading = false; }
    });
  }

  loadUsers(): void {
    this.delegationService.getUsers().subscribe({
      next: (data) => { this.users = data; },
      error: () => { this.errorMessage = 'ユーザー一覧の取得に失敗しました。'; }
    });
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editItem = { scope: 'APPROVAL' };
    this.showEditModal = true;
  }

  openEditModal(item: any): void {
    this.isEditing = true;
    this.editItem = {
      id: item.id,
      delegateFromId: item.delegateFromId,
      delegateToId: item.delegateToId,
      scope: item.scope,
      validFrom: this.formatDateForInput(item.validFrom),
      validTo: this.formatDateForInput(item.validTo)
    };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editItem = {};
  }

  save(): void {
    this.isSaving = true;
    this.clearMessages();

    const payload = {
      delegateFromId: Number(this.editItem.delegateFromId),
      delegateToId: Number(this.editItem.delegateToId),
      scope: this.editItem.scope,
      validFrom: this.editItem.validFrom,
      validTo: this.editItem.validTo
    };

    const obs = this.isEditing
      ? this.delegationService.updateDelegation(this.editItem.id, payload)
      : this.delegationService.createDelegation(payload);

    obs.subscribe({
      next: () => {
        this.successMessage = this.isEditing ? '更新しました。' : '作成しました。';
        this.isSaving = false;
        this.closeEditModal();
        this.loadData();
      },
      error: (err: any) => {
        this.errorMessage = err.error?.error || '保存に失敗しました。';
        this.isSaving = false;
      }
    });
  }

  deleteItem(item: any): void {
    const name = `${item.delegateFromName} → ${item.delegateToName}`;
    if (!confirm(`「${name}」の委譲ルールを削除しますか？`)) return;
    this.clearMessages();

    this.delegationService.deleteDelegation(item.id).subscribe({
      next: () => {
        this.successMessage = '削除しました。';
        this.loadData();
      },
      error: () => { this.errorMessage = '削除に失敗しました。'; }
    });
  }

  getScopeLabel(scope: string): string {
    const found = this.scopeOptions.find(o => o.value === scope);
    return found ? found.label : scope;
  }

  isActive(item: any): boolean {
    const now = new Date();
    const from = new Date(item.validFrom);
    const to = new Date(item.validTo);
    return from <= now && to >= now;
  }

  private formatDateForInput(dateStr: string): string {
    if (!dateStr) return '';
    return dateStr.substring(0, 10);
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
