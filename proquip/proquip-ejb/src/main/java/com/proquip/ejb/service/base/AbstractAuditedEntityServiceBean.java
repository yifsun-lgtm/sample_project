package com.proquip.ejb.service.base;

import com.proquip.common.constant.AppConstants;
import com.proquip.ejb.entity.base.BaseEntity;
import com.proquip.ejb.service.AuditServiceBean;

import jakarta.ejb.EJB;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 監査ログ付きエンティティサービスの抽象クラス（レベル2/5）。
 * <p>
 * {@link AbstractBaseEntityServiceBean}を拡張し、CRUD操作に監査ログ記録を追加する。
 * エンティティの作成・更新・削除時に自動的にAuditServiceBeanへの記録を行う。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化（5階層のうちレベル2）】
 * 監査ログ記録はAOPやCDIインターセプターで横断的関心事として実装すべき。
 * 継承で機能を追加する設計は、クラス爆発の原因となる。</p>
 *
 * @param <T> エンティティ型
 * @param <ID> ID型
 *
 * @author ProQuip開発チーム
 * @since 1.3.0
 */
public abstract class AbstractAuditedEntityServiceBean<T extends BaseEntity, ID extends Serializable>
        extends AbstractBaseEntityServiceBean<T, ID> {

    @EJB
    protected AuditServiceBean auditService;

    /**
     * {@inheritDoc}
     *
     * <p>作成後に監査ログを記録する。</p>
     */
    @Override
    public T create(T entity) {
        T created = super.create(entity);

        // 監査ログ記録
        try {
            Map<String, Object> newValues = new HashMap<String, Object>();
            newValues.put("id", created.getId());
            auditService.logActionWithMap(
                    getEntityName(), created.getId(), "CREATE",
                    getCurrentUser(), null, newValues);
        } catch (Exception e) {
            // 技術的負債 #7: 監査ログ記録失敗を握りつぶし
            logger.warning("監査ログの記録に失敗しました。エンティティ: " + getEntityName()
                    + ", ID: " + created.getId());
        }

        return created;
    }

    /**
     * {@inheritDoc}
     *
     * <p>更新後に監査ログを記録する。</p>
     */
    @Override
    public T update(T entity) {
        T updated = super.update(entity);

        try {
            auditService.logAction(
                    getEntityName(), updated.getId(), "UPDATE",
                    getCurrentUser(), null, null);
        } catch (Exception e) {
            logger.warning("監査ログの記録に失敗しました。エンティティ: " + getEntityName()
                    + ", ID: " + updated.getId());
        }

        return updated;
    }

    /**
     * {@inheritDoc}
     *
     * <p>削除後に監査ログを記録する。</p>
     */
    @Override
    public void delete(ID id) {
        super.delete(id);

        try {
            auditService.logAction(
                    getEntityName(), (Long) id, "DELETE",
                    getCurrentUser(), null, null);
        } catch (Exception e) {
            logger.warning("監査ログの記録に失敗しました。エンティティ: " + getEntityName()
                    + ", ID: " + id);
        }
    }

    /**
     * 現在のユーザー名を取得する。
     *
     * <p>サブクラスでオーバーライドしてSessionContextから取得すべき。
     * デフォルトではSYSTEMユーザーを返す。</p>
     *
     * <p>【技術的負債】SessionContextを注入して呼び出し元ユーザーを取得すべきだが、
     * ハードコードされたシステムユーザーを返している。</p>
     *
     * @return 現在のユーザー名
     */
    protected String getCurrentUser() {
        // 技術的負債: ハードコードされたシステムユーザー
        return AppConstants.SYSTEM_USER;
    }
}
