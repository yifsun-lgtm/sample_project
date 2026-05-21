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
 * 承認ステップエンティティ。
 * <p>
 * 承認ワークフロー内の個々の承認ステップを表す。
 * 各ステップには承認者のロール、承認状態、承認日時、コメントが含まれる。
 * ステップは {@code stepOrder} で順序付けされる。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "approval_step")
public class ApprovalStep extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** ステップ順序（1始まり） */
    @NotNull
    @Column(name = "step_number", nullable = false)
    private Integer stepOrder;

    /**
     * 承認者のユーザーID。
     */
    @NotNull
    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    /**
     * ステップのステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: PENDING, APPROVED, REJECTED, SKIPPED
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 判断日時 */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "decided_at")
    private Date decidedAt;

    /** 承認者のコメント */
    @Size(max = 1000)
    @Column(name = "comments", length = 1000)
    private String comments;

    /** 親のワークフローへの参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    /**
     * デフォルトコンストラクタ。
     */
    public ApprovalStep() {
        super();
    }

    // --- Getter / Setter ---

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Date decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public ApprovalWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(ApprovalWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public String toString() {
        return "ApprovalStep{" +
                "stepOrder=" + stepOrder +
                ", approverId=" + approverId +
                ", status='" + status + '\'' +
                ", decidedAt=" + decidedAt +
                '}';
    }
}
