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
 * 発注明細エンティティ。
 * <p>
 * 発注に紐づく個別の品目情報を保持する。
 * 各明細には行番号、数量、単価、税率、割引、小計が含まれる。
 * </p>
 * <p>
 * 【技術的負債】subtotal が非正規化されたストアドフィールドとして保持されている。
 * 本来は quantity * unitPrice * (1 - discount) * (1 + taxRate) で計算すべきだが、
 * パフォーマンス上の理由で永続化されている。値の不整合リスクがある。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "purchase_order_item")
public class PurchaseOrderItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 発注数量 */
    @NotNull
    @Min(1)
    @Column(name = "quantity_ordered", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    /** 単価 */
    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    /** 税率（例: 0.10 = 10%） */
    @Column(name = "tax_rate", precision = 5, scale = 4)
    private BigDecimal taxRate;

    /** 割引率（例: 0.05 = 5%） */
    @Column(name = "discount", precision = 5, scale = 4)
    private BigDecimal discount;

    /**
     * 小計金額。
     * <p>
     * 技術的負債: 非正規化された値。quantity × unitPrice に割引・税を適用した
     * 結果が保存されている。アプリケーション側で計算値と永続化値の整合性を
     * 手動で維持する必要がある。
     * </p>
     */
    @Column(name = "subtotal", precision = 18, scale = 2)
    private BigDecimal subtotal;

    /** 受入済み数量 */
    @Column(name = "received_quantity", precision = 15, scale = 3)
    private BigDecimal receivedQuantity;

    /**
     * 明細ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: PENDING, PARTIALLY_RECEIVED, RECEIVED, CANCELLED
     * </p>
     */
    @Size(max = 30)
    @Column(name = "status", length = 30)
    private String status;

    /** 親の発注への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /** 対象製品への参照 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseOrderItem() {
        super();
    }

    // --- Getter / Setter ---

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "PurchaseOrderItem{" +
                "quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                ", status='" + status + '\'' +
                '}';
    }
}
