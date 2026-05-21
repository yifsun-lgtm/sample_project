package com.proquip.ejb.entity.pricing;

import com.proquip.ejb.entity.base.BaseEntity;
import com.proquip.ejb.entity.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 価格表明細エンティティ。
 * <p>
 * 価格表に紐づく個別製品の価格情報を保持する。
 * 数量帯別の単価設定（最小数量〜最大数量）が可能。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "price_list_item")
public class PriceListItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 単価 */
    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    /** 最小適用数量（この数量以上で適用） */
    @Column(name = "min_quantity")
    private Integer minQuantity;

    /** 最大適用数量（この数量以下で適用、nullの場合は上限なし） */
    @Column(name = "max_quantity")
    private Integer maxQuantity;

    /** 親の価格表への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    /** 対象製品への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public PriceListItem() {
        super();
    }

    // --- Getter / Setter ---

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public PriceList getPriceList() {
        return priceList;
    }

    public void setPriceList(PriceList priceList) {
        this.priceList = priceList;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "PriceListItem{" +
                "id=" + getId() +
                ", unitPrice=" + unitPrice +
                ", minQuantity=" + minQuantity +
                ", maxQuantity=" + maxQuantity +
                '}';
    }
}
