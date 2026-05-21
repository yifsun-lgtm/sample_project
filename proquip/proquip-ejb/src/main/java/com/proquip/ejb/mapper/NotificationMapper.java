package com.proquip.ejb.mapper;

import com.proquip.ejb.entity.system.Notification;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 通知エンティティとDTO間の手書きマッパークラス。
 *
 * <p>技術的負債 #10: MapStructを使用せず、手動マッピングを行っている。
 * 通知は現在フラットなDTOを持たず、エンティティを直接APIで返しているため
 * このマッパーは限定的な変換しか行わない。</p>
 *
 * <p>技術的負債: 将来的にNotificationDtoを作成し、
 * ユーザーIDからユーザー名への変換を行うべき。</p>
 *
 * @author ProQuip開発チーム
 */
public class NotificationMapper {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(NotificationMapper.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public NotificationMapper() {
    }

    /**
     * 通知エンティティからシンプルなMap形式のデータに変換する。
     *
     * <p>技術的負債: 専用のNotificationDtoが存在しないため、
     * 主要フィールドの変換のみを行う簡易変換メソッド。
     * 将来的にNotificationDtoを作成して適切なマッピングを行うべき。</p>
     *
     * <p>技術的負債: userIdからユーザー名への変換が行えない。
     * API応答時にユーザー名を含める場合は、呼び出し側でUserProfileを
     * 検索して手動設定する必要がある。</p>
     *
     * @param entity 通知エンティティ
     * @return 通知サマリの文字列表現（entityがnullの場合はnull）
     */
    public String toSummary(Notification entity) {
        if (entity == null) {
            return null;
        }

        LOG.log(Level.FINE, "Notification -> summary 変換: id={0}", entity.getId());

        // 技術的負債: 文字列連結による簡易的な変換
        // 本来はNotificationDtoを返すべき
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(entity.getType()).append("] ");
        sb.append(entity.getTitle());
        if (entity.getStatus() != null) {
            sb.append(" (").append(entity.getStatus()).append(")");
        }
        return sb.toString();
    }

    /**
     * 通知エンティティのステータスを更新するための変換ヘルパー。
     *
     * <p>既読にする等のステータス変更操作で使用される。
     * マッパーの役割を逸脱しているが、サービス層から呼ばれている。</p>
     *
     * @param entity 通知エンティティ
     * @param newStatus 新しいステータス
     */
    public void updateStatus(Notification entity, String newStatus) {
        if (entity == null || newStatus == null) {
            return;
        }

        LOG.log(Level.FINE, "Notification ステータス更新: id={0}, {1} -> {2}",
                new Object[]{entity.getId(), entity.getStatus(), newStatus});

        entity.setStatus(newStatus);
    }
}
