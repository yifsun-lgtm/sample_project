package com.proquip.common.dto;

import java.io.Serializable;

/**
 * 在庫品目データ転送オブジェクト。
 *
 * <p>在庫品目の情報をプレゼンテーション層やAPI応答として転送するためのクラス。
 * 製品情報と倉庫情報をフラット化して保持する。</p>
 *
 * @author ProQuip開発チーム
 */
public class InventoryItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 在庫品目ID */
    private Long id;

    /** 製品ID */
    private Long productId;

    /** 製品名 */
    private String productName;

    /** SKUコード */
    private String skuCode;

    /** 倉庫ID */
    private Long warehouseId;

    /** 倉庫名 */
    private String warehouseName;

    /** 手持在庫数量 */
    private Integer quantity;

    /** 引当済み数量 */
    private Integer reservedQuantity;

    /** 再発注点 */
    private Integer reorderPoint;

    /** 保管場所 */
    private String storageLocation;

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryItemDto() {
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }
}
