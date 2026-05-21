package com.proquip.ejb.entity.inventory;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 保管ロケーションエンティティ。
 * <p>
 * 倉庫ゾーン内の具体的な保管場所を表す。
 * 通路（aisle）、ラック（rack）、棚（shelf）、ビン（bin）の4階層で
 * ロケーションを特定する。最大重量・最大容積の制約情報も保持する。
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "storage_location")
public class StorageLocation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** ロケーションコード（ゾーン内で一意） */
    @NotNull
    @Size(max = 30)
    @Column(name = "location_code", nullable = false, length = 30)
    private String code;

    /** 通路番号 */
    @Size(max = 10)
    @Column(name = "aisle", length = 10)
    private String aisle;

    /** ラック番号 */
    @Size(max = 10)
    @Column(name = "rack", length = 10)
    private String rack;

    /** 棚番号 */
    @Size(max = 10)
    @Column(name = "shelf", length = 10)
    private String shelf;

    /** ビン番号 */
    @Size(max = 10)
    @Column(name = "bin", length = 10)
    private String bin;

    /** 最大積載重量（kg） */
    @Column(name = "max_weight_kg", precision = 10, scale = 2)
    private BigDecimal maxWeight;

    /** 最大容積（m3） */
    @Column(name = "max_volume_m3", precision = 10, scale = 2)
    private BigDecimal maxVolume;

    @NotNull
    @Size(max = 20)
    @Column(name = "location_type", nullable = false, length = 20)
    private String locationType = "SHELF";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_occupied", nullable = false)
    private boolean occupied = false;

    @Size(max = 50)
    @Column(name = "barcode", length = 50)
    private String barcode;

    /** 親のゾーンへの参照 */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private WarehouseZone zone;

    /**
     * デフォルトコンストラクタ。
     */
    public StorageLocation() {
        super();
    }

    // --- Getter / Setter ---

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public String getShelf() {
        return shelf;
    }

    public void setShelf(String shelf) {
        this.shelf = shelf;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public BigDecimal getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(BigDecimal maxWeight) {
        this.maxWeight = maxWeight;
    }

    public BigDecimal getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(BigDecimal maxVolume) {
        this.maxVolume = maxVolume;
    }

    public WarehouseZone getZone() {
        return zone;
    }

    public void setZone(WarehouseZone zone) {
        this.zone = zone;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    @Override
    public String toString() {
        return "StorageLocation{" +
                "code='" + code + '\'' +
                ", aisle='" + aisle + '\'' +
                ", rack='" + rack + '\'' +
                ", shelf='" + shelf + '\'' +
                ", bin='" + bin + '\'' +
                '}';
    }
}
