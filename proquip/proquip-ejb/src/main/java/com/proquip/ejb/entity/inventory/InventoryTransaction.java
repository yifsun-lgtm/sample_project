package com.proquip.ejb.entity.inventory;

import com.proquip.ejb.entity.base.BaseEntity;

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

import java.util.Date;

/**
 * 在庫トランザクションエンティティ。
 * <p>
 * 在庫品目に対する入出庫・調整・移動などの変動履歴を記録する。
 * 各トランザクションはトランザクション種別、数量、参照情報を持ち、
 * 在庫品目の変動を追跡可能にする。
 * </p>
 * <p>
 * 【技術的負債】performedBy が UserProfile へのリレーションではなく
 * 生のIDで保持されている。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "inventory_transaction")
@NamedQueries({
    @NamedQuery(
        name = "InventoryTransaction.findByItem",
        query = "SELECT it FROM InventoryTransaction it WHERE it.productId = :productId ORDER BY it.transactionDate DESC"
    ),
    @NamedQuery(
        name = "InventoryTransaction.findByType",
        query = "SELECT it FROM InventoryTransaction it WHERE it.transactionType = :type ORDER BY it.transactionDate DESC"
    ),
    @NamedQuery(
        name = "InventoryTransaction.findByDateRange",
        query = "SELECT it FROM InventoryTransaction it WHERE it.transactionDate BETWEEN :startDate AND :endDate ORDER BY it.transactionDate DESC"
    )
})
public class InventoryTransaction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * トランザクション種別。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: IN, OUT, ADJUST, TRANSFER
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    /** 変動数量（入庫はプラス、出庫はマイナス） */
    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 参照元エンティティ種別 */
    @Size(max = 50)
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** 参照元エンティティID */
    @Column(name = "reference_id")
    private Long referenceId;

    /** トランザクション発生日時 */
    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "transaction_date", nullable = false)
    private Date transactionDate;

    /** 備考 */
    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    /** 対象商品のID */
    @NotNull
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 対象倉庫のID */
    @NotNull
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    /**
     * 実行者のユーザーID。
     * <p>
     * 技術的負債: UserProfileへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。
     * </p>
     */
    @Column(name = "performed_by")
    private Long performedBy;

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryTransaction() {
        super();
    }

    // --- Getter / Setter ---

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
    }

    @Override
    public String toString() {
        return "InventoryTransaction{" +
                "id=" + getId() +
                ", transactionType='" + transactionType + '\'' +
                ", quantity=" + quantity +
                ", transactionDate=" + transactionDate +
                '}';
    }
}
