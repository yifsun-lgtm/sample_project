package com.proquip.ejb.event;

import java.io.Serializable;
import java.util.Date;

/**
 * 発注イベントのCDIイベントペイロード。
 *
 * <p>発注に関する各種操作（作成、承認、送信、受入、キャンセル等）が
 * 発生した際にCDIイベントとして発火され、オブザーバーに通知される。</p>
 *
 * <p>技術的負債: タイムスタンプに {@link java.util.Date} を使用している。
 * {@code java.time.Instant} に移行すべき。</p>
 *
 * @author ProQuip開発チーム
 */
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 発注ID */
    private Long orderId;

    /**
     * イベント種別。
     * 想定値: "CREATED", "SUBMITTED", "APPROVED", "REJECTED",
     *         "SENT", "RECEIVED", "CANCELLED", "UPDATED"
     */
    private String eventType;

    /**
     * イベント発生日時。
     * 技術的負債: java.util.Dateを使用。java.time.Instantに移行すべき。
     */
    private Date timestamp;

    /** イベント発生者のユーザーID */
    private String userId;

    /**
     * デフォルトコンストラクタ。
     */
    public OrderEvent() {
    }

    /**
     * 全フィールドを指定するコンストラクタ。
     *
     * @param orderId   発注ID
     * @param eventType イベント種別
     * @param timestamp イベント発生日時
     * @param userId    イベント発生者のユーザーID
     */
    public OrderEvent(Long orderId, String eventType, Date timestamp, String userId) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    // --- Getter / Setter ---

    /**
     * 発注IDを返す。
     *
     * @return 発注ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 発注IDを設定する。
     *
     * @param orderId 発注ID
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    /**
     * イベント発生者のユーザーIDを返す。
     *
     * @return ユーザーID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * イベント発生者のユーザーIDを設定する。
     *
     * @param userId ユーザーID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "orderId=" + orderId +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                '}';
    }
}
