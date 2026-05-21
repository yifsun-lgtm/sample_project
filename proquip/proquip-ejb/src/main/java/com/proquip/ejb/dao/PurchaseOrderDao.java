package com.proquip.ejb.dao;

import com.proquip.ejb.entity.procurement.PurchaseOrder;

import jakarta.ejb.Stateless;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 発注エンティティに対するデータアクセスオブジェクト。
 *
 * <p>発注の検索・登録・更新に関するデータベースアクセスを提供する。
 * ネイティブSQLを多用しており、{@link ProductDao} のCriteria API方式、
 * {@link SupplierDao} のJPQL方式のいずれとも異なるアプローチを取っている。</p>
 *
 * <p>技術的負債 #18: クラス内でもネイティブSQLとJPQLが混在している。
 * 技術的負債 #11: {@link #searchOrders(String, String, String)} メソッドでは
 * ユーザー入力を直接SQL文字列に連結しており、SQLインジェクション脆弱性がある。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class PurchaseOrderDao extends AbstractBaseDao<PurchaseOrder, Long> {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(PurchaseOrderDao.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseOrderDao() {
        super();
    }

    /**
     * ステータスで発注を検索する（ネイティブSQL版）。
     *
     * <p>技術的負債: JPQLやCriteria APIではなくネイティブSQLを使用している。
     * これによりデータベース方言に依存し、移植性が低下する。</p>
     *
     * @param status 発注ステータス
     * @return 該当ステータスの発注リスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseOrder> findByStatus(String status) {
        LOG.log(Level.FINE, "ステータス別発注検索: status={0}", status);

        // 技術的負債: ネイティブSQLを使用（JPQLで十分なケース）
        Query query = em.createNativeQuery(
            "SELECT po.* FROM purchase_order po " +
            "WHERE po.status = ?1 " +
            "ORDER BY po.order_date DESC",
            PurchaseOrder.class
        );
        query.setParameter(1, status);
        return query.getResultList();
    }

    /**
     * 日付範囲で発注を検索する（ネイティブSQL版）。
     *
     * @param startDate 検索開始日
     * @param endDate   検索終了日
     * @return 指定期間内の発注リスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseOrder> findByDateRange(Date startDate, Date endDate) {
        LOG.log(Level.FINE, "日付範囲別発注検索: start={0}, end={1}",
                new Object[]{startDate, endDate});

        // 技術的負債: ネイティブSQLを使用
        Query query = em.createNativeQuery(
            "SELECT po.* FROM purchase_order po " +
            "WHERE po.order_date BETWEEN ?1 AND ?2 " +
            "ORDER BY po.order_date DESC",
            PurchaseOrder.class
        );
        query.setParameter(1, startDate);
        query.setParameter(2, endDate);
        return query.getResultList();
    }

    /**
     * 仕入先IDで発注を検索する（ネイティブSQL版）。
     *
     * @param supplierId 仕入先ID
     * @return 該当仕入先の発注リスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseOrder> findBySupplier(Long supplierId) {
        LOG.log(Level.FINE, "仕入先別発注検索: supplierId={0}", supplierId);

        Query query = em.createNativeQuery(
            "SELECT po.* FROM purchase_order po " +
            "WHERE po.supplier_id = ?1 " +
            "ORDER BY po.order_date DESC",
            PurchaseOrder.class
        );
        query.setParameter(1, supplierId);
        return query.getResultList();
    }

    /**
     * 複合条件で発注を検索する（ネイティブSQL + 文字列連結版）。
     *
     * <p><strong>警告: このメソッドにはSQLインジェクション脆弱性があります。</strong></p>
     *
     * <p>技術的負債 #11: ユーザー入力をSQL文字列に直接連結している。
     * パラメータバインディングを使用すべき。
     * 本来はCriteria APIまたはプリペアドステートメントに移行すべき。</p>
     *
     * @param poNumber     発注番号（部分一致、nullの場合は条件に含めない）
     * @param status       ステータス（nullの場合は条件に含めない）
     * @param supplierName 仕入先名（部分一致、nullの場合は条件に含めない）
     * @return 検索結果の発注リスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseOrder> searchOrders(String poNumber, String status, String supplierName) {
        LOG.log(Level.WARNING,
            "SQLインジェクション脆弱性のあるメソッドが呼び出されました: searchOrders");

        // 技術的負債 #11: SQL文字列連結によるSQLインジェクション脆弱性
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT po.* FROM purchase_order po ");
        sql.append("JOIN supplier s ON po.supplier_id = s.id ");
        sql.append("WHERE 1=1 ");

        // 危険: ユーザー入力の直接連結
        if (poNumber != null && !poNumber.isEmpty()) {
            sql.append("AND po.po_number LIKE '%" + poNumber + "%' ");
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND po.status = '" + status + "' ");
        }
        if (supplierName != null && !supplierName.isEmpty()) {
            sql.append("AND s.name LIKE '%" + supplierName + "%' ");
        }

        sql.append("ORDER BY po.order_date DESC");

        LOG.log(Level.FINE, "実行SQL: {0}", sql.toString());

        Query query = em.createNativeQuery(sql.toString(), PurchaseOrder.class);
        return query.getResultList();
    }

    /**
     * 承認待ちの発注を取得する。
     *
     * <p>技術的負債 #18: このメソッドだけJPQLを使用しており、
     * クラス内の他のメソッド（ネイティブSQL）と方式が異なる。</p>
     *
     * @return 承認待ちの発注リスト
     */
    public List<PurchaseOrder> findPendingApproval() {
        LOG.log(Level.FINE, "承認待ち発注取得");

        // 技術的負債: 他のメソッドがネイティブSQLなのに、ここだけJPQL
        TypedQuery<PurchaseOrder> query = em.createQuery(
            "SELECT po FROM PurchaseOrder po " +
            "WHERE po.status = 'SUBMITTED' " +
            "ORDER BY po.orderDate ASC",
            PurchaseOrder.class
        );
        return query.getResultList();
    }

    /**
     * 発注番号で発注を検索する（ネイティブSQL版）。
     *
     * @param poNumber 発注番号
     * @return 該当発注（見つからない場合は {@code null}）
     */
    @SuppressWarnings("unchecked")
    public PurchaseOrder findByPoNumber(String poNumber) {
        LOG.log(Level.FINE, "発注番号検索: poNumber={0}", poNumber);

        Query query = em.createNativeQuery(
            "SELECT po.* FROM purchase_order po WHERE po.po_number = ?1",
            PurchaseOrder.class
        );
        query.setParameter(1, poNumber);

        List<PurchaseOrder> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 購入担当者IDで発注を検索する（ネイティブSQL版）。
     *
     * @param buyerId 購入担当者のユーザーID
     * @return 該当担当者の発注リスト
     */
    @SuppressWarnings("unchecked")
    public List<PurchaseOrder> findByBuyer(Long buyerId) {
        LOG.log(Level.FINE, "購入担当者別発注検索: buyerId={0}", buyerId);

        Query query = em.createNativeQuery(
            "SELECT po.* FROM purchase_order po " +
            "WHERE po.buyerId = ?1 " +
            "ORDER BY po.order_date DESC",
            PurchaseOrder.class
        );
        query.setParameter(1, buyerId);
        return query.getResultList();
    }
}
