package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Date;

/**
 * 発注ステータス変更履歴エンティティ。
 * <p>
 * 発注のステータスが変更された際の履歴を時系列で記録する。
 * 変更前後のステータス、変更日時、変更者、コメントを保持する。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "purchase_order_status_history")
public class PurchaseOrderStatusHistory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 変更前ステータス */
    @Size(max = 30)
    @Column(name = "from_status", length = 30)
    private String fromStatus;

    /** 変更後ステータス */
    @NotNull
    @Size(max = 30)
    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    /** 変更日時 */
    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "changed_at", nullable = false)
    private Date changedAt;

    /** 変更者のユーザーID */
    @Column(name = "changed_by")
    private Long changedBy;

    /** 変更理由・コメント */
    @Size(max = 1000)
    @Column(name = "change_reason", length = 1000)
    private String comments;

    /** 親の発注への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseOrderStatusHistory() {
        super();
    }

    // --- Getter / Setter ---

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    @Override
    public String toString() {
        return "PurchaseOrderStatusHistory{" +
                "fromStatus='" + fromStatus + '\'' +
                ", toStatus='" + toStatus + '\'' +
                ", changedAt=" + changedAt +
                ", changedBy='" + changedBy + '\'' +
                '}';
    }
}
