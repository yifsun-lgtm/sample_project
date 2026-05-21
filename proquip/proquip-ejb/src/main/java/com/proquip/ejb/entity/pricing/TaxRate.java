package com.proquip.ejb.entity.pricing;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 税率マスタエンティティ。
 * <p>
 * 国・地域別の税率情報を管理する。
 * 税率コードで識別され、有効期間（effectiveFrom〜effectiveTo）により
 * 適用期間が制御される。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "tax_rate")
@NamedQueries({
    @NamedQuery(
        name = "TaxRate.findByCode",
        query = "SELECT tr FROM TaxRate tr WHERE tr.code = :code"
    ),
    @NamedQuery(
        name = "TaxRate.findActiveByCountry",
        query = "SELECT tr FROM TaxRate tr WHERE tr.country = :country AND tr.effectiveFrom <= :currentDate AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :currentDate)"
    )
})
public class TaxRate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 税率コード */
    @NotNull
    @Size(max = 20)
    @Column(name = "tax_code", nullable = false, length = 20)
    private String code;

    /** 税率名称 */
    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 税率（例: 0.10 = 10%） */
    @NotNull
    @Column(name = "rate_pct", nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    /** 適用国コード */
    @Size(max = 3)
    @Column(name = "country_code", length = 3)
    private String country;

    /** 適用州・地域コード */
    @Size(max = 10)
    @Column(name = "state_province", length = 10)
    private String state;

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
     * デフォルトコンストラクタ。
     */
    public TaxRate() {
        super();
    }

    // --- Getter / Setter ---

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    @Override
    public String toString() {
        return "TaxRate{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", rate=" + rate +
                ", country='" + country + '\'' +
                '}';
    }
}
