package com.proquip.ejb.entity.pricing;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 予算明細エンティティ。
 * <p>
 * 予算に紐づく個別の費目・カテゴリ別の配賦情報を保持する。
 * 各明細には配賦済み額と消化済み額が含まれ、費目別の執行状況を追跡できる。
 * </p>
 * <p>
 * 【技術的負債】categoryId がカテゴリエンティティへの
 * @ManyToOneリレーションではなく生のIDで保持されている。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "budget_line_item")
public class BudgetLineItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 費目の説明 */
    @NotNull
    @Size(max = 200)
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    /** 配賦済み額 */
    @NotNull
    @Column(name = "allocated_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal allocatedAmount;

    /** 消化済み額 */
    @Column(name = "spent_amount", precision = 18, scale = 2)
    private BigDecimal spentAmount;

    /** 親の予算への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    /**
     * カテゴリのID。
     * <p>
     * 技術的負債: カテゴリエンティティへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。カテゴリ情報の取得に別途クエリが必要。
     * </p>
     */
    @Column(name = "category_id")
    private Long categoryId;

    /**
     * デフォルトコンストラクタ。
     */
    public BudgetLineItem() {
        super();
    }

    // --- Getter / Setter ---

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public Budget getBudget() {
        return budget;
    }

    public void setBudget(Budget budget) {
        this.budget = budget;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        return "BudgetLineItem{" +
                "id=" + getId() +
                ", description='" + description + '\'' +
                ", allocatedAmount=" + allocatedAmount +
                ", spentAmount=" + spentAmount +
                '}';
    }
}
