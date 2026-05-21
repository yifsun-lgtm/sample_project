package com.proquip.common.dto.report;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 仕入先パフォーマンスレポートデータ転送オブジェクト。
 *
 * <p>仕入先の評価パフォーマンスを保持する。納期遵守率、品質スコア、
 * 価格競争力などの各種メトリクスを含む。</p>
 *
 * @author ProQuip開発チーム
 */
public class SupplierPerformanceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 仕入先ID */
    private Long supplierId;

    /** 仕入先名 */
    private String supplierName;

    /** 仕入先コード */
    private String supplierCode;

    /** 評価期間（開始） */
    private String periodStart;

    /** 評価期間（終了） */
    private String periodEnd;

    /** 総合スコア（0.00〜5.00） */
    private BigDecimal overallScore;

    /** 納期遵守率（パーセンテージ） */
    private BigDecimal onTimeDeliveryRate;

    /** 品質スコア（0.00〜5.00） */
    private BigDecimal qualityScore;

    /** 不良率（パーセンテージ） */
    private BigDecimal defectRate;

    /** 対応速度スコア（0.00〜5.00） */
    private BigDecimal responsivenessScore;

    /** 発注回数 */
    private int orderCount;

    /** 合計取引額 */
    private BigDecimal totalTransactionAmount;

    /** 平均納品日数 */
    private BigDecimal averageDeliveryDays;

    /** 返品件数 */
    private int returnCount;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierPerformanceDto() {
    }

    // --- Getter / Setter ---

    /**
     * 仕入先IDを返す。
     *
     * @return 仕入先ID
     */
    public Long getSupplierId() {
        return supplierId;
    }

    /**
     * 仕入先IDを設定する。
     *
     * @param supplierId 仕入先ID
     */
    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * 仕入先名を返す。
     *
     * @return 仕入先名
     */
    public String getSupplierName() {
        return supplierName;
    }

    /**
     * 仕入先名を設定する。
     *
     * @param supplierName 仕入先名
     */
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    /**
     * 仕入先コードを返す。
     *
     * @return 仕入先コード
     */
    public String getSupplierCode() {
        return supplierCode;
    }

    /**
     * 仕入先コードを設定する。
     *
     * @param supplierCode 仕入先コード
     */
    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    /**
     * 評価期間（開始）を返す。
     *
     * @return 期間開始日
     */
    public String getPeriodStart() {
        return periodStart;
    }

    /**
     * 評価期間（開始）を設定する。
     *
     * @param periodStart 期間開始日
     */
    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    /**
     * 評価期間（終了）を返す。
     *
     * @return 期間終了日
     */
    public String getPeriodEnd() {
        return periodEnd;
    }

    /**
     * 評価期間（終了）を設定する。
     *
     * @param periodEnd 期間終了日
     */
    public void setPeriodEnd(String periodEnd) {
        this.periodEnd = periodEnd;
    }

    /**
     * 総合スコアを返す。
     *
     * @return 総合スコア
     */
    public BigDecimal getOverallScore() {
        return overallScore;
    }

    /**
     * 総合スコアを設定する。
     *
     * @param overallScore 総合スコア
     */
    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    /**
     * 納期遵守率を返す。
     *
     * @return 納期遵守率（パーセンテージ）
     */
    public BigDecimal getOnTimeDeliveryRate() {
        return onTimeDeliveryRate;
    }

    /**
     * 納期遵守率を設定する。
     *
     * @param onTimeDeliveryRate 納期遵守率
     */
    public void setOnTimeDeliveryRate(BigDecimal onTimeDeliveryRate) {
        this.onTimeDeliveryRate = onTimeDeliveryRate;
    }

    /**
     * 品質スコアを返す。
     *
     * @return 品質スコア
     */
    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    /**
     * 品質スコアを設定する。
     *
     * @param qualityScore 品質スコア
     */
    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    /**
     * 不良率を返す。
     *
     * @return 不良率（パーセンテージ）
     */
    public BigDecimal getDefectRate() {
        return defectRate;
    }

    /**
     * 不良率を設定する。
     *
     * @param defectRate 不良率
     */
    public void setDefectRate(BigDecimal defectRate) {
        this.defectRate = defectRate;
    }

    /**
     * 対応速度スコアを返す。
     *
     * @return 対応速度スコア
     */
    public BigDecimal getResponsivenessScore() {
        return responsivenessScore;
    }

    /**
     * 対応速度スコアを設定する。
     *
     * @param responsivenessScore 対応速度スコア
     */
    public void setResponsivenessScore(BigDecimal responsivenessScore) {
        this.responsivenessScore = responsivenessScore;
    }

    /**
     * 発注回数を返す。
     *
     * @return 発注回数
     */
    public int getOrderCount() {
        return orderCount;
    }

    /**
     * 発注回数を設定する。
     *
     * @param orderCount 発注回数
     */
    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    /**
     * 合計取引額を返す。
     *
     * @return 合計取引額
     */
    public BigDecimal getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    /**
     * 合計取引額を設定する。
     *
     * @param totalTransactionAmount 合計取引額
     */
    public void setTotalTransactionAmount(BigDecimal totalTransactionAmount) {
        this.totalTransactionAmount = totalTransactionAmount;
    }

    /**
     * 平均納品日数を返す。
     *
     * @return 平均納品日数
     */
    public BigDecimal getAverageDeliveryDays() {
        return averageDeliveryDays;
    }

    /**
     * 平均納品日数を設定する。
     *
     * @param averageDeliveryDays 平均納品日数
     */
    public void setAverageDeliveryDays(BigDecimal averageDeliveryDays) {
        this.averageDeliveryDays = averageDeliveryDays;
    }

    /**
     * 返品件数を返す。
     *
     * @return 返品件数
     */
    public int getReturnCount() {
        return returnCount;
    }

    /**
     * 返品件数を設定する。
     *
     * @param returnCount 返品件数
     */
    public void setReturnCount(int returnCount) {
        this.returnCount = returnCount;
    }
}
