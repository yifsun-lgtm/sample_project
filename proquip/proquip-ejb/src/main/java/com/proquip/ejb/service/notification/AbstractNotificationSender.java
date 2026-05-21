package com.proquip.ejb.service.notification;

import com.proquip.ejb.entity.system.Notification;

import java.util.logging.Logger;

/**
 * 通知送信の抽象基底クラス。
 * <p>
 * テンプレートメソッドパターンを使用し、通知送信のワークフローを定義する。
 * サブクラスは具体的な送信処理（doSend）を実装する。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化】
 * 実装クラスがEmailNotificationSenderとInAppNotificationSenderの2つしかないのに、
 * Abstract Factory + テンプレートメソッドパターンを適用している。
 * 単純なif分岐またはStrategyパターンで十分である。
 * バリデーション → 前処理 → 送信 → 後処理の流れは、
 * 2種類の送信方法に対してオーバーエンジニアリングである。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.2.0
 */
public abstract class AbstractNotificationSender {

    /** ロガー */
    protected final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * 通知を送信する（テンプレートメソッド）。
     *
     * <p>バリデーション → 前処理 → 送信 → 後処理の順で処理を実行する。</p>
     *
     * @param notification 送信対象の通知
     * @return 送信成功の場合true
     */
    public final boolean send(Notification notification) {
        if (notification == null) {
            logger.warning("通知がnullです。送信をスキップします。");
            return false;
        }

        // バリデーション
        if (!validate(notification)) {
            logger.warning("通知のバリデーションに失敗しました。通知ID: " + notification.getId());
            return false;
        }

        // 前処理
        preProcess(notification);

        // 送信
        boolean result = false;
        try {
            result = doSend(notification);
        } catch (Exception e) {
            logger.warning("通知送信中にエラーが発生しました: " + e.getMessage());
            onError(notification, e);
            return false;
        }

        // 後処理
        if (result) {
            postProcess(notification);
        }

        return result;
    }

    /**
     * 通知のバリデーションを行う。
     *
     * <p>サブクラスでオーバーライド可能。デフォルトではユーザーIDの存在チェックのみ。</p>
     *
     * @param notification 通知
     * @return バリデーションOKの場合true
     */
    protected boolean validate(Notification notification) {
        if (notification.getUserId() == null) {
            return false;
        }
        if (notification.getTitle() == null || notification.getTitle().isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 送信前の前処理を行う。
     *
     * <p>サブクラスでオーバーライド可能。デフォルトではログ出力のみ。</p>
     *
     * @param notification 通知
     */
    protected void preProcess(Notification notification) {
        logger.fine("通知の前処理を実行。送信先ユーザーID: " + notification.getUserId());
    }

    /**
     * 具体的な送信処理を実行する。
     *
     * <p>サブクラスで必ず実装する。</p>
     *
     * @param notification 送信対象の通知
     * @return 送信成功の場合true
     */
    protected abstract boolean doSend(Notification notification);

    /**
     * 送信後の後処理を行う。
     *
     * <p>サブクラスでオーバーライド可能。デフォルトではログ出力のみ。</p>
     *
     * @param notification 通知
     */
    protected void postProcess(Notification notification) {
        logger.fine("通知の後処理を実行。通知ID: " + notification.getId());
    }

    /**
     * 送信エラー時のハンドリングを行う。
     *
     * <p>サブクラスでオーバーライド可能。デフォルトではログ出力のみ。</p>
     *
     * @param notification 通知
     * @param error 発生した例外
     */
    protected void onError(Notification notification, Exception error) {
        logger.warning("通知送信エラー。通知ID: " + notification.getId()
                + ", エラー: " + error.getMessage());
    }

    /**
     * 送信方法の種別名を返す。
     *
     * @return 送信種別名（例: "EMAIL", "IN_APP"）
     */
    public abstract String getSenderType();
}
