package com.proquip.ejb.entity.system;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

/**
 * 通知を表すエンティティ。
 *
 * <p>ユーザーへのシステム通知・承認依頼等を管理する。
 * 通知は未読・既読・アーカイブ済みの状態を持ち、有効期限を設定できる。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code userId} がLong型の生の外部キーとして保持されている。
 *       本来は {@code @ManyToOne} でユーザーエンティティと関連付けるべき。</li>
 *   <li>{@code templateId} も同様に生の外部キー。
 *       {@link NotificationTemplate} との {@code @ManyToOne} に移行すべき。</li>
 *   <li>{@code type} / {@code status} フィールドが文字列型で定義されている。
 *       本来はEnumを使用すべき。</li>
 *   <li>{@code findByUserId} クエリがJOIN FETCHを使用しておらず、
 *       テンプレート参照時にN+1問題を引き起こす可能性がある。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "notification")
@NamedQueries({
    // 技術的負債: N+1問題のリスクあり（関連エンティティのJOIN FETCHなし）
    @NamedQuery(
        name = "Notification.findByUserId",
        query = "SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC"
    ),
    @NamedQuery(
        name = "Notification.findUnreadByUserId",
        query = "SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = 'UNREAD' ORDER BY n.priority DESC, n.createdAt DESC"
    ),
    @NamedQuery(
        name = "Notification.findByType",
        query = "SELECT n FROM Notification n WHERE n.type = :type ORDER BY n.createdAt DESC"
    ),
    @NamedQuery(
        name = "Notification.countUnread",
        query = "SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = 'UNREAD'"
    )
})
public class Notification extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 通知先ユーザーのID。
     * 技術的負債: 生のLong型外部キー。{@code @ManyToOne} でUserエンティティを参照すべき。
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 通知タイトル */
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    /** 通知本文 */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * 通知種別。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "INFO", "WARNING", "ERROR", "APPROVAL_REQUEST"
     */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    /**
     * 通知ステータス。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "UNREAD", "READ", "ARCHIVED"
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 優先度（数値が大きいほど高優先） */
    @Column(name = "priority")
    private Integer priority;

    /** 参照先エンティティの種別（例: "PurchaseOrder", "ApprovalRequest"） */
    @Column(name = "reference_type", length = 100)
    private String referenceType;

    /** 参照先エンティティのID */
    @Column(name = "reference_id")
    private Long referenceId;

    /** 既読日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "read_at")
    private Date readAt;

    /** 有効期限 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires_at")
    private Date expiresAt;

    /**
     * 通知テンプレートのID。
     * 技術的負債: 生のLong型外部キー。{@code @ManyToOne} で {@link NotificationTemplate} を参照すべき。
     */
    @Column(name = "template_id")
    private Long templateId;

    /**
     * デフォルトコンストラクタ。
     */
    public Notification() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * ユーザーIDを返す。
     * 技術的負債: 生の外部キー。Userエンティティの参照に移行予定。
     *
     * @return ユーザーID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * ユーザーIDを設定する。
     *
     * @param userId ユーザーID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 通知タイトルを返す。
     *
     * @return 通知タイトル
     */
    public String getTitle() {
        return title;
    }

    /**
     * 通知タイトルを設定する。
     *
     * @param title 通知タイトル
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 通知本文を返す。
     *
     * @return 通知本文
     */
    public String getMessage() {
        return message;
    }

    /**
     * 通知本文を設定する。
     *
     * @param message 通知本文
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 通知種別を返す。
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return 通知種別（"INFO" / "WARNING" / "ERROR" / "APPROVAL_REQUEST"）
     */
    public String getType() {
        return type;
    }

    /**
     * 通知種別を設定する。
     *
     * @param type 通知種別
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 通知ステータスを返す。
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return 通知ステータス（"UNREAD" / "READ" / "ARCHIVED"）
     */
    public String getStatus() {
        return status;
    }

    /**
     * 通知ステータスを設定する。
     *
     * @param status 通知ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 優先度を返す。
     *
     * @return 優先度
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * 優先度を設定する。
     *
     * @param priority 優先度
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 参照先エンティティの種別を返す。
     *
     * @return 参照先エンティティ種別
     */
    public String getReferenceType() {
        return referenceType;
    }

    /**
     * 参照先エンティティの種別を設定する。
     *
     * @param referenceType 参照先エンティティ種別
     */
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    /**
     * 参照先エンティティのIDを返す。
     *
     * @return 参照先エンティティID
     */
    public Long getReferenceId() {
        return referenceId;
    }

    /**
     * 参照先エンティティのIDを設定する。
     *
     * @param referenceId 参照先エンティティID
     */
    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * 既読日時を返す。
     *
     * @return 既読日時
     */
    public Date getReadAt() {
        return readAt;
    }

    /**
     * 既読日時を設定する。
     *
     * @param readAt 既読日時
     */
    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    /**
     * 有効期限を返す。
     *
     * @return 有効期限
     */
    public Date getExpiresAt() {
        return expiresAt;
    }

    /**
     * 有効期限を設定する。
     *
     * @param expiresAt 有効期限
     */
    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * テンプレートIDを返す。
     * 技術的負債: 生の外部キー。NotificationTemplateエンティティの参照に移行予定。
     *
     * @return テンプレートID
     */
    public Long getTemplateId() {
        return templateId;
    }

    /**
     * テンプレートIDを設定する。
     *
     * @param templateId テンプレートID
     */
    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + getId() +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", priority=" + priority +
                '}';
    }
}
