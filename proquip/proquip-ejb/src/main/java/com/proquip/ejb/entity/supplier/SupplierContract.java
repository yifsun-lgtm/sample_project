package com.proquip.ejb.entity.supplier;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.json.bind.annotation.JsonbTransient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 仕入先契約を表すエンティティ。
 *
 * <p>仕入先との取引契約の情報を管理する。契約番号（contractNumber）で識別され、
 * 有効期間や契約条件などを保持する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code startDate} / {@code endDate} に {@link java.util.Date} を使用。</li>
 *   <li>{@code status} が文字列型で定義されている。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier_contract")
@NamedQueries({
    @NamedQuery(
        name = "SupplierContract.findBySupplier",
        query = "SELECT sc FROM SupplierContract sc WHERE sc.supplier.id = :supplierId ORDER BY sc.startDate DESC"
    ),
    @NamedQuery(
        name = "SupplierContract.findByContractNumber",
        query = "SELECT sc FROM SupplierContract sc WHERE sc.contractNumber = :contractNumber"
    ),
    @NamedQuery(
        name = "SupplierContract.findActiveContracts",
        query = "SELECT sc FROM SupplierContract sc WHERE sc.status = 'ACTIVE' AND sc.endDate >= :now"
    )
})
public class SupplierContract extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 契約番号（一意） */
    @Column(name = "contract_number", nullable = false, unique = true, length = 50)
    private String contractNumber;

    /** 契約タイトル */
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    /**
     * 契約開始日。
     * 技術的負債: java.util.Date を使用（LocalDateに移行すべき）
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "start_date", nullable = false)
    private Date startDate;

    /**
     * 契約終了日。
     * 技術的負債: java.util.Date を使用（LocalDateに移行すべき）
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "end_date", nullable = false)
    private Date endDate;

    /** 契約条件（長文テキスト） */
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String terms;

    /**
     * 契約ステータス。
     * 技術的負債: 文字列で管理。Enum型に移行すべき。
     * 想定値: "DRAFT", "ACTIVE", "EXPIRED", "TERMINATED"
     */
    @Column(name = "status", length = 30)
    private String status;

    /** 仕入先 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonbTransient
    private Supplier supplier;

    /** 契約明細一覧 */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @JsonbTransient
    private List<SupplierContractItem> contractItems = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierContract() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 契約番号を返す。
     *
     * @return 契約番号
     */
    public String getContractNumber() {
        return contractNumber;
    }

    /**
     * 契約番号を設定する。
     *
     * @param contractNumber 契約番号
     */
    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 契約開始日を返す。
     *
     * @return 契約開始日
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * 契約開始日を設定する。
     *
     * @param startDate 契約開始日
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * 契約終了日を返す。
     *
     * @return 契約終了日
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * 契約終了日を設定する。
     *
     * @param endDate 契約終了日
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * 契約条件を返す。
     *
     * @return 契約条件テキスト
     */
    public String getTerms() {
        return terms;
    }

    /**
     * 契約条件を設定する。
     *
     * @param terms 契約条件テキスト
     */
    public void setTerms(String terms) {
        this.terms = terms;
    }

    /**
     * 契約ステータスを返す。
     *
     * @return 契約ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * 契約ステータスを設定する。
     *
     * @param status 契約ステータス
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

    /**
     * 契約明細一覧を返す。
     *
     * @return 契約明細のリスト
     */
    public List<SupplierContractItem> getContractItems() {
        return contractItems;
    }

    /**
     * 契約明細一覧を設定する。
     *
     * @param contractItems 契約明細のリスト
     */
    public void setContractItems(List<SupplierContractItem> contractItems) {
        this.contractItems = contractItems;
    }

    @Override
    public String toString() {
        return "SupplierContract{" +
                "id=" + getId() +
                ", contractNumber='" + contractNumber + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                '}';
    }
}
