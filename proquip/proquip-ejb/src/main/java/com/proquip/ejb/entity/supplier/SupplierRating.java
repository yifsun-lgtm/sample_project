package com.proquip.ejb.entity.supplier;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.json.bind.annotation.JsonbTransient;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 仕入先評価を表すエンティティ。
 *
 * <p>仕入先に対する定期的な評価を記録する。品質・納期・価格の各観点でスコアを付け、
 * 総合スコアを算出する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code ratingDate} に {@link java.util.Date} を使用。</li>
 *   <li>{@code ratedBy} が {@code Long} 型の生IDであり、ユーザーエンティティへの
 *       リレーションではない。参照整合性がJPAレベルで保証されない。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier_rating")
@NamedQueries({
    @NamedQuery(
        name = "SupplierRating.findBySupplier",
        query = "SELECT sr FROM SupplierRating sr WHERE sr.supplier.id = :supplierId ORDER BY sr.ratingDate DESC"
    ),
    @NamedQuery(
        name = "SupplierRating.findLatestBySupplier",
        query = "SELECT sr FROM SupplierRating sr WHERE sr.supplier.id = :supplierId ORDER BY sr.ratingDate DESC"
    )
})
public class SupplierRating extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 評価日。
     * 技術的負債: java.util.Date を使用（LocalDateに移行すべき）
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "rated_at", nullable = false)
    private Date ratingDate;

    /** 品質スコア（0.00〜5.00） */
    @Column(name = "quality_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal qualityScore;

    /** 納期スコア（0.0〜5.0） */
    @Column(name = "delivery_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal deliveryScore;

    /** 価格スコア（0.0〜5.0） */
    @Column(name = "price_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal priceScore;

    /** 総合スコア（0.0〜5.0） */
    @Column(name = "overall_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal overallScore;

    /** 評価コメント */
    @Column(name = "comments", length = 2000)
    private String comments;

    /** 仕入先 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonbTransient
    private Supplier supplier;

    /** 評価期間（例: "2026-Q1"） */
    @Column(name = "rating_period", nullable = false, length = 20)
    private String ratingPeriod;

    /** サービス/対応スコア（0.0〜5.0） */
    @Column(name = "service_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal serviceScore;

    /**
     * 評価実施者のユーザーID。
     * 技術的負債: 生IDで保持。ユーザーエンティティへの {@code @ManyToOne} に移行すべき。
     */
    @Column(name = "rated_by")
    private Long ratedBy;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierRating() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 評価日を返す。
     *
     * @return 評価日
     */
    public Date getRatingDate() {
        return ratingDate;
    }

    /**
     * 評価日を設定する。
     *
     * @param ratingDate 評価日
     */
    public void setRatingDate(Date ratingDate) {
        this.ratingDate = ratingDate;
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
     * 納期スコアを返す。
     *
     * @return 納期スコア
     */
    public BigDecimal getDeliveryScore() {
        return deliveryScore;
    }

    /**
     * 納期スコアを設定する。
     *
     * @param deliveryScore 納期スコア
     */
    public void setDeliveryScore(BigDecimal deliveryScore) {
        this.deliveryScore = deliveryScore;
    }

    /**
     * 価格スコアを返す。
     *
     * @return 価格スコア
     */
    public BigDecimal getPriceScore() {
        return priceScore;
    }

    /**
     * 価格スコアを設定する。
     *
     * @param priceScore 価格スコア
     */
    public void setPriceScore(BigDecimal priceScore) {
        this.priceScore = priceScore;
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
     * 評価コメントを返す。
     *
     * @return コメント
     */
    public String getComments() {
        return comments;
    }

    /**
     * 評価コメントを設定する。
     *
     * @param comments コメント
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * 仕入先を返す。
     *
     * @return 仕入先
     */
    public Supplier getSupplier() {
        return supplier;
    }

    /**
     * 仕入先を設定する。
     *
     * @param supplier 仕入先
     */
    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public String getRatingPeriod() {
        return ratingPeriod;
    }

    public void setRatingPeriod(String ratingPeriod) {
        this.ratingPeriod = ratingPeriod;
    }

    public BigDecimal getServiceScore() {
        return serviceScore;
    }

    public void setServiceScore(BigDecimal serviceScore) {
        this.serviceScore = serviceScore;
    }

    /**
     * 評価実施者のユーザーIDを返す。
     * 技術的負債: 生IDを返す。エンティティ参照に移行予定。
     *
     * @return 評価実施者のユーザーID
     */
    public Long getRatedBy() {
        return ratedBy;
    }

    /**
     * 評価実施者のユーザーIDを設定する。
     * 技術的負債: 生IDを受け取る。エンティティ参照に移行予定。
     *
     * @param ratedBy 評価実施者のユーザーID
     */
    public void setRatedBy(Long ratedBy) {
        this.ratedBy = ratedBy;
    }

    @Override
    public String toString() {
        return "SupplierRating{" +
                "id=" + getId() +
                ", ratingDate=" + ratingDate +
                ", qualityScore=" + qualityScore +
                ", deliveryScore=" + deliveryScore +
                ", priceScore=" + priceScore +
                ", overallScore=" + overallScore +
                '}';
    }
}
