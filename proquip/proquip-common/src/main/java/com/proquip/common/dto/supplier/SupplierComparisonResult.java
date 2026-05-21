package com.proquip.common.dto.supplier;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 仕入先比較結果DTOクラス。
 *
 * <p>複数の仕入先を比較した結果を保持する。価格、品質、納期などの
 * メトリクスごとにスコアを算出し、推奨仕入先を判定する。</p>
 *
 * @author ProQuip開発チーム
 */
public class SupplierComparisonResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 比較対象の仕入先メトリクス一覧 */
    private List<SupplierMetric> suppliers = new ArrayList<>();

    /** 推奨仕入先ID */
    private Long recommendedSupplierId;

    /** 推奨理由 */
    private String recommendationReason;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierComparisonResult() {
    }

    // --- Getter / Setter ---

    /**
     * 比較対象の仕入先メトリクス一覧を返す。
     *
     * @return メトリクスのリスト
     */
    public List<SupplierMetric> getSuppliers() {
        return suppliers;
    }

    /**
     * 比較対象の仕入先メトリクス一覧を設定する。
     *
     * @param suppliers メトリクスのリスト
     */
    public void setSuppliers(List<SupplierMetric> suppliers) {
        this.suppliers = suppliers;
    }

    /**
     * 推奨仕入先IDを返す。
     *
     * @return 推奨仕入先ID
     */
    public Long getRecommendedSupplierId() {
        return recommendedSupplierId;
    }

    /**
     * 推奨仕入先IDを設定する。
     *
     * @param recommendedSupplierId 推奨仕入先ID
     */
    public void setRecommendedSupplierId(Long recommendedSupplierId) {
        this.recommendedSupplierId = recommendedSupplierId;
    }

    /**
     * 推奨理由を返す。
     *
     * @return 推奨理由
     */
    public String getRecommendationReason() {
        return recommendationReason;
    }

    /**
     * 推奨理由を設定する。
     *
     * @param recommendationReason 推奨理由
     */
    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    /**
     * 仕入先メトリクス内部クラス。
     *
     * <p>個別の仕入先に対する評価メトリクスを保持する。</p>
     */
    public static class SupplierMetric implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 仕入先ID */
        private Long supplierId;

        /** 仕入先名 */
        private String supplierName;

        /** 価格スコア（0.00〜5.00） */
        private BigDecimal priceScore;

        /** 品質スコア（0.00〜5.00） */
        private BigDecimal qualityScore;

        /** 納期スコア（0.00〜5.00） */
        private BigDecimal deliveryScore;

        /** 総合スコア（0.00〜5.00） */
        private BigDecimal overallScore;

        /** 平均納期（日数） */
        private Integer averageLeadTimeDays;

        /** 納期遵守率（パーセンテージ） */
        private BigDecimal onTimeDeliveryRate;

        /** 不良率（パーセンテージ） */
        private BigDecimal defectRate;

        /**
         * デフォルトコンストラクタ。
         */
        public SupplierMetric() {
        }

        // --- Getter / Setter ---

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
        }

        public BigDecimal getPriceScore() {
            return priceScore;
        }

        public void setPriceScore(BigDecimal priceScore) {
            this.priceScore = priceScore;
        }

        public BigDecimal getQualityScore() {
            return qualityScore;
        }

        public void setQualityScore(BigDecimal qualityScore) {
            this.qualityScore = qualityScore;
        }

        public BigDecimal getDeliveryScore() {
            return deliveryScore;
        }

        public void setDeliveryScore(BigDecimal deliveryScore) {
            this.deliveryScore = deliveryScore;
        }

        public BigDecimal getOverallScore() {
            return overallScore;
        }

        public void setOverallScore(BigDecimal overallScore) {
            this.overallScore = overallScore;
        }

        public Integer getAverageLeadTimeDays() {
            return averageLeadTimeDays;
        }

        public void setAverageLeadTimeDays(Integer averageLeadTimeDays) {
            this.averageLeadTimeDays = averageLeadTimeDays;
        }

        public BigDecimal getOnTimeDeliveryRate() {
            return onTimeDeliveryRate;
        }

        public void setOnTimeDeliveryRate(BigDecimal onTimeDeliveryRate) {
            this.onTimeDeliveryRate = onTimeDeliveryRate;
        }

        public BigDecimal getDefectRate() {
            return defectRate;
        }

        public void setDefectRate(BigDecimal defectRate) {
            this.defectRate = defectRate;
        }
    }
}
