package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 承認ワークフローエンティティ。
 * <p>
 * 購買依頼や発注などに対する承認プロセスを管理する。
 * ポリモーフィックパターンを使用し、entityType と entityId の組み合わせで
 * 対象エンティティを識別する。
 * </p>
 * <p>
 * 【技術的負債】ポリモーフィック関連をJPAリレーションではなく
 * entityType/entityId の文字列+ID で実現しており、型安全性がない。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "approval_workflow")
@NamedQueries({
    @NamedQuery(
        name = "ApprovalWorkflow.findByEntity",
        query = "SELECT aw FROM ApprovalWorkflow aw WHERE aw.entityType = :entityType AND aw.entityId = :entityId"
    ),
    @NamedQuery(
        name = "ApprovalWorkflow.findPendingByType",
        query = "SELECT aw FROM ApprovalWorkflow aw WHERE aw.entityType = :entityType AND aw.status = 'PENDING' ORDER BY aw.createdDate DESC"
    )
})
public class ApprovalWorkflow extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 対象エンティティの種別。
     * <p>
     * 技術的負債: ポリモーフィック関連パターン。型安全でない文字列で管理している。
     * 有効値: PURCHASE_REQUISITION, PURCHASE_ORDER, STOCK_TRANSFER
     * </p>
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * 対象エンティティのID。
     * <p>
     * 技術的負債: entityType との組み合わせで対象を特定するが、
     * 外部キー制約が設定できないため参照整合性が保証されない。
     * </p>
     */
    @NotNull
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** 現在の承認ステップ番号 */
    @Column(name = "current_step")
    private Integer currentStep;

    /**
     * ワークフローステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: PENDING, IN_PROGRESS, APPROVED, REJECTED, CANCELLED
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** ワークフロー作成日時 */
    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "initiated_at", nullable = false)
    private Date createdDate;

    /** 承認ステップのリスト（順序付き） */
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStep> steps = new ArrayList<ApprovalStep>();

    /**
     * デフォルトコンストラクタ。
     */
    public ApprovalWorkflow() {
        super();
    }

    // --- Getter / Setter ---

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public List<ApprovalStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ApprovalStep> steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "ApprovalWorkflow{" +
                "entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", currentStep=" + currentStep +
                ", status='" + status + '\'' +
                '}';
    }
}
