package com.proquip.common.dto.procurement;

import com.proquip.common.dto.common.SearchCriteria;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 発注書検索条件DTOクラス。
 *
 * <p>発注書一覧の検索・フィルタリングに使用する条件を保持する。
 * 発注番号、仕入先、ステータス、金額帯、期間での絞り込みが可能。</p>
 *
 * @author ProQuip開発チーム
 */
public class PurchaseOrderSearchCriteria extends SearchCriteria {

    private static final long serialVersionUID = 1L;

    /** キーワード（発注番号、仕入先名を対象） */
    private String keyword;

    /** 仕入先ID */
    private Long supplierId;

    /** ステータス（DRAFT, PENDING_APPROVAL, APPROVED, ORDERED, RECEIVED, CLOSED） */
    private String status;

    /** 最低金額 */
    private BigDecimal minAmount;

    /** 最高金額 */
    private BigDecimal maxAmount;

    /** 発注日（開始） */
    private Date orderDateFrom;

    /** 発注日（終了） */
    private Date orderDateTo;

    /** 納品予定日（開始） */
    private Date expectedDeliveryFrom;

    /** 納品予定日（終了） */
    private Date expectedDeliveryTo;

    /** 作成者ユーザーID */
    private Long createdByUserId;

    /** 部門ID */
    private Long departmentId;

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseOrderSearchCriteria() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * キーワードを返す。
     *
     * @return 検索キーワード
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * キーワードを設定する。
     *
     * @param keyword 検索キーワード
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
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
     * 最低金額を返す。
     *
     * @return 最低金額
     */
    public BigDecimal getMinAmount() {
        return minAmount;
    }

    /**
     * 最低金額を設定する。
     *
     * @param minAmount 最低金額
     */
    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    /**
     * 最高金額を返す。
     *
     * @return 最高金額
     */
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    /**
     * 最高金額を設定する。
     *
     * @param maxAmount 最高金額
     */
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    /**
     * 発注日（開始）を返す。
     *
     * @return 発注日（開始）
     */
    public Date getOrderDateFrom() {
        return orderDateFrom;
    }

    /**
     * 発注日（開始）を設定する。
     *
     * @param orderDateFrom 発注日（開始）
     */
    public void setOrderDateFrom(Date orderDateFrom) {
        this.orderDateFrom = orderDateFrom;
    }

    /**
     * 発注日（終了）を返す。
     *
     * @return 発注日（終了）
     */
    public Date getOrderDateTo() {
        return orderDateTo;
    }

    /**
     * 発注日（終了）を設定する。
     *
     * @param orderDateTo 発注日（終了）
     */
    public void setOrderDateTo(Date orderDateTo) {
        this.orderDateTo = orderDateTo;
    }

    /**
     * 納品予定日（開始）を返す。
     *
     * @return 納品予定日（開始）
     */
    public Date getExpectedDeliveryFrom() {
        return expectedDeliveryFrom;
    }

    /**
     * 納品予定日（開始）を設定する。
     *
     * @param expectedDeliveryFrom 納品予定日（開始）
     */
    public void setExpectedDeliveryFrom(Date expectedDeliveryFrom) {
        this.expectedDeliveryFrom = expectedDeliveryFrom;
    }

    /**
     * 納品予定日（終了）を返す。
     *
     * @return 納品予定日（終了）
     */
    public Date getExpectedDeliveryTo() {
        return expectedDeliveryTo;
    }

    /**
     * 納品予定日（終了）を設定する。
     *
     * @param expectedDeliveryTo 納品予定日（終了）
     */
    public void setExpectedDeliveryTo(Date expectedDeliveryTo) {
        this.expectedDeliveryTo = expectedDeliveryTo;
    }

    /**
     * 作成者ユーザーIDを返す。
     *
     * @return 作成者ユーザーID
     */
    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    /**
     * 作成者ユーザーIDを設定する。
     *
     * @param createdByUserId 作成者ユーザーID
     */
    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    /**
     * 部門IDを返す。
     *
     * @return 部門ID
     */
    public Long getDepartmentId() {
        return departmentId;
    }

    /**
     * 部門IDを設定する。
     *
     * @param departmentId 部門ID
     */
    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
