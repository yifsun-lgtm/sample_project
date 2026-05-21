package com.proquip.ejb.service.legacy;

import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;
import com.proquip.ejb.entity.supplier.Supplier;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

// 旧レポートシステム。新システム移行後に削除予定。2023年6月

/**
 * レガシーレポートジェネレーター。
 *
 * <p><strong>デッドコード:</strong> このクラスは旧レポートシステムの一部であり、
 * 新しいレポートモジュールへの移行後は使用されていない。
 * どこからもインジェクトされておらず、削除が推奨される。</p>
 *
 * <p>技術的負債 #9:
 * <ul>
 *   <li>デッドコードが残存している（2023年6月以降不使用）。</li>
 *   <li>{@link StringBuffer} によるHTML手動生成。テンプレートエンジンを使用すべき。</li>
 *   <li>Rawタイプの使用および未チェックキャストが多数存在。</li>
 *   <li>{@code @SuppressWarnings} なしでコンパイル警告が発生する。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @deprecated 新レポートモジュールに移行済み。2023年6月以降は不使用。次回リリースで削除予定。
 */
@Deprecated
@Stateless
public class LegacyReportGenerator {

    private static final Logger logger = Logger.getLogger(LegacyReportGenerator.class.getName());

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 発注レポートをHTML形式で生成する。
     *
     * <p>技術的負債: StringBufferによるHTML手動生成。
     * テンプレートエンジン（Thymeleaf等）を使用すべき。</p>
     *
     * @param startDate レポート開始日
     * @param endDate   レポート終了日
     * @return HTML文字列
     * @deprecated 新レポートモジュールを使用してください
     */
    @Deprecated
    public String generatePurchaseOrderReport(Date startDate, Date endDate) {
        logger.warning("非推奨メソッド generatePurchaseOrderReport が呼び出されました");

        // 技術的負債: Rawタイプの使用（ジェネリクスなし）
        List results = em.createNamedQuery("PurchaseOrder.findByDateRange")
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

        StringBuffer html = new StringBuffer();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head><title>発注レポート</title>");
        html.append("<style>");
        html.append("table { border-collapse: collapse; width: 100%; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #4472C4; color: white; }");
        html.append("tr:nth-child(even) { background-color: #f2f2f2; }");
        html.append(".total { font-weight: bold; background-color: #E2EFDA; }");
        html.append("</style>");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<h1>発注レポート</h1>\n");
        html.append("<p>期間: " + DATE_FORMAT.format(startDate) + " ～ " + DATE_FORMAT.format(endDate) + "</p>\n");
        html.append("<p>生成日時: " + DATETIME_FORMAT.format(new Date()) + "</p>\n");
        html.append("<table>\n");
        html.append("<tr><th>発注番号</th><th>サプライヤー</th><th>発注日</th>");
        html.append("<th>ステータス</th><th>金額</th><th>通貨</th></tr>\n");

        double totalAmount = 0;
        int orderCount = 0;

        for (int i = 0; i < results.size(); i++) {
            // 技術的負債: 未チェックキャスト
            PurchaseOrder po = (PurchaseOrder) results.get(i);
            html.append("<tr>");
            html.append("<td>" + po.getPoNumber() + "</td>");
            html.append("<td>" + (po.getSupplier() != null ? po.getSupplier().getName() : "N/A") + "</td>");
            html.append("<td>" + (po.getOrderDate() != null ? DATE_FORMAT.format(po.getOrderDate()) : "N/A") + "</td>");
            html.append("<td>" + po.getStatus() + "</td>");
            html.append("<td>" + (po.getTotalAmount() != null ? po.getTotalAmount().toString() : "0") + "</td>");
            html.append("<td>" + (po.getCurrency() != null ? po.getCurrency() : "JPY") + "</td>");
            html.append("</tr>\n");

            if (po.getTotalAmount() != null) {
                totalAmount += po.getTotalAmount().doubleValue();
            }
            orderCount++;

            // 明細行の展開
            if (po.getItems() != null) {
                for (int j = 0; j < po.getItems().size(); j++) {
                    // 技術的負債: 未チェックキャスト、List.get(i)のインデックスアクセス
                    PurchaseOrderItem item = po.getItems().get(j);
                    html.append("<tr style='background-color: #f9f9f9;'>");
                    html.append("<td></td>");
                    html.append("<td style='padding-left: 20px;'>└ " + (item.getProduct() != null ? item.getProduct().getName() : "商品不明") + "</td>");
                    html.append("<td>" + item.getQuantity() + "</td>");
                    html.append("<td>" + (item.getStatus() != null ? item.getStatus() : "-") + "</td>");
                    html.append("<td>" + (item.getSubtotal() != null ? item.getSubtotal().toString() : "0") + "</td>");
                    html.append("<td></td>");
                    html.append("</tr>\n");
                }
            }
        }

        html.append("<tr class='total'>");
        html.append("<td colspan='4'>合計 (" + orderCount + " 件)</td>");
        html.append("<td>" + totalAmount + "</td>");
        html.append("<td></td>");
        html.append("</tr>\n");
        html.append("</table>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 在庫レポートをHTML形式で生成する。
     *
     * <p>技術的負債: StringBufferによるHTML手動生成。</p>
     *
     * @param warehouseId 倉庫ID（nullの場合は全倉庫）
     * @return HTML文字列
     * @deprecated 新レポートモジュールを使用してください
     */
    @Deprecated
    public String generateInventoryReport(Long warehouseId) {
        logger.warning("非推奨メソッド generateInventoryReport が呼び出されました");

        // 技術的負債: Rawタイプの使用
        List results;
        if (warehouseId != null) {
            results = em.createNamedQuery("InventoryItem.findByWarehouse")
                    .setParameter("warehouseId", warehouseId)
                    .getResultList();
        } else {
            results = em.createQuery("SELECT ii FROM InventoryItem ii ORDER BY ii.product.id")
                    .getResultList();
        }

        StringBuffer html = new StringBuffer();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head><title>在庫レポート</title>");
        html.append("<style>");
        html.append("table { border-collapse: collapse; width: 100%; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #548235; color: white; }");
        html.append(".low-stock { background-color: #FFC7CE; }");
        html.append(".out-of-stock { background-color: #FF0000; color: white; }");
        html.append("</style>");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<h1>在庫レポート</h1>\n");
        html.append("<p>生成日時: " + DATETIME_FORMAT.format(new Date()) + "</p>\n");
        html.append("<table>\n");
        html.append("<tr><th>商品</th><th>倉庫</th><th>手持在庫</th>");
        html.append("<th>引当済</th><th>発注中</th><th>再発注点</th><th>状態</th></tr>\n");

        int totalItems = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        for (int i = 0; i < results.size(); i++) {
            // 技術的負債: 未チェックキャスト
            InventoryItem item = (InventoryItem) results.get(i);

            String rowClass = "";
            String status = "正常";
            if (item.getQuantityOnHand() == 0) {
                rowClass = " class='out-of-stock'";
                status = "在庫切れ";
                outOfStockCount++;
            // reorderPoint field was removed from InventoryItem (DDL alignment)
            } else if (item.getQuantityOnHand() != null && item.getQuantityOnHand() <= 0) {
                rowClass = " class='low-stock'";
                status = "在庫不足";
                lowStockCount++;
            }

            html.append("<tr" + rowClass + ">");
            html.append("<td>" + (item.getProduct() != null ? item.getProduct().getName() : "不明") + "</td>");
            html.append("<td>" + (item.getWarehouse() != null ? item.getWarehouse().toString() : "不明") + "</td>");
            html.append("<td>" + item.getQuantityOnHand() + "</td>");
            html.append("<td>" + item.getQuantityReserved() + "</td>");
            html.append("<td>" + item.getQuantityOnOrder() + "</td>");
            // reorderPoint was removed from InventoryItem (DDL alignment)
            html.append("<td>-</td>");
            html.append("<td>" + status + "</td>");
            html.append("</tr>\n");

            totalItems++;
        }

        html.append("</table>\n");
        html.append("<h3>サマリー</h3>\n");
        html.append("<ul>\n");
        html.append("<li>総品目数: " + totalItems + "</li>\n");
        html.append("<li>在庫不足: " + lowStockCount + " 件</li>\n");
        html.append("<li>在庫切れ: " + outOfStockCount + " 件</li>\n");
        html.append("</ul>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * サプライヤーレポートをHTML形式で生成する。
     *
     * <p>技術的負債: StringBufferによるHTML手動生成。
     * Rawタイプおよび未チェックキャストの使用。</p>
     *
     * @return HTML文字列
     * @deprecated 新レポートモジュールを使用してください
     */
    @Deprecated
    public String generateSupplierReport() {
        logger.warning("非推奨メソッド generateSupplierReport が呼び出されました");

        // 技術的負債: Rawタイプの使用
        List results = em.createNamedQuery("Supplier.findByStatus")
                .setParameter("status", "ACTIVE")
                .getResultList();

        StringBuffer html = new StringBuffer();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head><title>サプライヤーレポート</title>");
        html.append("<style>");
        html.append("table { border-collapse: collapse; width: 100%; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #7030A0; color: white; }");
        html.append(".high-rating { background-color: #C6EFCE; }");
        html.append(".low-rating { background-color: #FFC7CE; }");
        html.append("</style>");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<h1>サプライヤーレポート（アクティブ）</h1>\n");
        html.append("<p>生成日時: " + DATETIME_FORMAT.format(new Date()) + "</p>\n");
        html.append("<table>\n");
        html.append("<tr><th>コード</th><th>名称</th><th>ステータス</th>");
        html.append("<th>評価</th><th>支払条件(日)</th><th>連絡先数</th></tr>\n");

        int supplierCount = 0;
        double totalRating = 0;
        int ratedSuppliers = 0;

        for (int i = 0; i < results.size(); i++) {
            // 技術的負債: 未チェックキャスト
            Supplier supplier = (Supplier) results.get(i);

            // rating and paymentTermDays fields were removed from Supplier (DDL alignment)
            String rowClass = "";

            html.append("<tr" + rowClass + ">");
            html.append("<td>" + supplier.getCode() + "</td>");
            html.append("<td>" + supplier.getName() + "</td>");
            html.append("<td>" + supplier.getStatus() + "</td>");
            html.append("<td>未評価</td>");
            html.append("<td>-</td>");
            html.append("<td>" + (supplier.getContacts() != null ? supplier.getContacts().size() : 0) + "</td>");
            html.append("</tr>\n");

            supplierCount++;
        }

        html.append("</table>\n");
        html.append("<h3>サマリー</h3>\n");
        html.append("<ul>\n");
        html.append("<li>アクティブサプライヤー数: " + supplierCount + "</li>\n");
        if (ratedSuppliers > 0) {
            html.append("<li>平均評価: " + (totalRating / ratedSuppliers) + "</li>\n");
        }
        html.append("</ul>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * CSVフォーマットで発注データをエクスポートする。
     *
     * @param startDate 開始日
     * @param endDate   終了日
     * @return CSV文字列
     * @deprecated 新レポートモジュールを使用してください
     */
    @Deprecated
    public String exportPurchaseOrdersCsv(Date startDate, Date endDate) {
        logger.warning("非推奨メソッド exportPurchaseOrdersCsv が呼び出されました");

        // 技術的負債: Rawタイプの使用
        List results = em.createNamedQuery("PurchaseOrder.findByDateRange")
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();

        StringBuffer csv = new StringBuffer();
        csv.append("発注番号,サプライヤー,発注日,ステータス,金額,通貨\n");

        for (int i = 0; i < results.size(); i++) {
            PurchaseOrder po = (PurchaseOrder) results.get(i);
            csv.append(escapeCsv(po.getPoNumber()) + ",");
            csv.append(escapeCsv(po.getSupplier() != null ? po.getSupplier().getName() : "") + ",");
            csv.append((po.getOrderDate() != null ? DATE_FORMAT.format(po.getOrderDate()) : "") + ",");
            csv.append(escapeCsv(po.getStatus()) + ",");
            csv.append((po.getTotalAmount() != null ? po.getTotalAmount().toString() : "0") + ",");
            csv.append(po.getCurrency() != null ? po.getCurrency() : "JPY");
            csv.append("\n");
        }

        return csv.toString();
    }

    /**
     * CSV用のエスケープ処理。
     *
     * @param value エスケープ対象の文字列
     * @return エスケープ済み文字列
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
