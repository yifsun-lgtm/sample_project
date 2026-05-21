package com.proquip.common.dto.product;

import java.io.Serializable;

/**
 * 製造元データ転送オブジェクト。
 *
 * <p>製造元（メーカー）の基本情報をプレゼンテーション層やAPI応答として転送する。</p>
 *
 * @author ProQuip開発チーム
 */
public class ManufacturerDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 製造元ID */
    private Long id;

    /** 製造元名 */
    private String name;

    /** 製造元コード */
    private String code;

    /** 国 */
    private String country;

    /** ウェブサイト */
    private String website;

    /** 有効フラグ */
    private boolean active;

    /**
     * デフォルトコンストラクタ。
     */
    public ManufacturerDto() {
    }

    // --- Getter / Setter ---

    /**
     * 製造元IDを返す。
     *
     * @return 製造元ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 製造元IDを設定する。
     *
     * @param id 製造元ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 製造元名を返す。
     *
     * @return 製造元名
     */
    public String getName() {
        return name;
    }

    /**
     * 製造元名を設定する。
     *
     * @param name 製造元名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 製造元コードを返す。
     *
     * @return 製造元コード
     */
    public String getCode() {
        return code;
    }

    /**
     * 製造元コードを設定する。
     *
     * @param code 製造元コード
     */
    public void setCode(String code) {
        this.code = code;
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
     * 有効フラグを返す。
     *
     * @return 有効な場合 true
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 有効フラグを設定する。
     *
     * @param active 有効な場合 true
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
