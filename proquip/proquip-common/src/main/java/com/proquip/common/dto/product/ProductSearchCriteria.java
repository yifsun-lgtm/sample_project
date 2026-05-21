package com.proquip.common.dto.product;

import com.proquip.common.dto.common.SearchCriteria;

import java.math.BigDecimal;

/**
 * 商品検索条件DTOクラス。
 *
 * <p>商品一覧の検索・フィルタリングに使用する条件を保持する。
 * キーワード検索、カテゴリ・製造元での絞り込み、価格帯指定が可能。</p>
 *
 * <p>{@link SearchCriteria} を継承し、共通のページネーション・ソート機能を利用する。</p>
 *
 * @author ProQuip開発チーム
 */
public class ProductSearchCriteria extends SearchCriteria {

    private static final long serialVersionUID = 1L;

    /** キーワード（商品名、SKU、説明文を対象） */
    private String keyword;

    /** カテゴリID */
    private Long categoryId;

    /** 製造元ID */
    private Long manufacturerId;

    /** ステータス（ACTIVE, INACTIVE, DISCONTINUED） */
    private String status;

    /** 最低価格 */
    private BigDecimal minPrice;

    /** 最高価格 */
    private BigDecimal maxPrice;

    /** 在庫切れのみ表示フラグ */
    private Boolean outOfStockOnly;

    /** タグによるフィルタ */
    private String tag;

    /**
     * デフォルトコンストラクタ。
     */
    public ProductSearchCriteria() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * キーワードを返す。
     *
     * @return 検索キーワード
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * キーワードを設定する。
     *
     * @param keyword 検索キーワード
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * カテゴリIDを返す。
     *
     * @return カテゴリID
     */
    public Long getCategoryId() {
        return categoryId;
    }

    /**
     * カテゴリIDを設定する。
     *
     * @param categoryId カテゴリID
     */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 製造元IDを返す。
     *
     * @return 製造元ID
     */
    public Long getManufacturerId() {
        return manufacturerId;
    }

    /**
     * 製造元IDを設定する。
     *
     * @param manufacturerId 製造元ID
     */
    public void setManufacturerId(Long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    /**
     * ステータスを返す。
     *
     * @return ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 最低価格を返す。
     *
     * @return 最低価格
     */
    public BigDecimal getMinPrice() {
        return minPrice;
    }

    /**
     * 最低価格を設定する。
     *
     * @param minPrice 最低価格
     */
    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    /**
     * 最高価格を返す。
     *
     * @return 最高価格
     */
    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    /**
     * 最高価格を設定する。
     *
     * @param maxPrice 最高価格
     */
    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    /**
     * 在庫切れのみ表示フラグを返す。
     *
     * @return 在庫切れのみ表示する場合 true
     */
    public Boolean getOutOfStockOnly() {
        return outOfStockOnly;
    }

    /**
     * 在庫切れのみ表示フラグを設定する。
     *
     * @param outOfStockOnly 在庫切れのみ表示する場合 true
     */
    public void setOutOfStockOnly(Boolean outOfStockOnly) {
        this.outOfStockOnly = outOfStockOnly;
    }

    /**
     * タグを返す。
     *
     * @return タグ
     */
    public String getTag() {
        return tag;
    }

    /**
     * タグを設定する。
     *
     * @param tag タグ
     */
    public void setTag(String tag) {
        this.tag = tag;
    }
}
