package com.proquip.common.constant;

/**
 * 在庫トランザクション種別を表す列挙型。
 *
 * <p>在庫の入出庫・調整・移動・棚卸差異の種別を定義する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public enum InventoryTransactionType {

    /** 入庫 */
    IN("入庫"),
    /** 出庫 */
    OUT("出庫"),
    /** 在庫調整 */
    ADJUST("在庫調整"),
    /** 倉庫間移動 */
    TRANSFER("倉庫間移動"),
    /** 棚卸差異調整 */
    COUNT_ADJUST("棚卸差異調整");

    /** 日本語表示用ラベル */
    private final String label;

    InventoryTransactionType(String label) {
        this.label = label;
    }

    /**
     * 日本語ラベルを返す。
     *
     * @return 日本語表示用ラベル
     */
    public String getLabel() {
        return label;
    }
}
