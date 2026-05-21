package com.proquip.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 仕入先データ転送オブジェクト。
 *
 * <p>仕入先の基本情報をプレゼンテーション層やAPI応答として転送するためのクラス。
 * 連絡先や住所などの詳細はフラット化して主要な情報のみを保持する。</p>
 *
 * <p>技術的負債 #8: クラス名のサフィックスが "DTO"（大文字）。
 * {@link ProductDetailDto} 等の "Dto" サフィックスと不統一。</p>
 *
 * @author ProQuip開発チーム
 */
public class SupplierDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 仕入先ID */
    private Long id;

    /** 会社名 */
    private String name;

    /** 仕入先コード */
    private String code;

    /** ステータス */
    private String status;

    /** 評価（0.00〜5.00） */
    private BigDecimal rating;

    /** 連絡先メールアドレス */
    private String email;

    /** 連絡先電話番号 */
    private String phone;

    /** ウェブサイト */
    private String website;

    /** 住所（フラット化した文字列） */
    private String address;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierDTO() {
    }

    // --- Getter / Setter ---

    /**
     * 仕入先IDを返す。
     *
     * @return 仕入先ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 仕入先IDを設定する。
     *
     * @param id 仕入先ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 会社名を返す。
     *
     * @return 会社名
     */
    public String getName() {
        return name;
    }

    /**
     * 会社名を設定する。
     *
     * @param name 会社名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 仕入先コードを返す。
     *
     * @return 仕入先コード
     */
    public String getCode() {
        return code;
    }

    /**
     * 仕入先コードを設定する。
     *
     * @param code 仕入先コード
     */
    public void setCode(String code) {
        this.code = code;
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
     * 評価を返す。
     *
     * @return 評価
     */
    public BigDecimal getRating() {
        return rating;
    }

    /**
     * 評価を設定する。
     *
     * @param rating 評価
     */
    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    /**
     * 連絡先メールアドレスを返す。
     *
     * @return メールアドレス
     */
    public String getEmail() {
        return email;
    }

    /**
     * 連絡先メールアドレスを設定する。
     *
     * @param email メールアドレス
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 連絡先電話番号を返す。
     *
     * @return 電話番号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 連絡先電話番号を設定する。
     *
     * @param phone 電話番号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * ウェブサイトを返す。
     *
     * @return ウェブサイトURL
     */
    public String getWebsite() {
        return website;
    }

    /**
     * ウェブサイトを設定する。
     *
     * @param website ウェブサイトURL
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * 住所を返す。
     *
     * @return 住所文字列
     */
    public String getAddress() {
        return address;
    }

    /**
     * 住所を設定する。
     *
     * @param address 住所文字列
     */
    public void setAddress(String address) {
        this.address = address;
    }
}
