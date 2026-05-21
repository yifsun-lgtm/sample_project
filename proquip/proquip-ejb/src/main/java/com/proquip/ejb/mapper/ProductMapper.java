package com.proquip.ejb.mapper;

import com.proquip.common.dto.ProductDTO;
import com.proquip.common.dto.ProductDetailDto;
import com.proquip.ejb.entity.product.Product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 商品エンティティとDTO間のマッパーインターフェース（MapStruct使用）。
 *
 * <p>MapStructによるコンパイル時コード生成を利用して、
 * {@link Product} エンティティと {@link ProductDTO} / {@link ProductDetailDto}
 * 間の変換を行う。</p>
 *
 * @author ProQuip開発チーム
 */
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    /** インスタンス取得用（CDI環境外でのテスト用） */
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    /**
     * 商品エンティティをProductDTOに変換する。
     *
     * <p>カテゴリ名や製造元名はネストしたオブジェクトから取得する。
     * エンティティのフィールド名（sku）とDTOのフィールド名（skuCode）が
     * 異なるため、明示的な @Mapping が必要。</p>
     *
     * @param product 商品エンティティ
     * @return 商品DTO
     */
    @Mappings({
        @Mapping(source = "sku", target = "skuCode"),
        @Mapping(source = "category.id", target = "categoryId"),
        @Mapping(source = "category.name", target = "categoryName"),
        @Mapping(source = "manufacturer.id", target = "manufacturerId"),
        @Mapping(source = "manufacturer.name", target = "manufacturerName"),
        @Mapping(target = "unit", expression = "java(product.getUnit() != null ? product.getUnit().getName() : null)"),
        @Mapping(source = "minOrderQty", target = "minimumOrderQuantity"),
        @Mapping(target = "stockQuantity", ignore = true),
        @Mapping(target = "reorderPoint", ignore = true),
        @Mapping(target = "tags", ignore = true),
        @Mapping(target = "dimensions", ignore = true),
        @Mapping(target = "specifications", ignore = true)
    })
    ProductDTO toDto(Product product);

    /**
     * 商品エンティティをProductDetailDtoに変換する。
     *
     * <p>仕様、画像、ドキュメントなどの関連情報も変換対象とする。
     * 代替品（alternatives）はIDリストとして変換する。</p>
     *
     * @param product 商品エンティティ
     * @return 商品詳細DTO
     */
    @Mappings({
        @Mapping(source = "sku", target = "skuCode"),
        @Mapping(source = "category.id", target = "categoryId"),
        @Mapping(source = "category.name", target = "categoryName"),
        @Mapping(source = "manufacturer.id", target = "manufacturerId"),
        @Mapping(source = "manufacturer.name", target = "manufacturerName"),
        @Mapping(target = "stockQuantity", ignore = true),
        @Mapping(target = "reorderPoint", ignore = true),
        @Mapping(target = "tags", ignore = true),
        @Mapping(target = "alternatives", ignore = true),
        @Mapping(target = "specifications", ignore = true)
    })
    ProductDetailDto toDetailDto(Product product);

    /**
     * ProductDTOから商品エンティティに変換する。
     *
     * <p>技術的負債: 逆マッピング時にカテゴリや製造元の参照を設定できないため、
     * 呼び出し側で手動設定が必要。</p>
     *
     * @param dto 商品DTO
     * @return 商品エンティティ
     */
    @Mappings({
        @Mapping(source = "skuCode", target = "sku"),
        @Mapping(source = "minimumOrderQuantity", target = "minOrderQty"),
        @Mapping(target = "category", ignore = true),
        @Mapping(target = "manufacturer", ignore = true),
        @Mapping(target = "unit", ignore = true),
        @Mapping(target = "specifications", ignore = true),
        @Mapping(target = "images", ignore = true),
        @Mapping(target = "documents", ignore = true),
        @Mapping(target = "tags", ignore = true),
        @Mapping(target = "width", ignore = true),
        @Mapping(target = "height", ignore = true),
        @Mapping(target = "depth", ignore = true)
    })
    Product toEntity(ProductDTO dto);
}
