package com.proquip.common.dto.pricing;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 価格リスト明細データ転送オブジェクト。
 *
 * <p>価格リスト内の個別アイテム（製品ごとの価格設定）を表す。
 * 標準単価、リスト価格、割引率を保持する。</p>
 *
 * @author ProQuip開発チーム
 */
public class PriceListItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明細ID */
    private Long id;

    /** 製品ID */
    private Long productId;

    /** 製品名 */
    private String productName;

    /** SKUコード */
    private String skuCode;

    /** 標準単価 */
    private BigDecimal standardUnitPrice;

    /** リスト価格 */
    private BigDecimal listPrice;

    /** 割引率（パーセンテージ） */
    private BigDecimal discountRate;

    /** 有効開始日 */
    private Date effectiveStartDate;

    /** 有効終了日 */
    private Date effectiveEndDate;

    /**
     * デフォルトコンストラクタ。
     */
    public PriceListItemDto() {
    }

    // --- Getter / Setter ---

    /**
     * 明細IDを返す。
     *
     * @return 明細ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 明細IDを設定する。
     *
     * @param id 明細ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 製品IDを返す。
     *
     * @return 製品ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 製品IDを設定する。
     *
     * @param productId 製品ID
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 製品名を返す。
     *
     * @return 製品名
     */
    public String getProductName() {
        return productName;
    }

    /**
     * 製品名を設定する。
     *
     * @param productName 製品名
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * SKUコードを返す。
     *
     * @return SKUコード
     */
    public String getSkuCode() {
        return skuCode;
    }

    /**
     * SKUコードを設定する。
     *
     * @param skuCode SKUコード
     */
    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    /**
     * 標準単価を返す。
     *
     * @return 標準単価
     */
    public BigDecimal getStandardUnitPrice() {
        return standardUnitPrice;
    }

    /**
     * 標準単価を設定する。
     *
     * @param standardUnitPrice 標準単価
     */
    public void setStandardUnitPrice(BigDecimal standardUnitPrice) {
        this.standardUnitPrice = standardUnitPrice;
    }

    /**
     * リスト価格を返す。
     *
     * @return リスト価格
     */
    public BigDecimal getListPrice() {
        return listPrice;
    }

    /**
     * リスト価格を設定する。
     *
     * @param listPrice リスト価格
     */
    public void setListPrice(BigDecimal listPrice) {
        this.listPrice = listPrice;
    }

    /**
     * 割引率を返す。
     *
     * @return 割引率（パーセンテージ）
     */
    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    /**
     * 割引率を設定する。
     *
     * @param discountRate 割引率（パーセンテージ）
     */
    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    /**
     * 有効開始日を返す。
     *
     * @return 有効開始日
     */
    public Date getEffectiveStartDate() {
        return effectiveStartDate;
    }

    /**
     * 有効開始日を設定する。
     *
     * @param effectiveStartDate 有効開始日
     */
    public void setEffectiveStartDate(Date effectiveStartDate) {
        this.effectiveStartDate = effectiveStartDate;
    }

    /**
     * 有効終了日を返す。
     *
     * @return 有効終了日
     */
    public Date getEffectiveEndDate() {
        return effectiveEndDate;
    }

    /**
     * 有効終了日を設定する。
     *
     * @param effectiveEndDate 有効終了日
     */
    public void setEffectiveEndDate(Date effectiveEndDate) {
        this.effectiveEndDate = effectiveEndDate;
    }
}
