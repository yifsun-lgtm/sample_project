package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.system.Notification;
import com.proquip.ejb.entity.system.NotificationTemplate;
import com.proquip.ejb.service.notification.NotificationSenderFactory;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通知管理サービスBean。
 * <p>
 * ユーザーへの通知送信、既読管理、テンプレート処理を提供する。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化】
 * 2種類の送信方法（メール・アプリ内）しかないのに、
 * AbstractNotificationSender → EmailNotificationSender / InAppNotificationSender
 * というAbstract Factoryパターンを適用している。
 * Strategy パターンまたは単純なif分岐で十分である。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Stateless
public class NotificationServiceBean {

    private static final Logger logger = Logger.getLogger(NotificationServiceBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    @EJB
    private NotificationSenderFactory senderFactory;

    // ========================================================================
    // 通知送信
    // ========================================================================

    /**
     * ユーザーに通知を送信する。
     *
     * <p>アプリ内通知を作成し、設定に応じてメール通知も送信する。</p>
     *
     * @param userId 送信先ユーザーID
     * @param title 通知タイトル
     * @param message 通知メッセージ
     * @param type 通知種別（INFO, WARNING, ERROR, APPROVAL_REQUEST）
     * @param referenceType 参照先エンティティ種別
     * @param referenceId 参照先エンティティID
     */
    public void sendNotification(Long userId, String title, String message,
                                 String type, String referenceType, Long referenceId) {
        if (userId == null) {
            logger.warning("通知送信先のユーザーIDがnullです。通知をスキップします。");
            return;
        }

        try {
            // アプリ内通知の作成
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle(title != null ? title : "通知");
            notification.setMessage(message != null ? message : "");
            notification.setType(type != null ? type : "INFO");
            notification.setStatus("UNREAD");
            notification.setReferenceType(referenceType);
            notification.setReferenceId(referenceId);

            // 優先度の設定
            // 技術的負債 #14: マジックストリングによる種別判定
            if ("ERROR".equals(type)) {
                notification.setPriority(3);
            } else if ("WARNING".equals(type) || "APPROVAL_REQUEST".equals(type)) {
                notification.setPriority(2);
            } else {
                notification.setPriority(1);
            }

            // 有効期限の設定（30日後）
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 30);
            notification.setExpiresAt(cal.getTime());

            notification.setCreatedBy(AppConstants.SYSTEM_USER);
            notification.setUpdatedBy(AppConstants.SYSTEM_USER);

            em.persist(notification);

            // 技術的負債 #10: 過度な抽象化 — ファクトリー経由で送信
            try {
                senderFactory.getSender("IN_APP").send(notification);
            } catch (Exception e) {
                logger.warning("アプリ内通知の送信処理でエラー: " + e.getMessage());
            }

            // メール通知（承認依頼の場合のみ）
            if ("APPROVAL_REQUEST".equals(type)) {
                try {
                    senderFactory.getSender("EMAIL").send(notification);
                } catch (Exception e) {
                    // 技術的負債 #7: メール送信失敗を握りつぶし
                    logger.warning("メール通知の送信に失敗しました: " + e.getMessage());
                }
            }

            logger.info("通知送信完了。ユーザーID: " + userId + ", タイトル: " + title);

        } catch (Exception e) {
            // 技術的負債 #7: 通知失敗を握りつぶし
            logger.log(Level.SEVERE, "通知の送信に失敗しました。", e);
        }
    }

    /**
     * 複数ユーザーに一括通知を送信する。
     *
     * <p>【技術的負債 #3】N+1パターン。各ユーザーに個別に通知を送信している。
     * バッチ挿入に最適化すべき。</p>
     *
     * @param userIds 送信先ユーザーIDのリスト
     * @param title 通知タイトル
     * @param message 通知メッセージ
     * @param type 通知種別
     */
    public void sendBulkNotifications(List<Long> userIds, String title,
                                      String message, String type) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failCount = 0;

        // 技術的負債 #3: N+1パターン — 個別にsendNotificationを呼び出し
        for (int i = 0; i < userIds.size(); i++) {
            try {
                sendNotification(userIds.get(i), title, message, type, null, null);
                successCount++;
            } catch (Exception e) {
                failCount++;
                logger.warning("一括通知送信失敗。ユーザーID: " + userIds.get(i));
            }
        }

        logger.info("一括通知送信完了。成功: " + successCount + ", 失敗: " + failCount);
    }

    // ========================================================================
    // 既読管理
    // ========================================================================

    /**
     * 通知を既読にする。
     *
     * @param notificationId 通知ID
     * @param userId ユーザーID（権限チェック用）
     */
    public void markAsRead(Long notificationId, Long userId) {
        if (notificationId == null) {
            return;
        }

        Notification notification = em.find(Notification.class, notificationId);
        if (notification == null) {
            throw new EntityNotFoundException("Notification", notificationId);
        }

        // ユーザーの権限チェック
        if (userId != null && !userId.equals(notification.getUserId())) {
            logger.warning("通知の所有者と異なるユーザーが既読操作を試みました。通知ID: "
                    + notificationId + ", ユーザーID: " + userId);
            return;
        }

        if ("READ".equals(notification.getStatus())) {
            return; // 既に既読
        }

        notification.setStatus("READ");
        notification.setReadAt(new Date());
        notification.setUpdatedBy(userId != null ? userId.toString() : AppConstants.SYSTEM_USER);
        em.merge(notification);
    }

    /**
     * 指定ユーザーの全通知を既読にする。
     *
     * @param userId ユーザーID
     * @return 既読にした件数
     */
    public int markAllAsRead(Long userId) {
        if (userId == null) {
            return 0;
        }

        // 技術的負債: バルクアップデートを使用すべきだが、個別にmmergeしている
        int updated = em.createQuery(
                "UPDATE Notification n SET n.status = 'READ', n.readAt = :now " +
                "WHERE n.userId = :userId AND n.status = 'UNREAD'")
                .setParameter("now", new Date())
                .setParameter("userId", userId)
                .executeUpdate();

        logger.info("一括既読処理完了。ユーザーID: " + userId + ", 更新件数: " + updated);
        return updated;
    }

    // ========================================================================
    // 通知取得
    // ========================================================================

    /**
     * 未読通知を取得する。
     *
     * @param userId ユーザーID
     * @return 未読通知のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Notification> getUnreadNotifications(Long userId) {
        if (userId == null) {
            return new ArrayList<Notification>();
        }

        return em.createNamedQuery("Notification.findUnreadByUserId")
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * 未読通知件数を取得する。
     *
     * @param userId ユーザーID
     * @return 未読件数
     */
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0;
        }

        Long count = (Long) em.createNamedQuery("Notification.countUnread")
                .setParameter("userId", userId)
                .getSingleResult();

        return count != null ? count : 0;
    }

    // ========================================================================
    // テンプレート処理
    // ========================================================================

    /**
     * テンプレートからメッセージを生成する。
     *
     * <p>{{placeholder}} 形式のプレースホルダーを実際の値に置換する。</p>
     *
     * <p>【技術的負債】String.replace()による単純な置換。
     * テンプレートエンジン（Thymeleaf, FreeMarker等）を使用すべき。</p>
     *
     * @param templateCode テンプレートコード
     * @param parameters プレースホルダーと値のマップ
     * @return 処理済みメッセージ
     */
    @SuppressWarnings("unchecked")
    public String processTemplate(String templateCode, Map<String, String> parameters) {
        if (templateCode == null || templateCode.isEmpty()) {
            return "";
        }

        // テンプレート取得
        List<NotificationTemplate> templates = em.createNamedQuery("NotificationTemplate.findByCode")
                .setParameter("templateCode", templateCode)
                .getResultList();

        if (templates == null || templates.isEmpty()) {
            logger.warning("テンプレートが見つかりません: " + templateCode);
            return "";
        }

        NotificationTemplate template = templates.get(0);
        String body = template.getBodyText();

        if (body == null) {
            return "";
        }

        // 技術的負債: String.replace()によるプレースホルダー置換
        // テンプレートエンジンに移行すべき
        if (parameters != null) {
            // 技術的負債 #6: for-indexループでMap.entrySetをイテレート
            List<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>(
                    parameters.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, String> entry = entries.get(i);
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                body = body.replace(placeholder, value);
            }
        }

        return body;
    }

    // ========================================================================
    // クリーンアップ
    // ========================================================================

    /**
     * 有効期限切れの通知を削除する。
     *
     * <p>スケジューラーからの定期実行を想定。</p>
     *
     * @return 削除した件数
     */
    public int cleanupExpiredNotifications() {
        Date now = new Date();

        int deleted = em.createQuery(
                "DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
                .setParameter("now", now)
                .executeUpdate();

        logger.info("期限切れ通知のクリーンアップ完了。削除件数: " + deleted);
        return deleted;
    }
}
