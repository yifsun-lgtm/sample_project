package com.proquip.ejb.entity.product;

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
 * 商品画像を表すエンティティ。
 *
 * <p>商品に紐付く画像ファイルのメタ情報を管理する。
 * 画像バイナリ自体はファイルシステム上に保存され、
 * 本エンティティではパス情報のみを保持する。</p>
 *
 * <p>{@code isPrimary} フラグにより、メイン画像を識別する。
 * {@code sortOrder} により表示順序を制御する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_image")
@NamedQueries({
    @NamedQuery(
        name = "ProductImage.findByProduct",
        query = "SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId ORDER BY pi.sortOrder"
    ),
    @NamedQuery(
        name = "ProductImage.findPrimaryByProduct",
        query = "SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isPrimary = true"
    )
})
public class ProductImage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 画像URL */
    @Column(name = "image_url", nullable = false, length = 500)
    private String fileName;

    /** サムネイルURL */
    @Column(name = "thumbnail_url", length = 500)
    private String filePath;

    /** 元ファイル名 */
    @Column(name = "file_name", nullable = false, length = 255)
    private String originalFileName;

    /** ディスク上のファイルパス */
    @Column(name = "file_path", nullable = false, length = 1000)
    private String diskPath;

    /** 画像種別（例: "PHOTO", "DIAGRAM"） */
    @Column(name = "image_type", length = 20)
    private String mimeType;

    /** メイン画像フラグ */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    /** 表示順序 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 紐付く商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public ProductImage() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * ファイル名を返す。
     *
     * @return ファイル名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * ファイル名を設定する。
     *
     * @param fileName ファイル名
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * ファイルパスを返す。
     *
     * @return ファイルパス
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * ファイルパスを設定する。
     *
     * @param filePath ファイルパス
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getDiskPath() {
        return diskPath;
    }

    public void setDiskPath(String diskPath) {
        this.diskPath = diskPath;
    }

    /**
     * MIMEタイプを返す。
     *
     * @return MIMEタイプ
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * MIMEタイプを設定する。
     *
     * @param mimeType MIMEタイプ
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * メイン画像かどうかを返す。
     *
     * @return メイン画像の場合 {@code true}
     */
    public boolean isPrimary() {
        return isPrimary;
    }

    /**
     * メイン画像フラグを設定する。
     *
     * @param primary メイン画像フラグ
     */
    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
    }

    /**
     * 表示順序を返す。
     *
     * @return 表示順序
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 表示順序を設定する。
     *
     * @param sortOrder 表示順序
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 紐付く商品を返す。
     *
     * @return 商品
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 紐付く商品を設定する。
     *
     * @param product 商品
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "ProductImage{" +
                "id=" + getId() +
                ", fileName='" + fileName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", isPrimary=" + isPrimary +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
