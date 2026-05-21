package com.proquip.common.dto.supplier;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 仕入先契約データ転送オブジェクト。
 *
 * <p>仕入先との契約情報を保持する。契約番号、期間、金額上限などを含む。</p>
 *
 * @author ProQuip開発チーム
 */
public class SupplierContractDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 契約ID */
    private Long id;

    /** 契約番号 */
    private String contractNumber;

    /** 契約名 */
    private String name;

    /** 契約開始日 */
    private Date startDate;

    /** 契約終了日 */
    private Date endDate;

    /** 契約金額上限 */
    private BigDecimal maxAmount;

    /** 通貨コード */
    private String currency;

    /** ステータス（ACTIVE, EXPIRED, TERMINATED） */
    private String status;

    /** 自動更新フラグ */
    private boolean autoRenew;

    /** 備考 */
    private String notes;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierContractDto() {
    }

    // --- Getter / Setter ---

    /**
     * 契約IDを返す。
     *
     * @return 契約ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 契約IDを設定する。
     *
     * @param id 契約ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 契約番号を返す。
     *
     * @return 契約番号
     */
    public String getContractNumber() {
        return contractNumber;
    }

    /**
     * 契約番号を設定する。
     *
     * @param contractNumber 契約番号
     */
    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    /**
     * 契約名を返す。
     *
     * @return 契約名
     */
    public String getName() {
        return name;
    }

    /**
     * 契約名を設定する。
     *
     * @param name 契約名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 契約開始日を返す。
     *
     * @return 契約開始日
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * 契約開始日を設定する。
     *
     * @param startDate 契約開始日
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * 契約終了日を返す。
     *
     * @return 契約終了日
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * 契約終了日を設定する。
     *
     * @param endDate 契約終了日
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * 契約金額上限を返す。
     *
     * @return 金額上限
     */
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    /**
     * 契約金額上限を設定する。
     *
     * @param maxAmount 金額上限
     */
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
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
     * 自動更新フラグを返す。
     *
     * @return 自動更新の場合 true
     */
    public boolean isAutoRenew() {
        return autoRenew;
    }

    /**
     * 自動更新フラグを設定する。
     *
     * @param autoRenew 自動更新の場合 true
     */
    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    /**
     * 備考を返す。
     *
     * @return 備考
     */
    public String getNotes() {
        return notes;
    }

    /**
     * 備考を設定する。
     *
     * @param notes 備考
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
