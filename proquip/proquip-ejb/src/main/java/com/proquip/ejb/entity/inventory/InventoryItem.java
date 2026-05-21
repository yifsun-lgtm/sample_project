package com.proquip.ejb.entity.inventory;

import com.proquip.ejb.entity.base.BaseEntity;
import com.proquip.ejb.entity.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;



/**
 * 在庫品目エンティティ。
 * <p>
 * 特定の製品が特定の倉庫にどれだけ在庫されているかを管理する。
 * 手持在庫数、引当済み数量、発注中数量を保持し、在庫の可視性を提供する。
 * 製品と倉庫の組み合わせで一意に識別される。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "inventory_item", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "warehouse_id"})
})
@NamedQueries({
    @NamedQuery(
        name = "InventoryItem.findByProductAndWarehouse",
        query = "SELECT ii FROM InventoryItem ii WHERE ii.product.id = :productId AND ii.warehouse.id = :warehouseId"
    ),
    @NamedQuery(
        name = "InventoryItem.findByWarehouse",
        query = "SELECT ii FROM InventoryItem ii WHERE ii.warehouse.id = :warehouseId ORDER BY ii.product.id"
    ),
})
public class InventoryItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 手持在庫数量 */
    @NotNull
    @Min(0)
    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand = 0;

    /** 引当済み数量（出荷予定などで予約された数量） */
    @NotNull
    @Min(0)
    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    /** 発注中数量（入庫待ちの数量） */
    @NotNull
    @Min(0)
    @Column(name = "quantity_on_order", nullable = false)
    private Integer quantityOnOrder = 0;

    /** 対象製品への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 保管倉庫への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryItem() {
        super();
    }

    // --- Getter / Setter ---

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public Integer getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public Integer getQuantityOnOrder() {
        return quantityOnOrder;
    }

    public void setQuantityOnOrder(Integer quantityOnOrder) {
        this.quantityOnOrder = quantityOnOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "id=" + getId() +
                ", quantityOnHand=" + quantityOnHand +
                ", quantityReserved=" + quantityReserved +
                ", quantityOnOrder=" + quantityOnOrder +
                '}';
    }
}
