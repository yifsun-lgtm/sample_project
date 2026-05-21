package com.proquip.ejb.entity.pricing;

import com.proquip.ejb.entity.base.AuditableEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 予算エンティティ。
 * <p>
 * 部門別・年度別の予算情報を管理する。
 * 予算総額、配賦済み額、消化済み額を保持し、予算の執行状況を追跡する。
 * </p>
 * <p>
 * 【技術的負債】departmentId が Department エンティティへの
 * @ManyToOneリレーションではなく生のIDで保持されている。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "budget")
@NamedQueries({
    @NamedQuery(
        name = "Budget.findByDepartmentAndYear",
        query = "SELECT b FROM Budget b WHERE b.departmentId = :departmentId AND b.fiscalYear = :fiscalYear"
    ),
    @NamedQuery(
        name = "Budget.findByFiscalYear",
        query = "SELECT b FROM Budget b WHERE b.fiscalYear = :fiscalYear ORDER BY b.name"
    ),
    @NamedQuery(
        name = "Budget.findByStatus",
        query = "SELECT b FROM Budget b WHERE b.status = :status ORDER BY b.fiscalYear DESC, b.name"
    )
})
public class Budget extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 予算名 */
    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 会計年度 */
    @NotNull
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    /** 予算総額 */
    @NotNull
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /** 配賦済み額 */
    @Column(name = "allocated_amount", precision = 18, scale = 2)
    private BigDecimal allocatedAmount;

    /** 消化済み額 */
    @Column(name = "spent_amount", precision = 18, scale = 2)
    private BigDecimal spentAmount;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: DRAFT, APPROVED, ACTIVE, FROZEN, CLOSED
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 部門のID。
     * <p>
     * 技術的負債: Departmentエンティティへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。部門情報の取得に別途クエリが必要。
     * </p>
     */
    @Column(name = "department_id")
    private Long departmentId;

    /** 予算明細のリスト */
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BudgetLineItem> lineItems = new ArrayList<BudgetLineItem>();

    /**
     * デフォルトコンストラクタ。
     */
    public Budget() {
        super();
    }

    // --- Getter / Setter ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public List<BudgetLineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<BudgetLineItem> lineItems) {
        this.lineItems = lineItems;
    }

    @Override
    public String toString() {
        return "Budget{" +
                "name='" + name + '\'' +
                ", fiscalYear=" + fiscalYear +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                '}';
    }
}
