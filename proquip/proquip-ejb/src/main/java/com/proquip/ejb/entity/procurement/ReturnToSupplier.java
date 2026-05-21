package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.AuditableEntity;
import com.proquip.ejb.entity.supplier.Supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Date;

/**
 * サプライヤー返品エンティティ。
 * <p>
 * 品質不良や数量過剰などの理由でサプライヤーへ返品する際の情報を管理する。
 * 返品番号で一意に識別され、返品理由、ステータス、クレジット金額を保持する。
 * </p>
 * <p>
 * 【技術的負債】purchaseOrderId が PurchaseOrder への
 * @ManyToOneリレーションではなく生のIDで保持されている。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "return_to_supplier")
@NamedQueries({
    @NamedQuery(
        name = "ReturnToSupplier.findByReturnNumber",
        query = "SELECT r FROM ReturnToSupplier r WHERE r.returnNumber = :returnNumber"
    ),
    @NamedQuery(
        name = "ReturnToSupplier.findBySupplier",
        query = "SELECT r FROM ReturnToSupplier r WHERE r.supplier.id = :supplierId ORDER BY r.returnDate DESC"
    ),
    @NamedQuery(
        name = "ReturnToSupplier.findByStatus",
        query = "SELECT r FROM ReturnToSupplier r WHERE r.status = :status ORDER BY r.returnDate DESC"
    )
})
public class ReturnToSupplier extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 返品番号（一意制約） */
    @NotNull
    @Size(max = 30)
    @Column(name = "return_number", unique = true, nullable = false, length = 30)
    private String returnNumber;

    /** 返品日 */
    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "return_date", nullable = false)
    private Date returnDate;

    /** 返品理由 */
    @Size(max = 1000)
    @Column(name = "return_reason", length = 1000)
    private String reason;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: DRAFT, SUBMITTED, APPROVED, SHIPPED, RECEIVED_BY_SUPPLIER, CREDITED, CANCELLED
     * </p>
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** クレジット金額（返金予定額） */
    @Column(name = "credit_note_amount", precision = 18, scale = 2)
    private BigDecimal creditAmount;

    /** 返品先サプライヤーへの参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "initiated_by")
    private Long initiatedBy;

    /**
     * 元の発注ID。
     * <p>
     * 技術的負債: PurchaseOrderへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。発注情報の取得に別途クエリが必要。
     * </p>
     */
    @Column(name = "order_id")
    private Long purchaseOrderId;

    /**
     * デフォルトコンストラクタ。
     */
    public ReturnToSupplier() {
        super();
    }

    // --- Getter / Setter ---

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Long getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(Long initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    @Override
    public String toString() {
        return "ReturnToSupplier{" +
                "returnNumber='" + returnNumber + '\'' +
                ", returnDate=" + returnDate +
                ", status='" + status + '\'' +
                ", creditAmount=" + creditAmount +
                '}';
    }
}
