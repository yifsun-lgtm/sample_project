package com.proquip.common.dto.admin;

import java.io.Serializable;
import java.util.Date;

/**
 * システム設定データ転送オブジェクト。
 *
 * <p>システム全体の設定項目を保持する。キーと値のペアに加え、
 * カテゴリ、説明、データ型情報を持つ。</p>
 *
 * @author ProQuip開発チーム
 */
public class SystemConfigDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 設定ID */
    private Long id;

    /** 設定キー */
    private String configKey;

    /** 設定値 */
    private String configValue;

    /** カテゴリ（GENERAL, PROCUREMENT, INVENTORY, NOTIFICATION, SECURITY） */
    private String category;

    /** 説明 */
    private String description;

    /** データ型（STRING, INTEGER, BOOLEAN, DECIMAL） */
    private String dataType;

    /** 編集可能フラグ */
    private boolean editable;

    /** 最終更新日時 */
    private Date lastModified;

    /** 最終更新者 */
    private String lastModifiedBy;

    /**
     * デフォルトコンストラクタ。
     */
    public SystemConfigDto() {
    }

    // --- Getter / Setter ---

    /**
     * 設定IDを返す。
     *
     * @return 設定ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 設定IDを設定する。
     *
     * @param id 設定ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 設定キーを返す。
     *
     * @return 設定キー
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 設定キーを設定する。
     *
     * @param configKey 設定キー
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * 設定値を返す。
     *
     * @return 設定値
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * 設定値を設定する。
     *
     * @param configValue 設定値
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    /**
     * カテゴリを返す。
     *
     * @return カテゴリ
     */
    public String getCategory() {
        return category;
    }

    /**
     * カテゴリを設定する。
     *
     * @param category カテゴリ
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 説明を返す。
     *
     * @return 説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 説明を設定する。
     *
     * @param description 説明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * データ型を返す。
     *
     * @return データ型
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * データ型を設定する。
     *
     * @param dataType データ型
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * 編集可能フラグを返す。
     *
     * @return 編集可能な場合 true
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * 編集可能フラグを設定する。
     *
     * @param editable 編集可能な場合 true
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * 最終更新日時を返す。
     *
     * @return 最終更新日時
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * 最終更新日時を設定する。
     *
     * @param lastModified 最終更新日時
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * 最終更新者を返す。
     *
     * @return 最終更新者名
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * 最終更新者を設定する。
     *
     * @param lastModifiedBy 最終更新者名
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
