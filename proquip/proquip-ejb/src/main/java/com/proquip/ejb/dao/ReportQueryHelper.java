package com.proquip.ejb.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * レポート用ネイティブSQLヘルパークラス。
 *
 * <p>ダッシュボードやレポート画面で使用される集計クエリを提供する。
 * 全メソッドでネイティブSQLを使用し、結果を {@code List<Object[]>} で返す。</p>
 *
 * <p>技術的負債 #11: 一部メソッドでユーザー入力をSQL文字列に直接連結しており、
 * SQLインジェクション脆弱性がある。</p>
 *
 * <p>技術的負債 #12: 戻り値が {@code List<Object[]>} であり型安全性がない。
 * 本来はDTOクラスやJPQLのコンストラクタ式を使用すべき。
 * 呼び出し側でインデックスベースのキャストが必要で、
 * カラム順序の変更が破壊的変更になる。</p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class ReportQueryHelper {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(ReportQueryHelper.class.getName());

    /** 永続化コンテキスト */
    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * デフォルトコンストラクタ。
     */
    public ReportQueryHelper() {
    }

    /**
     * カテゴリ別の支出集計を取得する。
     *
     * <p>技術的負債 #11: 日付パラメータをSQL文字列に直接連結している。
     * プリペアドステートメントを使用すべき。</p>
     *
     * <p>技術的負債 #12: 戻り値が {@code List<Object[]>} で、
     * 各要素は [カテゴリ名, 発注件数, 合計金額] の配列。</p>
     *
     * @param startDate 集計開始日（文字列、"YYYY-MM-DD"形式）
     * @param endDate   集計終了日（文字列、"YYYY-MM-DD"形式）
     * @return カテゴリ別支出の集計結果
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getSpendingByCategory(String startDate, String endDate) {
        LOG.log(Level.FINE, "カテゴリ別支出集計: {0} ～ {1}", new Object[]{startDate, endDate});

        // 技術的負債 #11: 日付文字列の直接連結（SQLインジェクション脆弱性）
        String sql = "SELECT c.name AS category_name, " +
                     "       COUNT(DISTINCT po.id) AS order_count, " +
                     "       SUM(poi.subtotal) AS total_amount " +
                     "FROM purchase_order po " +
                     "JOIN purchase_order_item poi ON po.id = poi.purchase_order_id " +
                     "JOIN product p ON poi.product_id = p.id " +
                     "JOIN category c ON p.category_id = c.id " +
                     "WHERE po.order_date BETWEEN '" + startDate + "' AND '" + endDate + "' " +
                     "  AND po.status NOT IN ('CANCELLED', 'DRAFT') " +
                     "GROUP BY c.name " +
                     "ORDER BY total_amount DESC";

        Query query = em.createNativeQuery(sql);
        return query.getResultList();
    }

    /**
     * 在庫評価額を取得する。
     *
     * <p>各倉庫の在庫数量に単価を乗じた評価額を集計する。</p>
     *
     * <p>技術的負債 #12: 戻り値が {@code List<Object[]>} で、
     * 各要素は [倉庫名, 品目数, 合計数量, 評価額] の配列。</p>
     *
     * @return 倉庫別の在庫評価額
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getInventoryValuation() {
        LOG.log(Level.FINE, "在庫評価額取得");

        String sql = "SELECT w.name AS warehouse_name, " +
                     "       COUNT(ii.id) AS item_count, " +
                     "       SUM(ii.quantity_on_hand) AS total_quantity, " +
                     "       SUM(ii.quantity_on_hand * p.unit_price) AS total_value " +
                     "FROM inventory_item ii " +
                     "JOIN warehouse w ON ii.warehouse_id = w.id " +
                     "JOIN product p ON ii.product_id = p.id " +
                     "WHERE w.is_active = true " +
                     "GROUP BY w.name " +
                     "ORDER BY total_value DESC";

        Query query = em.createNativeQuery(sql);
        return query.getResultList();
    }

    /**
     * 仕入先パフォーマンスレポートを取得する。
     *
     * <p>仕入先ごとの発注件数、合計金額、平均納期遵守率を集計する。</p>
     *
     * <p>技術的負債 #11: フィルタ条件のステータスが文字列リテラルで
     * ハードコーディングされている。定数化すべき。</p>
     *
     * <p>技術的負債 #12: 戻り値が {@code List<Object[]>} で、
     * 各要素は [仕入先名, 仕入先コード, 発注件数, 合計金額, 平均評価] の配列。</p>
     *
     * @param year 対象年度
     * @return 仕入先別パフォーマンスの集計結果
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getSupplierPerformance(int year) {
        LOG.log(Level.FINE, "仕入先パフォーマンスレポート: year={0}", year);

        // 技術的負債 #11: 年のパラメータを直接連結
        String sql = "SELECT s.name AS supplier_name, " +
                     "       s.code AS supplier_code, " +
                     "       COUNT(po.id) AS order_count, " +
                     "       SUM(po.total_amount) AS total_amount, " +
                     "       s.rating AS avg_rating " +
                     "FROM supplier s " +
                     "LEFT JOIN purchase_order po ON s.id = po.supplier_id " +
                     "  AND EXTRACT(YEAR FROM po.order_date) = " + year + " " +
                     "  AND po.status NOT IN ('CANCELLED', 'DRAFT') " +
                     "WHERE s.status = 'ACTIVE' " +
                     "GROUP BY s.name, s.code, s.rating " +
                     "ORDER BY total_amount DESC";

        Query query = em.createNativeQuery(sql);
        return query.getResultList();
    }

    /**
     * 発注サマリを取得する。
     *
     * <p>ステータス別の発注件数と金額を集計する。</p>
     *
     * <p>技術的負債 #12: 戻り値が {@code List<Object[]>} で、
     * 各要素は [ステータス, 件数, 合計金額] の配列。</p>
     *
     * @param startDate 集計開始日
     * @param endDate   集計終了日
     * @return ステータス別発注サマリの集計結果
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getPurchaseOrderSummary(Date startDate, Date endDate) {
        LOG.log(Level.FINE, "発注サマリ取得: {0} ～ {1}", new Object[]{startDate, endDate});

        String sql = "SELECT po.status, " +
                     "       COUNT(po.id) AS order_count, " +
                     "       COALESCE(SUM(po.total_amount), 0) AS total_amount " +
                     "FROM purchase_order po " +
                     "WHERE po.order_date BETWEEN ?1 AND ?2 " +
                     "GROUP BY po.status " +
                     "ORDER BY order_count DESC";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, startDate);
        query.setParameter(2, endDate);
        return query.getResultList();
    }

    /**
     * 月別の発注推移を取得する。
     *
     * <p>技術的負債 #11: 年パラメータをSQL文字列に直接連結。</p>
     *
     * @param year 対象年度
     * @return 月別発注件数と金額の集計結果
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getMonthlyOrderTrend(int year) {
        LOG.log(Level.FINE, "月別発注推移: year={0}", year);

        // 技術的負債 #11: 年パラメータの直接連結
        String sql = "SELECT EXTRACT(MONTH FROM po.order_date) AS month, " +
                     "       COUNT(po.id) AS order_count, " +
                     "       COALESCE(SUM(po.total_amount), 0) AS total_amount " +
                     "FROM purchase_order po " +
                     "WHERE EXTRACT(YEAR FROM po.order_date) = " + year + " " +
                     "  AND po.status NOT IN ('CANCELLED') " +
                     "GROUP BY EXTRACT(MONTH FROM po.order_date) " +
                     "ORDER BY month";

        Query query = em.createNativeQuery(sql);
        return query.getResultList();
    }
}
