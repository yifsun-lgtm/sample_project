package com.proquip.ejb.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

import java.io.Serializable;
import java.util.Date;

/**
 * 全エンティティの基底クラス。
 * <p>
 * 共通フィールド（ID、作成日時、更新日時、バージョン）を提供する。
 * 楽観的ロックは {@code version} フィールドで制御される。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主キー（自動採番） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** レコード作成日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    /** レコード更新日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    /** 楽観的ロック用バージョン番号 */
    @Version
    @Column(name = "version")
    private Integer version;

    /**
     * デフォルトコンストラクタ。
     */
    public BaseEntity() {
    }

    /**
     * 永続化前にタイムスタンプを設定するコールバック。
     */
    @PrePersist
    protected void onPrePersist() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 更新前にタイムスタンプを更新するコールバック。
     */
    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = new Date();
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseEntity other = (BaseEntity) obj;
        if (id != null) {
            return id.equals(other.id);
        }
        return false;
    }
}
