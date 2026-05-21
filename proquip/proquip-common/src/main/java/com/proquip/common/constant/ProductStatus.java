package com.proquip.common.constant;

/**
 * 商品ステータスを表す列挙型。
 *
 * <p>商品マスタにおける商品の状態を定義する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public enum ProductStatus {

    /** 有効（販売・発注可能） */
    ACTIVE("有効"),
    /** 無効（販売・発注停止） */
    INACTIVE("無効"),
    /** 販売終了（廃番） */
    DISCONTINUED("販売終了"),
    /** 下書き（未公開） */
    DRAFT("下書き");

    /** 日本語表示用ラベル */
    private final String label;

    ProductStatus(String label) {
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
