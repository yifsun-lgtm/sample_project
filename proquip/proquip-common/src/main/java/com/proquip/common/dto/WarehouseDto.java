package com.proquip.common.dto;

import java.io.Serializable;

/**
 * 倉庫データ転送オブジェクト。
 *
 * <p>倉庫の基本情報をプレゼンテーション層やAPI応答として転送するためのクラス。</p>
 *
 * @author ProQuip開発チーム
 */
public class WarehouseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 倉庫ID */
    private Long id;

    /** 倉庫コード */
    private String code;

    /** 倉庫名 */
    private String name;

    /** 住所 */
    private String address;

    /** 最大収容量 */
    private Integer capacity;

    /** 利用率（パーセンテージ） */
    private Double utilizationPercentage;

    /** ゾーン数 */
    private Integer zoneCount;

    /** 有効フラグ */
    private boolean active;

    /**
     * デフォルトコンストラクタ。
     */
    public WarehouseDto() {
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Double getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(Double utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }

    public Integer getZoneCount() {
        return zoneCount;
    }

    public void setZoneCount(Integer zoneCount) {
        this.zoneCount = zoneCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
