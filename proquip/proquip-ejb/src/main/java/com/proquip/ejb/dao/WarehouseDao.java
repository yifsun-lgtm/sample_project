package com.proquip.ejb.dao;

import com.proquip.ejb.entity.inventory.Warehouse;

import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 倉庫エンティティに対するデータアクセスオブジェクト。
 *
 * <p>倉庫マスタの検索に関するデータベースアクセスを提供する。
 * NamedQueryとJPQLを使用している。</p>
 *
 * <p>技術的負債 #8: メソッド名に "find" と "get" プレフィックスが混在しており、
 * 命名規則に一貫性がない。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class WarehouseDao extends AbstractBaseDao<Warehouse, Long> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(WarehouseDao.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public WarehouseDao() {
        super();
    }

    /**
     * 有効な倉庫を全て取得する。
     *
     * @return 有効な倉庫のリスト
     */
    public List<Warehouse> findActiveWarehouses() {
        LOG.log(Level.FINE, "有効倉庫取得");

        TypedQuery<Warehouse> query = em.createNamedQuery("Warehouse.findActive", Warehouse.class);
        return query.getResultList();
    }

    /**
     * 倉庫コードで倉庫を検索する。
     *
     * @param code 倉庫コード
     * @return 該当倉庫（見つからない場合は {@code null}）
     */
    public Warehouse findByCode(String code) {
        LOG.log(Level.FINE, "倉庫コード検索: code={0}", code);

        TypedQuery<Warehouse> query = em.createNamedQuery("Warehouse.findByCode", Warehouse.class);
        query.setParameter("code", code);
        List<Warehouse> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 倉庫とそのゾーン情報を取得する。
     *
     * <p>技術的負債 #8: "get" プレフィックスを使用しており、
     * 他のメソッドの "find" プレフィックスと一貫性がない。
     * また、JOIN FETCHを使用しているが、他のメソッドでは
     * 遅延ロードに任せている。</p>
     *
     * @param warehouseId 倉庫ID
     * @return ゾーン情報を含む倉庫（見つからない場合は {@code null}）
     */
    public Warehouse getWarehouseWithZones(Long warehouseId) {
        LOG.log(Level.FINE, "倉庫（ゾーン付き）取得: id={0}", warehouseId);

        // 技術的負債: メソッド名が "get" で始まっており、 "find" と不統一
        TypedQuery<Warehouse> query = em.createQuery(
            "SELECT w FROM Warehouse w LEFT JOIN FETCH w.zones WHERE w.id = :id",
            Warehouse.class
        );
        query.setParameter("id", warehouseId);
        List<Warehouse> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 倉庫タイプで倉庫を検索する。
     *
     * @param type 倉庫タイプ（例: "MAIN", "SATELLITE", "VIRTUAL"）
     * @return 該当タイプの倉庫リスト
     */
    public List<Warehouse> findByType(String type) {
        LOG.log(Level.FINE, "タイプ別倉庫検索: type={0}", type);

        TypedQuery<Warehouse> query = em.createNamedQuery("Warehouse.findByType", Warehouse.class);
        query.setParameter("type", type);
        return query.getResultList();
    }
}
