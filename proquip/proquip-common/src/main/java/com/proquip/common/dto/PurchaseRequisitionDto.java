package com.proquip.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 購買依頼データ転送オブジェクト。
 *
 * <p>購買依頼の情報をプレゼンテーション層やAPI応答として転送するためのクラス。
 * 依頼者情報と明細情報をフラット化して保持する。</p>
 *
 * @author ProQuip開発チーム
 */
public class PurchaseRequisitionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 購買依頼ID */
    private Long id;

    /** 購買依頼番号 */
    private String requisitionNumber;

    /** 件名 */
    private String title;

    /** 依頼者名 */
    private String requestedBy;

    /** 部門名 */
    private String departmentName;

    /** ステータス */
    private String status;

    /** 合計金額 */
    private BigDecimal totalAmount;

    /** 依頼日 */
    private Date requestDate;

    /** 必要期日 */
    private Date requiredDate;

    /** 明細件数 */
    private int itemCount;

    /** 正当性・理由 */
    private String justification;

    /** 緊急度 */
    private String urgency;

    /** 作成日時 */
    private Date createdAt;

    /** 更新日時 */
    private Date updatedAt;

    /** 部門名（フロントエンド互換） */
    private String department;

    /** 明細一覧 */
    private List<RequisitionItemDto> items = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseRequisitionDto() {
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequisitionNumber() {
        return requisitionNumber;
    }

    public void setRequisitionNumber(String requisitionNumber) {
        this.requisitionNumber = requisitionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Date getRequiredDate() {
        return requiredDate;
    }

    public void setRequiredDate(Date requiredDate) {
        this.requiredDate = requiredDate;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<RequisitionItemDto> getItems() {
        return items;
    }

    public void setItems(List<RequisitionItemDto> items) {
        this.items = items;
    }

    /**
     * 購買依頼明細の内部DTO。
     */
    public static class RequisitionItemDto implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 製品ID */
        private Long productId;

        /** 製品名 */
        private String productName;

        /** 数量 */
        private BigDecimal quantity;

        /** 見積単価 */
        private BigDecimal estimatedUnitPrice;

        /** 見積小計 */
        private BigDecimal estimatedSubtotal;

        public RequisitionItemDto() {
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

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getEstimatedUnitPrice() {
            return estimatedUnitPrice;
        }

        public void setEstimatedUnitPrice(BigDecimal estimatedUnitPrice) {
            this.estimatedUnitPrice = estimatedUnitPrice;
        }

        public BigDecimal getEstimatedSubtotal() {
            return estimatedSubtotal;
        }

        public void setEstimatedSubtotal(BigDecimal estimatedSubtotal) {
            this.estimatedSubtotal = estimatedSubtotal;
        }
    }
}
