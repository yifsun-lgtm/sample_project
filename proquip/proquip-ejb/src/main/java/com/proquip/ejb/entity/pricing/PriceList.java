package com.proquip.ejb.entity.pricing;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 価格表エンティティ。
 * <p>
 * 製品の価格情報を時限的に管理する価格表を表す。
 * 有効期間（effectiveFrom〜effectiveTo）とステータスにより適用可否が決まる。
 * 通貨指定やデフォルトフラグを持つ。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "price_list")
@NamedQueries({
    @NamedQuery(
        name = "PriceList.findActive",
        query = "SELECT pl FROM PriceList pl WHERE pl.status = 'ACTIVE' AND pl.effectiveFrom <= :currentDate AND (pl.effectiveTo IS NULL OR pl.effectiveTo >= :currentDate) ORDER BY pl.name"
    ),
    @NamedQuery(
        name = "PriceList.findDefault",
        query = "SELECT pl FROM PriceList pl WHERE pl.isDefault = true AND pl.status = 'ACTIVE'"
    )
})
public class PriceList extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 50)
    @Column(name = "price_list_code", nullable = false, length = 50)
    private String priceListCode;

    @NotNull
    @Size(max = 20)
    @Column(name = "price_list_type", nullable = false, length = 20)
    private String priceListType = "STANDARD";

    /** 価格表名 */
    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 通貨コード。
     * <p>
     * 技術的負債: Currency エンティティへの参照ではなく文字列で管理している。
     * </p>
     */
    @NotNull
    @Size(max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currency;

    @Column(name = "description")
    private String description;

    /** 有効開始日 */
    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "effective_from", nullable = false)
    private Date effectiveFrom;

    /** 有効終了日（nullの場合は無期限） */
    @Temporal(TemporalType.DATE)
    @Column(name = "effective_until")
    private Date effectiveTo;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: DRAFT, ACTIVE, EXPIRED, ARCHIVED
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** デフォルト価格表フラグ */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /** 価格表明細のリスト */
    @OneToMany(mappedBy = "priceList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PriceListItem> items = new ArrayList<PriceListItem>();

    /**
     * デフォルトコンストラクタ。
     */
    public PriceList() {
        super();
    }

    // --- Getter / Setter ---

    public String getPriceListCode() {
        return priceListCode;
    }

    public void setPriceListCode(String priceListCode) {
        this.priceListCode = priceListCode;
    }

    public String getPriceListType() {
        return priceListType;
    }

    public void setPriceListType(String priceListType) {
        this.priceListType = priceListType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public List<PriceListItem> getItems() {
        return items;
    }

    public void setItems(List<PriceListItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PriceList{" +
                "name='" + name + '\'' +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}
