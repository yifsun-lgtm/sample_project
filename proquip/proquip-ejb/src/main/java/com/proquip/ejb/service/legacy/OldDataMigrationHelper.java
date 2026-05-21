package com.proquip.ejb.service.legacy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// 2022年のデータ移行時に使用。移行完了後は不要だが念のため残す

/**
 * 旧システムからのデータ移行ヘルパー。
 *
 * <p><strong>デッドコード:</strong> このクラスは2022年のデータ移行プロジェクトで
 * 使用されたものであり、移行完了後は一切使用されていない。
 * 「念のため」という理由で残されているが、削除すべきである。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>デッドコード (#9): どこからもインジェクト・参照されていない。</li>
 *   <li>ハードコードされた接続文字列 (#19): 存在しないデータベースへの接続情報が
 *       ソースコードに埋め込まれている。セキュリティリスク。</li>
 *   <li>{@link java.sql.DriverManager} を直接使用しており、
 *       コネクションプールを利用していない。</li>
 *   <li>リソースのクローズ処理が try-with-resources ではなく
 *       手動の finally ブロックで行われており、不完全。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @deprecated 2022年のデータ移行完了後、不要になったクラス。削除推奨。
 */
@Deprecated
public class OldDataMigrationHelper {

    private static final Logger logger = Logger.getLogger(OldDataMigrationHelper.class.getName());

    /**
     * 旧システムのデータベース接続URL。
     * 技術的負債 #19: ハードコードされた接続文字列。存在しないデータベース。
     * セキュリティリスク: 認証情報がソースコードに含まれている。
     */
    private static final String OLD_DB_URL = "jdbc:postgresql://old-procurement-db.internal:5432/legacy_procurement";

    /**
     * 旧システムのデータベースユーザー名。
     * 技術的負債 #19: ハードコードされた認証情報。
     */
    private static final String OLD_DB_USER = "legacy_admin";

    /**
     * 旧システムのデータベースパスワード。
     * 技術的負債 #19: ハードコードされた認証情報。重大なセキュリティリスク。
     */
    private static final String OLD_DB_PASSWORD = "Leg@cy_Pr0c_2022!";

    /**
     * 旧システムから仕入先データを移行する。
     *
     * <p>技術的負債: DriverManager.getConnection()を直接使用。
     * DataSourceやコネクションプールを使用すべき。</p>
     *
     * @return 移行された仕入先データのリスト（Map形式）
     * @deprecated データ移行完了済み。使用不可。
     */
    @Deprecated
    public List<Map<String, Object>> migrateSuppliers() {
        logger.warning("非推奨メソッド migrateSuppliers が呼び出されました。"
                + "このメソッドは2022年のデータ移行時に使用されたものです。");

        List<Map<String, Object>> suppliers = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 技術的負債: DriverManagerを直接使用
            conn = DriverManager.getConnection(OLD_DB_URL, OLD_DB_USER, OLD_DB_PASSWORD);

            String sql = "SELECT supplier_id, supplier_code, supplier_name, contact_email, "
                    + "contact_phone, address, status, created_date "
                    + "FROM old_suppliers WHERE status != 'DELETED' ORDER BY supplier_code";

            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                // 技術的負債: Rawタイプの使用
                Map<String, Object> supplier = new HashMap();
                supplier.put("oldId", rs.getLong("supplier_id"));
                supplier.put("code", rs.getString("supplier_code"));
                supplier.put("name", rs.getString("supplier_name"));
                supplier.put("email", rs.getString("contact_email"));
                supplier.put("phone", rs.getString("contact_phone"));
                supplier.put("address", rs.getString("address"));
                supplier.put("status", convertStatus(rs.getString("status")));
                supplier.put("createdDate", rs.getTimestamp("created_date"));
                suppliers.add(supplier);
            }

            logger.info("旧システムから " + suppliers.size() + " 件の仕入先データを取得しました");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "旧システムからの仕入先データ取得に失敗しました", e);
        } finally {
            // 技術的負債: try-with-resourcesを使用すべき
            // リソースのクローズ順序も逆順にすべき
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "ResultSetのクローズに失敗", e);
            }
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "PreparedStatementのクローズに失敗", e);
            }
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Connectionのクローズに失敗", e);
            }
        }

        return suppliers;
    }

    /**
     * 旧システムから商品データを移行する。
     *
     * @return 移行された商品データのリスト（Map形式）
     * @deprecated データ移行完了済み。使用不可。
     */
    @Deprecated
    public List<Map<String, Object>> migrateProducts() {
        logger.warning("非推奨メソッド migrateProducts が呼び出されました。");

        List<Map<String, Object>> products = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(OLD_DB_URL, OLD_DB_USER, OLD_DB_PASSWORD);

            String sql = "SELECT product_id, sku, product_name, description, "
                    + "unit_price, category_code, status "
                    + "FROM old_products WHERE is_deleted = false ORDER BY sku";

            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> product = new HashMap();
                product.put("oldId", rs.getLong("product_id"));
                product.put("sku", rs.getString("sku"));
                product.put("name", rs.getString("product_name"));
                product.put("description", rs.getString("description"));
                product.put("unitPrice", rs.getBigDecimal("unit_price"));
                product.put("categoryCode", rs.getString("category_code"));
                product.put("status", convertStatus(rs.getString("status")));
                products.add(product);
            }

            logger.info("旧システムから " + products.size() + " 件の商品データを取得しました");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "旧システムからの商品データ取得に失敗しました", e);
        } finally {
            closeQuietly(rs, stmt, conn);
        }

        return products;
    }

    /**
     * 旧システムから発注履歴データを移行する。
     *
     * @param yearFrom 開始年
     * @param yearTo   終了年
     * @return 移行された発注データのリスト（Map形式）
     * @deprecated データ移行完了済み。使用不可。
     */
    @Deprecated
    public List<Map<String, Object>> migratePurchaseOrders(int yearFrom, int yearTo) {
        logger.warning("非推奨メソッド migratePurchaseOrders が呼び出されました。");

        List<Map<String, Object>> orders = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(OLD_DB_URL, OLD_DB_USER, OLD_DB_PASSWORD);

            String sql = "SELECT po_id, po_number, supplier_code, order_date, "
                    + "total_amount, currency, status "
                    + "FROM old_purchase_orders "
                    + "WHERE EXTRACT(YEAR FROM order_date) BETWEEN ? AND ? "
                    + "ORDER BY order_date";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, yearFrom);
            stmt.setInt(2, yearTo);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> order = new HashMap();
                order.put("oldId", rs.getLong("po_id"));
                order.put("poNumber", rs.getString("po_number"));
                order.put("supplierCode", rs.getString("supplier_code"));
                order.put("orderDate", rs.getDate("order_date"));
                order.put("totalAmount", rs.getBigDecimal("total_amount"));
                order.put("currency", rs.getString("currency"));
                order.put("status", convertStatus(rs.getString("status")));
                orders.add(order);
            }

            logger.info("旧システムから " + orders.size() + " 件の発注データを取得しました "
                    + "(期間: " + yearFrom + "～" + yearTo + ")");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "旧システムからの発注データ取得に失敗しました", e);
        } finally {
            closeQuietly(rs, stmt, conn);
        }

        return orders;
    }

    /**
     * 旧システムのステータスコードを新システムのステータスに変換する。
     *
     * @param oldStatus 旧システムのステータスコード
     * @return 新システムのステータス文字列
     */
    private String convertStatus(String oldStatus) {
        if (oldStatus == null) {
            return "INACTIVE";
        }
        switch (oldStatus) {
            case "A":
            case "ACTIVE":
                return "ACTIVE";
            case "I":
            case "INACTIVE":
                return "INACTIVE";
            case "S":
            case "SUSPENDED":
                return "SUSPENDED";
            case "P":
            case "PENDING":
                return "PENDING_APPROVAL";
            default:
                logger.warning("不明な旧ステータスコード: " + oldStatus);
                return "INACTIVE";
        }
    }

    /**
     * JDBCリソースを安全にクローズする。
     *
     * <p>技術的負債: try-with-resourcesを使用すべき。</p>
     *
     * @param rs   ResultSet（nullの場合はスキップ）
     * @param stmt PreparedStatement（nullの場合はスキップ）
     * @param conn Connection（nullの場合はスキップ）
     */
    private void closeQuietly(ResultSet rs, PreparedStatement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "ResultSetのクローズに失敗", e);
        }
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "PreparedStatementのクローズに失敗", e);
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Connectionのクローズに失敗", e);
        }
    }
}
