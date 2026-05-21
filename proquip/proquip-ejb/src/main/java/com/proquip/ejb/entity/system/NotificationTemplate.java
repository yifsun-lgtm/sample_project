package com.proquip.ejb.entity.system;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * 通知テンプレートを表すエンティティ。
 *
 * <p>通知メッセージの雛型を管理する。テンプレート本文には
 * {@code {{userName}}} や {@code {{orderNumber}}} のようなプレースホルダーを
 * 含めることができ、通知生成時に実際の値に置換される。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code type} フィールドが文字列型で定義されている。本来はEnumを使用すべき。</li>
 *   <li>プレースホルダーの定義がテンプレート本文に埋め込まれており、
 *       利用可能なプレースホルダー一覧が型安全に管理されていない。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "notification_template")
@NamedQueries({
    @NamedQuery(
        name = "NotificationTemplate.findByCode",
        query = "SELECT t FROM NotificationTemplate t WHERE t.templateCode = :templateCode"
    ),
    @NamedQuery(
        name = "NotificationTemplate.findActiveByEventType",
        query = "SELECT t FROM NotificationTemplate t WHERE t.eventType = :eventType AND t.active = true ORDER BY t.name"
    )
})
public class NotificationTemplate extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel = "BOTH";

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body_text", nullable = false, columnDefinition = "text")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "text")
    private String bodyHtml;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale = "ja";

    /**
     * デフォルトコンストラクタ。
     */
    public NotificationTemplate() {
        super();
    }

    // --- Getter / Setter ---

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBodyText() { return bodyText; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }

    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    @Override
    public String toString() {
        return "NotificationTemplate{id=" + getId() + ", templateCode='" + templateCode + "', eventType='" + eventType + "', active=" + active + '}';
    }
}
