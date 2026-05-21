package com.proquip.ejb.scheduler;

import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.system.Notification;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 在庫不足アラートスケジューラ。
 *
 * <p>毎日午前6時に実行され、全在庫品目の在庫数を再発注点と比較し、
 * 在庫不足の品目に対して通知を作成する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>for-indexループで {@code List.get(i)} を使用している。
 *       拡張for文またはIteratorを使用すべき。</li>
 *   <li>{@code Exception} を広範にキャッチして握りつぶしている。</li>
 *   <li>メール送信先がハードコードされている。</li>
 *   <li>閾値やアラート設定が設定ファイルから読み込まれていない。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Singleton
@Startup
public class LowStockAlertScheduler {

    private static final Logger logger = Logger.getLogger(LowStockAlertScheduler.class.getName());

    /**
     * ハードコードされたアラート送信先メールアドレス。
     * 技術的負債 #4, #19: 送信先がソースコードに埋め込まれている。
     * システム設定やデータベースから読み込むべき。
     */
    private static final List<String> ALERT_RECIPIENTS = Arrays.asList(
            "procurement@proquip.example.com",
            "inventory-mgr@proquip.example.com",
            "tanaka.taro@proquip.example.com"
    );

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 初期化処理。アプリケーション起動時に実行される。
     */
    @PostConstruct
    public void init() {
        logger.info("在庫不足アラートスケジューラが初期化されました");
    }

    /**
     * 在庫不足チェックを実行するスケジュールメソッド。
     *
     * <p>毎日午前6時に実行される。全在庫品目を走査し、
     * 手持在庫数が再発注点以下の品目に対して通知を作成する。</p>
     *
     * <p>技術的負債:
     * <ul>
     *   <li>for-indexループで {@code get(i)} を使用。LinkedListの場合O(n)アクセス。</li>
     *   <li>{@code Exception} を広範にキャッチし、ログ出力のみで処理を継続。</li>
     * </ul>
     * </p>
     */
    @Schedule(hour = "6", minute = "0", persistent = false)
    public void checkLowStock() {
        logger.info("在庫不足チェックを開始します");

        try {
            List<InventoryItem> lowStockItems = em.createNamedQuery(
                    "InventoryItem.findBelowReorderPoint", InventoryItem.class)
                    .getResultList();

            logger.info("在庫不足品目数: " + lowStockItems.size());

            // 技術的負債 #6: for-indexループでList.get(i)を使用
            // 拡張for文を使用すべき
            for (int i = 0; i < lowStockItems.size(); i++) {
                try {
                    InventoryItem item = lowStockItems.get(i);
                    processLowStockItem(item);
                } catch (Exception e) {
                    // 技術的負債 #7: Exceptionを広範にキャッチし、ログ出力のみで継続
                    // 個別のアイテム処理失敗で全体が停止しないようにしているが、
                    // エラーの種類によっては全体を停止すべきケースもある
                    logger.log(Level.SEVERE,
                            "在庫不足アラート処理中にエラーが発生しました（インデックス: " + i + "）", e);
                }
            }

            // アラートメール送信
            sendAlertSummary(lowStockItems.size());

        } catch (Exception e) {
            // 技術的負債 #7: 最外部でもExceptionを広範にキャッチ
            logger.log(Level.SEVERE, "在庫不足チェック全体でエラーが発生しました", e);
        }

        logger.info("在庫不足チェックが完了しました");
    }

    /**
     * 在庫不足品目に対して通知を作成する。
     *
     * @param item 在庫不足の在庫品目
     */
    private void processLowStockItem(InventoryItem item) {
        String productName = item.getProduct() != null ? item.getProduct().getName() : "不明";
        String warehouseName = item.getWarehouse() != null ? item.getWarehouse().toString() : "不明";

        Notification notification = new Notification();
        notification.setTitle("在庫不足アラート: " + productName);
        // reorderPoint/reorderQuantity fields were removed from InventoryItem (DDL alignment)
        notification.setMessage(
                "商品「" + productName + "」の在庫が低下しています。\n"
                + "倉庫: " + warehouseName + "\n"
                + "現在在庫: " + item.getQuantityOnHand());
        notification.setType("WARNING");
        notification.setStatus("UNREAD");
        notification.setPriority(7);
        notification.setReferenceType("InventoryItem");
        notification.setReferenceId(item.getId());
        // 技術的負債: 固定ユーザーIDへの通知。動的な通知先の仕組みが必要
        notification.setUserId(1L);

        em.persist(notification);

        logger.info("在庫不足通知を作成しました: " + productName
                + " 在庫: " + item.getQuantityOnHand());
    }

    /**
     * アラートサマリーのメール送信を行う（スタブ実装）。
     *
     * <p>技術的負債 #4, #19: 送信先がハードコードされている。</p>
     *
     * @param alertCount アラート件数
     */
    private void sendAlertSummary(int alertCount) {
        if (alertCount == 0) {
            return;
        }

        // 技術的負債 #4, #19: ハードコードされた送信先
        for (String recipient : ALERT_RECIPIENTS) {
            logger.info("在庫不足アラートサマリーメール送信先: " + recipient
                    + " アラート件数: " + alertCount);
            // TODO: 実際のメール送信処理を実装
        }
    }
}
