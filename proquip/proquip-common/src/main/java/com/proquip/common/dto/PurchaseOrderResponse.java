package com.proquip.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 発注応答データ転送オブジェクト。
 *
 * <p>発注情報をAPI応答として返すためのクラス。
 * 発注明細を内包した階層構造を持つ。</p>
 *
 * <p>技術的負債 #8: クラス名のサフィックスが "Response" であり、
 * "DTO" や "Dto" とは異なるパターン。同じプロジェクト内で
 * 3種類の命名規則が混在しており、新規開発者が混乱しやすい。</p>
 *
 * @author ProQuip開発チーム
 */
public class PurchaseOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 発注ID */
    private Long id;

    /** 発注番号 */
    private String orderNumber;

    /** 仕入先名 */
    private String supplierName;

    /** 仕入先ID */
    private Long supplierId;

    /** ステータス */
    private String status;

    /** 発注日 */
    private Date orderDate;

    /** 納品予定日 */
    private Date expectedDeliveryDate;

    /** 合計金額 */
    private BigDecimal totalAmount;

    /** 通貨コード */
    private String currency;

    /** 発注明細のリスト */
    private List<PurchaseOrderItemResponse> items = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseOrderResponse() {
    }

    // --- Getter / Setter ---

    /**
     * 発注IDを返す。
     *
     * @return 発注ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 発注IDを設定する。
     *
     * @param id 発注ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 発注番号を返す。
     *
     * @return 発注番号
     */
    public String getOrderNumber() {
        return orderNumber;
    }

    /**
     * 発注番号を設定する。
     *
     * @param orderNumber 発注番号
     */
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    /**
     * 仕入先名を返す。
     *
     * @return 仕入先名
     */
    public String getSupplierName() {
        return supplierName;
    }

    /**
     * 仕入先名を設定する。
     *
     * @param supplierName 仕入先名
     */
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    /**
     * 仕入先IDを返す。
     *
     * @return 仕入先ID
     */
    public Long getSupplierId() {
        return supplierId;
    }

    /**
     * 仕入先IDを設定する。
     *
     * @param supplierId 仕入先ID
     */
    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * ステータスを返す。
     *
     * @return ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 発注日を返す。
     *
     * @return 発注日
     */
    public Date getOrderDate() {
        return orderDate;
    }

    /**
     * 発注日を設定する。
     *
     * @param orderDate 発注日
     */
    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * 納品予定日を返す。
     *
     * @return 納品予定日
     */
    public Date getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    /**
     * 納品予定日を設定する。
     *
     * @param expectedDeliveryDate 納品予定日
     */
    public void setExpectedDeliveryDate(Date expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    /**
     * 合計金額を返す。
     *
     * @return 合計金額
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * 合計金額を設定する。
     *
     * @param totalAmount 合計金額
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 通貨コードを返す。
     *
     * @return 通貨コード
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * 通貨コードを設定する。
     *
     * @param currency 通貨コード
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * 発注明細のリストを返す。
     *
     * @return 発注明細リスト
     */
    public List<PurchaseOrderItemResponse> getItems() {
        return items;
    }

    /**
     * 発注明細のリストを設定する。
     *
     * @param items 発注明細リスト
     */
    public void setItems(List<PurchaseOrderItemResponse> items) {
        this.items = items;
    }

    // --- 内部DTOクラス ---

    /**
     * 発注明細の応答DTO。
     *
     * <p>技術的負債: 親クラスが "Response" サフィックスなので、
     * 内部クラスも "Response" で統一しているが、プロジェクト全体としては不統一。</p>
     */
    public static class PurchaseOrderItemResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 明細ID */
        private Long id;

        /** 行番号 */
        private Integer lineNumber;

        /** 製品ID */
        private Long productId;

        /** 製品名 */
        private String productName;

        /** SKUコード */
        private String skuCode;

        /** 数量 */
        private BigDecimal quantity;

        /** 単価 */
        private BigDecimal unitPrice;

        /** 小計 */
        private BigDecimal subtotal;

        /** 受入済み数量 */
        private BigDecimal receivedQuantity;

        /** 明細ステータス */
        private String status;

        public PurchaseOrderItemResponse() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getSkuCode() {
            return skuCode;
        }

        public void setSkuCode(String skuCode) {
            this.skuCode = skuCode;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }

        public BigDecimal getReceivedQuantity() {
            return receivedQuantity;
        }

        public void setReceivedQuantity(BigDecimal receivedQuantity) {
            this.receivedQuantity = receivedQuantity;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
