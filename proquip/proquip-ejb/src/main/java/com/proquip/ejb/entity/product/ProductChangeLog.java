package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;



/**
 * 商品変更履歴を表すエンティティ。
 *
 * <p>商品マスタに対する変更を監査証跡として記録する。
 * フィールド単位で変更前後の値を保持し、いつ誰が変更したかを追跡する。</p>
 *
 * <p>変更日時はBaseEntityの {@code createdAt} を使用する。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "product_change_log")
@NamedQueries({
    @NamedQuery(
        name = "ProductChangeLog.findByProduct",
        query = "SELECT cl FROM ProductChangeLog cl WHERE cl.product.id = :productId ORDER BY cl.createdAt DESC"
    ),
    @NamedQuery(
        name = "ProductChangeLog.findByChangeType",
        query = "SELECT cl FROM ProductChangeLog cl WHERE cl.changeType = :changeType ORDER BY cl.createdAt DESC"
    )
})
public class ProductChangeLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 変更種別。
     * 想定値: "CREATE", "UPDATE", "DELETE", "STATUS_CHANGE"
     */
    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType;

    /** 変更対象フィールド名 */
    @Column(name = "field_name", length = 100)
    private String fieldName;

    /** 変更前の値 */
    @Column(name = "old_value", length = 2000)
    private String oldValue;

    /** 変更後の値 */
    @Column(name = "new_value", length = 2000)
    private String newValue;

    /** 変更実行者のユーザーID */
    @Column(name = "changed_by")
    private Long changedBy;

    /** 変更対象の商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public ProductChangeLog() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 変更種別を返す。
     *
     * @return 変更種別
     */
    public String getChangeType() {
        return changeType;
    }

    /**
     * 変更種別を設定する。
     *
     * @param changeType 変更種別
     */
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    /**
     * 変更対象フィールド名を返す。
     *
     * @return フィールド名
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * 変更対象フィールド名を設定する。
     *
     * @param fieldName フィールド名
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * 変更前の値を返す。
     *
     * @return 変更前の値
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * 変更前の値を設定する。
     *
     * @param oldValue 変更前の値
     */
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    /**
     * 変更後の値を返す。
     *
     * @return 変更後の値
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * 変更後の値を設定する。
     *
     * @param newValue 変更後の値
     */
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    /**
     * 変更実行者のユーザーIDを返す。
     *
     * @return 変更実行者のユーザーID
     */
    public Long getChangedBy() {
        return changedBy;
    }

    /**
     * 変更実行者のユーザーIDを設定する。
     *
     * @param changedBy 変更実行者のユーザーID
     */
    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    /**
     * 変更対象の商品を返す。
     *
     * @return 商品
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 変更対象の商品を設定する。
     *
     * @param product 商品
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "ProductChangeLog{" +
                "id=" + getId() +
                ", changeType='" + changeType + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", changedBy=" + changedBy +
                '}';
    }
}
