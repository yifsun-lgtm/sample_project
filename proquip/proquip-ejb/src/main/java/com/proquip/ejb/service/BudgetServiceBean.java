package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.BusinessException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.pricing.Budget;
import com.proquip.ejb.entity.pricing.BudgetLineItem;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 予算管理サービスBean。
 * <p>
 * 部門別・年度別の予算のCRUD操作、配賦、消化チェック、年度クローズを提供する。
 * </p>
 *
 * <p>【技術的負債】
 * <ul>
 *   <li>マジックストリングによるステータス管理。</li>
 *   <li>for-indexループの多用。</li>
 *   <li>Calendar APIの使用（java.time移行すべき）。</li>
 *   <li>予算チェックと引当が非アトミック（トランザクション分離レベルの問題）。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Stateless
public class BudgetServiceBean {

    private static final Logger logger = Logger.getLogger(BudgetServiceBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private NotificationServiceBean notificationService;

    // ========================================================================
    // CRUD操作
    // ========================================================================

    /**
     * 予算を新規作成する。
     *
     * @param budget 予算エンティティ
     * @return 永続化された予算
     * @throws ValidationException バリデーションエラー
     */
    public Budget createBudget(Budget budget) {
        if (budget == null) {
            throw new ValidationException("budget", "予算情報は必須です。");
        }
        if (budget.getName() == null || budget.getName().isEmpty()) {
            throw new ValidationException("name", "予算名は必須です。");
        }
        if (budget.getFiscalYear() == null) {
            throw new ValidationException("fiscalYear", "会計年度は必須です。");
        }
        if (budget.getTotalAmount() == null
                || budget.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("totalAmount", "予算総額は0より大きい値を指定してください。");
        }

        // 同一部門・同一年度の予算重複チェック
        if (budget.getDepartmentId() != null) {
            Budget existing = findByDepartmentAndYear(
                    budget.getDepartmentId(), budget.getFiscalYear());
            if (existing != null) {
                throw new ValidationException("budget",
                        "同一部門・同一年度の予算が既に存在します。部門ID: " + budget.getDepartmentId()
                        + ", 年度: " + budget.getFiscalYear());
            }
        }

        budget.setStatus("DRAFT");
        budget.setAllocatedAmount(BigDecimal.ZERO);
        budget.setSpentAmount(BigDecimal.ZERO);
        budget.setCreatedBy(AppConstants.SYSTEM_USER);
        budget.setUpdatedBy(AppConstants.SYSTEM_USER);

        em.persist(budget);

        auditService.logAction("Budget", budget.getId(), "CREATE",
                AppConstants.SYSTEM_USER, null, "予算作成: " + budget.getName());

        logger.info("予算を作成しました。予算名: " + budget.getName()
                + ", 年度: " + budget.getFiscalYear());

        return budget;
    }

    /**
     * 予算を更新する。
     *
     * @param budget 更新する予算エンティティ
     * @return 更新後の予算
     */
    public Budget updateBudget(Budget budget) {
        if (budget == null || budget.getId() == null) {
            throw new ValidationException("budget", "予算情報とIDは必須です。");
        }

        Budget existing = em.find(Budget.class, budget.getId());
        if (existing == null) {
            throw new EntityNotFoundException("Budget", budget.getId());
        }

        // DRAFT/APPROVEDのみ更新可能
        if (!"DRAFT".equals(existing.getStatus()) && !"APPROVED".equals(existing.getStatus())) {
            throw new ValidationException("status",
                    "ドラフトまたは承認済みの予算のみ更新できます。現在のステータス: " + existing.getStatus());
        }

        budget.setUpdatedBy(AppConstants.SYSTEM_USER);
        Budget merged = em.merge(budget);

        auditService.logAction("Budget", merged.getId(), "UPDATE",
                AppConstants.SYSTEM_USER, null, "予算更新");

        return merged;
    }

    /**
     * IDで予算を取得する。
     *
     * @param budgetId 予算ID
     * @return 予算エンティティ
     * @throws EntityNotFoundException 予算が見つからない場合
     */
    public Budget findById(Long budgetId) {
        if (budgetId == null) {
            throw new ValidationException("budgetId", "予算IDは必須です。");
        }

        Budget budget = em.find(Budget.class, budgetId);
        if (budget == null) {
            throw new EntityNotFoundException("Budget", budgetId);
        }
        return budget;
    }

    /**
     * 部門IDと会計年度で予算を取得する。
     *
     * @param departmentId 部門ID
     * @param fiscalYear 会計年度
     * @return 予算エンティティ（見つからない場合はnull）
     */
    @SuppressWarnings("unchecked")
    public Budget findByDepartmentAndYear(Long departmentId, Integer fiscalYear) {
        if (departmentId == null || fiscalYear == null) {
            return null;
        }

        List<Budget> results = em.createNamedQuery("Budget.findByDepartmentAndYear")
                .setParameter("departmentId", departmentId)
                .setParameter("fiscalYear", fiscalYear)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 会計年度で予算を検索する。
     *
     * @param fiscalYear 会計年度
     * @return 予算のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Budget> findByFiscalYear(Integer fiscalYear) {
        if (fiscalYear == null) {
            return new ArrayList<Budget>();
        }

        return em.createNamedQuery("Budget.findByFiscalYear")
                .setParameter("fiscalYear", fiscalYear)
                .getResultList();
    }

    // ========================================================================
    // 予算配賦
    // ========================================================================

    /**
     * 予算明細を追加する（配賦）。
     *
     * @param budgetId 予算ID
     * @param description 費目の説明
     * @param amount 配賦額
     * @param categoryId カテゴリID（任意）
     * @return 作成された予算明細
     */
    public BudgetLineItem allocateBudget(Long budgetId, String description,
                                         BigDecimal amount, Long categoryId) {
        Budget budget = findById(budgetId);

        // 技術的負債 #14: マジックストリング
        if (!"ACTIVE".equals(budget.getStatus()) && !"APPROVED".equals(budget.getStatus())) {
            throw new BusinessException("BDG_001",
                    "有効または承認済みの予算のみ配賦できます。現在のステータス: " + budget.getStatus());
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "配賦額は0より大きい値を指定してください。");
        }

        // 配賦可能残高チェック
        BigDecimal currentAllocated = budget.getAllocatedAmount() != null
                ? budget.getAllocatedAmount() : BigDecimal.ZERO;
        BigDecimal remaining = budget.getTotalAmount().subtract(currentAllocated);

        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("BDG_002",
                    "配賦額が予算残高を超過しています。配賦額: " + amount + ", 残高: " + remaining);
        }

        // 明細の作成
        BudgetLineItem lineItem = new BudgetLineItem();
        lineItem.setBudget(budget);
        lineItem.setDescription(description != null ? description : "未分類");
        lineItem.setAllocatedAmount(amount);
        lineItem.setSpentAmount(BigDecimal.ZERO);
        lineItem.setCategoryId(categoryId);

        em.persist(lineItem);

        // 予算の配賦済み額を更新
        budget.setAllocatedAmount(currentAllocated.add(amount));
        budget.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(budget);

        logger.info("予算配賦完了。予算ID: " + budgetId + ", 配賦額: " + amount);
        return lineItem;
    }

    // ========================================================================
    // 予算消化チェック
    // ========================================================================

    /**
     * 予算の利用可否をチェックする。
     *
     * <p>指定された金額が予算の残高内に収まるかを確認する。</p>
     *
     * <p>【技術的負債】チェックと消化が別メソッドであり、
     * チェック後に他トランザクションが消化する可能性がある（楽観ロック頼み）。</p>
     *
     * @param budgetId 予算ID
     * @param requestedAmount 要求金額
     * @return 利用可能な場合true
     */
    public boolean checkBudgetAvailability(Long budgetId, BigDecimal requestedAmount) {
        Budget budget = findById(budgetId);

        if (!"ACTIVE".equals(budget.getStatus())) {
            return false;
        }

        BigDecimal spent = budget.getSpentAmount() != null
                ? budget.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = budget.getTotalAmount().subtract(spent);

        return requestedAmount.compareTo(remaining) <= 0;
    }

    /**
     * 予算を消化する（消化済み額を加算する）。
     *
     * <p>【技術的負債】checkBudgetAvailabilityとアトミックでない。
     * 楽観ロック（@Version）で競合を検出するが、リトライ処理がない。</p>
     *
     * @param budgetId 予算ID
     * @param amount 消化額
     * @param reference 参照情報（例: 発注番号）
     */
    public void spendBudget(Long budgetId, BigDecimal amount, String reference) {
        Budget budget = findById(budgetId);

        if (!"ACTIVE".equals(budget.getStatus())) {
            throw new BusinessException("BDG_003",
                    "有効な予算のみ消化できます。現在のステータス: " + budget.getStatus());
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "消化額は0より大きい値を指定してください。");
        }

        BigDecimal spent = budget.getSpentAmount() != null
                ? budget.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = budget.getTotalAmount().subtract(spent);

        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("BDG_004",
                    "消化額が予算残高を超過しています。消化額: " + amount + ", 残高: " + remaining);
        }

        budget.setSpentAmount(spent.add(amount));
        budget.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(budget);

        // 予算消化率のチェック（90%超過で警告通知）
        BigDecimal utilizationRate = budget.getSpentAmount()
                .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // 技術的負債 #14: マジックナンバー（90%）
        if (utilizationRate.compareTo(new BigDecimal("90")) >= 0) {
            try {
                notificationService.sendNotification(
                        1L, // 技術的負債: 通知先ハードコード
                        "予算消化率警告: " + budget.getName(),
                        "予算「" + budget.getName() + "」の消化率が"
                                + utilizationRate.setScale(1, RoundingMode.HALF_UP) + "%に達しました。"
                                + " 残高: " + budget.getTotalAmount().subtract(budget.getSpentAmount()),
                        "WARNING",
                        "Budget", budget.getId());
            } catch (Exception e) {
                // 技術的負債 #7: 通知失敗を握りつぶし
                logger.warning("予算消化率警告通知の送信に失敗しました。");
            }
        }

        auditService.logAction("Budget", budgetId, "UPDATE",
                AppConstants.SYSTEM_USER, null,
                "予算消化: " + amount + ", 参照: " + reference);

        logger.info("予算消化完了。予算ID: " + budgetId + ", 消化額: " + amount
                + ", 参照: " + reference);
    }

    /**
     * 予算消化を戻す（返金・取消時）。
     *
     * @param budgetId 予算ID
     * @param amount 戻し額
     * @param reason 理由
     */
    public void reverseBudgetSpend(Long budgetId, BigDecimal amount, String reason) {
        Budget budget = findById(budgetId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "戻し額は0より大きい値を指定してください。");
        }

        BigDecimal spent = budget.getSpentAmount() != null
                ? budget.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal newSpent = spent.subtract(amount);

        if (newSpent.compareTo(BigDecimal.ZERO) < 0) {
            newSpent = BigDecimal.ZERO;
        }

        budget.setSpentAmount(newSpent);
        budget.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(budget);

        auditService.logAction("Budget", budgetId, "UPDATE",
                AppConstants.SYSTEM_USER, null,
                "予算消化戻し: " + amount + ", 理由: " + reason);

        logger.info("予算消化戻し完了。予算ID: " + budgetId + ", 戻し額: " + amount);
    }

    // ========================================================================
    // 予算利用状況
    // ========================================================================

    /**
     * 予算の利用状況を取得する。
     *
     * <p>【技術的負債 #12】Map<String, Object>で返却。型安全でない。</p>
     *
     * @param budgetId 予算ID
     * @return 利用状況のMap
     */
    public Map<String, Object> getBudgetUtilization(Long budgetId) {
        Budget budget = findById(budgetId);
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("budgetName", budget.getName());
        result.put("fiscalYear", budget.getFiscalYear());
        result.put("status", budget.getStatus());
        result.put("totalAmount", budget.getTotalAmount());

        BigDecimal allocated = budget.getAllocatedAmount() != null
                ? budget.getAllocatedAmount() : BigDecimal.ZERO;
        BigDecimal spent = budget.getSpentAmount() != null
                ? budget.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = budget.getTotalAmount().subtract(spent);

        result.put("allocatedAmount", allocated);
        result.put("spentAmount", spent);
        result.put("remainingAmount", remaining);

        // 消化率
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal utilizationRate = spent
                    .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            result.put("utilizationRate", utilizationRate.setScale(2, RoundingMode.HALF_UP));
        } else {
            result.put("utilizationRate", BigDecimal.ZERO);
        }

        // 配賦率
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal allocationRate = allocated
                    .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            result.put("allocationRate", allocationRate.setScale(2, RoundingMode.HALF_UP));
        } else {
            result.put("allocationRate", BigDecimal.ZERO);
        }

        // 明細件数
        result.put("lineItemCount", budget.getLineItems().size());

        return result;
    }

    // ========================================================================
    // ステータス管理
    // ========================================================================

    /**
     * 予算を有効化する（APPROVED → ACTIVE）。
     *
     * @param budgetId 予算ID
     */
    public void activateBudget(Long budgetId) {
        Budget budget = findById(budgetId);

        // 技術的負債 #14: マジックストリング
        if (!"APPROVED".equals(budget.getStatus())) {
            throw new BusinessException("BDG_005",
                    "承認済みの予算のみ有効化できます。現在のステータス: " + budget.getStatus());
        }

        budget.setStatus("ACTIVE");
        budget.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(budget);

        auditService.logAction("Budget", budgetId, "UPDATE",
                AppConstants.SYSTEM_USER, "status=APPROVED", "status=ACTIVE");
    }

    /**
     * 予算を凍結する（ACTIVE → FROZEN）。
     *
     * @param budgetId 予算ID
     */
    public void freezeBudget(Long budgetId) {
        Budget budget = findById(budgetId);

        if (!"ACTIVE".equals(budget.getStatus())) {
            throw new BusinessException("BDG_006",
                    "有効な予算のみ凍結できます。現在のステータス: " + budget.getStatus());
        }

        budget.setStatus("FROZEN");
        budget.setUpdatedBy(AppConstants.SYSTEM_USER);
        em.merge(budget);

        auditService.logAction("Budget", budgetId, "UPDATE",
                AppConstants.SYSTEM_USER, "status=ACTIVE", "status=FROZEN");

        logger.info("予算を凍結しました。予算ID: " + budgetId);
    }

    // ========================================================================
    // 年度クローズ
    // ========================================================================

    /**
     * 会計年度の予算をクローズする。
     *
     * <p>指定年度の全予算をCLOSEDステータスに変更する。
     * 通常は年度末の定期バッチで実行される。</p>
     *
     * <p>【技術的負債 #3】N+1パターン。各予算を個別に更新している。
     * バルクアップデートを使用すべき。</p>
     *
     * @param fiscalYear 会計年度
     * @return クローズした予算件数
     */
    public int closeFiscalYear(int fiscalYear) {
        List<Budget> budgets = findByFiscalYear(fiscalYear);

        int closedCount = 0;

        // 技術的負債 #3: N+1パターン — 個別にステータス更新
        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < budgets.size(); i++) {
            Budget budget = budgets.get(i);

            if ("CLOSED".equals(budget.getStatus())) {
                continue; // 既にクローズ済み
            }

            String oldStatus = budget.getStatus();
            budget.setStatus("CLOSED");
            budget.setUpdatedBy(AppConstants.SYSTEM_USER);
            em.merge(budget);

            auditService.logAction("Budget", budget.getId(), "UPDATE",
                    AppConstants.SYSTEM_USER, "status=" + oldStatus, "status=CLOSED");

            closedCount++;
        }

        logger.info("会計年度クローズ完了。年度: " + fiscalYear + ", クローズ件数: " + closedCount);
        return closedCount;
    }

    /**
     * 現在の会計年度を取得する。
     *
     * <p>日本の会計年度（4月始まり）に基づいて現在の会計年度を算出する。</p>
     *
     * <p>【技術的負債】Calendar API使用。java.time移行すべき。
     * また、会計年度の開始月がハードコードされている。</p>
     *
     * @return 現在の会計年度
     */
    public int getCurrentFiscalYear() {
        // 技術的負債: Calendar API使用、開始月ハードコード
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH); // 0-based

        // 技術的負債 #14: マジックナンバー（4月＝月3）
        if (month < 3) { // 1月〜3月は前年度
            return year - 1;
        }
        return year;
    }
}
