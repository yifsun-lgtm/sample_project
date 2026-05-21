package com.proquip.common.dto.supplier;

import com.proquip.common.dto.common.SearchCriteria;

import java.math.BigDecimal;

/**
 * 仕入先検索条件DTOクラス。
 *
 * <p>仕入先一覧の検索・フィルタリングに使用する条件を保持する。
 * キーワード検索、ステータス、評価での絞り込みが可能。</p>
 *
 * @author ProQuip開発チーム
 */
public class SupplierSearchCriteria extends SearchCriteria {

    private static final long serialVersionUID = 1L;

    /** キーワード（会社名、コードを対象） */
    private String keyword;

    /** ステータス（ACTIVE, INACTIVE, SUSPENDED） */
    private String status;

    /** 最低評価 */
    private BigDecimal minRating;

    /** 国 */
    private String country;

    /** 認証名でフィルタ */
    private String certification;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierSearchCriteria() {
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
     * 最低評価を返す。
     *
     * @return 最低評価
     */
    public BigDecimal getMinRating() {
        return minRating;
    }

    /**
     * 最低評価を設定する。
     *
     * @param minRating 最低評価
     */
    public void setMinRating(BigDecimal minRating) {
        this.minRating = minRating;
    }

    /**
     * 国を返す。
     *
     * @return 国名
     */
    public String getCountry() {
        return country;
    }

    /**
     * 国を設定する。
     *
     * @param country 国名
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * 認証名を返す。
     *
     * @return 認証名
     */
    public String getCertification() {
        return certification;
    }

    /**
     * 認証名を設定する。
     *
     * @param certification 認証名
     */
    public void setCertification(String certification) {
        this.certification = certification;
    }
}
