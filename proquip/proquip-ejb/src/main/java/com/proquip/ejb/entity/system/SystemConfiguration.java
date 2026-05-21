package com.proquip.ejb.entity.system;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * システム設定を表すエンティティ。
 *
 * <p>アプリケーション全体の設定値をキーバリュー形式で管理する。
 * 設定値はカテゴリで分類され、データ型情報と共に保存される。
 * 暗号化フラグにより機密情報の管理にも対応する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code dataType} フィールドが文字列型で定義されている。本来はEnumを使用すべき。</li>
 *   <li>{@link #getTypedValue()} メソッドで手動の型変換を行っており、
 *       instanceof/キャストによる非型安全な実装になっている。</li>
 *   <li>暗号化設定値の復号処理がエンティティ層に実装されていない
 *       （サービス層で個別に対応する必要がある）。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "system_configuration")
@NamedQueries({
    @NamedQuery(
        name = "SystemConfiguration.findByKey",
        query = "SELECT c FROM SystemConfiguration c WHERE c.configKey = :configKey"
    ),
    @NamedQuery(
        name = "SystemConfiguration.findByCategory",
        query = "SELECT c FROM SystemConfiguration c WHERE c.category = :category ORDER BY c.configKey"
    ),
    @NamedQuery(
        name = "SystemConfiguration.findEditableConfigs",
        query = "SELECT c FROM SystemConfiguration c WHERE c.editable = true ORDER BY c.category, c.configKey"
    )
})
public class SystemConfiguration extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 設定キー（一意識別子） */
    @Column(name = "config_key", nullable = false, unique = true, length = 200)
    private String configKey;

    /** 設定値 */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /** 設定カテゴリ（例: "mail", "security", "batch"） */
    @Column(name = "config_group", nullable = false, length = 50)
    private String category;

    /** 設定の説明 */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 設定値のデータ型。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "STRING", "INTEGER", "BOOLEAN", "JSON"
     */
    @Column(name = "value_type", nullable = false, length = 20)
    private String dataType;

    /** 暗号化フラグ（パスワード等の機密情報を示す） */
    @Column(name = "is_encrypted", nullable = false)
    private Boolean encrypted = false;

    /** 編集可能フラグ（管理画面からの変更可否） */
    @Column(name = "is_editable", nullable = false)
    private Boolean editable = true;

    /** 最終更新者のユーザー名 */
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    /**
     * デフォルトコンストラクタ。
     */
    public SystemConfiguration() {
        super();
    }

    // --- ビジネスメソッド ---

    /**
     * 設定値を {@code dataType} に基づいて適切な型に変換して返す。
     *
     * <p>技術的負債: 手動の型変換を行っている。ジェネリクスやストラテジパターンを
     * 使用した型安全な実装に移行すべき。nullチェックも不十分。</p>
     *
     * @return 型変換された設定値。変換できない場合は文字列のまま返す。
     */
    public Object getTypedValue() {
        if (configValue == null) {
            return null;
        }

        // 技術的負債: instanceof/キャストによる非型安全な型変換
        if ("INTEGER".equals(dataType)) {
            try {
                return Integer.valueOf(configValue);
            } catch (NumberFormatException e) {
                // 技術的負債: 例外を握りつぶして文字列を返す
                return configValue;
            }
        } else if ("BOOLEAN".equals(dataType)) {
            return Boolean.valueOf(configValue);
        } else if ("JSON".equals(dataType)) {
            // 技術的負債: JSON文字列をそのまま返す。パース処理はサービス層に委譲。
            return configValue;
        }
        // STRING またはその他の場合
        return configValue;
    }

    // --- Getter / Setter ---

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
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return データ型（"STRING" / "INTEGER" / "BOOLEAN" / "JSON"）
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
     * 暗号化フラグを返す。
     *
     * @return 暗号化されている場合 {@code true}
     */
    public Boolean getEncrypted() {
        return encrypted;
    }

    /**
     * 暗号化フラグを設定する。
     *
     * @param encrypted 暗号化フラグ
     */
    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    /**
     * 編集可能フラグを返す。
     *
     * @return 編集可能な場合 {@code true}
     */
    public Boolean getEditable() {
        return editable;
    }

    /**
     * 編集可能フラグを設定する。
     *
     * @param editable 編集可能フラグ
     */
    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    /**
     * 最終更新者のユーザー名を返す。
     *
     * @return 最終更新者
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * 最終更新者のユーザー名を設定する。
     *
     * @param lastModifiedBy 最終更新者
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public String toString() {
        return "SystemConfiguration{" +
                "id=" + getId() +
                ", configKey='" + configKey + '\'' +
                ", category='" + category + '\'' +
                ", dataType='" + dataType + '\'' +
                ", encrypted=" + encrypted +
                ", editable=" + editable +
                '}';
    }
}
