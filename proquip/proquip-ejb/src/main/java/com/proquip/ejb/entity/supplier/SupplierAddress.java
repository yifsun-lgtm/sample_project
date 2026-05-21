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

/**
 * 仕入先住所を表すエンティティ。
 *
 * <p>仕入先の住所情報を管理する。1つの仕入先に対して複数の住所
 * （本社、工場、倉庫など）を登録できる。
 * {@code addressType} により住所の用途を区別する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier_address")
@NamedQueries({
    @NamedQuery(
        name = "SupplierAddress.findBySupplier",
        query = "SELECT sa FROM SupplierAddress sa WHERE sa.supplier.id = :supplierId ORDER BY sa.addressType"
    ),
    @NamedQuery(
        name = "SupplierAddress.findByType",
        query = "SELECT sa FROM SupplierAddress sa WHERE sa.supplier.id = :supplierId AND sa.addressType = :addressType"
    )
})
public class SupplierAddress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 住所種別。
     * 想定値: "HEAD_OFFICE", "FACTORY", "WAREHOUSE", "BILLING"
     */
    @Column(name = "address_type", nullable = false, length = 30)
    private String addressType;

    /** 通り・番地 */
    @Column(name = "address_line1", length = 500)
    private String street;

    /** 市区町村 */
    @Column(name = "city", length = 100)
    private String city;

    /** 都道府県・州 */
    @Column(name = "state_province", length = 100)
    private String state;

    /** 郵便番号 */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /** 国 */
    @Column(name = "country_code", length = 100)
    private String country;

    /** 仕入先 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierAddress() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 住所種別を返す。
     *
     * @return 住所種別
     */
    public String getAddressType() {
        return addressType;
    }

    /**
     * 住所種別を設定する。
     *
     * @param addressType 住所種別
     */
    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    /**
     * 通り・番地を返す。
     *
     * @return 通り・番地
     */
    public String getStreet() {
        return street;
    }

    /**
     * 通り・番地を設定する。
     *
     * @param street 通り・番地
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * 市区町村を返す。
     *
     * @return 市区町村
     */
    public String getCity() {
        return city;
    }

    /**
     * 市区町村を設定する。
     *
     * @param city 市区町村
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * 都道府県・州を返す。
     *
     * @return 都道府県・州
     */
    public String getState() {
        return state;
    }

    /**
     * 都道府県・州を設定する。
     *
     * @param state 都道府県・州
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 郵便番号を返す。
     *
     * @return 郵便番号
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * 郵便番号を設定する。
     *
     * @param postalCode 郵便番号
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * 国を返す。
     *
     * @return 国
     */
    public String getCountry() {
        return country;
    }

    /**
     * 国を設定する。
     *
     * @param country 国
     */
    public void setCountry(String country) {
        this.country = country;
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
        return "SupplierAddress{" +
                "id=" + getId() +
                ", addressType='" + addressType + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
