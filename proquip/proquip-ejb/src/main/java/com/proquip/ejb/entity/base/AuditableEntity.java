package com.proquip.ejb.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * 監査情報を保持する基底エンティティ。
 * <p>
 * {@link BaseEntity} を拡張し、レコードの作成者・更新者を追跡する。
 * 操作を行ったユーザーの識別子が文字列として保存される。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** レコード作成者のユーザー識別子 */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /** レコード最終更新者のユーザー識別子 */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * デフォルトコンストラクタ。
     */
    public AuditableEntity() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * レコード作成者を返す。
     *
     * @return 作成者のユーザー識別子
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * レコード作成者を設定する。
     *
     * @param createdBy 作成者のユーザー識別子
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * レコード最終更新者を返す。
     *
     * @return 最終更新者のユーザー識別子
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * レコード最終更新者を設定する。
     *
     * @param updatedBy 最終更新者のユーザー識別子
     */
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
