package com.proquip.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品詳細データ転送オブジェクト。
 *
 * <p>商品の詳細情報（仕様、画像、ドキュメント、代替品を含む）を
 * プレゼンテーション層やAPI応答として転送するためのクラス。
 * {@link ProductDTO} の全フィールドに加え、関連情報を内包する。</p>
 *
 * <p>技術的負債 #8: クラス名のサフィックスが "Dto"（混合ケース）であり、
 * {@link ProductDTO} の "DTO"（大文字）と命名規則が異なる。
 * 同じ商品ドメインでも命名が不統一。</p>
 *
 * @author ProQuip開発チーム
 */
public class ProductDetailDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long id;

    /** SKUコード */
    private String skuCode;

    /** 商品名 */
    private String name;

    /** 商品説明 */
    private String description;

    /** カテゴリID */
    private Long categoryId;

    /** カテゴリ名 */
    private String categoryName;

    /** 製造元ID */
    private Long manufacturerId;

    /** 製造元名 */
    private String manufacturerName;

    /** 単価 */
    private BigDecimal unitPrice;

    /** ステータス */
    private String status;

    /** 在庫数量 */
    private Integer stockQuantity;

    /** 再発注点 */
    private Integer reorderPoint;

    /** タグ一覧 */
    private List<String> tags = new ArrayList<>();

    /** 重量（kg） */
    private BigDecimal weight;

    /** 幅（mm） */
    private BigDecimal width;

    /** 高さ（mm） */
    private BigDecimal height;

    /** 奥行（mm） */
    private BigDecimal depth;

    /** 最小発注数量 */
    private Integer minOrderQty;

    /** 有効フラグ */
    private boolean active;

    /** 仕様（JSON文字列） */
    private String specifications;

    /** 画像一覧 */
    private List<ImageDto> images = new ArrayList<>();

    /** 代替品IDリスト */
    private List<Long> alternatives = new ArrayList<>();

    /** ドキュメント一覧 */
    private List<DocumentDto> documents = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public ProductDetailDto() {
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getSku() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getDepth() {
        return depth;
    }

    public void setDepth(BigDecimal depth) {
        this.depth = depth;
    }

    public Integer getMinOrderQty() {
        return minOrderQty;
    }

    public void setMinOrderQty(Integer minOrderQty) {
        this.minOrderQty = minOrderQty;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public List<ImageDto> getImages() {
        return images;
    }

    public void setImages(List<ImageDto> images) {
        this.images = images;
    }

    public List<Long> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<Long> alternatives) {
        this.alternatives = alternatives;
    }

    public List<DocumentDto> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentDto> documents) {
        this.documents = documents;
    }

    // --- 内部DTOクラス ---

    /**
     * 商品仕様の内部DTO。
     */
    public static class SpecificationDto implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 仕様名 */
        private String specName;

        /** 仕様値 */
        private String specValue;

        /** 仕様単位 */
        private String specUnit;

        /** 表示順序 */
        private Integer displayOrder;

        public SpecificationDto() {
        }

        public String getSpecName() {
            return specName;
        }

        public void setSpecName(String specName) {
            this.specName = specName;
        }

        public String getSpecValue() {
            return specValue;
        }

        public void setSpecValue(String specValue) {
            this.specValue = specValue;
        }

        public String getSpecUnit() {
            return specUnit;
        }

        public void setSpecUnit(String specUnit) {
            this.specUnit = specUnit;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
    }

    /**
     * 商品画像の内部DTO。
     */
    public static class ImageDto implements Serializable {

        private static final long serialVersionUID = 1L;

        /** ID */
        private Long id;

        /** ファイル名 */
        private String fileName;

        /** ファイルパス */
        private String filePath;

        /** MIMEタイプ */
        private String mimeType;

        /** メイン画像フラグ */
        private boolean primary;

        /** 表示順序 */
        private Integer sortOrder;

        public ImageDto() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public boolean isPrimary() {
            return primary;
        }

        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * 商品ドキュメントの内部DTO。
     */
    public static class DocumentDto implements Serializable {

        private static final long serialVersionUID = 1L;

        /** ID */
        private Long id;

        /** ドキュメント種別 */
        private String docType;

        /** ファイル名 */
        private String fileName;

        /** ファイルパス */
        private String filePath;

        /** バージョン */
        private String docVersion;

        public DocumentDto() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getDocVersion() {
            return docVersion;
        }

        public void setDocVersion(String docVersion) {
            this.docVersion = docVersion;
        }
    }
}
