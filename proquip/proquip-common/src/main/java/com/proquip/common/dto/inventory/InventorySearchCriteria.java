package com.proquip.common.dto.inventory;

import com.proquip.common.dto.common.SearchCriteria;

/**
 * 在庫検索条件DTOクラス。
 *
 * <p>在庫一覧の検索・フィルタリングに使用する条件を保持する。
 * 倉庫、製品、在庫数量での絞り込みが可能。</p>
 *
 * @author ProQuip開発チーム
 */
public class InventorySearchCriteria extends SearchCriteria {

    private static final long serialVersionUID = 1L;

    /** キーワード（製品名、SKUコードを対象） */
    private String keyword;

    /** 倉庫ID */
    private Long warehouseId;

    /** カテゴリID */
    private Long categoryId;

    /** 在庫切れのみ表示フラグ */
    private Boolean outOfStockOnly;

    /** 再発注点以下のみ表示フラグ */
    private Boolean belowReorderPointOnly;

    /** 最小在庫数 */
    private Integer minQuantity;

    /** 最大在庫数 */
    private Integer maxQuantity;

    /**
     * デフォルトコンストラクタ。
     */
    public InventorySearchCriteria() {
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
     * 倉庫IDを返す。
     *
     * @return 倉庫ID
     */
    public Long getWarehouseId() {
        return warehouseId;
    }

    /**
     * 倉庫IDを設定する。
     *
     * @param warehouseId 倉庫ID
     */
    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
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
     * 在庫切れのみ表示フラグを返す。
     *
     * @return 在庫切れのみの場合 true
     */
    public Boolean getOutOfStockOnly() {
        return outOfStockOnly;
    }

    /**
     * 在庫切れのみ表示フラグを設定する。
     *
     * @param outOfStockOnly 在庫切れのみの場合 true
     */
    public void setOutOfStockOnly(Boolean outOfStockOnly) {
        this.outOfStockOnly = outOfStockOnly;
    }

    /**
     * 再発注点以下のみ表示フラグを返す。
     *
     * @return 再発注点以下のみの場合 true
     */
    public Boolean getBelowReorderPointOnly() {
        return belowReorderPointOnly;
    }

    /**
     * 再発注点以下のみ表示フラグを設定する。
     *
     * @param belowReorderPointOnly 再発注点以下のみの場合 true
     */
    public void setBelowReorderPointOnly(Boolean belowReorderPointOnly) {
        this.belowReorderPointOnly = belowReorderPointOnly;
    }

    /**
     * 最小在庫数を返す。
     *
     * @return 最小在庫数
     */
    public Integer getMinQuantity() {
        return minQuantity;
    }

    /**
     * 最小在庫数を設定する。
     *
     * @param minQuantity 最小在庫数
     */
    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    /**
     * 最大在庫数を返す。
     *
     * @return 最大在庫数
     */
    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    /**
     * 最大在庫数を設定する。
     *
     * @param maxQuantity 最大在庫数
     */
    public void setMaxQuantity(Integer maxQuantity) {
        this.maxQuantity = maxQuantity;
    }
}
