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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * 倉庫マスタエンティティ。
 * <p>
 * 在庫を保管する物理的または論理的な倉庫を表す。
 * 倉庫コードで一意に識別され、ゾーンおよび在庫品目への関連を持つ。
 * </p>
 * <p>
 * 【技術的負債】type フィールドが Enum型ではなく文字列で管理されている。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "warehouse")
@NamedQueries({
    @NamedQuery(
        name = "Warehouse.findByCode",
        query = "SELECT w FROM Warehouse w WHERE w.code = :code"
    ),
    @NamedQuery(
        name = "Warehouse.findActive",
        query = "SELECT w FROM Warehouse w WHERE w.isActive = true ORDER BY w.name"
    ),
    @NamedQuery(
        name = "Warehouse.findByType",
        query = "SELECT w FROM Warehouse w WHERE w.type = :type ORDER BY w.name"
    )
})
public class Warehouse extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 倉庫コード（一意制約） */
    @NotNull
    @Size(max = 20)
    @Column(name = "warehouse_code", unique = true, nullable = false, length = 20)
    private String code;

    /** 倉庫名 */
    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 住所 */
    @Size(max = 500)
    @Column(name = "address_line1", length = 500)
    private String address;

    /**
     * 倉庫タイプ。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: MAIN, SATELLITE, VIRTUAL
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "warehouse_type", nullable = false, length = 20)
    private String type = "GENERAL";

    @NotNull
    @Size(max = 100)
    @Column(name = "city", nullable = false, length = 100)
    private String city = "-";

    @NotNull
    @Size(max = 20)
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode = "-";

    @NotNull
    @Size(max = 3)
    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode = "JPN";

    /** 最大収容量（パレット数など） */
    @Column(name = "capacity_sqm")
    private Integer capacity;

    /** 有効フラグ */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** 倉庫内ゾーンのリスト */
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WarehouseZone> zones = new ArrayList<WarehouseZone>();

    /** 在庫品目のリスト */
    @OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
    private List<InventoryItem> inventoryItems = new ArrayList<InventoryItem>();

    /**
     * デフォルトコンストラクタ。
     */
    public Warehouse() {
        super();
    }

    // --- Getter / Setter ---

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<WarehouseZone> getZones() {
        return zones;
    }

    public void setZones(List<WarehouseZone> zones) {
        this.zones = zones;
    }

    public List<InventoryItem> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    @Override
    public String toString() {
        return "Warehouse{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
