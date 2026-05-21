package com.proquip.common.dto.procurement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 入荷検収データ転送オブジェクト。
 *
 * <p>入荷検収の情報を保持する。発注書に対する入荷処理で、
 * 受領明細と備考を含む。</p>
 *
 * @author ProQuip開発チーム
 */
public class GoodsReceiptDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 入荷検収ID */
    private Long id;

    /** 入荷番号 */
    private String receiptNumber;

    /** 発注書ID */
    private Long orderId;

    /** 発注番号 */
    private String orderNumber;

    /** 仕入先名 */
    private String supplierName;

    /** 受領日 */
    private Date receivedDate;

    /** ステータス（PENDING, INSPECTING, ACCEPTED, PARTIAL, REJECTED） */
    private String status;

    /** 備考 */
    private String notes;

    /** 受領者名 */
    private String receivedBy;

    /** 倉庫ID */
    private Long warehouseId;

    /** 倉庫名 */
    private String warehouseName;

    /** 入荷明細一覧 */
    private List<GoodsReceiptItemDto> items = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public GoodsReceiptDto() {
    }

    // --- Getter / Setter ---

    /**
     * 入荷検収IDを返す。
     *
     * @return 入荷検収ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 入荷検収IDを設定する。
     *
     * @param id 入荷検収ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 入荷番号を返す。
     *
     * @return 入荷番号
     */
    public String getReceiptNumber() {
        return receiptNumber;
    }

    /**
     * 入荷番号を設定する。
     *
     * @param receiptNumber 入荷番号
     */
    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    /**
     * 発注書IDを返す。
     *
     * @return 発注書ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 発注書IDを設定する。
     *
     * @param orderId 発注書ID
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 発注番号を返す。
     *
     * @return 発注番号
     */
    public String getOrderNumber() {
        return orderNumber;
    }

    /**
     * 発注番号を設定する。
     *
     * @param orderNumber 発注番号
     */
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    /**
     * 仕入先名を返す。
     *
     * @return 仕入先名
     */
    public String getSupplierName() {
        return supplierName;
    }

    /**
     * 仕入先名を設定する。
     *
     * @param supplierName 仕入先名
     */
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    /**
     * 受領日を返す。
     *
     * @return 受領日
     */
    public Date getReceivedDate() {
        return receivedDate;
    }

    /**
     * 受領日を設定する。
     *
     * @param receivedDate 受領日
     */
    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
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
     * 受領者名を返す。
     *
     * @return 受領者名
     */
    public String getReceivedBy() {
        return receivedBy;
    }

    /**
     * 受領者名を設定する。
     *
     * @param receivedBy 受領者名
     */
    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
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
     * 入荷明細一覧を返す。
     *
     * @return 入荷明細DTOのリスト
     */
    public List<GoodsReceiptItemDto> getItems() {
        return items;
    }

    /**
     * 入荷明細一覧を設定する。
     *
     * @param items 入荷明細DTOのリスト
     */
    public void setItems(List<GoodsReceiptItemDto> items) {
        this.items = items;
    }
}
