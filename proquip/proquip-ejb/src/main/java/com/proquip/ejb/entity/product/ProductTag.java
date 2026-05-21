package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * 商品タグを表すエンティティ。
 *
 * <p>商品に付与される分類用タグを管理する。
 * 多対多の関係で商品と紐付き、柔軟なグルーピングを実現する。</p>
 *
 * <p>{@code color} フィールドにより、UI上でのタグ表示色を制御できる。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_tag")
@NamedQueries({
    @NamedQuery(
        name = "ProductTag.findByName",
        query = "SELECT t FROM ProductTag t WHERE t.name = :name"
    ),
    @NamedQuery(
        name = "ProductTag.findAll",
        query = "SELECT t FROM ProductTag t ORDER BY t.name"
    )
})
public class ProductTag extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** タグ名 */
    @Column(name = "tag_name", nullable = false, unique = true, length = 100)
    private String name;

    /** タグ表示色（HTML色コード、例: "#FF5733"） */
    @Column(name = "tag_color", length = 20)
    private String color;

    /** このタグが付与された商品セット */
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    /**
     * デフォルトコンストラクタ。
     */
    public ProductTag() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * タグ名を返す。
     *
     * @return タグ名
     */
    public String getName() {
        return name;
    }

    /**
     * タグ名を設定する。
     *
     * @param name タグ名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * タグ表示色を返す。
     *
     * @return HTML色コード
     */
    public String getColor() {
        return color;
    }

    /**
     * タグ表示色を設定する。
     *
     * @param color HTML色コード
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * このタグが付与された商品セットを返す。
     *
     * @return 商品のセット
     */
    public Set<Product> getProducts() {
        return products;
    }

    /**
     * このタグが付与された商品セットを設定する。
     *
     * @param products 商品のセット
     */
    public void setProducts(Set<Product> products) {
        this.products = products;
    }

    @Override
    public String toString() {
        return "ProductTag{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
