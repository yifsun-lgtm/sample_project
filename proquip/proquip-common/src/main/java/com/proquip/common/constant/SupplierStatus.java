package com.proquip.common.constant;

/**
 * 仕入先（サプライヤー）のステータスを表す列挙型。
 *
 * <p>仕入先の取引状態を管理するためのステータスを定義する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public enum SupplierStatus {

    /** 有効（取引可能） */
    ACTIVE("有効"),
    /** 無効（取引停止） */
    INACTIVE("無効"),
    /** 一時停止 */
    SUSPENDED("一時停止"),
    /** 審査中 */
    PENDING_REVIEW("審査中"),
    /** ブラックリスト */
    BLACKLISTED("ブラックリスト");

    /** 日本語表示用ラベル */
    private final String label;

    SupplierStatus(String label) {
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
