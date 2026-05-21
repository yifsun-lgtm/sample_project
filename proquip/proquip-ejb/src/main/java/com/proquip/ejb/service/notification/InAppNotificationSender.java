package com.proquip.ejb.service.notification;

import com.proquip.ejb.entity.system.Notification;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * アプリ内通知送信クラス。
 * <p>
 * {@link AbstractNotificationSender}を継承し、データベースへの保存による
 * アプリ内通知を実装する。ユーザーがWebアプリにログインした際に
 * 未読通知として表示される。
 * </p>
 *
 * <p>【技術的負債 #10】この処理はNotificationServiceBean内の
 * em.persist()呼び出しと実質的に重複している。
 * AbstractNotificationSenderの階層構造自体が過度な抽象化。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.2.0
 */
@Stateless
public class InAppNotificationSender extends AbstractNotificationSender {

    @PersistenceContext
    private EntityManager em;

    /**
     * {@inheritDoc}
     *
     * <p>アプリ内通知はDB永続化で実現する。
     * 通知エンティティは既にNotificationServiceBeanで永続化されているため、
     * このメソッドではステータスの最終確認と更新のみ行う。</p>
     *
     * <p>【技術的負債 #10】NotificationServiceBeanのem.persist()と処理が重複。
     * 送信は既に完了しているため、ここではログ出力のみ。</p>
     */
    @Override
    protected boolean doSend(Notification notification) {
        // 通知エンティティは既に永続化されているため、
        // ここではステータスの確認のみ行う
        if (notification.getId() != null) {
            Notification persisted = em.find(Notification.class, notification.getId());
            if (persisted != null) {
                // ステータスがUNREADであることを確認
                if (!"UNREAD".equals(persisted.getStatus())) {
                    persisted.setStatus("UNREAD");
                    em.merge(persisted);
                }
            }
        }

        logger.info("アプリ内通知送信完了。ユーザーID: " + notification.getUserId()
                + ", タイトル: " + notification.getTitle());

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postProcess(Notification notification) {
        super.postProcess(notification);
        // 技術的負債: ここでWebSocketプッシュ通知を行うべきだが未実装
        // TODO: WebSocketによるリアルタイム通知を実装する
        logger.fine("アプリ内通知の後処理完了。通知ID: " + notification.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSenderType() {
        return "IN_APP";
    }
}
