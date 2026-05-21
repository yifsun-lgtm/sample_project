package com.proquip.common.dto.procurement;

import java.io.Serializable;

/**
 * 入荷検収明細データ転送オブジェクト。
 *
 * <p>入荷検収の個別明細行を表す。発注明細に対応し、
 * 受領数量と品質メモを保持する。</p>
 *
 * @author ProQuip開発チーム
 */
public class GoodsReceiptItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明細ID */
    private Long id;

    /** 発注明細ID */
    private Long orderItemId;

    /** 製品ID */
    private Long productId;

    /** 製品名 */
    private String productName;

    /** SKUコード */
    private String skuCode;

    /** 発注数量 */
    private Integer orderedQuantity;

    /** 受領数量 */
    private Integer receivedQuantity;

    /** 不良数量 */
    private Integer rejectedQuantity;

    /** 品質備考 */
    private String qualityNotes;

    /** 検品合格フラグ */
    private boolean accepted;

    /**
     * デフォルトコンストラクタ。
     */
    public GoodsReceiptItemDto() {
    }

    // --- Getter / Setter ---

    /**
     * 明細IDを返す。
     *
     * @return 明細ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 明細IDを設定する。
     *
     * @param id 明細ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 発注明細IDを返す。
     *
     * @return 発注明細ID
     */
    public Long getOrderItemId() {
        return orderItemId;
    }

    /**
     * 発注明細IDを設定する。
     *
     * @param orderItemId 発注明細ID
     */
    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

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
     * 発注数量を返す。
     *
     * @return 発注数量
     */
    public Integer getOrderedQuantity() {
        return orderedQuantity;
    }

    /**
     * 発注数量を設定する。
     *
     * @param orderedQuantity 発注数量
     */
    public void setOrderedQuantity(Integer orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    /**
     * 受領数量を返す。
     *
     * @return 受領数量
     */
    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    /**
     * 受領数量を設定する。
     *
     * @param receivedQuantity 受領数量
     */
    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    /**
     * 不良数量を返す。
     *
     * @return 不良数量
     */
    public Integer getRejectedQuantity() {
        return rejectedQuantity;
    }

    /**
     * 不良数量を設定する。
     *
     * @param rejectedQuantity 不良数量
     */
    public void setRejectedQuantity(Integer rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    /**
     * 品質備考を返す。
     *
     * @return 品質備考
     */
    public String getQualityNotes() {
        return qualityNotes;
    }

    /**
     * 品質備考を設定する。
     *
     * @param qualityNotes 品質備考
     */
    public void setQualityNotes(String qualityNotes) {
        this.qualityNotes = qualityNotes;
    }

    /**
     * 検品合格フラグを返す。
     *
     * @return 合格の場合 true
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * 検品合格フラグを設定する。
     *
     * @param accepted 合格の場合 true
     */
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
