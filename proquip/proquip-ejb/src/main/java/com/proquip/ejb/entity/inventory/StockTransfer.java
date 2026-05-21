package com.proquip.ejb.entity.inventory;

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
 * 在庫移動エンティティ。
 * <p>
 * 倉庫間の在庫移動を管理する。移動番号で一意に識別され、
 * 移動元倉庫から移動先倉庫への品目移動情報を保持する。
 * </p>
 * <p>
 * 【技術的負債】sourceWarehouseId および destinationWarehouseId が
 * Warehouse エンティティへのリレーションではなく生のIDで保持されている。
 * 倉庫情報を取得するには別途クエリが必要。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "stock_transfer")
@NamedQueries({
    @NamedQuery(
        name = "StockTransfer.findByTransferNumber",
        query = "SELECT st FROM StockTransfer st WHERE st.transferNumber = :transferNumber"
    ),
    @NamedQuery(
        name = "StockTransfer.findByStatus",
        query = "SELECT st FROM StockTransfer st WHERE st.status = :status ORDER BY st.requestDate DESC"
    ),
    @NamedQuery(
        name = "StockTransfer.findBySourceWarehouse",
        query = "SELECT st FROM StockTransfer st WHERE st.sourceWarehouseId = :warehouseId ORDER BY st.requestDate DESC"
    )
})
public class StockTransfer extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 移動番号（一意制約） */
    @NotNull
    @Size(max = 30)
    @Column(name = "transfer_number", unique = true, nullable = false, length = 30)
    private String transferNumber;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: DRAFT, REQUESTED, APPROVED, IN_TRANSIT, COMPLETED, CANCELLED
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 移動依頼日 */
    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "request_date", nullable = false)
    private Date requestDate;

    /** 移動完了日 */
    @Temporal(TemporalType.DATE)
    @Column(name = "completed_date")
    private Date completedDate;

    /**
     * 移動元倉庫のID。
     * <p>
     * 技術的負債: Warehouseエンティティへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。
     * </p>
     */
    @NotNull
    @Column(name = "source_warehouse_id", nullable = false)
    private Long sourceWarehouseId;

    /**
     * 移動先倉庫のID。
     * <p>
     * 技術的負債: Warehouseエンティティへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。
     * </p>
     */
    @NotNull
    @Column(name = "destination_warehouse_id", nullable = false)
    private Long destinationWarehouseId;

    /** 移動品目のリスト */
    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StockTransferItem> transferItems = new ArrayList<StockTransferItem>();

    /**
     * デフォルトコンストラクタ。
     */
    public StockTransfer() {
        super();
    }

    // --- Getter / Setter ---

    public String getTransferNumber() {
        return transferNumber;
    }

    public void setTransferNumber(String transferNumber) {
        this.transferNumber = transferNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public Long getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(Long sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public List<StockTransferItem> getTransferItems() {
        return transferItems;
    }

    public void setTransferItems(List<StockTransferItem> transferItems) {
        this.transferItems = transferItems;
    }

    @Override
    public String toString() {
        return "StockTransfer{" +
                "transferNumber='" + transferNumber + '\'' +
                ", status='" + status + '\'' +
                ", requestDate=" + requestDate +
                '}';
    }
}
