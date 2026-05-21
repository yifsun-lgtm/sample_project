package com.proquip.ejb.mapper;

import com.proquip.common.dto.admin.AuditLogDto;
import com.proquip.ejb.entity.system.AuditLog;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 監査ログエンティティとDTO間の手書きマッパークラス。
 *
 * <p>技術的負債 #10: MapStructを使用せず、手動マッピングを行っている。
 * フィールド名の差異が大きいため手書きで実装されたが、
 * MapStructの @Mapping アノテーションで対応可能。</p>
 *
 * @author ProQuip開発チーム
 */
public class AuditLogMapper {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(AuditLogMapper.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public AuditLogMapper() {
    }

    /**
     * 監査ログエンティティをAuditLogDtoに変換する。
     *
     * <p>技術的負債: エンティティの performedBy（ユーザー名文字列）を
     * DTOの userName にマッピングしているが、userId は設定できない。
     * ユーザーIDが必要な場合は呼び出し側でUserProfileを検索する必要がある。</p>
     *
     * <p>技術的負債: entityName はエンティティに対応するフィールドがなく、
     * 設定できない。表示名が必要な場合は呼び出し側で対象エンティティを
     * 別途検索して手動設定する必要がある。</p>
     *
     * @param entity 監査ログエンティティ
     * @return 監査ログDTO（entityがnullの場合はnull）
     */
    public AuditLogDto toDto(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        LOG.log(Level.FINE, "AuditLog -> AuditLogDto 変換: id={0}", entity.getId());

        AuditLogDto dto = new AuditLogDto();

        dto.setId(entity.getId());
        dto.setAction(entity.getAction());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setUserName(entity.getPerformedBy() != null ? String.valueOf(entity.getPerformedBy()) : null);
        dto.setTimestamp(entity.getPerformedAt());
        dto.setOldValue(entity.getOldValues());
        dto.setNewValue(entity.getNewValues());
        dto.setIpAddress(entity.getIpAddress());

        // 技術的負債: userId は AuditLog エンティティに存在しない
        // dto.setUserId(...);

        // 技術的負債: entityName は AuditLog エンティティに存在しない
        // dto.setEntityName(...);

        // 技術的負債: description は AuditLog エンティティに存在しない
        // dto.setDescription(...);

        return dto;
    }
}
