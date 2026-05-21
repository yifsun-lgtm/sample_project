package com.proquip.common.dto.report;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 支出レポートデータ転送オブジェクト。
 *
 * <p>指定期間の支出分析結果を保持する。カテゴリ別、部門別の
 * 内訳を含む集計データを提供する。</p>
 *
 * @author ProQuip開発チーム
 */
public class SpendingReportDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** レポート対象期間（開始） */
    private String periodStart;

    /** レポート対象期間（終了） */
    private String periodEnd;

    /** 合計支出額 */
    private BigDecimal totalSpending;

    /** 前期比（パーセンテージ） */
    private BigDecimal previousPeriodChangeRate;

    /** 発注書件数 */
    private int orderCount;

    /** 仕入先数 */
    private int supplierCount;

    /** カテゴリ別内訳 */
    private List<CategorySpending> categoryBreakdown = new ArrayList<>();

    /** 部門別内訳 */
    private List<DepartmentSpending> departmentBreakdown = new ArrayList<>();

    /** 月別推移 */
    private List<MonthlySpending> monthlyTrend = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public SpendingReportDto() {
    }

    // --- Getter / Setter ---

    /**
     * レポート対象期間（開始）を返す。
     *
     * @return 期間開始日（yyyy-MM-dd形式）
     */
    public String getPeriodStart() {
        return periodStart;
    }

    /**
     * レポート対象期間（開始）を設定する。
     *
     * @param periodStart 期間開始日
     */
    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    /**
     * レポート対象期間（終了）を返す。
     *
     * @return 期間終了日（yyyy-MM-dd形式）
     */
    public String getPeriodEnd() {
        return periodEnd;
    }

    /**
     * レポート対象期間（終了）を設定する。
     *
     * @param periodEnd 期間終了日
     */
    public void setPeriodEnd(String periodEnd) {
        this.periodEnd = periodEnd;
    }

    /**
     * 合計支出額を返す。
     *
     * @return 合計支出額
     */
    public BigDecimal getTotalSpending() {
        return totalSpending;
    }

    /**
     * 合計支出額を設定する。
     *
     * @param totalSpending 合計支出額
     */
    public void setTotalSpending(BigDecimal totalSpending) {
        this.totalSpending = totalSpending;
    }

    /**
     * 前期比を返す。
     *
     * @return 前期比（パーセンテージ）
     */
    public BigDecimal getPreviousPeriodChangeRate() {
        return previousPeriodChangeRate;
    }

    /**
     * 前期比を設定する。
     *
     * @param previousPeriodChangeRate 前期比（パーセンテージ）
     */
    public void setPreviousPeriodChangeRate(BigDecimal previousPeriodChangeRate) {
        this.previousPeriodChangeRate = previousPeriodChangeRate;
    }

    /**
     * 発注書件数を返す。
     *
     * @return 発注書件数
     */
    public int getOrderCount() {
        return orderCount;
    }

    /**
     * 発注書件数を設定する。
     *
     * @param orderCount 発注書件数
     */
    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    /**
     * 仕入先数を返す。
     *
     * @return 仕入先数
     */
    public int getSupplierCount() {
        return supplierCount;
    }

    /**
     * 仕入先数を設定する。
     *
     * @param supplierCount 仕入先数
     */
    public void setSupplierCount(int supplierCount) {
        this.supplierCount = supplierCount;
    }

    /**
     * カテゴリ別内訳を返す。
     *
     * @return カテゴリ別支出のリスト
     */
    public List<CategorySpending> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    /**
     * カテゴリ別内訳を設定する。
     *
     * @param categoryBreakdown カテゴリ別支出のリスト
     */
    public void setCategoryBreakdown(List<CategorySpending> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    /**
     * 部門別内訳を返す。
     *
     * @return 部門別支出のリスト
     */
    public List<DepartmentSpending> getDepartmentBreakdown() {
        return departmentBreakdown;
    }

    /**
     * 部門別内訳を設定する。
     *
     * @param departmentBreakdown 部門別支出のリスト
     */
    public void setDepartmentBreakdown(List<DepartmentSpending> departmentBreakdown) {
        this.departmentBreakdown = departmentBreakdown;
    }

    /**
     * 月別推移を返す。
     *
     * @return 月別支出のリスト
     */
    public List<MonthlySpending> getMonthlyTrend() {
        return monthlyTrend;
    }

    /**
     * 月別推移を設定する。
     *
     * @param monthlyTrend 月別支出のリスト
     */
    public void setMonthlyTrend(List<MonthlySpending> monthlyTrend) {
        this.monthlyTrend = monthlyTrend;
    }

    /** カテゴリ別支出内部クラス */
    public static class CategorySpending implements Serializable {
        private static final long serialVersionUID = 1L;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;

        public CategorySpending() {}
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    }

    /** 部門別支出内部クラス */
    public static class DepartmentSpending implements Serializable {
        private static final long serialVersionUID = 1L;
        private String departmentName;
        private BigDecimal amount;
        private BigDecimal percentage;

        public DepartmentSpending() {}
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    }

    /** 月別支出内部クラス */
    public static class MonthlySpending implements Serializable {
        private static final long serialVersionUID = 1L;
        private String month;
        private BigDecimal amount;

        public MonthlySpending() {}
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
