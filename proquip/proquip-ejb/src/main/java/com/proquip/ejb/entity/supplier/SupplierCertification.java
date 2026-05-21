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
import java.util.Date;

/**
 * 仕入先の認証・資格情報を表すエンティティ。
 *
 * <p>仕入先が取得している各種認証（ISO、品質認証等）の情報を管理する。
 * 認証番号、発行日、有効期限を保持し、資格の有効性を追跡する。</p>
 *
 * <p>技術的負債: {@code issuedDate} / {@code expiryDate} に
 * {@link java.util.Date} を使用している。
 * また {@code status} が文字列型。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier_certification")
@NamedQueries({
    @NamedQuery(
        name = "SupplierCertification.findBySupplier",
        query = "SELECT sc FROM SupplierCertification sc WHERE sc.supplier.id = :supplierId ORDER BY sc.expiryDate"
    ),
    @NamedQuery(
        name = "SupplierCertification.findExpiringSoon",
        query = "SELECT sc FROM SupplierCertification sc WHERE sc.expiryDate BETWEEN :now AND :threshold AND sc.status = 'ACTIVE'"
    )
})
public class SupplierCertification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 認証種別。
     * 想定値: "ISO_9001", "ISO_14001", "ISO_27001", "OHSAS_18001", "OTHER"
     */
    @Column(name = "certification_name", nullable = false, length = 50)
    private String certType;

    /** 認証番号 */
    @Column(name = "certificate_number", length = 100)
    private String certNumber;

    /**
     * 発行日。
     * 技術的負債: java.util.Date を使用（LocalDateに移行すべき）
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "issued_date")
    private Date issuedDate;

    /**
     * 有効期限。
     * 技術的負債: java.util.Date を使用（LocalDateに移行すべき）
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "expiry_date")
    private Date expiryDate;

    /**
     * 認証ステータス。
     * 技術的負債: 文字列で管理。Enum型に移行すべき。
     * 想定値: "ACTIVE", "EXPIRED", "REVOKED", "PENDING"
     */
    @Column(name = "status", length = 30)
    private String status;

    /** 仕入先 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonbTransient
    private Supplier supplier;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierCertification() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 認証種別を返す。
     *
     * @return 認証種別
     */
    public String getCertType() {
        return certType;
    }

    /**
     * 認証種別を設定する。
     *
     * @param certType 認証種別
     */
    public void setCertType(String certType) {
        this.certType = certType;
    }

    /**
     * 認証番号を返す。
     *
     * @return 認証番号
     */
    public String getCertNumber() {
        return certNumber;
    }

    /**
     * 認証番号を設定する。
     *
     * @param certNumber 認証番号
     */
    public void setCertNumber(String certNumber) {
        this.certNumber = certNumber;
    }

    /**
     * 発行日を返す。
     *
     * @return 発行日
     */
    public Date getIssuedDate() {
        return issuedDate;
    }

    /**
     * 発行日を設定する。
     *
     * @param issuedDate 発行日
     */
    public void setIssuedDate(Date issuedDate) {
        this.issuedDate = issuedDate;
    }

    /**
     * 有効期限を返す。
     *
     * @return 有効期限
     */
    public Date getExpiryDate() {
        return expiryDate;
    }

    /**
     * 有効期限を設定する。
     *
     * @param expiryDate 有効期限
     */
    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * 認証ステータスを返す。
     *
     * @return 認証ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * 認証ステータスを設定する。
     *
     * @param status 認証ステータス
     */
    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public String toString() {
        return "SupplierCertification{" +
                "id=" + getId() +
                ", certType='" + certType + '\'' +
                ", certNumber='" + certNumber + '\'' +
                ", expiryDate=" + expiryDate +
                ", status='" + status + '\'' +
                '}';
    }
}
