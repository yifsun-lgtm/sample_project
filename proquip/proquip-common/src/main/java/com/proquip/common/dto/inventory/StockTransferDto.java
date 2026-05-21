package com.proquip.common.dto.inventory;

import java.io.Serializable;
import java.util.Date;

/**
 * 在庫移動データ転送オブジェクト。
 *
 * <p>倉庫間の在庫移動リクエストおよび結果を表現する。
 * 移動元・移動先の倉庫情報と、対象製品・数量を保持する。</p>
 *
 * @author ProQuip開発チーム
 */
public class StockTransferDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 移動ID */
    private Long id;

    /** 移動番号 */
    private String transferNumber;

    /** 移動元倉庫ID */
    private Long sourceWarehouseId;

    /** 移動元倉庫名 */
    private String sourceWarehouseName;

    /** 移動先倉庫ID */
    private Long destinationWarehouseId;

    /** 移動先倉庫名 */
    private String destinationWarehouseName;

    /** 製品ID */
    private Long productId;

    /** 製品名 */
    private String productName;

    /** 移動数量 */
    private Integer quantity;

    /** ステータス（REQUESTED, IN_TRANSIT, COMPLETED, CANCELLED） */
    private String status;

    /** リクエスト日 */
    private Date requestDate;

    /** 完了日 */
    private Date completedDate;

    /** 備考 */
    private String notes;

    /** リクエスト者 */
    private String requestedBy;

    /**
     * デフォルトコンストラクタ。
     */
    public StockTransferDto() {
    }

    // --- Getter / Setter ---

    /**
     * 移動IDを返す。
     *
     * @return 移動ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 移動IDを設定する。
     *
     * @param id 移動ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 移動番号を返す。
     *
     * @return 移動番号
     */
    public String getTransferNumber() {
        return transferNumber;
    }

    /**
     * 移動番号を設定する。
     *
     * @param transferNumber 移動番号
     */
    public void setTransferNumber(String transferNumber) {
        this.transferNumber = transferNumber;
    }

    /**
     * 移動元倉庫IDを返す。
     *
     * @return 移動元倉庫ID
     */
    public Long getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    /**
     * 移動元倉庫IDを設定する。
     *
     * @param sourceWarehouseId 移動元倉庫ID
     */
    public void setSourceWarehouseId(Long sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    /**
     * 移動元倉庫名を返す。
     *
     * @return 移動元倉庫名
     */
    public String getSourceWarehouseName() {
        return sourceWarehouseName;
    }

    /**
     * 移動元倉庫名を設定する。
     *
     * @param sourceWarehouseName 移動元倉庫名
     */
    public void setSourceWarehouseName(String sourceWarehouseName) {
        this.sourceWarehouseName = sourceWarehouseName;
    }

    /**
     * 移動先倉庫IDを返す。
     *
     * @return 移動先倉庫ID
     */
    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    /**
     * 移動先倉庫IDを設定する。
     *
     * @param destinationWarehouseId 移動先倉庫ID
     */
    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    /**
     * 移動先倉庫名を返す。
     *
     * @return 移動先倉庫名
     */
    public String getDestinationWarehouseName() {
        return destinationWarehouseName;
    }

    /**
     * 移動先倉庫名を設定する。
     *
     * @param destinationWarehouseName 移動先倉庫名
     */
    public void setDestinationWarehouseName(String destinationWarehouseName) {
        this.destinationWarehouseName = destinationWarehouseName;
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
     * 移動数量を返す。
     *
     * @return 移動数量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 移動数量を設定する。
     *
     * @param quantity 移動数量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
     * リクエスト日を返す。
     *
     * @return リクエスト日
     */
    public Date getRequestDate() {
        return requestDate;
    }

    /**
     * リクエスト日を設定する。
     *
     * @param requestDate リクエスト日
     */
    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    /**
     * 完了日を返す。
     *
     * @return 完了日
     */
    public Date getCompletedDate() {
        return completedDate;
    }

    /**
     * 完了日を設定する。
     *
     * @param completedDate 完了日
     */
    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
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

    /**
     * リクエスト者を返す。
     *
     * @return リクエスト者名
     */
    public String getRequestedBy() {
        return requestedBy;
    }

    /**
     * リクエスト者を設定する。
     *
     * @param requestedBy リクエスト者名
     */
    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
}
