package com.proquip.ejb.entity.system;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

/**
 * 監査ログを表すエンティティ。
 *
 * <p>システム内のエンティティに対する操作履歴（作成・更新・削除）を記録する。
 * セキュリティ監査およびデータ変更追跡のために使用される。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code action} フィールドが文字列型で定義されている。本来はEnumを使用すべき。</li>
 *   <li>{@code oldValues} / {@code newValues} がプレーンテキストのJSON文字列。
 *       本来はJSONB型カラムを使用すべき。</li>
 *   <li>{@code findByEntityType} クエリがJOIN FETCHを使用しておらず、
 *       関連エンティティアクセス時にN+1問題を引き起こす可能性がある。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "audit_log")
@NamedQueries({
    @NamedQuery(
        name = "AuditLog.findByEntityType",
        query = "SELECT a FROM AuditLog a WHERE a.entityType = :entityType ORDER BY a.performedAt DESC"
    ),
    @NamedQuery(
        name = "AuditLog.findByPerformedBy",
        query = "SELECT a FROM AuditLog a WHERE a.performedBy = :performedBy ORDER BY a.performedAt DESC"
    ),
    // 技術的負債: 大量データ時にパフォーマンス問題が発生する可能性がある（インデックスが適切に設定されていない前提）
    @NamedQuery(
        name = "AuditLog.findByDateRange",
        query = "SELECT a FROM AuditLog a WHERE a.performedAt BETWEEN :startDate AND :endDate ORDER BY a.performedAt DESC"
    )
})
public class AuditLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 操作対象のエンティティ種別（例: "Product", "Supplier"） */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /** 操作対象のエンティティID */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * 操作種別。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "CREATE", "UPDATE", "DELETE"
     */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    /** 操作実行者のユーザーID */
    @Column(name = "performed_by")
    private Long performedBy;

    /** 操作実行日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "performed_at", nullable = false)
    private Date performedAt;

    /** 操作元のIPアドレス */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** 操作元のユーザーエージェント */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 変更前の値（JSON文字列）。
     * 技術的負債: プレーンテキストでJSONを格納している。JSONB型カラムに移行すべき。
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    /**
     * 変更後の値（JSON文字列）。
     * 技術的負債: プレーンテキストでJSONを格納している。JSONB型カラムに移行すべき。
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    /** 追加情報（任意のメモ等） */
    @Column(name = "description", length = 1000)
    private String additionalInfo;

    /**
     * デフォルトコンストラクタ。
     */
    public AuditLog() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * エンティティ種別を返す。
     *
     * @return エンティティ種別
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * エンティティ種別を設定する。
     *
     * @param entityType エンティティ種別
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * エンティティIDを返す。
     *
     * @return エンティティID
     */
    public Long getEntityId() {
        return entityId;
    }

    /**
     * エンティティIDを設定する。
     *
     * @param entityId エンティティID
     */
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    /**
     * 操作種別を返す。
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return 操作種別（"CREATE" / "UPDATE" / "DELETE"）
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
     * 操作実行者のユーザーIDを返す。
     *
     * @return 操作実行者ID
     */
    public Long getPerformedBy() {
        return performedBy;
    }

    /**
     * 操作実行者のユーザーIDを設定する。
     *
     * @param performedBy 操作実行者ID
     */
    public void setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
    }

    /**
     * 操作実行日時を返す。
     *
     * @return 操作実行日時
     */
    public Date getPerformedAt() {
        return performedAt;
    }

    /**
     * 操作実行日時を設定する。
     *
     * @param performedAt 操作実行日時
     */
    public void setPerformedAt(Date performedAt) {
        this.performedAt = performedAt;
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
     * ユーザーエージェントを返す。
     *
     * @return ユーザーエージェント
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * ユーザーエージェントを設定する。
     *
     * @param userAgent ユーザーエージェント
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * 変更前の値（JSON文字列）を返す。
     *
     * @return 変更前の値
     */
    public String getOldValues() {
        return oldValues;
    }

    /**
     * 変更前の値（JSON文字列）を設定する。
     *
     * @param oldValues 変更前の値
     */
    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    /**
     * 変更後の値（JSON文字列）を返す。
     *
     * @return 変更後の値
     */
    public String getNewValues() {
        return newValues;
    }

    /**
     * 変更後の値（JSON文字列）を設定する。
     *
     * @param newValues 変更後の値
     */
    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    /**
     * 追加情報を返す。
     *
     * @return 追加情報
     */
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * 追加情報を設定する。
     *
     * @param additionalInfo 追加情報
     */
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + getId() +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", action='" + action + '\'' +
                ", performedBy='" + performedBy + '\'' +
                ", performedAt=" + performedAt +
                '}';
    }
}
