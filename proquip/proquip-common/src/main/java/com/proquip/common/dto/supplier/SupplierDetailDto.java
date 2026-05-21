package com.proquip.common.dto.supplier;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 仕入先詳細データ転送オブジェクト。
 *
 * <p>仕入先の全詳細情報（連絡先一覧、住所、認証情報を含む）を
 * 保持する。仕入先詳細画面や仕入先編集画面で使用される。</p>
 *
 * @author ProQuip開発チーム
 */
public class SupplierDetailDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 仕入先ID */
    private Long id;

    /** 会社名 */
    private String companyName;

    /** 仕入先コード */
    private String code;

    /** ステータス */
    private String status;

    /** 評価（0.00〜5.00） */
    private BigDecimal rating;

    /** 税務ID */
    private String taxId;

    /** 支払条件（日数） */
    private Integer paymentTermDays;

    /** ウェブサイト */
    private String website;

    /** メインの住所 */
    private String address;

    /** 市区町村 */
    private String city;

    /** 都道府県 */
    private String state;

    /** 郵便番号 */
    private String postalCode;

    /** 国 */
    private String country;

    /** 登録日 */
    private Date registrationDate;

    /** 備考 */
    private String notes;

    /** 連絡先一覧 */
    private List<SupplierContactDto> contacts = new ArrayList<>();

    /** 認証情報一覧 */
    private List<String> certifications = new ArrayList<>();

    /** 契約一覧 */
    private List<SupplierContractDto> contracts = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierDetailDto() {
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
    public String getCompanyName() {
        return companyName;
    }

    /**
     * 会社名を設定する。
     *
     * @param companyName 会社名
     */
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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
     * 税務IDを返す。
     *
     * @return 税務ID
     */
    public String getTaxId() {
        return taxId;
    }

    /**
     * 税務IDを設定する。
     *
     * @param taxId 税務ID
     */
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    /**
     * 支払条件（日数）を返す。
     *
     * @return 支払条件日数
     */
    public Integer getPaymentTermDays() {
        return paymentTermDays;
    }

    /**
     * 支払条件（日数）を設定する。
     *
     * @param paymentTermDays 支払条件日数
     */
    public void setPaymentTermDays(Integer paymentTermDays) {
        this.paymentTermDays = paymentTermDays;
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
     * メインの住所を返す。
     *
     * @return 住所
     */
    public String getAddress() {
        return address;
    }

    /**
     * メインの住所を設定する。
     *
     * @param address 住所
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * 市区町村を返す。
     *
     * @return 市区町村
     */
    public String getCity() {
        return city;
    }

    /**
     * 市区町村を設定する。
     *
     * @param city 市区町村
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * 都道府県を返す。
     *
     * @return 都道府県
     */
    public String getState() {
        return state;
    }

    /**
     * 都道府県を設定する。
     *
     * @param state 都道府県
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * 郵便番号を返す。
     *
     * @return 郵便番号
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * 郵便番号を設定する。
     *
     * @param postalCode 郵便番号
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
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
     * 登録日を返す。
     *
     * @return 登録日
     */
    public Date getRegistrationDate() {
        return registrationDate;
    }

    /**
     * 登録日を設定する。
     *
     * @param registrationDate 登録日
     */
    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
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

    /**
     * 連絡先一覧を返す。
     *
     * @return 連絡先DTOのリスト
     */
    public List<SupplierContactDto> getContacts() {
        return contacts;
    }

    /**
     * 連絡先一覧を設定する。
     *
     * @param contacts 連絡先DTOのリスト
     */
    public void setContacts(List<SupplierContactDto> contacts) {
        this.contacts = contacts;
    }

    /**
     * 認証情報一覧を返す。
     *
     * @return 認証名のリスト
     */
    public List<String> getCertifications() {
        return certifications;
    }

    /**
     * 認証情報一覧を設定する。
     *
     * @param certifications 認証名のリスト
     */
    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    /**
     * 契約一覧を返す。
     *
     * @return 契約DTOのリスト
     */
    public List<SupplierContractDto> getContracts() {
        return contracts;
    }

    /**
     * 契約一覧を設定する。
     *
     * @param contracts 契約DTOのリスト
     */
    public void setContracts(List<SupplierContractDto> contracts) {
        this.contracts = contracts;
    }
}
