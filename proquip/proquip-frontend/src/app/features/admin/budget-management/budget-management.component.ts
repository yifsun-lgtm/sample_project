import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BudgetService } from '@shared/services/budget.service';
import { Budget } from '@shared/models/budget.model';

@Component({
  selector: 'app-budget-management',
  templateUrl: './budget-management.component.html',
  styleUrls: ['./budget-management.component.scss']
})
export class BudgetManagementComponent implements OnInit {

  budgets: Budget[] = [];
  filteredBudgets: Budget[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  filterFiscalYear: number;
  fiscalYearOptions: number[] = [];

  showEditModal = false;
  isEditing = false;
  editForm!: FormGroup;
  isSaving = false;

  selectedBudget: Budget | null = null;
  utilization: any = null;

  departmentOptions = [
    { id: 1, name: '経営企画部' },
    { id: 2, name: '総務部' },
    { id: 3, name: '営業部' },
    { id: 4, name: '技術部' },
    { id: 5, name: '製造部' },
    { id: 6, name: '品質管理部' },
    { id: 7, name: '経理部' },
    { id: 8, name: '人事部' }
  ];

  constructor(
    private fb: FormBuilder,
    private budgetService: BudgetService
  ) {
    const now = new Date();
    const currentFy = now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1;
    this.filterFiscalYear = currentFy;
    for (let y = currentFy + 1; y >= currentFy - 3; y--) {
      this.fiscalYearOptions.push(y);
    }
  }

  ngOnInit(): void {
    this.editForm = this.fb.group({
      departmentId: [null, Validators.required],
      fiscalYear: [this.filterFiscalYear, Validators.required],
      totalAmount: [0, [Validators.required, Validators.min(0)]]
    });
    this.loadBudgets();
  }

  loadBudgets(): void {
    this.isLoading = true;
    this.clearMessages();
    this.budgetService.getBudgets(this.filterFiscalYear).subscribe({
      next: (budgets) => {
        this.budgets = budgets;
        this.filteredBudgets = budgets;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('予算一覧取得エラー:', err);
        this.errorMessage = '予算一覧の取得に失敗しました。';
        this.isLoading = false;
      }
    });
  }

  onFiscalYearChange(): void {
    this.loadBudgets();
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editForm.reset({
      departmentId: null,
      fiscalYear: this.filterFiscalYear,
      totalAmount: 0
    });
    this.showEditModal = true;
  }

  openEditModal(budget: Budget): void {
    this.isEditing = true;
    this.editForm.patchValue({
      departmentId: budget.departmentId,
      fiscalYear: budget.fiscalYear,
      totalAmount: budget.totalAmount
    });
    this.selectedBudget = budget;
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedBudget = null;
  }

  saveBudget(): void {
    if (this.editForm.invalid) return;
    this.isSaving = true;
    this.clearMessages();

    const data = this.editForm.value;

    if (this.isEditing && this.selectedBudget) {
      this.budgetService.updateBudget(this.selectedBudget.id, data).subscribe({
        next: () => {
          this.successMessage = '予算を更新しました。';
          this.isSaving = false;
          this.closeEditModal();
          this.loadBudgets();
        },
        error: (err) => {
          console.error('予算更新エラー:', err);
          this.errorMessage = '予算の更新に失敗しました。';
          this.isSaving = false;
        }
      });
    } else {
      this.budgetService.createBudget(data).subscribe({
        next: () => {
          this.successMessage = '予算を作成しました。';
          this.isSaving = false;
          this.closeEditModal();
          this.loadBudgets();
        },
        error: (err) => {
          console.error('予算作成エラー:', err);
          this.errorMessage = '予算の作成に失敗しました。';
          this.isSaving = false;
        }
      });
    }
  }

  showUtilization(budget: Budget): void {
    this.selectedBudget = budget;
    this.budgetService.getBudgetUtilization(budget.id).subscribe({
      next: (data) => { this.utilization = data; },
      error: () => { this.utilization = null; }
    });
  }

  closeUtilization(): void {
    this.utilization = null;
    this.selectedBudget = null;
  }

  getUtilizationRate(budget: Budget): number {
    if (!budget.totalAmount || budget.totalAmount === 0) return 0;
    return Math.round(((budget.usedAmount || 0) / budget.totalAmount) * 100);
  }

  getUtilizationClass(rate: number): string {
    if (rate >= 90) return 'utilization-danger';
    if (rate >= 70) return 'utilization-warning';
    return 'utilization-ok';
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'DRAFT': '下書き',
      'APPROVED': '承認済み',
      'ACTIVE': '有効',
      'FROZEN': '凍結',
      'CLOSED': 'クローズ'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    const classes: { [key: string]: string } = {
      'DRAFT': 'status-draft',
      'APPROVED': 'status-approved',
      'ACTIVE': 'status-active',
      'FROZEN': 'status-frozen',
      'CLOSED': 'status-closed'
    };
    return classes[status] || 'status-default';
  }

  formatCurrency(amount: number): string {
    if (amount == null) return '¥0';
    return '¥' + Math.round(amount).toLocaleString('ja-JP');
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
