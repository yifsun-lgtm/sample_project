package com.proquip.common.exception;

/**
 * エンティティが見つからない場合にスローされる例外。
 *
 * <p>指定されたIDでデータベースからエンティティを取得できなかった場合に使用する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public class EntityNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** エンティティ名 */
    private final String entityName;

    /** 検索に使用したID */
    private final Object entityId;

    /**
     * エンティティ名とIDを指定して例外を生成する。
     *
     * @param entityName エンティティ名（例: "PurchaseOrder"）
     * @param entityId 検索に使用したID
     */
    public EntityNotFoundException(String entityName, Object entityId) {
        super("ENT_NOT_FOUND",
                entityName + "が見つかりません。ID: " + entityId);
        this.entityName = entityName;
        this.entityId = entityId;
    }

    /**
     * エンティティ名を返す。
     *
     * @return エンティティ名
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * 検索に使用したIDを返す。
     *
     * @return エンティティID
     */
    public Object getEntityId() {
        return entityId;
    }
}
