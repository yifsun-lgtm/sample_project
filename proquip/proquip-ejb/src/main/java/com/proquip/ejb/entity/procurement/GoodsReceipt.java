package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.AuditableEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * 入庫エンティティ。
 * <p>
 * サプライヤーからの納品を記録する。発注に対する物品の受入情報を管理し、
 * 受入番号で一意に識別される。各入庫には複数の入庫明細が紐づく。
 * </p>
 * <p>
 * 【技術的負債】warehouseId が Warehouse エンティティへのリレーションではなく
 * 生のIDで保持されている。倉庫情報の取得に追加クエリが必要。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "goods_receipt")
@NamedQueries({
    @NamedQuery(
        name = "GoodsReceipt.findByReceiptNumber",
        query = "SELECT gr FROM GoodsReceipt gr WHERE gr.receiptNumber = :receiptNumber"
    ),
    @NamedQuery(
        name = "GoodsReceipt.findByPurchaseOrder",
        query = "SELECT gr FROM GoodsReceipt gr WHERE gr.purchaseOrder.id = :purchaseOrderId ORDER BY gr.receiptDate DESC"
    ),
    @NamedQuery(
        name = "GoodsReceipt.findByWarehouse",
        query = "SELECT gr FROM GoodsReceipt gr WHERE gr.warehouseId = :warehouseId ORDER BY gr.receiptDate DESC"
    )
})
public class GoodsReceipt extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 受入番号（一意制約） */
    @NotNull
    @Size(max = 30)
    @Column(name = "receipt_number", unique = true, nullable = false, length = 30)
    private String receiptNumber;

    /** 受入日 */
    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "receipt_date", nullable = false)
    private Date receiptDate;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: DRAFT, INSPECTING, ACCEPTED, PARTIALLY_ACCEPTED, REJECTED
     * </p>
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** 備考 */
    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;

    /** 対象発注への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /**
     * 受入倉庫のID。
     * <p>
     * 技術的負債: Warehouseエンティティへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。倉庫情報を取得するには別途クエリが必要。
     * </p>
     */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    /** 入庫明細のリスト */
    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GoodsReceiptItem> receiptItems = new ArrayList<GoodsReceiptItem>();

    /**
     * デフォルトコンストラクタ。
     */
    public GoodsReceipt() {
        super();
    }

    // --- Getter / Setter ---

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public Date getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(Date receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<GoodsReceiptItem> getReceiptItems() {
        return receiptItems;
    }

    public void setReceiptItems(List<GoodsReceiptItem> receiptItems) {
        this.receiptItems = receiptItems;
    }

    @Override
    public String toString() {
        return "GoodsReceipt{" +
                "receiptNumber='" + receiptNumber + '\'' +
                ", receiptDate=" + receiptDate +
                ", status='" + status + '\'' +
                ", warehouseId=" + warehouseId +
                '}';
    }
}
