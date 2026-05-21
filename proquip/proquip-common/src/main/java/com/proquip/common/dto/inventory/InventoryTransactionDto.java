package com.proquip.common.dto.inventory;

import java.io.Serializable;
import java.util.Date;

/**
 * 在庫トランザクションデータ転送オブジェクト。
 *
 * <p>在庫の入出庫履歴を表す。入荷、出荷、移動、調整などの各種取引を
 * 統一的に記録する。</p>
 *
 * @author ProQuip開発チーム
 */
public class InventoryTransactionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** トランザクションID */
    private Long id;

    /** トランザクション種別（RECEIPT, ISSUE, TRANSFER, ADJUSTMENT, RETURN） */
    private String transactionType;

    /** 製品ID */
    private Long productId;

    /** 製品名 */
    private String productName;

    /** 倉庫ID */
    private Long warehouseId;

    /** 倉庫名 */
    private String warehouseName;

    /** 数量（正：入庫、負：出庫） */
    private Integer quantity;

    /** トランザクション前の在庫数 */
    private Integer previousQuantity;

    /** トランザクション後の在庫数 */
    private Integer newQuantity;

    /** 参照番号（発注番号、移動番号など） */
    private String referenceNumber;

    /** トランザクション日時 */
    private Date transactionDate;

    /** 実行者 */
    private String performedBy;

    /** 備考 */
    private String notes;

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryTransactionDto() {
    }

    // --- Getter / Setter ---

    /**
     * トランザクションIDを返す。
     *
     * @return トランザクションID
     */
    public Long getId() {
        return id;
    }

    /**
     * トランザクションIDを設定する。
     *
     * @param id トランザクションID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * トランザクション種別を返す。
     *
     * @return トランザクション種別
     */
    public String getTransactionType() {
        return transactionType;
    }

    /**
     * トランザクション種別を設定する。
     *
     * @param transactionType トランザクション種別
     */
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
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
     * 倉庫名を返す。
     *
     * @return 倉庫名
     */
    public String getWarehouseName() {
        return warehouseName;
    }

    /**
     * 倉庫名を設定する。
     *
     * @param warehouseName 倉庫名
     */
    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    /**
     * 数量を返す。
     *
     * @return 数量（正：入庫、負：出庫）
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 数量を設定する。
     *
     * @param quantity 数量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * トランザクション前の在庫数を返す。
     *
     * @return 変更前在庫数
     */
    public Integer getPreviousQuantity() {
        return previousQuantity;
    }

    /**
     * トランザクション前の在庫数を設定する。
     *
     * @param previousQuantity 変更前在庫数
     */
    public void setPreviousQuantity(Integer previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    /**
     * トランザクション後の在庫数を返す。
     *
     * @return 変更後在庫数
     */
    public Integer getNewQuantity() {
        return newQuantity;
    }

    /**
     * トランザクション後の在庫数を設定する。
     *
     * @param newQuantity 変更後在庫数
     */
    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

    /**
     * 参照番号を返す。
     *
     * @return 参照番号
     */
    public String getReferenceNumber() {
        return referenceNumber;
    }

    /**
     * 参照番号を設定する。
     *
     * @param referenceNumber 参照番号
     */
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    /**
     * トランザクション日時を返す。
     *
     * @return トランザクション日時
     */
    public Date getTransactionDate() {
        return transactionDate;
    }

    /**
     * トランザクション日時を設定する。
     *
     * @param transactionDate トランザクション日時
     */
    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    /**
     * 実行者を返す。
     *
     * @return 実行者名
     */
    public String getPerformedBy() {
        return performedBy;
    }

    /**
     * 実行者を設定する。
     *
     * @param performedBy 実行者名
     */
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
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
}
