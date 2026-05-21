package com.proquip.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品データ転送オブジェクト。
 *
 * <p>商品の基本情報をプレゼンテーション層やAPI応答として転送するためのクラス。
 * エンティティのフィールドを必要最小限にフラット化して公開する。</p>
 *
 * <p>技術的負債 #8: クラス名のサフィックスが "DTO"（大文字）であり、
 * 他のDTOクラスの "Dto" サフィックスと命名規則が統一されていない。
 * プロジェクト内で DTO / Dto / Response の3パターンが混在している。</p>
 *
 * @author ProQuip開発チーム
 */
public class ProductDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long id;

    /** SKUコード */
    private String skuCode;

    /** 商品名 */
    private String name;

    /** 商品説明 */
    private String description;

    /** カテゴリID */
    private Long categoryId;

    /** カテゴリ名 */
    private String categoryName;

    /** 製造元ID */
    private Long manufacturerId;

    /** 製造元名 */
    private String manufacturerName;

    /** 単価 */
    private BigDecimal unitPrice;

    /** ステータス */
    private String status;

    /** 在庫数量 */
    private Integer stockQuantity;

    /** 再発注点 */
    private Integer reorderPoint;

    /** 単位名（フロントエンドから送信） */
    private String unit;

    /** 最低発注数 */
    private Integer minimumOrderQuantity;

    /** リードタイム（日） */
    private Integer leadTimeDays;

    /** 重量（kg） */
    private BigDecimal weight;

    /** 寸法（文字列） */
    private String dimensions;

    /** 仕様（JSON文字列） */
    private String specifications;

    /** 備考 */
    private String notes;

    /** タグ一覧 */
    private List<String> tags = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public ProductDTO() {
    }

    // --- Getter / Setter ---

    /**
     * 商品IDを返す。
     *
     * @return 商品ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 商品IDを設定する。
     *
     * @param id 商品ID
     */
    public void setId(Long id) {
        this.id = id;
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
     * SKUを返す（フロントエンド互換）。
     *
     * @return SKUコード
     */
    public String getSku() {
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
     * SKUを設定する（フロントエンド互換）。
     *
     * @param sku SKUコード
     */
    public void setSku(String sku) {
        this.skuCode = sku;
    }

    /**
     * 商品名を返す。
     *
     * @return 商品名
     */
    public String getName() {
        return name;
    }

    /**
     * 商品名を設定する。
     *
     * @param name 商品名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 商品説明を返す。
     *
     * @return 商品説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 商品説明を設定する。
     *
     * @param description 商品説明
     */
    public void setDescription(String description) {
        this.description = description;
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
     * カテゴリ名を返す。
     *
     * @return カテゴリ名
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * カテゴリ名を設定する。
     *
     * @param categoryName カテゴリ名
     */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
     * 製造元名を返す。
     *
     * @return 製造元名
     */
    public String getManufacturerName() {
        return manufacturerName;
    }

    /**
     * 製造元名を設定する。
     *
     * @param manufacturerName 製造元名
     */
    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    /**
     * 単価を返す。
     *
     * @return 単価
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * 単価を設定する。
     *
     * @param unitPrice 単価
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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
     * 在庫数量を返す。
     *
     * @return 在庫数量
     */
    public Integer getStockQuantity() {
        return stockQuantity;
    }

    /**
     * 在庫数量を設定する。
     *
     * @param stockQuantity 在庫数量
     */
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    /**
     * 再発注点を返す。
     *
     * @return 再発注点
     */
    public Integer getReorderPoint() {
        return reorderPoint;
    }

    /**
     * 再発注点を設定する。
     *
     * @param reorderPoint 再発注点
     */
    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getMinimumOrderQuantity() {
        return minimumOrderQuantity;
    }

    public void setMinimumOrderQuantity(Integer minimumOrderQuantity) {
        this.minimumOrderQuantity = minimumOrderQuantity;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * タグ一覧を返す。
     *
     * @return タグのリスト
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * タグ一覧を設定する。
     *
     * @param tags タグのリスト
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
