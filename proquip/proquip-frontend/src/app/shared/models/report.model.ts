/**
 * レポートモデル定義
 */

/** 支出レポート */
export interface SpendingReport {
  reportId: string;
  period: {
    startDate: string;
    endDate: string;
    label: string;
  };
  summary: {
    totalSpending: number;
    orderCount: number;
    averageOrderValue: number;
    topCategoryName: string;
    topCategoryAmount: number;
  };
  byCategory: SpendingBreakdown[];
  bySupplier: SpendingBreakdown[];
  monthlyTrend: MonthlySpending[];
  generatedAt: string;
}

/** 支出内訳行 */
export interface SpendingBreakdown {
  name: string;
  orderCount: number;
  totalAmount: number;
  percentage: number;
}

/** 月別支出 */
export interface MonthlySpending {
  month: string;
  amount: number;
  orderCount: number;
}

/** 在庫評価レポート */
export interface InventoryValuation {
  reportId: string;
  totalValue: number;
  totalItems: number;
  byWarehouse: {
    warehouseId: number;
    warehouseName: string;
    itemCount: number;
    totalValue: number;
  }[];
  byCategory: {
    categoryId: number;
    categoryName: string;
    itemCount: number;
    totalValue: number;
    turnoverRate: number;
  }[];
  generatedAt: string;
}

/** サプライヤー実績レポート */
export interface SupplierPerformance {
  reportId: string;
  period: {
    startDate: string;
    endDate: string;
  };
  suppliers: {
    supplierId: number;
    supplierName: string;
    orderCount: number;
    totalAmount: number;
    deliveryOnTimeRate: number;
    qualityPassRate: number;
    averageLeadTime: number;
    overallScore: number;
  }[];
  generatedAt: string;
}

/** 予算実績対比レポート */
export interface BudgetActual {
  reportId: string;
  fiscalYear: number;
  totalBudget: number;
  totalActual: number;
  totalVariance: number;
  utilizationRate: number;
  byDepartment: {
    department: string;
    budget: number;
    actual: number;
    variance: number;
    utilizationRate: number;
  }[];
  byCategory: {
    categoryId: number;
    categoryName: string;
    budget: number;
    actual: number;
    variance: number;
  }[];
  monthlyTrend: {
    month: string;
    budget: number;
    actual: number;
    cumulative: number;
  }[];
  generatedAt: string;
}
