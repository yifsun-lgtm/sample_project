package com.proquip.ejb.entity.inventory;

import com.proquip.ejb.entity.base.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Date;

/**
 * 棚卸エンティティ。
 * <p>
 * 倉庫内の在庫を実地棚卸する際の情報を管理する。
 * 棚卸番号で一意に識別され、棚卸タイプ（全数、循環、スポット）を指定できる。
 * </p>
 * <p>
 * 【技術的負債】
 * <ul>
 *   <li>warehouseId が Warehouse エンティティへのリレーションではなく生のIDで保持されている</li>
 *   <li>InventoryCountItem への @OneToMany リレーションが定義されていない
 *       （他のヘッダ/明細パターンとの一貫性がない）</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "inventory_count")
@NamedQueries({
    @NamedQuery(
        name = "InventoryCount.findByCountNumber",
        query = "SELECT ic FROM InventoryCount ic WHERE ic.countNumber = :countNumber"
    ),
    @NamedQuery(
        name = "InventoryCount.findByWarehouse",
        query = "SELECT ic FROM InventoryCount ic WHERE ic.warehouseId = :warehouseId ORDER BY ic.countDate DESC"
    ),
    @NamedQuery(
        name = "InventoryCount.findByStatus",
        query = "SELECT ic FROM InventoryCount ic WHERE ic.status = :status ORDER BY ic.countDate DESC"
    )
})
public class InventoryCount extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 棚卸番号（一意制約） */
    @NotNull
    @Size(max = 30)
    @Column(name = "count_number", unique = true, nullable = false, length = 30)
    private String countNumber;

    /** 棚卸実施日 */
    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "planned_date", nullable = false)
    private Date countDate;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 棚卸タイプ。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: FULL（全数棚卸）, CYCLE（循環棚卸）, SPOT（スポット棚卸）
     * </p>
     */
    @NotNull
    @Size(max = 10)
    @Column(name = "count_type", nullable = false, length = 10)
    private String type;

    /**
     * 対象倉庫のID。
     * <p>
     * 技術的負債: Warehouseエンティティへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。
     * </p>
     */
    @NotNull
    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    /*
     * 技術的負債: InventoryCountItem への @OneToMany リレーションが未定義。
     * 他のヘッダ/明細エンティティ（PurchaseOrder/PurchaseOrderItem 等）との
     * 一貫性がなく、棚卸明細の取得には別途クエリが必要。
     */

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryCount() {
        super();
    }

    // --- Getter / Setter ---

    public String getCountNumber() {
        return countNumber;
    }

    public void setCountNumber(String countNumber) {
        this.countNumber = countNumber;
    }

    public Date getCountDate() {
        return countDate;
    }

    public void setCountDate(Date countDate) {
        this.countDate = countDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    @Override
    public String toString() {
        return "InventoryCount{" +
                "countNumber='" + countNumber + '\'' +
                ", countDate=" + countDate +
                ", status='" + status + '\'' +
                ", type='" + type + '\'' +
                ", warehouseId=" + warehouseId +
                '}';
    }
}
