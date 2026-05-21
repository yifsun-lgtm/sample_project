package com.proquip.ejb.mapper;

import com.proquip.common.dto.WarehouseDto;
import com.proquip.ejb.entity.inventory.Warehouse;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 倉庫エンティティとDTO間の手書きマッパークラス。
 *
 * <p>技術的負債 #10: MapStructを使用せず、手動マッピングを行っている。
 * {@link ProductMapper} や {@link SupplierMapper} がMapStructを使用しているのに対し、
 * このクラスは {@link PurchaseOrderMapper} と同様に手書き実装になっている。</p>
 *
 * <p>手書きにした理由は特になく、開発者によって実装方針が異なっただけ。
 * MapStructに移行すべき。</p>
 *
 * @author ProQuip開発チーム
 */
public class WarehouseMapper {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(WarehouseMapper.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public WarehouseMapper() {
    }

    /**
     * 倉庫エンティティをWarehouseDtoに変換する。
     *
     * <p>技術的負債: utilizationPercentage はエンティティに対応するフィールドがなく、
     * 在庫データから計算する必要があるが、このマッパーでは設定できない。
     * 呼び出し側で別途計算して手動設定する必要がある。</p>
     *
     * @param entity 倉庫エンティティ
     * @return 倉庫DTO（entityがnullの場合はnull）
     */
    public WarehouseDto toDto(Warehouse entity) {
        if (entity == null) {
            return null;
        }

        LOG.log(Level.FINE, "Warehouse -> WarehouseDto 変換: id={0}", entity.getId());

        WarehouseDto dto = new WarehouseDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setCapacity(entity.getCapacity());
        dto.setActive(entity.isActive());

        // 技術的負債: utilizationPercentage の計算がここではできない
        // dto.setUtilizationPercentage(...);

        // ゾーン数の設定
        if (entity.getZones() != null) {
            dto.setZoneCount(entity.getZones().size());
        } else {
            dto.setZoneCount(0);
        }

        return dto;
    }

    /**
     * WarehouseDtoから倉庫エンティティに変換する。
     *
     * <p>技術的負債: ゾーン情報やタイプ情報がDTOにないため、
     * 逆マッピングが不完全。新規登録時のみ使用可能。</p>
     *
     * @param dto 倉庫DTO
     * @return 倉庫エンティティ（dtoがnullの場合はnull）
     */
    public Warehouse toEntity(WarehouseDto dto) {
        if (dto == null) {
            return null;
        }

        LOG.log(Level.FINE, "WarehouseDto -> Warehouse 変換: id={0}", dto.getId());

        Warehouse entity = new Warehouse();

        entity.setId(dto.getId());
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setAddress(dto.getAddress());
        entity.setCapacity(dto.getCapacity());
        entity.setActive(dto.isActive());

        // 技術的負債: type がDTOにない
        // 技術的負債: zones がDTOにない

        return entity;
    }
}
