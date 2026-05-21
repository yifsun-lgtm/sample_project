package com.proquip.ejb.entity.product;

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
 * 製造元（メーカー）を表すエンティティ。
 *
 * <p>商品を供給するメーカーの基本情報を管理する。
 * 各商品は必ず1つのメーカーに紐付く。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "manufacturer")
@NamedQueries({
    @NamedQuery(
        name = "Manufacturer.findByCode",
        query = "SELECT m FROM Manufacturer m WHERE m.code = :code"
    ),
    @NamedQuery(
        name = "Manufacturer.findByCountry",
        query = "SELECT m FROM Manufacturer m WHERE m.country = :country ORDER BY m.name"
    )
})
public class Manufacturer extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** メーカー名 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** メーカーコード（一意） */
    @Column(name = "manufacturer_code", nullable = false, unique = true, length = 50)
    private String code;

    /** メーカーのWebサイトURL */
    @Column(name = "website", length = 500)
    private String website;

    /** メーカーの所在国 */
    @Column(name = "country", length = 100)
    private String country;

    /** このメーカーが製造する商品一覧 */
    @OneToMany(mappedBy = "manufacturer", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public Manufacturer() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * メーカー名を返す。
     *
     * @return メーカー名
     */
    public String getName() {
        return name;
    }

    /**
     * メーカー名を設定する。
     *
     * @param name メーカー名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * メーカーコードを返す。
     *
     * @return メーカーコード
     */
    public String getCode() {
        return code;
    }

    /**
     * メーカーコードを設定する。
     *
     * @param code メーカーコード
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * メーカーのWebサイトURLを返す。
     *
     * @return WebサイトURL
     */
    public String getWebsite() {
        return website;
    }

    /**
     * メーカーのWebサイトURLを設定する。
     *
     * @param website WebサイトURL
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * メーカーの所在国を返す。
     *
     * @return 所在国
     */
    public String getCountry() {
        return country;
    }

    /**
     * メーカーの所在国を設定する。
     *
     * @param country 所在国
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * このメーカーが製造する商品一覧を返す。
     *
     * @return 商品のリスト
     */
    public List<Product> getProducts() {
        return products;
    }

    /**
     * このメーカーが製造する商品一覧を設定する。
     *
     * @param products 商品のリスト
     */
    public void setProducts(List<Product> products) {
        this.products = products;
    }

    @Override
    public String toString() {
        return "Manufacturer{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
