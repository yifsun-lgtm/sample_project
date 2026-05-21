package com.proquip.ejb.entity.supplier;

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
import java.math.BigDecimal;

/**
 * 仕入先取扱商品を表すエンティティ。
 *
 * <p>仕入先が取り扱う商品の情報を管理する。同一商品を複数の仕入先が扱う場合、
 * 仕入先ごとに異なる単価（unitCost）やリードタイム（leadTimeDays）を保持できる。</p>
 *
 * <p>{@code isPreferred} フラグにより、優先仕入先を識別する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier_product")
@NamedQueries({
    @NamedQuery(
        name = "SupplierProduct.findBySupplier",
        query = "SELECT sp FROM SupplierProduct sp WHERE sp.supplier.id = :supplierId ORDER BY sp.product.name"
    ),
    @NamedQuery(
        name = "SupplierProduct.findByProduct",
        query = "SELECT sp FROM SupplierProduct sp WHERE sp.product.id = :productId ORDER BY sp.unitCost ASC"
    ),
    @NamedQuery(
        name = "SupplierProduct.findPreferredByProduct",
        query = "SELECT sp FROM SupplierProduct sp WHERE sp.product.id = :productId AND sp.isPreferred = true"
    )
})
public class SupplierProduct extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 仕入先側のSKU */
    @Column(name = "supplier_sku", length = 50)
    private String supplierSku;

    /** 仕入単価 */
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost;

    /** リードタイム（日数） */
    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    /** 最小発注数量 */
    @Column(name = "min_order_qty")
    private Integer minOrderQty;

    /** 優先仕入先フラグ */
    @Column(name = "is_preferred", nullable = false)
    private boolean isPreferred;

    /** 仕入先 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** 商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierProduct() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 仕入先側のSKUを返す。
     *
     * @return 仕入先SKU
     */
    public String getSupplierSku() {
        return supplierSku;
    }

    /**
     * 仕入先側のSKUを設定する。
     *
     * @param supplierSku 仕入先SKU
     */
    public void setSupplierSku(String supplierSku) {
        this.supplierSku = supplierSku;
    }

    /**
     * 仕入単価を返す。
     *
     * @return 仕入単価
     */
    public BigDecimal getUnitCost() {
        return unitCost;
    }

    /**
     * 仕入単価を設定する。
     *
     * @param unitCost 仕入単価
     */
    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    /**
     * リードタイム（日数）を返す。
     *
     * @return リードタイム
     */
    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    /**
     * リードタイム（日数）を設定する。
     *
     * @param leadTimeDays リードタイム
     */
    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    /**
     * 最小発注数量を返す。
     *
     * @return 最小発注数量
     */
    public Integer getMinOrderQty() {
        return minOrderQty;
    }

    /**
     * 最小発注数量を設定する。
     *
     * @param minOrderQty 最小発注数量
     */
    public void setMinOrderQty(Integer minOrderQty) {
        this.minOrderQty = minOrderQty;
    }

    /**
     * 優先仕入先かどうかを返す。
     *
     * @return 優先仕入先の場合 {@code true}
     */
    public boolean isPreferred() {
        return isPreferred;
    }

    /**
     * 優先仕入先フラグを設定する。
     *
     * @param preferred 優先仕入先フラグ
     */
    public void setPreferred(boolean preferred) {
        this.isPreferred = preferred;
    }

    /**
     * 仕入先を返す。
     *
     * @return 仕入先
     */
    public Supplier getSupplier() {
        return supplier;
    }

    /**
     * 仕入先を設定する。
     *
     * @param supplier 仕入先
     */
    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    /**
     * 商品を返す。
     *
     * @return 商品
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 商品を設定する。
     *
     * @param product 商品
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "SupplierProduct{" +
                "id=" + getId() +
                ", supplierSku='" + supplierSku + '\'' +
                ", unitCost=" + unitCost +
                ", leadTimeDays=" + leadTimeDays +
                ", isPreferred=" + isPreferred +
                '}';
    }
}
