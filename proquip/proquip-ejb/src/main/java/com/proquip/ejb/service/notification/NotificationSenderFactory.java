package com.proquip.ejb.service.notification;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.logging.Logger;

/**
 * 通知送信者のファクトリクラス。
 * <p>
 * 通知種別（メール、アプリ内）に応じた{@link AbstractNotificationSender}の
 * 具象インスタンスを返す。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化】
 * 実装が2つしかないのにAbstract Factoryパターンを適用している。
 * 単純なif分岐またはMapベースのルックアップで十分である。
 * さらに、CDIの@Anyや@Qualifier等を使えばファクトリクラス自体が不要。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.2.0
 */
@Stateless
public class NotificationSenderFactory {

    private static final Logger logger = Logger.getLogger(NotificationSenderFactory.class.getName());

    @EJB
    private EmailNotificationSender emailSender;

    @EJB
    private InAppNotificationSender inAppSender;

    /**
     * 送信種別に対応する通知送信者を返す。
     *
     * <p>【技術的負債 #10】新しい送信方法が追加されるたびにこのメソッドの
     * if-else分岐を修正する必要がある。OCP（開放閉鎖原則）に違反。
     * CDIの@Qualifierや、送信者をMapで管理する方式に移行すべき。</p>
     *
     * @param senderType 送信種別（"EMAIL", "IN_APP"）
     * @return 対応する通知送信者
     * @throws IllegalArgumentException 未対応の送信種別が指定された場合
     */
    public AbstractNotificationSender getSender(String senderType) {
        if (senderType == null) {
            throw new IllegalArgumentException("送信種別がnullです。");
        }

        // 技術的負債 #14: マジックストリングによる分岐
        if ("EMAIL".equals(senderType)) {
            return emailSender;
        } else if ("IN_APP".equals(senderType)) {
            return inAppSender;
        } else {
            logger.warning("未対応の送信種別が指定されました: " + senderType);
            throw new IllegalArgumentException("未対応の送信種別: " + senderType);
        }
    }

    /**
     * 指定された送信種別がサポートされているかを判定する。
     *
     * @param senderType 送信種別
     * @return サポートされている場合true
     */
    public boolean isSupportedType(String senderType) {
        // 技術的負債: ハードコード判定
        return "EMAIL".equals(senderType) || "IN_APP".equals(senderType);
    }
}
