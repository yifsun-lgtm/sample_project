package com.proquip.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 予算データ転送オブジェクト。
 *
 * <p>予算情報をプレゼンテーション層やAPI応答として転送するためのクラス。
 * 残額（remainingAmount）は計算値として提供される。</p>
 *
 * @author ProQuip開発チーム
 */
public class BudgetDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 予算ID */
    private Long id;

    /** 部門ID */
    private Long departmentId;

    /** 部門名 */
    private String departmentName;

    /** 会計年度 */
    private Integer fiscalYear;

    /** 予算総額 */
    private BigDecimal totalAmount;

    /** 消化済み額 */
    private BigDecimal usedAmount;

    /** 残額（計算値） */
    private BigDecimal remainingAmount;

    /** ステータス */
    private String status;

    /**
     * デフォルトコンストラクタ。
     */
    public BudgetDto() {
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
