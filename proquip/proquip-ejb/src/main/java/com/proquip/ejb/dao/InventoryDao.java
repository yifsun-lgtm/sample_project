package com.proquip.ejb.dao;

import com.proquip.ejb.entity.inventory.InventoryItem;

import jakarta.ejb.Stateless;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 在庫品目エンティティに対するデータアクセスオブジェクト。
 *
 * <p>在庫品目の検索・更新に関するデータベースアクセスを提供する。
 * Criteria API、JPQL、ネイティブSQLの3つの方式を混在させており、
 * クエリ方式の一貫性が極めて低い。</p>
 *
 * <p>技術的負債 #18: 同一DAO内でCriteria API、JPQL、ネイティブSQLを混在使用。
 * {@link #updateQuantity(Long, int)} メソッドでは直接SQLのUPDATEを実行しており、
 * JPAのエンティティライフサイクル管理を完全に迂回している。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class InventoryDao extends AbstractBaseDao<InventoryItem, Long> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(InventoryDao.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryDao() {
        super();
    }

    /**
     * 倉庫IDで在庫品目を検索する（Criteria API版）。
     *
     * @param warehouseId 倉庫ID
     * @return 該当倉庫の在庫品目リスト
     */
    public List<InventoryItem> findByWarehouse(Long warehouseId) {
        LOG.log(Level.FINE, "倉庫別在庫検索: warehouseId={0}", warehouseId);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
        Root<InventoryItem> root = cq.from(InventoryItem.class);

        cq.select(root)
          .where(cb.equal(root.get("warehouse").get("id"), warehouseId))
          .orderBy(cb.asc(root.get("product").get("id")));

        return em.createQuery(cq).getResultList();
    }

    /**
     * 製品IDで在庫品目を検索する（JPQL版）。
     *
     * <p>技術的負債 #18: findByWarehouse()がCriteria APIなのに、
     * このメソッドはJPQLを使用している。</p>
     *
     * @param productId 製品ID
     * @return 該当製品の在庫品目リスト
     */
    public List<InventoryItem> findByProduct(Long productId) {
        LOG.log(Level.FINE, "製品別在庫検索: productId={0}", productId);

        // 技術的負債: findByWarehouse()がCriteria APIなのにJPQL
        TypedQuery<InventoryItem> query = em.createQuery(
            "SELECT ii FROM InventoryItem ii " +
            "WHERE ii.product.id = :productId " +
            "ORDER BY ii.warehouse.id",
            InventoryItem.class
        );
        query.setParameter("productId", productId);
        return query.getResultList();
    }

    /**
     * 在庫が再発注点以下の品目を検索する（ネイティブSQL版）。
     *
     * <p>技術的負債 #18: さらに別のクエリ方式（ネイティブSQL）を使用。
     * 同一クラス内に3種類のクエリパターンが混在している。</p>
     *
     * @return 在庫不足の品目リスト
     */
    @SuppressWarnings("unchecked")
    public List<InventoryItem> findLowStock() {
        LOG.log(Level.FINE, "在庫不足品目検索");

        // 技術的負債: ネイティブSQL（3種目のクエリ方式）
        Query query = em.createNativeQuery(
            "SELECT ii.* FROM inventory_item ii " +
            "WHERE ii.quantity_on_hand <= ii.reorder_point " +
            "  AND ii.reorder_point IS NOT NULL " +
            "ORDER BY (ii.quantity_on_hand - ii.reorder_point) ASC",
            InventoryItem.class
        );
        return query.getResultList();
    }

    /**
     * 保管場所で在庫品目を検索する（JPQL版）。
     *
     * <p>倉庫内の特定ゾーンに保管されている在庫品目を検索する。
     * 実際のStorageLocationとの直接的なリレーションがなく、
     * 倉庫ゾーン経由で検索するため非効率。</p>
     *
     * @param warehouseId 倉庫ID
     * @param zoneCode    ゾーンコード
     * @return 該当ロケーションの在庫品目リスト
     */
    public List<InventoryItem> findByStorageLocation(Long warehouseId, String zoneCode) {
        LOG.log(Level.FINE, "保管場所別在庫検索: warehouseId={0}, zoneCode={1}",
                new Object[]{warehouseId, zoneCode});

        TypedQuery<InventoryItem> query = em.createQuery(
            "SELECT ii FROM InventoryItem ii " +
            "WHERE ii.warehouse.id = :warehouseId " +
            "ORDER BY ii.product.id",
            InventoryItem.class
        );
        query.setParameter("warehouseId", warehouseId);
        // 技術的負債: zoneCodeパラメータが実際にはクエリで使われていない
        return query.getResultList();
    }

    /**
     * 在庫数量を直接更新する（ネイティブSQL UPDATE版）。
     *
     * <p><strong>警告: JPAエンティティのライフサイクルを迂回しています。</strong></p>
     *
     * <p>技術的負債: EntityManagerのmerge/persistを使用せず、
     * ネイティブSQLで直接UPDATEを実行している。
     * これにより以下の問題が発生する：</p>
     * <ul>
     *   <li>永続化コンテキストとDBの不整合</li>
     *   <li>楽観的ロック（@Version）が機能しない</li>
     *   <li>@PreUpdate コールバックが呼ばれない</li>
     *   <li>2次キャッシュが無効化されない</li>
     * </ul>
     *
     * @param inventoryItemId 在庫品目ID
     * @param newQuantity     新しい数量
     * @return 更新された行数
     */
    public int updateQuantity(Long inventoryItemId, int newQuantity) {
        LOG.log(Level.WARNING,
            "JPAライフサイクルを迂回する直接UPDATEが実行されます: inventoryItemId={0}",
            inventoryItemId);

        // 技術的負債: ネイティブSQLでの直接UPDATE
        // @Versionによる楽観的ロックが機能しない
        // @PreUpdate コールバックが呼ばれない
        Query query = em.createNativeQuery(
            "UPDATE inventory_item SET quantity_on_hand = ?1 WHERE id = ?2"
        );
        query.setParameter(1, newQuantity);
        query.setParameter(2, inventoryItemId);
        return query.executeUpdate();
    }

    /**
     * 製品と倉庫の組み合わせで在庫品目を検索する（Criteria API版）。
     *
     * @param productId   製品ID
     * @param warehouseId 倉庫ID
     * @return 該当在庫品目（見つからない場合は {@code null}）
     */
    public InventoryItem findByProductAndWarehouse(Long productId, Long warehouseId) {
        LOG.log(Level.FINE, "製品・倉庫別在庫検索: productId={0}, warehouseId={1}",
                new Object[]{productId, warehouseId});

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
        Root<InventoryItem> root = cq.from(InventoryItem.class);

        cq.select(root).where(
            cb.and(
                cb.equal(root.get("product").get("id"), productId),
                cb.equal(root.get("warehouse").get("id"), warehouseId)
            )
        );

        List<InventoryItem> results = em.createQuery(cq).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
