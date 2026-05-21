package com.proquip.ejb.dao;

import com.proquip.ejb.entity.supplier.Supplier;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 仕入先エンティティに対するデータアクセスオブジェクト。
 *
 * <p>仕入先の検索・登録・更新に関するデータベースアクセスを提供する。
 * 全メソッドでJPQLを使用しており、{@link ProductDao} のCriteria API方式とは
 * 一貫性がない。</p>
 *
 * <p>技術的負債 #18: プロジェクト内のDAO間でクエリ方式が統一されていない。
 * 本クラスはJPQL一貫だが、他のDAOはCriteria APIやネイティブSQLを混在させている。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class SupplierDao extends AbstractBaseDao<Supplier, Long> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(SupplierDao.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierDao() {
        super();
    }

    /**
     * ステータスで仕入先を検索する。
     *
     * @param status 仕入先ステータス（例: "ACTIVE", "INACTIVE", "SUSPENDED"）
     * @return 該当ステータスの仕入先リスト
     */
    public List<Supplier> findByStatus(String status) {
        LOG.log(Level.FINE, "ステータス別仕入先検索: status={0}", status);

        TypedQuery<Supplier> query = em.createQuery(
            "SELECT s FROM Supplier s WHERE s.status = :status ORDER BY s.name",
            Supplier.class
        );
        query.setParameter("status", status);
        return query.getResultList();
    }

    /**
     * 仕入先名で部分一致検索を行う。
     *
     * <p>仕入先名とコードの両方を検索対象とする。
     * 大文字・小文字を区別しない検索を行う。</p>
     *
     * <p>技術的負債: LIKE '%keyword%' による部分一致検索のため、
     * インデックスが使われずフルテーブルスキャンが発生する。</p>
     *
     * @param keyword 検索キーワード
     * @return 検索結果の仕入先リスト
     */
    public List<Supplier> searchByName(String keyword) {
        LOG.log(Level.FINE, "仕入先名検索: keyword={0}", keyword);

        TypedQuery<Supplier> query = em.createQuery(
            "SELECT s FROM Supplier s " +
            "WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY s.name",
            Supplier.class
        );
        query.setParameter("keyword", keyword);
        return query.getResultList();
    }

    /**
     * 認証種別で仕入先を検索する。
     *
     * <p>指定された認証種別（ISO 9001等）を取得している仕入先を返す。
     * サブクエリを使用して認証テーブルと結合している。</p>
     *
     * @param certType 認証種別（例: "ISO_9001", "ISO_14001"）
     * @return 該当認証を持つ仕入先のリスト
     */
    public List<Supplier> findByCertificationType(String certType) {
        LOG.log(Level.FINE, "認証種別別仕入先検索: certType={0}", certType);

        TypedQuery<Supplier> query = em.createQuery(
            "SELECT DISTINCT s FROM Supplier s " +
            "JOIN s.certifications c " +
            "WHERE c.certType = :certType " +
            "  AND c.status = 'ACTIVE' " +
            "ORDER BY s.name",
            Supplier.class
        );
        query.setParameter("certType", certType);
        return query.getResultList();
    }

    /**
     * 有効な仕入先のみを取得する。
     *
     * <p>ステータスが "ACTIVE" の仕入先を名前順で返す。</p>
     *
     * @return 有効な仕入先のリスト
     */
    public List<Supplier> findActiveSuppliers() {
        LOG.log(Level.FINE, "有効仕入先取得");

        TypedQuery<Supplier> query = em.createQuery(
            "SELECT s FROM Supplier s WHERE s.status = 'ACTIVE' ORDER BY s.name",
            Supplier.class
        );
        return query.getResultList();
    }

    /**
     * 仕入先コードで仕入先を検索する。
     *
     * @param code 仕入先コード
     * @return 該当仕入先（見つからない場合は {@code null}）
     */
    public Supplier findByCode(String code) {
        LOG.log(Level.FINE, "仕入先コード検索: code={0}", code);

        TypedQuery<Supplier> query = em.createQuery(
            "SELECT s FROM Supplier s WHERE s.code = :code",
            Supplier.class
        );
        query.setParameter("code", code);
        List<Supplier> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 評価が指定値以上の仕入先を検索する。
     *
     * @param minRating 最低評価値
     * @return 評価が基準以上の仕入先リスト
     */
    public List<Supplier> findByRatingAbove(java.math.BigDecimal minRating) {
        LOG.log(Level.FINE, "評価基準以上の仕入先検索: minRating={0}", minRating);

        TypedQuery<Supplier> query = em.createNamedQuery("Supplier.findByRatingAbove", Supplier.class);
        query.setParameter("minRating", minRating);
        return query.getResultList();
    }
}
