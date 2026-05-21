package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.AuditableEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 購買依頼エンティティ。
 * <p>
 * 購買部門への資材・サービスの購入依頼を表す。
 * 依頼者が必要な品目と数量を指定し、承認ワークフローを経て発注に変換される。
 * </p>
 * <p>
 * 【技術的負債】ステータスがEnum型ではなく文字列で管理されている。
 * 【技術的負債】requesterId がリレーションではなく生のIDで保持されている。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "purchase_requisition")
@NamedQueries({
    @NamedQuery(
        name = "PurchaseRequisition.findByStatus",
        query = "SELECT pr FROM PurchaseRequisition pr WHERE pr.status = :status ORDER BY pr.createdAt DESC"
    ),
    @NamedQuery(
        name = "PurchaseRequisition.findByRequester",
        query = "SELECT pr FROM PurchaseRequisition pr WHERE pr.requesterId = :requesterId ORDER BY pr.createdAt DESC"
    ),
    @NamedQuery(
        name = "PurchaseRequisition.findByReqNumber",
        query = "SELECT pr FROM PurchaseRequisition pr WHERE pr.reqNumber = :reqNumber"
    )
})
public class PurchaseRequisition extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 購買依頼番号（一意制約） */
    @NotNull
    @Size(max = 30)
    @Column(name = "requisition_number", unique = true, nullable = false, length = 30)
    private String reqNumber;

    /** 購買依頼の件名 */
    @NotNull
    @Size(max = 300)
    @Column(name = "title", nullable = false, length = 300)
    private String title;

    /** 必要期日 */
    @Temporal(TemporalType.DATE)
    @Column(name = "required_date")
    private Date requiredDate;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: DRAFT, SUBMITTED, APPROVED, REJECTED, CANCELLED, CONVERTED
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 購買依頼の正当性・理由 */
    @Size(max = 1000)
    @Column(name = "justification", length = 1000)
    private String justification;

    /**
     * 優先度。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: LOW, NORMAL, HIGH, URGENT
     * </p>
     */
    @Size(max = 10)
    @Column(name = "priority", length = 10)
    private String priority;

    /**
     * 依頼者のユーザーID。
     * <p>
     * 技術的負債: UserProfileへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。これにより参照整合性がJPAレベルで保証されない。
     * </p>
     */
    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "department_id")
    private Long departmentId;

    /** 購買依頼明細のリスト */
    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseRequisitionItem> items = new ArrayList<PurchaseRequisitionItem>();

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseRequisition() {
        super();
    }

    // --- Getter / Setter ---

    public String getReqNumber() {
        return reqNumber;
    }

    public void setReqNumber(String reqNumber) {
        this.reqNumber = reqNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getRequiredDate() {
        return requiredDate;
    }

    public void setRequiredDate(Date requiredDate) {
        this.requiredDate = requiredDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public List<PurchaseRequisitionItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseRequisitionItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PurchaseRequisition{" +
                "reqNumber='" + reqNumber + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                '}';
    }
}
