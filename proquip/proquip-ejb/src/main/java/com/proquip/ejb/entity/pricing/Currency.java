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
 * 通貨マスタエンティティ。
 * <p>
 * システムで使用する通貨の基本情報と為替レートを管理する。
 * 通貨コード（ISO 4217準拠）で一意に識別される。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "currency")
@NamedQueries({
    @NamedQuery(
        name = "Currency.findByCode",
        query = "SELECT c FROM Currency c WHERE c.code = :code"
    ),
    @NamedQuery(
        name = "Currency.findAll",
        query = "SELECT c FROM Currency c ORDER BY c.code"
    )
})
public class Currency extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 通貨コード（ISO 4217、例: JPY, USD, EUR） */
    @NotNull
    @Size(max = 3)
    @Column(name = "currency_code", unique = true, nullable = false, length = 3)
    private String code;

    /** 通貨名称 */
    @NotNull
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 通貨記号（例: ¥, $） */
    @Size(max = 5)
    @Column(name = "symbol", length = 5)
    private String symbol;

    /** 基準通貨に対する為替レート */
    @Column(name = "exchange_rate", precision = 18, scale = 6)
    private BigDecimal exchangeRate;

    /** 為替レート最終更新日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "rate_updated_at")
    private Date lastUpdated;

    /**
     * デフォルトコンストラクタ。
     */
    public Currency() {
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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Currency{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", exchangeRate=" + exchangeRate +
                '}';
    }
}
