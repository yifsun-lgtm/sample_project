package com.proquip.common.dto.admin;

import java.io.Serializable;
import java.util.Date;

/**
 * 監査ログデータ転送オブジェクト。
 *
 * <p>システム操作の監査ログ情報を保持する。誰が、いつ、何に対して、
 * どのような操作を行ったかを記録する。</p>
 *
 * @author ProQuip開発チーム
 */
public class AuditLogDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 監査ログID */
    private Long id;

    /** 操作種別（CREATE, UPDATE, DELETE, LOGIN, LOGOUT, APPROVE, REJECT） */
    private String action;

    /** 対象エンティティ種別 */
    private String entityType;

    /** 対象エンティティID */
    private Long entityId;

    /** 対象エンティティの表示名 */
    private String entityName;

    /** 操作ユーザーID */
    private Long userId;

    /** 操作ユーザー名 */
    private String userName;

    /** 操作日時 */
    private Date timestamp;

    /** 変更前の値（JSON形式） */
    private String oldValue;

    /** 変更後の値（JSON形式） */
    private String newValue;

    /** IPアドレス */
    private String ipAddress;

    /** 備考 */
    private String description;

    /**
     * デフォルトコンストラクタ。
     */
    public AuditLogDto() {
    }

    // --- Getter / Setter ---

    /**
     * 監査ログIDを返す。
     *
     * @return 監査ログID
     */
    public Long getId() {
        return id;
    }

    /**
     * 監査ログIDを設定する。
     *
     * @param id 監査ログID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 操作種別を返す。
     *
     * @return 操作種別
     */
    public String getAction() {
        return action;
    }

    /**
     * 操作種別を設定する。
     *
     * @param action 操作種別
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 対象エンティティ種別を返す。
     *
     * @return エンティティ種別
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * 対象エンティティ種別を設定する。
     *
     * @param entityType エンティティ種別
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * 対象エンティティIDを返す。
     *
     * @return エンティティID
     */
    public Long getEntityId() {
        return entityId;
    }

    /**
     * 対象エンティティIDを設定する。
     *
     * @param entityId エンティティID
     */
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    /**
     * 対象エンティティの表示名を返す。
     *
     * @return エンティティ表示名
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * 対象エンティティの表示名を設定する。
     *
     * @param entityName エンティティ表示名
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    /**
     * 操作ユーザーIDを返す。
     *
     * @return ユーザーID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 操作ユーザーIDを設定する。
     *
     * @param userId ユーザーID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 操作ユーザー名を返す。
     *
     * @return ユーザー名
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 操作ユーザー名を設定する。
     *
     * @param userName ユーザー名
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 操作日時を返す。
     *
     * @return 操作日時
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * 操作日時を設定する。
     *
     * @param timestamp 操作日時
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 変更前の値を返す。
     *
     * @return 変更前の値（JSON形式）
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * 変更前の値を設定する。
     *
     * @param oldValue 変更前の値（JSON形式）
     */
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    /**
     * 変更後の値を返す。
     *
     * @return 変更後の値（JSON形式）
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * 変更後の値を設定する。
     *
     * @param newValue 変更後の値（JSON形式）
     */
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    /**
     * IPアドレスを返す。
     *
     * @return IPアドレス
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * IPアドレスを設定する。
     *
     * @param ipAddress IPアドレス
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 備考を返す。
     *
     * @return 備考
     */
    public String getDescription() {
        return description;
    }

    /**
     * 備考を設定する。
     *
     * @param description 備考
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
