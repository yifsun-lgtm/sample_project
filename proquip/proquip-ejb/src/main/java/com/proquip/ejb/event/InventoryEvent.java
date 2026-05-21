package com.proquip.ejb.event;

import java.io.Serializable;
import java.util.Date;

/**
 * 在庫イベントのCDIイベントペイロード。
 *
 * <p>在庫に関する各種操作（入庫、出庫、移動、棚卸調整等）が
 * 発生した際にCDIイベントとして発火され、オブザーバーに通知される。</p>
 *
 * <p>技術的負債: タイムスタンプに {@link java.util.Date} を使用している。
 * {@code java.time.Instant} に移行すべき。</p>
 *
 * @author ProQuip開発チーム
 */
public class InventoryEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 対象商品のID */
    private Long productId;

    /** 対象倉庫のID */
    private Long warehouseId;

    /**
     * イベント種別。
     * 想定値: "RECEIVED", "SHIPPED", "ADJUSTED", "TRANSFERRED",
     *         "RESERVED", "RELEASED", "COUNT_COMPLETED"
     */
    private String eventType;

    /** 変動数量（正: 増加、負: 減少） */
    private Integer quantity;

    /**
     * イベント発生日時。
     * 技術的負債: java.util.Dateを使用。java.time.Instantに移行すべき。
     */
    private Date timestamp;

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryEvent() {
    }

    /**
     * 全フィールドを指定するコンストラクタ。
     *
     * @param productId   商品ID
     * @param warehouseId 倉庫ID
     * @param eventType   イベント種別
     * @param quantity    変動数量
     * @param timestamp   イベント発生日時
     */
    public InventoryEvent(Long productId, Long warehouseId, String eventType,
                          Integer quantity, Date timestamp) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.eventType = eventType;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    // --- Getter / Setter ---

    /**
     * 商品IDを返す。
     *
     * @return 商品ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 商品IDを設定する。
     *
     * @param productId 商品ID
     */
    public void setProductId(Long productId) {
        this.productId = productId;
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
     * イベント種別を返す。
     *
     * @return イベント種別
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * イベント種別を設定する。
     *
     * @param eventType イベント種別
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * 変動数量を返す。
     *
     * @return 変動数量
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * 変動数量を設定する。
     *
     * @param quantity 変動数量
     */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /**
     * イベント発生日時を返す。
     *
     * @return イベント発生日時
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * イベント発生日時を設定する。
     *
     * @param timestamp イベント発生日時
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "InventoryEvent{" +
                "productId=" + productId +
                ", warehouseId=" + warehouseId +
                ", eventType='" + eventType + '\'' +
                ", quantity=" + quantity +
                ", timestamp=" + timestamp +
                '}';
    }
}
