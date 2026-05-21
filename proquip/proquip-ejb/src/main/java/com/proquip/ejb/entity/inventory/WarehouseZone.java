package com.proquip.ejb.entity.inventory;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * 倉庫ゾーンエンティティ。
 * <p>
 * 倉庫内の区画（ゾーン）を表す。各ゾーンは用途別に分類され、
 * 複数の保管ロケーションを持つことができる。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "warehouse_zone")
public class WarehouseZone extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** ゾーンコード */
    @NotNull
    @Size(max = 20)
    @Column(name = "zone_code", nullable = false, length = 20)
    private String code;

    /** ゾーン名 */
    @NotNull
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * ゾーンタイプ。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: BULK, PICKING, RECEIVING, SHIPPING
     * </p>
     */
    @NotNull
    @Size(max = 20)
    @Column(name = "zone_type", nullable = false, length = 20)
    private String zoneType;

    /** 親の倉庫への参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** 保管ロケーションのリスト */
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StorageLocation> storageLocations = new ArrayList<StorageLocation>();

    /**
     * デフォルトコンストラクタ。
     */
    public WarehouseZone() {
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

    public String getZoneType() {
        return zoneType;
    }

    public void setZoneType(String zoneType) {
        this.zoneType = zoneType;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public List<StorageLocation> getStorageLocations() {
        return storageLocations;
    }

    public void setStorageLocations(List<StorageLocation> storageLocations) {
        this.storageLocations = storageLocations;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "WarehouseZone{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", zoneType='" + zoneType + '\'' +
                '}';
    }
}
