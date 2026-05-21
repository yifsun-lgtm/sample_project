package com.proquip.ejb.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 全DAOクラスの抽象基底クラス。
 *
 * <p>永続化コンテキストの管理と、CRUD操作の共通メソッドを提供する。
 * サブクラスはエンティティ型とIDの型をジェネリクスで指定する。</p>
 *
 * <p>技術的負債 #10: 過剰に設計された抽象基底クラス。
 * 多くのサブクラスが基底メソッドをそのまま使わず、独自のクエリパターン
 * （Criteria API、JPQL、ネイティブSQL）を混在させている。
 * 基底クラスの汎用メソッドは冗長になりがちで、各DAOの一貫性を
 * 保証するものにはなっていない。</p>
 *
 * <p>本来はRepositoryパターンやSpring Data JPAのような仕組みに移行すべきだが、
 * レガシー制約により手動で共通処理をまとめている。</p>
 *
 * @param <T>  エンティティの型
 * @param <ID> エンティティのID型（通常は {@link Long}）
 *
 * @author ProQuip開発チーム
 */
public abstract class AbstractBaseDao<T, ID extends Serializable> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(AbstractBaseDao.class.getName());

    /**
     * 永続化コンテキスト。
     * 技術的負債: ユニット名がハードコーディングされている。
     */
    @PersistenceContext(unitName = "proquipPU")
    protected EntityManager em;

    /** エンティティのクラス（リフレクションで解決される） */
    private Class<T> entityClass;

    /**
     * デフォルトコンストラクタ。
     * リフレクションによりジェネリクスの型パラメータからエンティティクラスを解決する。
     */
    @SuppressWarnings("unchecked")
    public AbstractBaseDao() {
        Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            this.entityClass = (Class<T>) paramType.getActualTypeArguments()[0];
        }
    }

    /**
     * 主キーでエンティティを検索する。
     *
     * @param id 主キー
     * @return 検索結果のエンティティ（見つからない場合は空の {@link Optional}）
     */
    public Optional<T> findById(ID id) {
        LOG.log(Level.FINE, "エンティティ検索: {0}, ID={1}", new Object[]{entityClass.getSimpleName(), id});
        T entity = em.find(entityClass, id);
        return Optional.ofNullable(entity);
    }

    /**
     * 全エンティティを取得する。
     *
     * <p>技術的負債: ページネーションなしで全件取得するため、
     * 大量データの場合にメモリ不足を引き起こす可能性がある。</p>
     *
     * @return エンティティのリスト
     */
    public List<T> findAll() {
        LOG.log(Level.FINE, "全エンティティ取得: {0}", entityClass.getSimpleName());
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        cq.select(root);
        TypedQuery<T> query = em.createQuery(cq);
        return query.getResultList();
    }

    /**
     * エンティティを永続化する。
     *
     * @param entity 永続化するエンティティ
     * @return 永続化されたエンティティ
     */
    public T save(T entity) {
        LOG.log(Level.FINE, "エンティティ保存: {0}", entity);
        em.persist(entity);
        return entity;
    }

    /**
     * エンティティを更新する。
     *
     * @param entity 更新するエンティティ
     * @return マージされたエンティティ
     */
    public T update(T entity) {
        LOG.log(Level.FINE, "エンティティ更新: {0}", entity);
        return em.merge(entity);
    }

    /**
     * エンティティを削除する。
     *
     * <p>管理対象外のエンティティの場合は、先にマージしてから削除する。</p>
     *
     * @param entity 削除するエンティティ
     */
    public void delete(T entity) {
        LOG.log(Level.FINE, "エンティティ削除: {0}", entity);
        if (!em.contains(entity)) {
            entity = em.merge(entity);
        }
        em.remove(entity);
    }

    /**
     * エンティティの件数を取得する。
     *
     * @return エンティティの総件数
     */
    public long count() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<T> root = cq.from(entityClass);
        cq.select(cb.count(root));
        return em.createQuery(cq).getSingleResult();
    }

    /**
     * 永続化コンテキストをフラッシュする。
     *
     * <p>保留中の変更をデータベースに反映させる。
     * トランザクション管理と組み合わせて使用する必要がある。</p>
     */
    public void flush() {
        em.flush();
    }

    /**
     * エンティティクラスを返す。
     *
     * @return エンティティのクラスオブジェクト
     */
    protected Class<T> getEntityClass() {
        return entityClass;
    }
}
