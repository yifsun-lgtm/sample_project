package com.proquip.common.constant;

/**
 * 購入依頼（購買要求）のステータスを表す列挙型。
 *
 * <p>購入依頼書の申請から変換・キャンセルまでのステータスを定義する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public enum RequisitionStatus {

    /** 下書き */
    DRAFT("下書き"),
    /** 申請済み */
    SUBMITTED("申請済み"),
    /** 承認済み */
    APPROVED("承認済み"),
    /** 却下 */
    REJECTED("却下"),
    /** 発注書に変換済み */
    CONVERTED("変換済み"),
    /** キャンセル */
    CANCELLED("キャンセル");

    /** 日本語表示用ラベル */
    private final String label;

    RequisitionStatus(String label) {
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
