package com.proquip.common.constant;

/**
 * 発注書のステータスを表す列挙型。
 *
 * <p>発注書のライフサイクル全体をカバーするステータスを定義する。
 * 各ステータスには日本語ラベルが付与されている。</p>
 *
 * <p>【技術的負債】一部のサービスではこのenumを使わず、
 * ステータスを文字列リテラルで直接比較している箇所がある。
 * 例: OrderServiceImpl, LegacyImportBatch</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public enum PurchaseOrderStatus {

    /** 下書き */
    DRAFT("下書き"),
    /** 申請済み */
    SUBMITTED("申請済み"),
    /** 承認済み */
    APPROVED("承認済み"),
    /** 却下 */
    REJECTED("却下"),
    /** 発注済み */
    ORDERED("発注済み"),
    /** 一部入荷 */
    PARTIALLY_RECEIVED("一部入荷"),
    /** 入荷完了 */
    RECEIVED("入荷完了"),
    /** 請求済み */
    INVOICED("請求済み"),
    /** 支払済み */
    PAID("支払済み"),
    /** キャンセル */
    CANCELLED("キャンセル");

    /** 日本語表示用ラベル */
    private final String label;

    PurchaseOrderStatus(String label) {
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

    /**
     * 文字列からステータスを取得する。
     * 該当するステータスが存在しない場合はnullを返す。
     *
     * <p>【技術的負債】IllegalArgumentExceptionをスローすべきだが、
     * レガシーコードとの互換性のためnullを返している。</p>
     *
     * @param value ステータス文字列
     * @return 対応するステータス、存在しない場合はnull
     */
    public static PurchaseOrderStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        for (PurchaseOrderStatus status : values()) {
            if (status.name().equals(value) || status.label.equals(value)) {
                return status;
            }
        }
        // TODO: 本来はIllegalArgumentExceptionをスローすべき
        return null;
    }
}
