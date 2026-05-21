/**
 * 予算モデル定義
 */

/** 予算 */
export interface Budget {
  id: number;
  departmentId: number;
  departmentName: string;
  fiscalYear: number;
  totalAmount: number;
  usedAmount: number;
  remainingAmount: number;
  status: string;
}

/** 予算明細 */
export interface BudgetLineItem {
  id: number;
  budgetId: number;
  categoryId: number;
  categoryName: string;
  description: string;
  allocatedAmount: number;
  spentAmount: number;
  remainingAmount: number;
}
