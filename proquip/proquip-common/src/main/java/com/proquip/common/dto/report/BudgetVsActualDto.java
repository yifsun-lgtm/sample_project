package com.proquip.common.dto.report;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 予算対実績レポートデータ転送オブジェクト。
 *
 * <p>各部門の予算と実績の比較情報を保持する。
 * 消化率や差異額を含み、予算管理画面で使用される。</p>
 *
 * @author ProQuip開発チーム
 */
public class BudgetVsActualDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会計年度 */
    private int fiscalYear;

    /** レポート生成日 */
    private String reportDate;

    /** 全体予算合計 */
    private BigDecimal totalBudget;

    /** 全体実績合計 */
    private BigDecimal totalActual;

    /** 全体差異 */
    private BigDecimal totalVariance;

    /** 全体消化率（パーセンテージ） */
    private BigDecimal overallUtilizationRate;

    /** 部門別内訳 */
    private List<DepartmentBudgetActual> departments = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public BudgetVsActualDto() {
    }

    // --- Getter / Setter ---

    /**
     * 会計年度を返す。
     *
     * @return 会計年度
     */
    public int getFiscalYear() {
        return fiscalYear;
    }

    /**
     * 会計年度を設定する。
     *
     * @param fiscalYear 会計年度
     */
    public void setFiscalYear(int fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    /**
     * レポート生成日を返す。
     *
     * @return レポート生成日
     */
    public String getReportDate() {
        return reportDate;
    }

    /**
     * レポート生成日を設定する。
     *
     * @param reportDate レポート生成日
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    /**
     * 全体予算合計を返す。
     *
     * @return 予算合計
     */
    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    /**
     * 全体予算合計を設定する。
     *
     * @param totalBudget 予算合計
     */
    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    /**
     * 全体実績合計を返す。
     *
     * @return 実績合計
     */
    public BigDecimal getTotalActual() {
        return totalActual;
    }

    /**
     * 全体実績合計を設定する。
     *
     * @param totalActual 実績合計
     */
    public void setTotalActual(BigDecimal totalActual) {
        this.totalActual = totalActual;
    }

    /**
     * 全体差異を返す。
     *
     * @return 差異額
     */
    public BigDecimal getTotalVariance() {
        return totalVariance;
    }

    /**
     * 全体差異を設定する。
     *
     * @param totalVariance 差異額
     */
    public void setTotalVariance(BigDecimal totalVariance) {
        this.totalVariance = totalVariance;
    }

    /**
     * 全体消化率を返す。
     *
     * @return 消化率（パーセンテージ）
     */
    public BigDecimal getOverallUtilizationRate() {
        return overallUtilizationRate;
    }

    /**
     * 全体消化率を設定する。
     *
     * @param overallUtilizationRate 消化率（パーセンテージ）
     */
    public void setOverallUtilizationRate(BigDecimal overallUtilizationRate) {
        this.overallUtilizationRate = overallUtilizationRate;
    }

    /**
     * 部門別内訳を返す。
     *
     * @return 部門別予算実績のリスト
     */
    public List<DepartmentBudgetActual> getDepartments() {
        return departments;
    }

    /**
     * 部門別内訳を設定する。
     *
     * @param departments 部門別予算実績のリスト
     */
    public void setDepartments(List<DepartmentBudgetActual> departments) {
        this.departments = departments;
    }

    /** 部門別予算対実績内部クラス */
    public static class DepartmentBudgetActual implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long departmentId;
        private String departmentName;
        private BigDecimal budgetAmount;
        private BigDecimal actualAmount;
        private BigDecimal variance;
        private BigDecimal utilizationRate;

        public DepartmentBudgetActual() {}
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public BigDecimal getBudgetAmount() { return budgetAmount; }
        public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
        public BigDecimal getActualAmount() { return actualAmount; }
        public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }
        public BigDecimal getVariance() { return variance; }
        public void setVariance(BigDecimal variance) { this.variance = variance; }
        public BigDecimal getUtilizationRate() { return utilizationRate; }
        public void setUtilizationRate(BigDecimal utilizationRate) { this.utilizationRate = utilizationRate; }
    }
}
