package com.proquip.ejb.scheduler;

import com.proquip.ejb.entity.pricing.Budget;
import com.proquip.ejb.entity.system.Notification;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 予算アラートスケジューラ。
 *
 * <p>毎日午前8時に実行され、予算の消化率をチェックし、
 * 80%超過および95%超過の場合にそれぞれ通知を作成する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>アラート閾値（80%、95%）がハードコードされている。
 *       システム設定から読み込むべき。</li>
 *   <li>通知の重複チェックが行われていない。毎日実行されるため、
 *       同じ予算に対して重複通知が作成される可能性がある。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Singleton
@Startup
public class BudgetAlertScheduler {

    private static final Logger logger = Logger.getLogger(BudgetAlertScheduler.class.getName());

    /**
     * 警告閾値（消化率80%）。
     * 技術的負債 #4: ハードコードされた閾値。システム設定から読み込むべき。
     */
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("80.00");

    /**
     * 危険閾値（消化率95%）。
     * 技術的負債 #4: ハードコードされた閾値。システム設定から読み込むべき。
     */
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("95.00");

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 初期化処理。アプリケーション起動時に実行される。
     */
    @PostConstruct
    public void init() {
        logger.info("予算アラートスケジューラが初期化されました");
    }

    /**
     * 予算消化率チェックを実行するスケジュールメソッド。
     *
     * <p>毎日午前8時に実行される。アクティブな全予算を走査し、
     * 消化率が閾値を超えた予算に対して通知を作成する。</p>
     */
    @Schedule(hour = "8", minute = "0", persistent = false)
    public void checkBudgetUtilization() {
        logger.info("予算消化率チェックを開始します");

        try {
            List<Budget> activeBudgets = em.createNamedQuery(
                    "Budget.findByStatus", Budget.class)
                    .setParameter("status", "ACTIVE")
                    .getResultList();

            logger.info("チェック対象予算数: " + activeBudgets.size());

            for (Budget budget : activeBudgets) {
                try {
                    checkBudget(budget);
                } catch (Exception e) {
                    logger.log(Level.SEVERE,
                            "予算チェック中にエラーが発生しました: " + budget.getName(), e);
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "予算消化率チェック全体でエラーが発生しました", e);
        }

        logger.info("予算消化率チェックが完了しました");
    }

    /**
     * 個別の予算に対して消化率をチェックし、必要に応じて通知を作成する。
     *
     * @param budget チェック対象の予算エンティティ
     */
    private void checkBudget(Budget budget) {
        if (budget.getTotalAmount() == null
                || budget.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.warning("予算総額が0以下です: " + budget.getName());
            return;
        }

        BigDecimal spentAmount = budget.getSpentAmount() != null
                ? budget.getSpentAmount() : BigDecimal.ZERO;

        // 消化率を計算（パーセント）
        BigDecimal utilizationPercent = spentAmount
                .multiply(new BigDecimal("100"))
                .divide(budget.getTotalAmount(), 2, RoundingMode.HALF_UP);

        // 技術的負債 #4: ハードコードされた閾値でチェック
        if (utilizationPercent.compareTo(CRITICAL_THRESHOLD) >= 0) {
            createBudgetAlert(budget, utilizationPercent, "ERROR",
                    "予算超過危険", 9);
        } else if (utilizationPercent.compareTo(WARNING_THRESHOLD) >= 0) {
            createBudgetAlert(budget, utilizationPercent, "WARNING",
                    "予算消化率警告", 6);
        }
    }

    /**
     * 予算アラート通知を作成する。
     *
     * @param budget           対象予算
     * @param utilizationPercent 消化率（パーセント）
     * @param type             通知タイプ（WARNING/ERROR）
     * @param titlePrefix      通知タイトルのプレフィックス
     * @param priority         優先度
     */
    private void createBudgetAlert(Budget budget, BigDecimal utilizationPercent,
                                    String type, String titlePrefix, int priority) {
        Notification notification = new Notification();
        notification.setTitle(titlePrefix + ": " + budget.getName());
        notification.setMessage(
                "予算「" + budget.getName() + "」の消化率が " + utilizationPercent + "% に達しました。\n"
                + "会計年度: " + budget.getFiscalYear() + "\n"
                + "予算総額: " + budget.getTotalAmount() + "円\n"
                + "消化済み: " + budget.getSpentAmount() + "円\n"
                + "残額: " + budget.getTotalAmount().subtract(
                        budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO)
                + "円");
        notification.setType(type);
        notification.setStatus("UNREAD");
        notification.setPriority(priority);
        notification.setReferenceType("Budget");
        notification.setReferenceId(budget.getId());
        // 技術的負債: 固定ユーザーIDへの通知
        notification.setUserId(1L);

        em.persist(notification);

        logger.info(titlePrefix + " 通知作成: " + budget.getName()
                + " 消化率: " + utilizationPercent + "%");
    }
}
