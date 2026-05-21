package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * 商品関連ドキュメントを表すエンティティ。
 *
 * <p>商品に紐付くドキュメント（仕様書、カタログ、マニュアル等）のメタ情報を管理する。
 * ドキュメントファイル自体はファイルシステム上に保存され、
 * 本エンティティではパス情報とバージョンを保持する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_document")
@NamedQueries({
    @NamedQuery(
        name = "ProductDocument.findByProduct",
        query = "SELECT pd FROM ProductDocument pd WHERE pd.product.id = :productId ORDER BY pd.docType, pd.version"
    ),
    @NamedQuery(
        name = "ProductDocument.findByDocType",
        query = "SELECT pd FROM ProductDocument pd WHERE pd.docType = :docType"
    )
})
public class ProductDocument extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /**
     * ドキュメント種別。
     * 想定値: "DATASHEET", "MSDS", "MANUAL", "DRAWING", "CERTIFICATE", "WARRANTY", "BROCHURE", "OTHER"
     */
    @Column(name = "document_type", nullable = false, length = 50)
    private String docType;

    /** ファイル名 */
    @Column(name = "document_name", nullable = false, length = 255)
    private String fileName;

    /** ファイルパス（ストレージ上の保存先） */
    @Column(name = "file_url", nullable = false, length = 1000)
    private String filePath;

    /** ドキュメントバージョン（例: "1.0", "2.1"） */
    @Column(name = "document_version", length = 20)
    private String docVersion;

    /** 紐付く商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public ProductDocument() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * ドキュメント種別を返す。
     *
     * @return ドキュメント種別
     */
    public String getDocType() {
        return docType;
    }

    /**
     * ドキュメント種別を設定する。
     *
     * @param docType ドキュメント種別
     */
    public void setDocType(String docType) {
        this.docType = docType;
    }

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

    /**
     * ドキュメントバージョンを返す。
     *
     * @return バージョン文字列
     */
    public String getDocVersion() {
        return docVersion;
    }

    /**
     * ドキュメントバージョンを設定する。
     *
     * @param docVersion バージョン文字列
     */
    public void setDocVersion(String docVersion) {
        this.docVersion = docVersion;
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
        return "ProductDocument{" +
                "id=" + getId() +
                ", docType='" + docType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", docVersion='" + docVersion + '\'' +
                '}';
    }
}
