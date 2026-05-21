package com.proquip.common.exception;

/**
 * 在庫不足の場合にスローされる例外。
 *
 * <p>出庫や引当処理において、要求数量が利用可能在庫を超過した場合に使用する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public class InsufficientStockException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** 対象商品ID */
    private final Long productId;

    /** 要求数量 */
    private final int requestedQuantity;

    /** 利用可能在庫数 */
    private final int availableQuantity;

    /**
     * 商品ID・要求数量・利用可能在庫数を指定して例外を生成する。
     *
     * @param productId 対象商品ID
     * @param requestedQuantity 要求された数量
     * @param availableQuantity 利用可能な在庫数
     */
    public InsufficientStockException(Long productId, int requestedQuantity,
                                       int availableQuantity) {
        super("INSUFFICIENT_STOCK",
                "在庫不足です。商品ID: " + productId
                        + ", 要求数量: " + requestedQuantity
                        + ", 利用可能数量: " + availableQuantity);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    /**
     * 対象商品IDを返す。
     *
     * @return 商品ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 要求数量を返す。
     *
     * @return 要求数量
     */
    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    /**
     * 利用可能在庫数を返す。
     *
     * @return 利用可能在庫数
     */
    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
