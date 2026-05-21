package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 商品バンドル（セット商品）を表すエンティティ。
 *
 * <p>複数の商品をまとめたセット販売を管理する。
 * バンドルには割引率と有効期間を設定できる。</p>
 *
 * <p>技術的負債: {@code validFrom} / {@code validTo} に
 * {@link java.util.Date} を使用している。
 * {@link java.time.LocalDate} に移行すべき。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_bundle")
@NamedQueries({
    @NamedQuery(
        name = "ProductBundle.findActive",
        query = "SELECT pb FROM ProductBundle pb WHERE pb.validFrom <= :now AND pb.validTo >= :now"
    ),
    @NamedQuery(
        name = "ProductBundle.findByName",
        query = "SELECT pb FROM ProductBundle pb WHERE pb.bundleName = :name"
    )
})
public class ProductBundle extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** バンドルコード */
    @Column(name = "bundle_code", nullable = false, unique = true, length = 50)
    private String bundleCode;

    /** バンドル名 */
    @Column(name = "name", nullable = false, length = 200)
    private String bundleName;

    /** 説明 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** バンドル価格 */
    @Column(name = "bundle_price", precision = 15, scale = 2)
    private BigDecimal bundlePrice;

    /** 割引率（パーセンテージ） */
    @Column(name = "discount_pct", precision = 5, scale = 2)
    private BigDecimal discount;

    /** ステータス */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 有効開始日。
     * 技術的負債: java.util.Date を使用（LocalDateに移行すべき）
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "valid_from")
    private Date validFrom;

    /**
     * 有効終了日。
     * 技術的負債: java.util.Date を使用（LocalDateに移行すべき）
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "valid_until")
    private Date validTo;

    /** バンドル構成商品一覧 */
    @OneToMany(mappedBy = "bundle", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<ProductBundleItem> bundleItems = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public ProductBundle() {
        super();
    }

    // --- Getter / Setter ---

    public String getBundleCode() {
        return bundleCode;
    }

    public void setBundleCode(String bundleCode) {
        this.bundleCode = bundleCode;
    }

    public String getBundleName() {
        return bundleName;
    }

    /**
     * バンドル名を設定する。
     *
     * @param bundleName バンドル名
     */
    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBundlePrice() {
        return bundlePrice;
    }

    public void setBundlePrice(BigDecimal bundlePrice) {
        this.bundlePrice = bundlePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 割引率を返す。
     *
     * @return 割引率（パーセンテージ）
     */
    public BigDecimal getDiscount() {
        return discount;
    }

    /**
     * 割引率を設定する。
     *
     * @param discount 割引率（パーセンテージ）
     */
    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    /**
     * 有効開始日を返す。
     *
     * @return 有効開始日
     */
    public Date getValidFrom() {
        return validFrom;
    }

    /**
     * 有効開始日を設定する。
     *
     * @param validFrom 有効開始日
     */
    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    /**
     * 有効終了日を返す。
     *
     * @return 有効終了日
     */
    public Date getValidTo() {
        return validTo;
    }

    /**
     * 有効終了日を設定する。
     *
     * @param validTo 有効終了日
     */
    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    /**
     * バンドル構成商品一覧を返す。
     *
     * @return バンドル構成商品のリスト
     */
    public List<ProductBundleItem> getBundleItems() {
        return bundleItems;
    }

    /**
     * バンドル構成商品一覧を設定する。
     *
     * @param bundleItems バンドル構成商品のリスト
     */
    public void setBundleItems(List<ProductBundleItem> bundleItems) {
        this.bundleItems = bundleItems;
    }

    @Override
    public String toString() {
        return "ProductBundle{" +
                "id=" + getId() +
                ", bundleName='" + bundleName + '\'' +
                ", discount=" + discount +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                '}';
    }
}
