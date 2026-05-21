package com.proquip.common.constant;

/**
 * 承認ステータスを表す列挙型。
 *
 * <p>承認ワークフローにおける各段階のステータスを定義する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public enum ApprovalStatus {

    /** 承認待ち */
    PENDING("承認待ち"),
    /** 承認済み */
    APPROVED("承認済み"),
    /** 却下 */
    REJECTED("却下"),
    /** 上位承認者へエスカレーション */
    ESCALATED("エスカレーション");

    /** 日本語表示用ラベル */
    private final String label;

    ApprovalStatus(String label) {
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
