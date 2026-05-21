package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商品マスタを表すエンティティ。
 *
 * <p>調達・在庫管理システムにおける商品の基本情報を管理する。
 * SKU（在庫管理単位）をキーとして商品を一意に識別する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@code status} フィールドが文字列型で定義されている。本来はEnumを使用すべき。</li>
 *   <li>一部の {@code @NamedQuery} が最適でないJPQLを含む。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product")
@NamedQueries({
    @NamedQuery(
        name = "Product.findBySku",
        query = "SELECT p FROM Product p WHERE p.sku = :sku"
    ),
    @NamedQuery(
        name = "Product.findByCategory",
        query = "SELECT p FROM Product p WHERE p.category.id = :categoryId"
    ),
    // 技術的負債: N+1問題を引き起こす可能性のあるクエリ（JOIN FETCHなし）
    @NamedQuery(
        name = "Product.findByStatus",
        query = "SELECT p FROM Product p WHERE p.status = :status"
    ),
    // 技術的負債: 非効率なLIKE検索（前方一致ではなく部分一致）
    @NamedQuery(
        name = "Product.searchByName",
        query = "SELECT p FROM Product p WHERE p.name LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%')"
    ),
    @NamedQuery(
        name = "Product.findByPriceRange",
        query = "SELECT p FROM Product p WHERE p.unitPrice BETWEEN :minPrice AND :maxPrice ORDER BY p.unitPrice ASC"
    ),
    // 技術的負債: SELECT * 相当（必要なフィールドだけ取得すべき）
    @NamedQuery(
        name = "Product.findByManufacturer",
        query = "SELECT p FROM Product p WHERE p.manufacturer.id = :manufacturerId ORDER BY p.name"
    ),
    @NamedQuery(
        name = "Product.findActiveProducts",
        query = "SELECT p FROM Product p WHERE p.status = 'ACTIVE' ORDER BY p.name"
    )
})
public class Product extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** SKU（在庫管理単位、一意） */
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    /** 商品名 */
    @Column(name = "name", nullable = false, length = 300)
    private String name;

    /** 商品詳細説明 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 単価 */
    @Column(name = "unit_price", precision = 18, scale = 4)
    private BigDecimal unitPrice;

    /**
     * 商品ステータス。
     * 技術的負債: 文字列で管理している。Enum型に移行すべき。
     * 想定値: "ACTIVE", "INACTIVE", "DISCONTINUED", "PENDING"
     */
    @Column(name = "status", length = 30)
    private String status;

    /** 最小発注数量 */
    @Column(name = "min_order_qty")
    private Integer minOrderQty;

    /** 重量（kg） */
    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weight;

    /** 幅（mm） */
    @Column(name = "width_mm", precision = 10, scale = 2)
    private BigDecimal width;

    /** 高さ（mm） */
    @Column(name = "height_mm", precision = 10, scale = 2)
    private BigDecimal height;

    /** 奥行（mm） */
    @Column(name = "depth_mm", precision = 10, scale = 2)
    private BigDecimal depth;

    /** 計量単位 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private UnitOfMeasure unit;

    /** リードタイム（日） */
    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    /** 備考 */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** 所属カテゴリ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** 製造元 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Manufacturer manufacturer;

    /** 商品仕様一覧 */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<ProductSpecification> specifications = new ArrayList<>();

    /** 商品画像一覧 */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    /** 商品ドキュメント一覧 */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductDocument> documents = new ArrayList<>();

    /** 商品タグ（多対多） */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_tag_mapping",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<ProductTag> tags = new HashSet<>();

    /**
     * デフォルトコンストラクタ。
     */
    public Product() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * SKUを返す。
     *
     * @return SKU
     */
    public String getSku() {
        return sku;
    }

    /**
     * SKUを設定する。
     *
     * @param sku SKU
     */
    public void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * 商品名を返す。
     *
     * @return 商品名
     */
    public String getName() {
        return name;
    }

    /**
     * 商品名を設定する。
     *
     * @param name 商品名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 商品詳細説明を返す。
     *
     * @return 商品詳細説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 商品詳細説明を設定する。
     *
     * @param description 商品詳細説明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 単価を返す。
     *
     * @return 単価
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * 単価を設定する。
     *
     * @param unitPrice 単価
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /**
     * 商品ステータスを返す。
     * 技術的負債: 文字列型。Enumに移行予定。
     *
     * @return 商品ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * 商品ステータスを設定する。
     *
     * @param status 商品ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 最小発注数量を返す。
     *
     * @return 最小発注数量
     */
    public Integer getMinOrderQty() {
        return minOrderQty;
    }

    /**
     * 最小発注数量を設定する。
     *
     * @param minOrderQty 最小発注数量
     */
    public void setMinOrderQty(Integer minOrderQty) {
        this.minOrderQty = minOrderQty;
    }

    /**
     * 重量（kg）を返す。
     *
     * @return 重量
     */
    public BigDecimal getWeight() {
        return weight;
    }

    /**
     * 重量（kg）を設定する。
     *
     * @param weight 重量
     */
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    /**
     * 幅（mm）を返す。
     *
     * @return 幅
     */
    public BigDecimal getWidth() {
        return width;
    }

    /**
     * 幅（mm）を設定する。
     *
     * @param width 幅
     */
    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    /**
     * 高さ（mm）を返す。
     *
     * @return 高さ
     */
    public BigDecimal getHeight() {
        return height;
    }

    /**
     * 高さ（mm）を設定する。
     *
     * @param height 高さ
     */
    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    /**
     * 奥行（mm）を返す。
     *
     * @return 奥行
     */
    public BigDecimal getDepth() {
        return depth;
    }

    /**
     * 奥行（mm）を設定する。
     *
     * @param depth 奥行
     */
    public void setDepth(BigDecimal depth) {
        this.depth = depth;
    }

    public UnitOfMeasure getUnit() {
        return unit;
    }

    public void setUnit(UnitOfMeasure unit) {
        this.unit = unit;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * 所属カテゴリを返す。
     *
     * @return カテゴリ
     */
    public Category getCategory() {
        return category;
    }

    /**
     * 所属カテゴリを設定する。
     *
     * @param category カテゴリ
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * 製造元を返す。
     *
     * @return 製造元
     */
    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    /**
     * 製造元を設定する。
     *
     * @param manufacturer 製造元
     */
    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * 商品仕様一覧を返す。
     *
     * @return 商品仕様のリスト
     */
    public List<ProductSpecification> getSpecifications() {
        return specifications;
    }

    /**
     * 商品仕様一覧を設定する。
     *
     * @param specifications 商品仕様のリスト
     */
    public void setSpecifications(List<ProductSpecification> specifications) {
        this.specifications = specifications;
    }

    /**
     * 商品画像一覧を返す。
     *
     * @return 商品画像のリスト
     */
    public List<ProductImage> getImages() {
        return images;
    }

    /**
     * 商品画像一覧を設定する。
     *
     * @param images 商品画像のリスト
     */
    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public List<ProductDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<ProductDocument> documents) {
        this.documents = documents;
    }

    /**
     * 商品タグセットを返す。
     *
     * @return 商品タグのセット
     */
    public Set<ProductTag> getTags() {
        return tags;
    }

    /**
     * 商品タグセットを設定する。
     *
     * @param tags 商品タグのセット
     */
    public void setTags(Set<ProductTag> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + getId() +
                ", sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", unitPrice=" + unitPrice +
                ", status='" + status + '\'' +
                '}';
    }
}
