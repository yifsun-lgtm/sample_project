package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品カテゴリを表すエンティティ。
 *
 * <p>カテゴリは自己参照構造（親子関係）を持ち、階層的な商品分類を実現する。
 * {@code level} フィールドにより、カテゴリの深さを明示的に保持する。</p>
 *
 * <p>ルートカテゴリの場合、{@code parent} は {@code null} となる。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "category")
@NamedQueries({
    @NamedQuery(
        name = "Category.findByCode",
        query = "SELECT c FROM Category c WHERE c.code = :code"
    ),
    @NamedQuery(
        name = "Category.findRootCategories",
        query = "SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.name"
    ),
    @NamedQuery(
        name = "Category.findByLevel",
        query = "SELECT c FROM Category c WHERE c.level = :level ORDER BY c.name"
    )
})
public class Category extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** カテゴリ名 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** カテゴリコード（一意） */
    @Column(name = "category_code", nullable = false, unique = true, length = 50)
    private String code;

    /** カテゴリの説明 */
    @Column(name = "description", length = 1000)
    private String description;

    /** 階層レベル（0がルート） */
    @Column(name = "level")
    private Integer level;

    /** 親カテゴリ（ルートの場合はnull） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** 子カテゴリ一覧 */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    /** このカテゴリに属する商品一覧 */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public Category() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * カテゴリ名を返す。
     *
     * @return カテゴリ名
     */
    public String getName() {
        return name;
    }

    /**
     * カテゴリ名を設定する。
     *
     * @param name カテゴリ名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * カテゴリコードを返す。
     *
     * @return カテゴリコード
     */
    public String getCode() {
        return code;
    }

    /**
     * カテゴリコードを設定する。
     *
     * @param code カテゴリコード
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * カテゴリの説明を返す。
     *
     * @return カテゴリの説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * カテゴリの説明を設定する。
     *
     * @param description カテゴリの説明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 階層レベルを返す。
     *
     * @return 階層レベル（0がルート）
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * 階層レベルを設定する。
     *
     * @param level 階層レベル
     */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
     * 親カテゴリを返す。
     *
     * @return 親カテゴリ（ルートの場合はnull）
     */
    public Category getParent() {
        return parent;
    }

    /**
     * 親カテゴリを設定する。
     *
     * @param parent 親カテゴリ
     */
    public void setParent(Category parent) {
        this.parent = parent;
    }

    /**
     * 子カテゴリ一覧を返す。
     *
     * @return 子カテゴリのリスト
     */
    public List<Category> getChildren() {
        return children;
    }

    /**
     * 子カテゴリ一覧を設定する。
     *
     * @param children 子カテゴリのリスト
     */
    public void setChildren(List<Category> children) {
        this.children = children;
    }

    /**
     * このカテゴリに属する商品一覧を返す。
     *
     * @return 商品のリスト
     */
    public List<Product> getProducts() {
        return products;
    }

    /**
     * このカテゴリに属する商品一覧を設定する。
     *
     * @param products 商品のリスト
     */
    public void setProducts(List<Product> products) {
        this.products = products;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", level=" + level +
                '}';
    }
}
