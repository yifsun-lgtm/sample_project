package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * 商品仕様を表すエンティティ。
 *
 * <p>商品の技術仕様やスペック情報をキー・バリュー形式で管理する。
 * 各仕様には任意で単位（specUnit）を設定できる。</p>
 *
 * <p>表示順序（displayOrder）により、画面上の並び順を制御する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_specification")
@NamedQueries({
    @NamedQuery(
        name = "ProductSpecification.findByProduct",
        query = "SELECT ps FROM ProductSpecification ps WHERE ps.product.id = :productId ORDER BY ps.displayOrder"
    ),
    @NamedQuery(
        name = "ProductSpecification.findBySpecName",
        query = "SELECT ps FROM ProductSpecification ps WHERE ps.specName = :specName"
    )
})
public class ProductSpecification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 仕様名（例: "電圧", "消費電力"） */
    @Column(name = "spec_name", nullable = false, length = 200)
    private String specName;

    /** 仕様値（例: "100V", "500W"） */
    @Column(name = "spec_value", nullable = false, length = 500)
    private String specValue;

    /** 仕様の単位（例: "V", "W", "kg"） */
    @Column(name = "spec_unit", length = 50)
    private String specUnit;

    /** 表示順序 */
    @Column(name = "sort_order")
    private Integer displayOrder;

    /** 紐付く商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public ProductSpecification() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 仕様名を返す。
     *
     * @return 仕様名
     */
    public String getSpecName() {
        return specName;
    }

    /**
     * 仕様名を設定する。
     *
     * @param specName 仕様名
     */
    public void setSpecName(String specName) {
        this.specName = specName;
    }

    /**
     * 仕様値を返す。
     *
     * @return 仕様値
     */
    public String getSpecValue() {
        return specValue;
    }

    /**
     * 仕様値を設定する。
     *
     * @param specValue 仕様値
     */
    public void setSpecValue(String specValue) {
        this.specValue = specValue;
    }

    /**
     * 仕様の単位を返す。
     *
     * @return 仕様の単位
     */
    public String getSpecUnit() {
        return specUnit;
    }

    /**
     * 仕様の単位を設定する。
     *
     * @param specUnit 仕様の単位
     */
    public void setSpecUnit(String specUnit) {
        this.specUnit = specUnit;
    }

    /**
     * 表示順序を返す。
     *
     * @return 表示順序
     */
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    /**
     * 表示順序を設定する。
     *
     * @param displayOrder 表示順序
     */
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * 紐付く商品を返す。
     *
     * @return 商品
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 紐付く商品を設定する。
     *
     * @param product 商品
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "ProductSpecification{" +
                "id=" + getId() +
                ", specName='" + specName + '\'' +
                ", specValue='" + specValue + '\'' +
                ", specUnit='" + specUnit + '\'' +
                ", displayOrder=" + displayOrder +
                '}';
    }
}
