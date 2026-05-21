import { Component, OnInit } from '@angular/core';
import { DepartmentService } from '@shared/services/department.service';

@Component({
  selector: 'app-department-management',
  templateUrl: './department-management.component.html',
  styleUrls: ['./department-management.component.scss']
})
export class DepartmentManagementComponent implements OnInit {

  departments: any[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  showEditModal = false;
  isEditing = false;
  editItem: any = {};
  isSaving = false;

  constructor(private departmentService: DepartmentService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.clearMessages();
    this.departmentService.getDepartments().subscribe({
      next: (data) => { this.departments = data; this.isLoading = false; },
      error: () => { this.errorMessage = '部門一覧の取得に失敗しました。'; this.isLoading = false; }
    });
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editItem = { active: true, sortOrder: 0 };
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

    const payload: any = {
      code: this.editItem.code,
      name: this.editItem.name,
      costCenter: this.editItem.costCenter || null,
      sortOrder: Number(this.editItem.sortOrder) || 0,
      active: this.editItem.active,
      parentId: this.editItem.parentId ? Number(this.editItem.parentId) : null
    };

    const obs = this.isEditing
      ? this.departmentService.updateDepartment(this.editItem.id, payload)
      : this.departmentService.createDepartment(payload);

    obs.subscribe({
      next: () => {
        this.successMessage = this.isEditing ? '更新しました。' : '作成しました。';
        this.isSaving = false;
        this.closeEditModal();
        this.loadData();
      },
      error: () => {
        this.errorMessage = '保存に失敗しました。';
        this.isSaving = false;
      }
    });
  }

  deleteItem(item: any): void {
    if (!confirm(`「${item.name}」を削除しますか？関連データがある場合は削除できません。`)) return;
    this.clearMessages();

    this.departmentService.deleteDepartment(item.id).subscribe({
      next: () => {
        this.successMessage = '削除しました。';
        this.loadData();
      },
      error: () => { this.errorMessage = '削除に失敗しました。関連データが存在する可能性があります。'; }
    });
  }

  getLevelIndent(level: number): string {
    return '\u00A0\u00A0'.repeat(level);
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
