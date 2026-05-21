package com.proquip.ejb.mapper;

import com.proquip.common.dto.UserProfileDto;
import com.proquip.ejb.entity.organization.UserProfile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * ユーザープロファイルエンティティとDTO間のマッパーインターフェース（MapStruct使用）。
 *
 * <p>MapStructによるコンパイル時コード生成を利用して、
 * {@link UserProfile} エンティティと {@link UserProfileDto} 間の変換を行う。</p>
 *
 * <p>技術的負債: UserProfile のロール情報（roles）は遅延ロードされるため、
 * トランザクション外でマッピングするとLazyInitializationExceptionが発生する。
 * サービス層でロールを事前にフェッチしてからマッピングする必要がある。</p>
 *
 * @author ProQuip開発チーム
 */
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserProfileMapper {

    /** インスタンス取得用（CDI環境外でのテスト用） */
    UserProfileMapper INSTANCE = Mappers.getMapper(UserProfileMapper.class);

    /**
     * ユーザープロファイルエンティティをUserProfileDtoに変換する。
     *
     * <p>部門情報はネストしたオブジェクトからフラット化して取得する。
     * ロール情報は遅延ロードのため ignore としている。</p>
     *
     * @param userProfile ユーザープロファイルエンティティ
     * @return ユーザープロファイルDTO
     */
    @Mappings({
        @Mapping(source = "department.id", target = "departmentId"),
        @Mapping(source = "department.name", target = "departmentName"),
        @Mapping(target = "department", ignore = true),
        @Mapping(target = "roles", ignore = true),
        @Mapping(target = "roleNames", ignore = true),
        @Mapping(target = "enabled", ignore = true),
        @Mapping(target = "createdAt", ignore = true)
    })
    UserProfileDto toDto(UserProfile userProfile);

    /**
     * UserProfileDtoからユーザープロファイルエンティティに変換する。
     *
     * <p>技術的負債: 部門の参照設定ができない。
     * 呼び出し側でdepartmentIdからDepartmentエンティティを検索して手動設定が必要。
     * ロール情報のマッピングも不可。</p>
     *
     * @param dto ユーザープロファイルDTO
     * @return ユーザープロファイルエンティティ
     */
    @Mappings({
        @Mapping(target = "department", ignore = true),
        @Mapping(target = "roles", ignore = true),
        @Mapping(target = "keycloakId", ignore = true)
    })
    UserProfile toEntity(UserProfileDto dto);
}
