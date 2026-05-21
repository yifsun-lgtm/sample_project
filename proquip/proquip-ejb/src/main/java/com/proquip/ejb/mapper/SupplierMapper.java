package com.proquip.ejb.mapper;

import com.proquip.common.dto.SupplierDTO;
import com.proquip.ejb.entity.supplier.Supplier;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 仕入先エンティティとDTO間のマッパーインターフェース（MapStruct使用）。
 *
 * <p>MapStructによるコンパイル時コード生成を利用して、
 * {@link Supplier} エンティティと {@link SupplierDTO} 間の変換を行う。</p>
 *
 * @author ProQuip開発チーム
 */
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper {

    /** インスタンス取得用（CDI環境外でのテスト用） */
    SupplierMapper INSTANCE = Mappers.getMapper(SupplierMapper.class);

    /**
     * 仕入先エンティティをSupplierDTOに変換する。
     *
     * <p>エンティティの name フィールドをDTOの companyName にマッピングする。
     * 連絡先情報（contactEmail, contactPhone）は主担当者から取得する必要があるが、
     * MapStructでは遅延ロードされたコレクションの処理が難しいため、
     * ignore としている。</p>
     *
     * @param supplier 仕入先エンティティ
     * @return 仕入先DTO
     */
    @Mappings({
        @Mapping(target = "email", ignore = true),
        @Mapping(target = "phone", ignore = true),
        @Mapping(target = "website", ignore = true),
        @Mapping(target = "address", ignore = true),
        @Mapping(target = "rating", ignore = true)
    })
    SupplierDTO toDto(Supplier supplier);

    /**
     * SupplierDTOから仕入先エンティティに変換する。
     *
     * <p>技術的負債: 連絡先、住所、認証等のリレーション情報は変換できない。
     * 呼び出し側で手動設定が必要。</p>
     *
     * @param dto 仕入先DTO
     * @return 仕入先エンティティ
     */
    @Mappings({
        @Mapping(target = "taxId", ignore = true),
        @Mapping(target = "contacts", ignore = true),
        @Mapping(target = "products", ignore = true),
        @Mapping(target = "contracts", ignore = true),
        @Mapping(target = "addresses", ignore = true),
        @Mapping(target = "ratings", ignore = true),
        @Mapping(target = "certifications", ignore = true)
    })
    Supplier toEntity(SupplierDTO dto);
}
