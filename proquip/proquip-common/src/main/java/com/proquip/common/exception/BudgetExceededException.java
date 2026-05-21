package com.proquip.common.exception;

import java.math.BigDecimal;

/**
 * 予算超過時にスローされる例外。
 *
 * <p>発注金額が予算の残額を超過した場合に使用する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public class BudgetExceededException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** 対象予算ID */
    private final Long budgetId;

    /** 要求された金額 */
    private final BigDecimal requestedAmount;

    /** 予算残額 */
    private final BigDecimal remainingAmount;

    /**
     * 予算ID・要求金額・残額を指定して例外を生成する。
     *
     * @param budgetId 対象予算ID
     * @param requestedAmount 要求された金額
     * @param remainingAmount 予算の残額
     */
    public BudgetExceededException(Long budgetId, BigDecimal requestedAmount,
                                    BigDecimal remainingAmount) {
        super("BUDGET_EXCEEDED",
                "予算超過です。予算ID: " + budgetId
                        + ", 要求金額: " + requestedAmount
                        + ", 予算残額: " + remainingAmount);
        this.budgetId = budgetId;
        this.requestedAmount = requestedAmount;
        this.remainingAmount = remainingAmount;
    }

    /**
     * 予算IDを返す。
     *
     * @return 予算ID
     */
    public Long getBudgetId() {
        return budgetId;
    }

    /**
     * 要求された金額を返す。
     *
     * @return 要求金額
     */
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    /**
     * 予算の残額を返す。
     *
     * @return 予算残額
     */
    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }
}
