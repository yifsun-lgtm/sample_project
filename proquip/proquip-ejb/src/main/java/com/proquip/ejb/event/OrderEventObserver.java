package com.proquip.ejb.event;

import com.proquip.ejb.entity.organization.UserProfile;
import com.proquip.ejb.entity.system.AuditLog;
import com.proquip.ejb.entity.system.Notification;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 発注イベントオブザーバー。
 *
 * <p>CDIイベントとして発火された {@link OrderEvent} を監視し、
 * イベント種別に応じた通知作成および監査ログ記録を行う。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>イベント処理が同期的に実行される。本来は非同期 ({@code @ObservesAsync})
 *       で処理すべきだが、現在の実装ではトランザクション内で同期実行されている。
 *       これにより、イベント処理が遅い場合に元の操作全体が遅延する。</li>
 *   <li>通知先ユーザーの解決ロジックがハードコードされている。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class OrderEventObserver {

    private static final Logger logger = Logger.getLogger(OrderEventObserver.class.getName());

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 発注イベントを監視し、イベント種別に応じた処理を実行する。
     *
     * <p>TODO: 本来はAsyncで処理すべき。現在は同期実行のため、
     * イベント処理が遅い場合に元の操作のレスポンスタイムに影響する。</p>
     *
     * @param event 発注イベント
     */
    // TODO: 本来はAsyncで処理すべき
    public void onOrderEvent(@Observes OrderEvent event) {
        logger.info("発注イベントを受信しました: " + event);

        try {
            // 監査ログの記録
            createAuditLog(event);

            // イベント種別に応じた通知の作成
            switch (event.getEventType()) {
                case "CREATED":
                    createNotification(event, "新規発注が作成されました",
                            "INFO", 3);
                    break;
                case "SUBMITTED":
                    createNotification(event, "発注が承認依頼されました",
                            "APPROVAL_REQUEST", 7);
                    break;
                case "APPROVED":
                    createNotification(event, "発注が承認されました",
                            "INFO", 5);
                    break;
                case "REJECTED":
                    createNotification(event, "発注が却下されました",
                            "WARNING", 8);
                    break;
                case "SENT":
                    createNotification(event, "発注がサプライヤーに送信されました",
                            "INFO", 4);
                    break;
                case "RECEIVED":
                    createNotification(event, "発注品が受入されました",
                            "INFO", 5);
                    break;
                case "CANCELLED":
                    createNotification(event, "発注がキャンセルされました",
                            "WARNING", 6);
                    break;
                default:
                    logger.warning("未知の発注イベント種別: " + event.getEventType());
                    createNotification(event, "発注ステータスが更新されました",
                            "INFO", 3);
                    break;
            }

        } catch (Exception e) {
            // 技術的負債: イベント処理のエラーを握りつぶしている
            logger.log(Level.SEVERE, "発注イベント処理中にエラーが発生しました: " + event, e);
        }
    }

    /**
     * 発注イベントに基づいて監査ログを作成する。
     *
     * @param event 発注イベント
     */
    private void createAuditLog(OrderEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("PurchaseOrder");
        auditLog.setEntityId(event.getOrderId());
        auditLog.setAction(event.getEventType());
        auditLog.setPerformedBy(0L);
        auditLog.setPerformedAt(event.getTimestamp() != null ? event.getTimestamp() : new Date());
        auditLog.setAdditionalInfo("CDIイベント経由で記録");

        em.persist(auditLog);

        logger.fine("発注監査ログを記録しました: orderId=" + event.getOrderId()
                + " action=" + event.getEventType());
    }

    /**
     * 発注イベントに基づいて通知を作成する。
     *
     * @param event    発注イベント
     * @param message  通知メッセージ
     * @param type     通知タイプ
     * @param priority 優先度
     */
    private void createNotification(OrderEvent event, String message,
                                     String type, int priority) {
        Notification notification = new Notification();
        notification.setTitle("発注 #" + event.getOrderId() + ": " + event.getEventType());
        notification.setMessage(message + "\n"
                + "発注ID: " + event.getOrderId() + "\n"
                + "操作者: " + event.getUserId() + "\n"
                + "日時: " + event.getTimestamp());
        notification.setType(type);
        notification.setStatus("UNREAD");
        notification.setPriority(priority);
        notification.setReferenceType("PurchaseOrder");
        notification.setReferenceId(event.getOrderId());
        // イベントのuserIdからUserProfileのIDを解決する
        notification.setUserId(resolveNotificationUserId(event));

        em.persist(notification);
    }

    /**
     * OrderEventのuserId（keycloakId）からUserProfileのIDを解決する。
     *
     * <p>EJBコンテキストではSecurityContextが利用できないため、
     * イベントに含まれるuserIdを使用してUserProfileを検索する。</p>
     *
     * @param event 発注イベント
     * @return ユーザーID（解決できない場合は1L）
     */
    private Long resolveNotificationUserId(OrderEvent event) {
        if (event.getUserId() != null && !event.getUserId().isEmpty()) {
            try {
                List<UserProfile> users = em.createQuery(
                        "SELECT u FROM UserProfile u WHERE u.keycloakId = :keycloakId",
                        UserProfile.class)
                        .setParameter("keycloakId", event.getUserId())
                        .getResultList();
                if (!users.isEmpty()) {
                    return users.get(0).getId();
                }
            } catch (Exception e) {
                logger.warning("イベントのuserId=" + event.getUserId()
                        + " からUserProfileを解決できませんでした: " + e.getMessage());
            }
        }
        // フォールバック: ユーザーが解決できない場合はデフォルト値を使用
        return 1L;
    }
}
