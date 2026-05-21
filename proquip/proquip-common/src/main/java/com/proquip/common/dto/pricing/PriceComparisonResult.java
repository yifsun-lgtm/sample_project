package com.proquip.common.dto.pricing;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 価格比較結果データ転送オブジェクト。
 *
 * <p>同一製品に対する複数仕入先の価格を比較した結果を保持する。
 * 最安値、最高値、平均価格などの集計情報を含む。</p>
 *
 * @author ProQuip開発チーム
 */
public class PriceComparisonResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 製品ID */
    private Long productId;

    /** 製品名 */
    private String productName;

    /** SKUコード */
    private String skuCode;

    /** 最安値 */
    private BigDecimal lowestPrice;

    /** 最高値 */
    private BigDecimal highestPrice;

    /** 平均価格 */
    private BigDecimal averagePrice;

    /** 最安仕入先名 */
    private String lowestPriceSupplierName;

    /** 仕入先別価格一覧 */
    private List<SupplierPriceEntry> supplierPrices = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public PriceComparisonResult() {
    }

    // --- Getter / Setter ---

    /**
     * 製品IDを返す。
     *
     * @return 製品ID
     */
    public Long getProductId() {
        return productId;
    }

    /**
     * 製品IDを設定する。
     *
     * @param productId 製品ID
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * 製品名を返す。
     *
     * @return 製品名
     */
    public String getProductName() {
        return productName;
    }

    /**
     * 製品名を設定する。
     *
     * @param productName 製品名
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * SKUコードを返す。
     *
     * @return SKUコード
     */
    public String getSkuCode() {
        return skuCode;
    }

    /**
     * SKUコードを設定する。
     *
     * @param skuCode SKUコード
     */
    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    /**
     * 最安値を返す。
     *
     * @return 最安値
     */
    public BigDecimal getLowestPrice() {
        return lowestPrice;
    }

    /**
     * 最安値を設定する。
     *
     * @param lowestPrice 最安値
     */
    public void setLowestPrice(BigDecimal lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    /**
     * 最高値を返す。
     *
     * @return 最高値
     */
    public BigDecimal getHighestPrice() {
        return highestPrice;
    }

    /**
     * 最高値を設定する。
     *
     * @param highestPrice 最高値
     */
    public void setHighestPrice(BigDecimal highestPrice) {
        this.highestPrice = highestPrice;
    }

    /**
     * 平均価格を返す。
     *
     * @return 平均価格
     */
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    /**
     * 平均価格を設定する。
     *
     * @param averagePrice 平均価格
     */
    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    /**
     * 最安仕入先名を返す。
     *
     * @return 仕入先名
     */
    public String getLowestPriceSupplierName() {
        return lowestPriceSupplierName;
    }

    /**
     * 最安仕入先名を設定する。
     *
     * @param lowestPriceSupplierName 仕入先名
     */
    public void setLowestPriceSupplierName(String lowestPriceSupplierName) {
        this.lowestPriceSupplierName = lowestPriceSupplierName;
    }

    /**
     * 仕入先別価格一覧を返す。
     *
     * @return 仕入先価格エントリのリスト
     */
    public List<SupplierPriceEntry> getSupplierPrices() {
        return supplierPrices;
    }

    /**
     * 仕入先別価格一覧を設定する。
     *
     * @param supplierPrices 仕入先価格エントリのリスト
     */
    public void setSupplierPrices(List<SupplierPriceEntry> supplierPrices) {
        this.supplierPrices = supplierPrices;
    }

    /**
     * 仕入先別価格エントリの内部クラス。
     */
    public static class SupplierPriceEntry implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 仕入先ID */
        private Long supplierId;

        /** 仕入先名 */
        private String supplierName;

        /** 単価 */
        private BigDecimal unitPrice;

        /** 通貨コード */
        private String currency;

        /** 最終更新日 */
        private String lastUpdated;

        public SupplierPriceEntry() {
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
    }
}
