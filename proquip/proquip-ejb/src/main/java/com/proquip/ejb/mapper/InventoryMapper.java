package com.proquip.ejb.mapper;

import com.proquip.common.dto.InventoryItemDto;
import com.proquip.ejb.entity.inventory.InventoryItem;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 在庫品目エンティティとDTO間のマッパーインターフェース（MapStruct使用）。
 *
 * <p>MapStructによるコンパイル時コード生成を利用して、
 * {@link InventoryItem} エンティティと {@link InventoryItemDto} 間の変換を行う。</p>
 *
 * @author ProQuip開発チーム
 */
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {

    /** インスタンス取得用（CDI環境外でのテスト用） */
    InventoryMapper INSTANCE = Mappers.getMapper(InventoryMapper.class);

    /**
     * 在庫品目エンティティをInventoryItemDtoに変換する。
     *
     * <p>製品情報と倉庫情報はネストしたオブジェクトからフラット化して取得する。
     * quantityOnHand は quantity に、quantityReserved は reservedQuantity にマッピングする。</p>
     *
     * @param entity 在庫品目エンティティ
     * @return 在庫品目DTO
     */
    @Mappings({
        @Mapping(source = "product.id", target = "productId"),
        @Mapping(source = "product.name", target = "productName"),
        @Mapping(source = "product.sku", target = "skuCode"),
        @Mapping(source = "warehouse.id", target = "warehouseId"),
        @Mapping(source = "warehouse.name", target = "warehouseName"),
        @Mapping(source = "quantityOnHand", target = "quantity"),
        @Mapping(source = "quantityReserved", target = "reservedQuantity"),
        @Mapping(target = "storageLocation", ignore = true)
    })
    InventoryItemDto toDto(InventoryItem entity);
}
