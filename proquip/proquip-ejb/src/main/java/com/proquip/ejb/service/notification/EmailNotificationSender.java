package com.proquip.ejb.service.notification;

import com.proquip.ejb.entity.organization.UserProfile;
import com.proquip.ejb.entity.system.Notification;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


/**
 * メール通知送信クラス。
 * <p>
 * {@link AbstractNotificationSender}を継承し、メールによる通知送信を実装する。
 * </p>
 *
 * <p>【技術的負債】メール送信の実体が未実装（TODOコメントのみ）。
 * JavaMail API または Jakarta Mail の設定・実装が必要。
 * 現在はログ出力のみ行い、実際のメール送信は行わない。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.2.0
 */
@Stateless
public class EmailNotificationSender extends AbstractNotificationSender {

    @PersistenceContext
    private EntityManager em;

    /**
     * {@inheritDoc}
     *
     * <p>メール通知では、タイトルとメッセージに加えて送信先のメールアドレスが必要。</p>
     */
    @Override
    protected boolean validate(Notification notification) {
        if (!super.validate(notification)) {
            return false;
        }

        // メールアドレスの取得可否チェック
        Long userId = notification.getUserId();
        if (userId == null) {
            return false;
        }

        // ユーザーのメールアドレスが設定されているか確認
        UserProfile user = em.find(UserProfile.class, userId);
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            logger.warning("ユーザーのメールアドレスが未設定です。ユーザーID: " + userId);
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>メール送信の前処理。送信先アドレスのフォーマットチェック等を行う。</p>
     */
    @Override
    protected void preProcess(Notification notification) {
        super.preProcess(notification);
        logger.info("メール通知の前処理を実行。通知ID: " + notification.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>【技術的負債】メール送信の実体が未実装。
     * Jakarta Mail (旧JavaMail) による SMTP 送信を実装する必要がある。
     * 現在はログ出力のみで、実際のメールは送信されない。</p>
     */
    @Override
    protected boolean doSend(Notification notification) {
        Long userId = notification.getUserId();
        UserProfile user = em.find(UserProfile.class, userId);

        if (user == null) {
            logger.warning("ユーザーが見つかりません。ユーザーID: " + userId);
            return false;
        }

        String toAddress = user.getEmail();
        String subject = notification.getTitle();
        String body = notification.getMessage();

        // TODO: Jakarta Mail によるメール送信を実装する
        // 以下はスタブ実装
        // Session session = Session.getDefaultInstance(mailProperties);
        // MimeMessage message = new MimeMessage(session);
        // message.setFrom(new InternetAddress("noreply@proquip.example.com"));
        // message.setRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
        // message.setSubject(subject, "UTF-8");
        // message.setText(body, "UTF-8");
        // Transport.send(message);

        logger.info("【スタブ】メール送信: 宛先=" + toAddress
                + ", 件名=" + subject
                + ", 本文=" + (body != null && body.length() > 50 ? body.substring(0, 50) + "..." : body));

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postProcess(Notification notification) {
        super.postProcess(notification);
        logger.info("メール通知の後処理完了。通知ID: " + notification.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSenderType() {
        return "EMAIL";
    }
}
