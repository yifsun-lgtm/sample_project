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
 * 代替商品を表すエンティティ。
 *
 * <p>ある商品に対する代替品の関係を管理する。
 * 代替の種類（alternativeType）により、互換品・後継品・類似品などを区別する。</p>
 *
 * <p>元商品（sourceProduct）と代替商品（alternativeProduct）の
 * 2つの外部キーにより、方向性を持った代替関係を表現する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_alternative")
@NamedQueries({
    @NamedQuery(
        name = "ProductAlternative.findBySourceProduct",
        query = "SELECT pa FROM ProductAlternative pa WHERE pa.sourceProduct.id = :productId"
    ),
    @NamedQuery(
        name = "ProductAlternative.findByType",
        query = "SELECT pa FROM ProductAlternative pa WHERE pa.alternativeType = :type"
    )
})
public class ProductAlternative extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 代替の種類。
     * 想定値: "COMPATIBLE"（互換品）, "SUCCESSOR"（後継品）, "SIMILAR"（類似品）
     */
    @Column(name = "compatibility", nullable = false, length = 30)
    private String alternativeType;

    /** 備考 */
    @Column(name = "notes", length = 1000)
    private String notes;

    /** 元商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product sourceProduct;

    /** 代替商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternative_product_id", nullable = false)
    private Product alternativeProduct;

    /**
     * デフォルトコンストラクタ。
     */
    public ProductAlternative() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 代替の種類を返す。
     *
     * @return 代替の種類
     */
    public String getAlternativeType() {
        return alternativeType;
    }

    /**
     * 代替の種類を設定する。
     *
     * @param alternativeType 代替の種類
     */
    public void setAlternativeType(String alternativeType) {
        this.alternativeType = alternativeType;
    }

    /**
     * 備考を返す。
     *
     * @return 備考
     */
    public String getNotes() {
        return notes;
    }

    /**
     * 備考を設定する。
     *
     * @param notes 備考
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * 元商品を返す。
     *
     * @return 元商品
     */
    public Product getSourceProduct() {
        return sourceProduct;
    }

    /**
     * 元商品を設定する。
     *
     * @param sourceProduct 元商品
     */
    public void setSourceProduct(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
    }

    /**
     * 代替商品を返す。
     *
     * @return 代替商品
     */
    public Product getAlternativeProduct() {
        return alternativeProduct;
    }

    /**
     * 代替商品を設定する。
     *
     * @param alternativeProduct 代替商品
     */
    public void setAlternativeProduct(Product alternativeProduct) {
        this.alternativeProduct = alternativeProduct;
    }

    @Override
    public String toString() {
        return "ProductAlternative{" +
                "id=" + getId() +
                ", alternativeType='" + alternativeType + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
