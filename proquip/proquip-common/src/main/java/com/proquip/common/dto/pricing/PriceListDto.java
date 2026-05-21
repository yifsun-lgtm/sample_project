package com.proquip.common.dto.pricing;

import java.io.Serializable;
import java.util.Date;

/**
 * 価格リストデータ転送オブジェクト。
 *
 * <p>価格リストのヘッダー情報を保持する。通貨、有効期間、
 * ステータスなどの基本情報を含む。</p>
 *
 * @author ProQuip開発チーム
 */
public class PriceListDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 価格リストID */
    private Long id;

    /** 価格リスト名 */
    private String name;

    /** 通貨コード */
    private String currency;

    /** 有効開始日 */
    private Date effectiveFrom;

    /** 有効終了日 */
    private Date effectiveTo;

    /** ステータス（DRAFT, ACTIVE, EXPIRED, ARCHIVED） */
    private String status;

    /** 登録アイテム数 */
    private int itemCount;

    /** 説明 */
    private String description;

    /**
     * デフォルトコンストラクタ。
     */
    public PriceListDto() {
    }

    // --- Getter / Setter ---

    /**
     * 価格リストIDを返す。
     *
     * @return 価格リストID
     */
    public Long getId() {
        return id;
    }

    /**
     * 価格リストIDを設定する。
     *
     * @param id 価格リストID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 価格リスト名を返す。
     *
     * @return 価格リスト名
     */
    public String getName() {
        return name;
    }

    /**
     * 価格リスト名を設定する。
     *
     * @param name 価格リスト名
     */
    public void setName(String name) {
        this.name = name;
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
     * 有効開始日を返す。
     *
     * @return 有効開始日
     */
    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    /**
     * 有効開始日を設定する。
     *
     * @param effectiveFrom 有効開始日
     */
    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    /**
     * 有効終了日を返す。
     *
     * @return 有効終了日
     */
    public Date getEffectiveTo() {
        return effectiveTo;
    }

    /**
     * 有効終了日を設定する。
     *
     * @param effectiveTo 有効終了日
     */
    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
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
     * 登録アイテム数を返す。
     *
     * @return アイテム数
     */
    public int getItemCount() {
        return itemCount;
    }

    /**
     * 登録アイテム数を設定する。
     *
     * @param itemCount アイテム数
     */
    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    /**
     * 説明を返す。
     *
     * @return 説明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 説明を設定する。
     *
     * @param description 説明
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
