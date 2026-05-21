package com.proquip.ejb.entity.inventory;

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

/**
 * 在庫移動明細エンティティ。
 * <p>
 * 在庫移動に紐づく個別品目の移動情報を保持する。
 * 移動予定数量と実際の移動数量を管理し、移動の進捗を追跡する。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "stock_transfer_item")
public class StockTransferItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 移動予定数量 */
    @NotNull
    @Min(1)
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantity;

    /** 実際の移動数量（移動完了後に設定） */
    @Min(0)
    @Column(name = "quantity_shipped")
    private Integer transferredQuantity;

    /** 親の在庫移動への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer stockTransfer;

    /** 対象製品への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public StockTransferItem() {
        super();
    }

    // --- Getter / Setter ---

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTransferredQuantity() {
        return transferredQuantity;
    }

    public void setTransferredQuantity(Integer transferredQuantity) {
        this.transferredQuantity = transferredQuantity;
    }

    public StockTransfer getStockTransfer() {
        return stockTransfer;
    }

    public void setStockTransfer(StockTransfer stockTransfer) {
        this.stockTransfer = stockTransfer;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "StockTransferItem{" +
                "id=" + getId() +
                ", quantity=" + quantity +
                ", transferredQuantity=" + transferredQuantity +
                '}';
    }
}
