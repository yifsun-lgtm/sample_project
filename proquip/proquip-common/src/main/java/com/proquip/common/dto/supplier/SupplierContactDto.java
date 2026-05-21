package com.proquip.common.dto.supplier;

import java.io.Serializable;

/**
 * 仕入先連絡先データ転送オブジェクト。
 *
 * <p>仕入先に所属する担当者の連絡先情報を保持する。
 * {@link SupplierDetailDto} の内部DTOとして使用される。</p>
 *
 * @author ProQuip開発チーム
 */
public class SupplierContactDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 連絡先ID */
    private Long id;

    /** 担当者名 */
    private String name;

    /** 役職 */
    private String title;

    /** メールアドレス */
    private String email;

    /** 電話番号 */
    private String phone;

    /** 主担当者フラグ */
    private boolean primary;

    /** 部門 */
    private String department;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierContactDto() {
    }

    // --- Getter / Setter ---

    /**
     * 連絡先IDを返す。
     *
     * @return 連絡先ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 連絡先IDを設定する。
     *
     * @param id 連絡先ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 担当者名を返す。
     *
     * @return 担当者名
     */
    public String getName() {
        return name;
    }

    /**
     * 担当者名を設定する。
     *
     * @param name 担当者名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 役職を返す。
     *
     * @return 役職
     */
    public String getTitle() {
        return title;
    }

    /**
     * 役職を設定する。
     *
     * @param title 役職
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * メールアドレスを返す。
     *
     * @return メールアドレス
     */
    public String getEmail() {
        return email;
    }

    /**
     * メールアドレスを設定する。
     *
     * @param email メールアドレス
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 電話番号を返す。
     *
     * @return 電話番号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 電話番号を設定する。
     *
     * @param phone 電話番号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 主担当者フラグを返す。
     *
     * @return 主担当者の場合 true
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * 主担当者フラグを設定する。
     *
     * @param primary 主担当者の場合 true
     */
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * 部門を返す。
     *
     * @return 部門名
     */
    public String getDepartment() {
        return department;
    }

    /**
     * 部門を設定する。
     *
     * @param department 部門名
     */
    public void setDepartment(String department) {
        this.department = department;
    }
}
