package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.BaseEntity;
import com.proquip.ejb.entity.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 購買依頼明細エンティティ。
 * <p>
 * 購買依頼に紐づく個別の品目情報を保持する。
 * 各明細には依頼数量、見積単価、および対象製品への参照が含まれる。
 * </p>
 * <p>
 * 【技術的負債】Product へのフェッチタイプが EAGER になっており、
 * 一覧取得時にN+1問題を引き起こす可能性がある。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "purchase_requisition_item")
public class PurchaseRequisitionItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 依頼数量 */
    @NotNull
    @Min(1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 見積単価 */
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal estimatedUnitCost;

    /** 備考 */
    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    /** 親の購買依頼への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    private PurchaseRequisition requisition;

    /**
     * 対象製品への参照。
     * <p>
     * 技術的負債: EAGER フェッチが指定されており、LAZY に変更すべきである。
     * 購買依頼明細を大量に取得する際にパフォーマンスが劣化する。
     * </p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseRequisitionItem() {
        super();
    }

    // --- Getter / Setter ---

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getEstimatedUnitCost() {
        return estimatedUnitCost;
    }

    public void setEstimatedUnitCost(BigDecimal estimatedUnitCost) {
        this.estimatedUnitCost = estimatedUnitCost;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PurchaseRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(PurchaseRequisition requisition) {
        this.requisition = requisition;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "PurchaseRequisitionItem{" +
                "id=" + getId() +
                ", quantity=" + quantity +
                ", estimatedUnitCost=" + estimatedUnitCost +
                '}';
    }
}
