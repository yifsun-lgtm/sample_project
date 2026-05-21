package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 入庫明細エンティティ。
 * <p>
 * 入庫に紐づく個別品目の受入情報を保持する。
 * 受入数量、合格数量、不合格数量、不合格理由を記録する。
 * </p>
 * <p>
 * 【技術的負債】purchaseOrderItemId が PurchaseOrderItem への
 * @ManyToOneリレーションではなく生のIDで保持されている。
 * 発注明細情報の取得に別途クエリが必要となる。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "goods_receipt_item")
public class GoodsReceiptItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 受入数量 */
    @NotNull
    @Min(0)
    @Column(name = "quantity_received", nullable = false)
    private Integer receivedQuantity;

    /** 検品合格数量 */
    @Min(0)
    @Column(name = "quantity_accepted")
    private Integer acceptedQuantity;

    /** 検品不合格数量 */
    @Min(0)
    @Column(name = "quantity_rejected")
    private Integer rejectedQuantity;

    /** 不合格理由 */
    @Size(max = 500)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** 親の入庫への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    /**
     * 対応する発注明細のID。
     * <p>
     * 技術的負債: PurchaseOrderItemへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。発注明細の詳細情報を取得するには
     * 別途クエリを発行する必要がある。
     * </p>
     */
    @Column(name = "order_item_id")
    private Long purchaseOrderItemId;

    /**
     * デフォルトコンストラクタ。
     */
    public GoodsReceiptItem() {
        super();
    }

    // --- Getter / Setter ---

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public Integer getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(Integer acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public Integer getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(Integer rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public GoodsReceipt getGoodsReceipt() {
        return goodsReceipt;
    }

    public void setGoodsReceipt(GoodsReceipt goodsReceipt) {
        this.goodsReceipt = goodsReceipt;
    }

    public Long getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public void setPurchaseOrderItemId(Long purchaseOrderItemId) {
        this.purchaseOrderItemId = purchaseOrderItemId;
    }

    @Override
    public String toString() {
        return "GoodsReceiptItem{" +
                "id=" + getId() +
                ", receivedQuantity=" + receivedQuantity +
                ", acceptedQuantity=" + acceptedQuantity +
                ", rejectedQuantity=" + rejectedQuantity +
                '}';
    }
}
