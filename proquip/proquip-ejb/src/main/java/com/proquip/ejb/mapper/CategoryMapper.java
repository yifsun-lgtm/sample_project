package com.proquip.ejb.mapper;

import com.proquip.common.dto.product.CategoryDto;
import com.proquip.ejb.entity.product.Category;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * カテゴリエンティティとDTO間のマッパーインターフェース（MapStruct使用）。
 *
 * <p>MapStructによるコンパイル時コード生成を利用して、
 * {@link Category} エンティティと {@link CategoryDto} 間の変換を行う。</p>
 *
 * <p>技術的負債: children のマッピングは自己参照的であるため、
 * 深い階層構造の場合にスタックオーバーフローのリスクがある。
 * 階層数に制限を設けるか、遅延ロードを検討すべき。</p>
 *
 * @author ProQuip開発チーム
 */
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    /** インスタンス取得用（CDI環境外でのテスト用） */
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    /**
     * カテゴリエンティティをCategoryDtoに変換する。
     *
     * <p>親カテゴリ情報はネストしたオブジェクトからフラット化して取得する。
     * productCount はエンティティに対応するフィールドがないため ignore としている。</p>
     *
     * @param category カテゴリエンティティ
     * @return カテゴリDTO
     */
    @Mappings({
        @Mapping(source = "parent.id", target = "parentId"),
        @Mapping(source = "parent.name", target = "parentName"),
        @Mapping(target = "productCount", ignore = true),
        @Mapping(target = "children", ignore = true)
    })
    CategoryDto toDto(Category category);

    /**
     * CategoryDtoからカテゴリエンティティに変換する。
     *
     * <p>技術的負債: 親カテゴリの参照設定ができない。
     * 呼び出し側でparentIdからCategoryエンティティを検索して手動設定が必要。</p>
     *
     * @param dto カテゴリDTO
     * @return カテゴリエンティティ
     */
    @Mappings({
        @Mapping(target = "parent", ignore = true),
        @Mapping(target = "children", ignore = true),
        @Mapping(target = "code", ignore = true),
        @Mapping(target = "products", ignore = true)
    })
    Category toEntity(CategoryDto dto);
}
