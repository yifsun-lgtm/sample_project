package com.proquip.ejb.scheduler;

import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.system.Notification;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 発注納期超過チェックスケジューラ。
 *
 * <p>毎日午前7時に実行され、納品予定日を過ぎているにもかかわらず
 * まだ受入が完了していない発注を検出し、通知およびステータス更新を行う。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@link java.util.Calendar} を使用した日付演算を行っている。
 *       {@code java.time.LocalDate} に移行すべき。</li>
 *   <li>ステータス遷移のルールがこのスケジューラ内にハードコードされている。
 *       ステートマシンパターンに移行すべき。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Singleton
@Startup
public class OverdueOrderScheduler {

    private static final Logger logger = Logger.getLogger(OverdueOrderScheduler.class.getName());

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 初期化処理。アプリケーション起動時に実行される。
     */
    @PostConstruct
    public void init() {
        logger.info("発注納期超過チェックスケジューラが初期化されました");
    }

    /**
     * 納期超過発注チェックを実行するスケジュールメソッド。
     *
     * <p>毎日午前7時に実行される。納品予定日を過ぎた未受入の発注を検出し、
     * 通知作成およびステータス更新を行う。</p>
     *
     * <p>技術的負債 #6: {@code java.util.Calendar} による日付演算。
     * {@code java.time.LocalDate} に移行すべき。</p>
     */
    @Schedule(hour = "7", minute = "0", persistent = false)
    public void checkOverdueOrders() {
        logger.info("納期超過発注チェックを開始します");

        try {
            // 技術的負債 #6: java.util.Calendarによる日付取得
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();

            // 納品予定日が今日以前で、未受入の発注を検索
            TypedQuery<PurchaseOrder> query = em.createQuery(
                    "SELECT po FROM PurchaseOrder po "
                    + "WHERE po.expectedDeliveryDate < :today "
                    + "AND po.status IN ('SENT', 'APPROVED', 'PARTIALLY_RECEIVED') "
                    + "ORDER BY po.expectedDeliveryDate ASC",
                    PurchaseOrder.class);
            query.setParameter("today", today);

            List<PurchaseOrder> overdueOrders = query.getResultList();

            logger.info("納期超過発注数: " + overdueOrders.size());

            for (PurchaseOrder order : overdueOrders) {
                try {
                    processOverdueOrder(order, today);
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                            "納期超過発注処理中にエラーが発生しました: " + order.getPoNumber(), e);
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "納期超過発注チェック全体でエラーが発生しました", e);
        }

        logger.info("納期超過発注チェックが完了しました");
    }

    /**
     * 納期超過発注に対して通知作成とステータス更新を行う。
     *
     * <p>技術的負債 #6: {@code java.util.Calendar} による日数計算。</p>
     *
     * @param order 納期超過発注エンティティ
     * @param today 本日日付
     */
    private void processOverdueOrder(PurchaseOrder order, Date today) {
        // 技術的負債 #6: Calendarによる超過日数の計算
        Calendar orderCal = Calendar.getInstance();
        orderCal.setTime(order.getExpectedDeliveryDate());

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);

        // 超過日数の計算（技術的負債: ミリ秒から日数に変換する不正確な計算）
        long diffMillis = today.getTime() - order.getExpectedDeliveryDate().getTime();
        int overdueDays = (int) (diffMillis / (24L * 60 * 60 * 1000));

        // 通知の作成
        Notification notification = new Notification();
        notification.setTitle("納期超過アラート: " + order.getPoNumber());
        notification.setMessage(
                "発注「" + order.getPoNumber() + "」が納品予定日を " + overdueDays + " 日超過しています。\n"
                + "サプライヤー: " + (order.getSupplier() != null
                        ? order.getSupplier().getName() : "不明") + "\n"
                + "納品予定日: " + order.getExpectedDeliveryDate() + "\n"
                + "現在ステータス: " + order.getStatus() + "\n"
                + "合計金額: " + order.getTotalAmount() + " " + order.getCurrency());

        // 超過日数に応じて通知タイプと優先度を設定
        if (overdueDays > 30) {
            notification.setType("ERROR");
            notification.setPriority(10);
        } else if (overdueDays > 7) {
            notification.setType("WARNING");
            notification.setPriority(7);
        } else {
            notification.setType("INFO");
            notification.setPriority(5);
        }

        notification.setStatus("UNREAD");
        notification.setReferenceType("PurchaseOrder");
        notification.setReferenceId(order.getId());
        notification.setUserId(order.getBuyerId() != null ? order.getBuyerId() : 1L);

        em.persist(notification);

        logger.info("納期超過通知を作成しました: " + order.getPoNumber()
                + " 超過日数: " + overdueDays + "日");
    }
}
