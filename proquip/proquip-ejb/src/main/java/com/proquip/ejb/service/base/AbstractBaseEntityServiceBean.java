package com.proquip.ejb.service.base;

import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * エンティティサービスの抽象基底クラス（レベル1/5）。
 * <p>
 * 基本的なCRUD操作（Create, Read, Update, Delete）を提供する。
 * サブクラスはエンティティの型パラメータとID型を指定する。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化（5階層のうちレベル1）】
 * このクラスから始まる5階層の継承ツリーは過剰な抽象化の典型例である。
 * <pre>
 *   AbstractBaseEntityServiceBean          (レベル1: 基本CRUD)
 *     └─ AbstractAuditedEntityServiceBean   (レベル2: 監査ログ付き)
 *         └─ AbstractValidatedEntityServiceBean (レベル3: バリデーション付き)
 *             └─ AbstractSearchableEntityServiceBean (レベル4: 検索機能付き)
 *                 └─ AbstractPaginatedEntityServiceBean (レベル5: ページネーション付き)
 * </pre>
 * 実際のサービスBeanはこの階層を使用しておらず、EntityManagerを直接操作している。
 * コンポジションまたはMixinパターンの方が適切。</p>
 *
 * @param <T> エンティティ型（BaseEntityの拡張）
 * @param <ID> ID型（Serializableの拡張）
 *
 * @author ProQuip開発チーム
 * @since 1.3.0
 */
public abstract class AbstractBaseEntityServiceBean<T extends BaseEntity, ID extends Serializable> {

    /** ロガー */
    protected final Logger logger = Logger.getLogger(getClass().getName());

    @PersistenceContext
    protected EntityManager em;

    /**
     * エンティティクラスを返す。
     *
     * <p>サブクラスで必ず実装する。リフレクション型消去の回避策。</p>
     *
     * @return エンティティクラス
     */
    protected abstract Class<T> getEntityClass();

    /**
     * エンティティ種別名を返す（ログ・例外メッセージ用）。
     *
     * @return エンティティ種別名（例: "Product", "Supplier"）
     */
    protected abstract String getEntityName();

    // ========================================================================
    // 基本CRUD操作
    // ========================================================================

    /**
     * エンティティをIDで取得する。
     *
     * @param id エンティティID
     * @return エンティティ
     * @throws EntityNotFoundException エンティティが見つからない場合
     */
    public T findById(ID id) {
        if (id == null) {
            throw new ValidationException("id", getEntityName() + "のIDは必須です。");
        }

        T entity = em.find(getEntityClass(), id);
        if (entity == null) {
            throw new EntityNotFoundException(getEntityName(), id);
        }
        return entity;
    }

    /**
     * エンティティを永続化する。
     *
     * @param entity 永続化するエンティティ
     * @return 永続化されたエンティティ
     */
    public T create(T entity) {
        if (entity == null) {
            throw new ValidationException(getEntityName(),
                    getEntityName() + "情報は必須です。");
        }

        em.persist(entity);
        logger.fine(getEntityName() + "を作成しました。ID: " + entity.getId());
        return entity;
    }

    /**
     * エンティティを更新する。
     *
     * @param entity 更新するエンティティ
     * @return 更新後のエンティティ
     */
    public T update(T entity) {
        if (entity == null) {
            throw new ValidationException(getEntityName(),
                    getEntityName() + "情報は必須です。");
        }
        if (entity.getId() == null) {
            throw new ValidationException("id",
                    getEntityName() + "のIDは必須です。");
        }

        // 存在チェック
        T existing = em.find(getEntityClass(), entity.getId());
        if (existing == null) {
            throw new EntityNotFoundException(getEntityName(), entity.getId());
        }

        T merged = em.merge(entity);
        logger.fine(getEntityName() + "を更新しました。ID: " + entity.getId());
        return merged;
    }

    /**
     * エンティティを削除する。
     *
     * <p>物理削除。論理削除はサブクラスでオーバーライドして実装する。</p>
     *
     * @param id 削除するエンティティのID
     */
    public void delete(ID id) {
        T entity = findById(id);
        em.remove(entity);
        logger.fine(getEntityName() + "を削除しました。ID: " + id);
    }

    /**
     * 全エンティティを取得する。
     *
     * <p>【技術的負債】ページネーション未対応。大量データで問題。</p>
     *
     * @return エンティティのリスト
     */
    @SuppressWarnings("unchecked")
    public java.util.List<T> findAll() {
        // 技術的負債: 文字列連結によるJPQL構築
        return em.createQuery(
                "SELECT e FROM " + getEntityClass().getSimpleName() + " e")
                .getResultList();
    }

    /**
     * エンティティ件数を取得する。
     *
     * @return 件数
     */
    public long count() {
        return (Long) em.createQuery(
                "SELECT COUNT(e) FROM " + getEntityClass().getSimpleName() + " e")
                .getSingleResult();
    }
}
