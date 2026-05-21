package com.proquip.ejb.entity.supplier;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * 仕入先（サプライヤー）を表すエンティティ。
 *
 * <p>調達先の基本情報を管理する。仕入先コード（code）で一意に識別され、
 * 評価（rating）や支払条件（paymentTermDays）などの取引条件を保持する。</p>
 *
 * <p>技術的負債: {@code status} フィールドが文字列型で定義されている。
 * 本来はEnumを使用すべきだが、{@link com.proquip.ejb.entity.product.Product} の
 * status フィールドとも一貫性がなく、将来的に統一する必要がある。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier")
@NamedQueries({
    @NamedQuery(
        name = "Supplier.findByCode",
        query = "SELECT s FROM Supplier s WHERE s.code = :code"
    ),
    @NamedQuery(
        name = "Supplier.findByStatus",
        query = "SELECT s FROM Supplier s WHERE s.status = :status ORDER BY s.name"
    ),
})
public class Supplier extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 仕入先コード（一意） */
    @Column(name = "supplier_code", nullable = false, unique = true, length = 50)
    private String code;

    /** 仕入先名 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 税務登録番号 */
    @Column(name = "tax_id", length = 50)
    private String taxId;

    /**
     * 仕入先ステータス。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "ACTIVE", "INACTIVE", "SUSPENDED", "PENDING_APPROVAL"
     */
    @Column(name = "status", length = 30)
    private String status;

    /** 連絡先一覧 */
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<SupplierContact> contacts = new ArrayList<>();

    /** 取扱商品一覧 */
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<SupplierProduct> products = new ArrayList<>();

    /** 契約一覧 */
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<SupplierContract> contracts = new ArrayList<>();

    /** 住所一覧 */
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<SupplierAddress> addresses = new ArrayList<>();

    /** 評価履歴一覧 */
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<SupplierRating> ratings = new ArrayList<>();

    /** 認証・資格一覧 */
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<SupplierCertification> certifications = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public Supplier() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 仕入先コードを返す。
     *
     * @return 仕入先コード
     */
    public String getCode() {
        return code;
    }

    /**
     * 仕入先コードを設定する。
     *
     * @param code 仕入先コード
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 仕入先名を返す。
     *
     * @return 仕入先名
     */
    public String getName() {
        return name;
    }

    /**
     * 仕入先名を設定する。
     *
     * @param name 仕入先名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 税務登録番号を返す。
     *
     * @return 税務登録番号
     */
    public String getTaxId() {
        return taxId;
    }

    /**
     * 税務登録番号を設定する。
     *
     * @param taxId 税務登録番号
     */
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    /**
     * 仕入先ステータスを返す。
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return 仕入先ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * 仕入先ステータスを設定する。
     *
     * @param status 仕入先ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 連絡先一覧を返す。
     *
     * @return 連絡先のリスト
     */
    public List<SupplierContact> getContacts() {
        return contacts;
    }

    /**
     * 連絡先一覧を設定する。
     *
     * @param contacts 連絡先のリスト
     */
    public void setContacts(List<SupplierContact> contacts) {
        this.contacts = contacts;
    }

    /**
     * 取扱商品一覧を返す。
     *
     * @return 取扱商品のリスト
     */
    public List<SupplierProduct> getProducts() {
        return products;
    }

    /**
     * 取扱商品一覧を設定する。
     *
     * @param products 取扱商品のリスト
     */
    public void setProducts(List<SupplierProduct> products) {
        this.products = products;
    }

    /**
     * 契約一覧を返す。
     *
     * @return 契約のリスト
     */
    public List<SupplierContract> getContracts() {
        return contracts;
    }

    /**
     * 契約一覧を設定する。
     *
     * @param contracts 契約のリスト
     */
    public void setContracts(List<SupplierContract> contracts) {
        this.contracts = contracts;
    }

    /**
     * 住所一覧を返す。
     *
     * @return 住所のリスト
     */
    public List<SupplierAddress> getAddresses() {
        return addresses;
    }

    /**
     * 住所一覧を設定する。
     *
     * @param addresses 住所のリスト
     */
    public void setAddresses(List<SupplierAddress> addresses) {
        this.addresses = addresses;
    }

    /**
     * 評価履歴一覧を返す。
     *
     * @return 評価履歴のリスト
     */
    public List<SupplierRating> getRatings() {
        return ratings;
    }

    /**
     * 評価履歴一覧を設定する。
     *
     * @param ratings 評価履歴のリスト
     */
    public void setRatings(List<SupplierRating> ratings) {
        this.ratings = ratings;
    }

    /**
     * 認証・資格一覧を返す。
     *
     * @return 認証・資格のリスト
     */
    public List<SupplierCertification> getCertifications() {
        return certifications;
    }

    /**
     * 認証・資格一覧を設定する。
     *
     * @param certifications 認証・資格のリスト
     */
    public void setCertifications(List<SupplierCertification> certifications) {
        this.certifications = certifications;
    }

    @Override
    public String toString() {
        return "Supplier{" +
                "id=" + getId() +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
