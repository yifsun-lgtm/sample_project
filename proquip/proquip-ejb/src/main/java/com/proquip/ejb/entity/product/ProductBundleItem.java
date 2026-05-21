package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


/**
 * バンドル構成商品を表すエンティティ。
 *
 * <p>商品バンドル（{@link ProductBundle}）に含まれる個々の商品と
 * その数量・上書き価格を管理する。</p>
 *
 * <p>{@code overridePrice} が設定されている場合、バンドル内では
 * 商品本来の単価ではなくこの価格が適用される。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_bundle_item")
public class ProductBundleItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** バンドル内の数量 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 所属バンドル */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", nullable = false)
    private ProductBundle bundle;

    /** 構成商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public ProductBundleItem() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * バンドル内の数量を返す。
     *
     * @return 数量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * バンドル内の数量を設定する。
     *
     * @param quantity 数量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * 所属バンドルを返す。
     *
     * @return バンドル
     */
    public ProductBundle getBundle() {
        return bundle;
    }

    /**
     * 所属バンドルを設定する。
     *
     * @param bundle バンドル
     */
    public void setBundle(ProductBundle bundle) {
        this.bundle = bundle;
    }

    /**
     * 構成商品を返す。
     *
     * @return 商品
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 構成商品を設定する。
     *
     * @param product 商品
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "ProductBundleItem{" +
                "id=" + getId() +
                ", quantity=" + quantity +
                '}';
    }
}
