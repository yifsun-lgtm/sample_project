package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.ImportException;
import com.proquip.common.exception.ValidationException;
import com.proquip.common.util.CsvParser;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.entity.system.ImportJob;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * データインポート/エクスポートサービスBean。
 * <p>
 * CSV形式のデータインポートおよびエクスポート処理を提供する。
 * 商品マスタ、仕入先マスタ、在庫データの一括取込に対応。
 * </p>
 *
 * <p>【技術的負債】
 * <ul>
 *   <li>メガswitch/case文によるインポート種別判定。</li>
 *   <li>ハードコードされたファイルパス（ローカルファイルシステム前提）。</li>
 *   <li>手動CSV解析（CsvParser使用、Apache Commons CSV移行すべき）。</li>
 *   <li>大量データのインポートでOOMのリスク（全件メモリ上に展開）。</li>
 *   <li>StringBufferによるCSV出力構築。</li>
 *   <li>エラーハンドリングが不十分（一部のエラーを握りつぶし）。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.1.0
 */
@Stateless
public class ImportExportServiceBean {

    private static final Logger logger = Logger.getLogger(ImportExportServiceBean.class.getName());

    // 技術的負債: ハードコードされたファイルパス
    private static final String IMPORT_BASE_DIR = "/var/proquip/import/";
    private static final String EXPORT_BASE_DIR = "/var/proquip/export/";

    @PersistenceContext
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private ProductServiceBean productService;

    @EJB
    private SupplierServiceBean supplierService;

    // ========================================================================
    // インポート処理
    // ========================================================================

    /**
     * CSVデータをインポートする。
     *
     * <p>【技術的負債】メガswitch/case文でエンティティ種別を判定。
     * Strategy パターンまたはポリモーフィズムで分離すべき。</p>
     *
     * @param entityType インポート対象種別（"PRODUCT", "SUPPLIER", "INVENTORY"）
     * @param csvData CSV文字列データ
     * @param startedBy 実行者のユーザー名
     * @return インポートジョブエンティティ
     */
    public ImportJob importCsvData(String entityType, String csvData, String startedBy) {
        if (entityType == null || entityType.isEmpty()) {
            throw new ValidationException("entityType", "インポート対象種別は必須です。");
        }
        if (csvData == null || csvData.isEmpty()) {
            throw new ValidationException("csvData", "CSVデータは必須です。");
        }

        // インポートジョブの作成
        ImportJob job = new ImportJob();
        job.setJobName(entityType + "インポート");
        job.setEntityType(entityType);
        job.setFileName("inline-import-" + System.currentTimeMillis() + ".csv");
        // 技術的負債: ハードコードされたファイルパス
        job.setFilePath(IMPORT_BASE_DIR + job.getFileName());
        job.setFileSize((long) csvData.getBytes(StandardCharsets.UTF_8).length);
        job.setStatus("PROCESSING");
        job.setStartedAt(new Date());
        job.setStartedBy(startedBy != null ? startedBy : AppConstants.SYSTEM_USER);
        job.setCreatedBy(startedBy != null ? startedBy : AppConstants.SYSTEM_USER);
        job.setUpdatedBy(startedBy != null ? startedBy : AppConstants.SYSTEM_USER);

        em.persist(job);

        try {
            // CSV解析
            InputStream inputStream = new ByteArrayInputStream(
                    csvData.getBytes(StandardCharsets.UTF_8));
            List<String[]> rows = CsvParser.parse(inputStream);

            if (rows.isEmpty()) {
                job.setStatus("COMPLETED");
                job.setTotalRecords(0);
                job.setProcessedRecords(0);
                job.setErrorRecords(0);
                job.setCompletedAt(new Date());
                em.merge(job);
                return job;
            }

            // 技術的負債: 最大行数チェック
            if (rows.size() > AppConstants.MAX_CSV_IMPORT_ROWS + 1) { // +1 for header
                job.setStatus("FAILED");
                job.setErrorDetails("CSVの行数が上限(" + AppConstants.MAX_CSV_IMPORT_ROWS
                        + "行)を超えています。行数: " + (rows.size() - 1));
                job.setCompletedAt(new Date());
                em.merge(job);
                return job;
            }

            // ヘッダ行をスキップ
            String[] headers = rows.get(0);
            int dataRowCount = rows.size() - 1;
            job.setTotalRecords(dataRowCount);

            // 技術的負債: メガswitch/case文によるインポート種別判定
            int successCount = 0;
            int errorCount = 0;
            StringBuffer errorDetails = new StringBuffer();

            switch (entityType) {
                case "PRODUCT":
                    for (int i = 1; i < rows.size(); i++) {
                        try {
                            importProductRow(rows.get(i), headers, i);
                            successCount++;
                        } catch (ImportException e) {
                            errorCount++;
                            errorDetails.append(e.getMessage()).append("\n");
                        } catch (Exception e) {
                            errorCount++;
                            errorDetails.append("行").append(i).append(": ")
                                    .append(e.getMessage()).append("\n");
                        }

                        // 進捗更新（100件ごと）
                        if (i % 100 == 0) {
                            job.setProcessedRecords(successCount + errorCount);
                            em.merge(job);
                        }
                    }
                    break;

                case "SUPPLIER":
                    for (int i = 1; i < rows.size(); i++) {
                        try {
                            importSupplierRow(rows.get(i), headers, i);
                            successCount++;
                        } catch (ImportException e) {
                            errorCount++;
                            errorDetails.append(e.getMessage()).append("\n");
                        } catch (Exception e) {
                            errorCount++;
                            errorDetails.append("行").append(i).append(": ")
                                    .append(e.getMessage()).append("\n");
                        }

                        if (i % 100 == 0) {
                            job.setProcessedRecords(successCount + errorCount);
                            em.merge(job);
                        }
                    }
                    break;

                case "INVENTORY":
                    // 技術的負債: 在庫インポートは未実装（TODO）
                    job.setStatus("FAILED");
                    job.setErrorDetails("在庫インポートは未実装です。");
                    job.setCompletedAt(new Date());
                    em.merge(job);
                    logger.warning("在庫インポートは未実装です。");
                    return job;

                default:
                    job.setStatus("FAILED");
                    job.setErrorDetails("未対応のインポート種別: " + entityType);
                    job.setCompletedAt(new Date());
                    em.merge(job);
                    return job;
            }

            // ジョブ結果の更新
            job.setProcessedRecords(successCount);
            job.setErrorRecords(errorCount);
            job.setStatus(errorCount > 0 ? "COMPLETED" : "COMPLETED");
            job.setCompletedAt(new Date());

            if (errorDetails.length() > 0) {
                // 技術的負債: エラー詳細を4000文字で打ち切り（DB制約回避）
                String details = errorDetails.toString();
                if (details.length() > 4000) {
                    details = details.substring(0, 4000) + "\n... (truncated)";
                }
                job.setErrorDetails(details);
            }

            em.merge(job);

            auditService.logAction("ImportJob", job.getId(), "CREATE",
                    startedBy != null ? startedBy : AppConstants.SYSTEM_USER,
                    null, "インポート完了。成功: " + successCount + ", 失敗: " + errorCount);

            logger.info("インポート完了。種別: " + entityType
                    + ", 成功: " + successCount + ", 失敗: " + errorCount);

        } catch (IOException e) {
            job.setStatus("FAILED");
            job.setErrorDetails("CSV解析エラー: " + e.getMessage());
            job.setCompletedAt(new Date());
            em.merge(job);

            logger.log(Level.SEVERE, "CSV解析エラー", e);
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorDetails("予期せぬエラー: " + e.getMessage());
            job.setCompletedAt(new Date());
            em.merge(job);

            logger.log(Level.SEVERE, "インポート中に予期せぬエラーが発生しました。", e);
        }

        return job;
    }

    // ========================================================================
    // エクスポート処理
    // ========================================================================

    /**
     * 商品マスタをCSV形式でエクスポートする。
     *
     * <p>【技術的負債 #11】StringBufferでCSVを手動構築。
     * ProductServiceBean.exportProductsToCsv()と処理が重複している。</p>
     *
     * @return CSV文字列
     */
    @SuppressWarnings("unchecked")
    public String exportProducts() {
        List<Product> products = em.createNamedQuery("Product.findByStatus")
                .setParameter("status", "ACTIVE")
                .getResultList();

        // 技術的負債 #11: StringBufferによるCSV構築
        StringBuffer csv = new StringBuffer();

        // ヘッダ
        csv.append("SKU,商品名,説明,単価,ステータス,最小発注数量,有効フラグ");
        csv.append("\r\n");

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);

            csv.append(escapeCsvField(p.getSku()));
            csv.append(",");
            csv.append(escapeCsvField(p.getName()));
            csv.append(",");
            csv.append(escapeCsvField(p.getDescription()));
            csv.append(",");
            csv.append(p.getUnitPrice() != null ? p.getUnitPrice().toPlainString() : "");
            csv.append(",");
            csv.append(p.getStatus() != null ? p.getStatus() : "");
            csv.append(",");
            csv.append(p.getMinOrderQty() != null ? p.getMinOrderQty() : "");
            csv.append(",");
            csv.append("ACTIVE".equals(p.getStatus()));
            csv.append("\r\n");
        }

        return csv.toString();
    }

    /**
     * 仕入先マスタをCSV形式でエクスポートする。
     *
     * <p>【技術的負債 #11】SupplierServiceBean.exportSuppliersToCsv()と処理が重複。</p>
     *
     * @return CSV文字列
     */
    @SuppressWarnings("unchecked")
    public String exportSuppliers() {
        List<Supplier> suppliers = em.createQuery(
                "SELECT s FROM Supplier s WHERE s.status = 'ACTIVE' ORDER BY s.name")
                .getResultList();

        StringBuffer csv = new StringBuffer();

        csv.append("コード,名称,税務登録番号,ステータス,評価,支払条件（日）");
        csv.append("\r\n");

        for (int i = 0; i < suppliers.size(); i++) {
            Supplier s = suppliers.get(i);
            csv.append(escapeCsvField(s.getCode()));
            csv.append(",");
            csv.append(escapeCsvField(s.getName()));
            csv.append(",");
            csv.append(escapeCsvField(s.getTaxId()));
            csv.append(",");
            csv.append(s.getStatus() != null ? s.getStatus() : "");
            csv.append(",");
            // rating and paymentTermDays fields were removed from Supplier (DDL alignment)
            csv.append("");
            csv.append(",");
            csv.append("");
            csv.append("\r\n");
        }

        return csv.toString();
    }

    // ========================================================================
    // インポートジョブ管理
    // ========================================================================

    /**
     * インポートジョブの状態を取得する。
     *
     * @param jobId ジョブID
     * @return インポートジョブエンティティ
     */
    public ImportJob getImportJobStatus(Long jobId) {
        if (jobId == null) {
            throw new ValidationException("jobId", "ジョブIDは必須です。");
        }
        ImportJob job = em.find(ImportJob.class, jobId);
        if (job == null) {
            throw new com.proquip.common.exception.EntityNotFoundException("ImportJob", jobId);
        }
        return job;
    }

    /**
     * 直近のインポートジョブを取得する。
     *
     * @param limit 取得件数
     * @return インポートジョブのリスト
     */
    @SuppressWarnings("unchecked")
    public List<ImportJob> getRecentImportJobs(int limit) {
        if (limit <= 0) {
            limit = 10;
        }

        return em.createNamedQuery("ImportJob.findRecentJobs")
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * インポートジョブをキャンセルする。
     *
     * @param jobId ジョブID
     */
    public void cancelImportJob(Long jobId) {
        ImportJob job = getImportJobStatus(jobId);

        if (!"PENDING".equals(job.getStatus()) && !"PROCESSING".equals(job.getStatus())) {
            throw new ValidationException("status",
                    "保留中または処理中のジョブのみキャンセルできます。現在のステータス: " + job.getStatus());
        }

        job.setStatus("CANCELLED");
        job.setCompletedAt(new Date());
        em.merge(job);

        logger.info("インポートジョブをキャンセルしました。ジョブID: " + jobId);
    }

    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================

    /**
     * 商品データの1行をインポートする。
     *
     * <p>【技術的負債】カラム位置がハードコードされている。
     * ヘッダー名によるマッピングを行うべき。</p>
     *
     * @param row CSVの1行データ
     * @param headers ヘッダー行
     * @param rowNumber 行番号（エラー報告用）
     */
    private void importProductRow(String[] row, String[] headers, int rowNumber) {
        // 技術的負債: カラム位置のハードコード
        if (row.length < 3) {
            throw new ImportException(rowNumber, "ALL",
                    "カラム数が不足しています。必要: 3以上, 実際: " + row.length);
        }

        String sku = row[0] != null ? row[0].trim() : "";
        String name = row[1] != null ? row[1].trim() : "";
        String unitPriceStr = row.length > 2 ? (row[2] != null ? row[2].trim() : "") : "";

        if (sku.isEmpty()) {
            throw new ImportException(rowNumber, "SKU", "SKUは必須です。");
        }
        if (name.isEmpty()) {
            throw new ImportException(rowNumber, "name", "商品名は必須です。");
        }

        // 既存チェック
        Product existing = productService.findBySku(sku);
        if (existing != null) {
            throw new ImportException(rowNumber, "SKU",
                    "SKU「" + sku + "」は既に存在します。");
        }

        Product product = new Product();
        product.setSku(sku);
        product.setName(name);

        if (!unitPriceStr.isEmpty()) {
            try {
                product.setUnitPrice(new BigDecimal(unitPriceStr));
            } catch (NumberFormatException e) {
                throw new ImportException(rowNumber, "unitPrice",
                        "単価の形式が不正です: " + unitPriceStr);
            }
        }

        // ステータスの設定（4番目のカラムがあれば使用）
        if (row.length > 3 && row[3] != null && !row[3].trim().isEmpty()) {
            product.setStatus(row[3].trim());
        } else {
            product.setStatus("PENDING");
        }

        // active field was removed from Product; status is used instead (DDL alignment)
        // product.setStatus already set above
        product.setCreatedBy(AppConstants.SYSTEM_USER);
        product.setUpdatedBy(AppConstants.SYSTEM_USER);

        em.persist(product);
    }

    /**
     * 仕入先データの1行をインポートする。
     *
     * <p>【技術的負債】商品インポートと同様、カラム位置がハードコード。</p>
     *
     * @param row CSVの1行データ
     * @param headers ヘッダー行
     * @param rowNumber 行番号
     */
    private void importSupplierRow(String[] row, String[] headers, int rowNumber) {
        if (row.length < 2) {
            throw new ImportException(rowNumber, "ALL",
                    "カラム数が不足しています。必要: 2以上, 実際: " + row.length);
        }

        String code = row[0] != null ? row[0].trim() : "";
        String name = row[1] != null ? row[1].trim() : "";

        if (code.isEmpty()) {
            throw new ImportException(rowNumber, "code", "仕入先コードは必須です。");
        }
        if (name.isEmpty()) {
            throw new ImportException(rowNumber, "name", "仕入先名は必須です。");
        }

        // 既存チェック
        Supplier existing = supplierService.findByCode(code);
        if (existing != null) {
            throw new ImportException(rowNumber, "code",
                    "仕入先コード「" + code + "」は既に存在します。");
        }

        Supplier supplier = new Supplier();
        supplier.setCode(code);
        supplier.setName(name);

        // 税務登録番号（3番目のカラム）
        if (row.length > 2 && row[2] != null && !row[2].trim().isEmpty()) {
            supplier.setTaxId(row[2].trim());
        }

        // 支払条件日数（4番目のカラム）
        if (row.length > 3 && row[3] != null && !row[3].trim().isEmpty()) {
            try {
                // paymentTermDays field was removed from Supplier entity (DDL alignment)
                // Skipping paymentTermDays import: Integer.parseInt(row[3].trim());
            } catch (NumberFormatException e) {
                throw new ImportException(rowNumber, "paymentTermDays",
                        "支払条件日数の形式が不正です: " + row[3].trim());
            }
        }

        supplier.setStatus("PENDING_APPROVAL");
        supplier.setCreatedBy(AppConstants.SYSTEM_USER);
        supplier.setUpdatedBy(AppConstants.SYSTEM_USER);

        em.persist(supplier);
    }

    /**
     * CSVフィールドのエスケープ処理。
     *
     * <p>【技術的負債】CsvParser.escape()と同じ処理を重複実装。
     * AuditServiceBeanにも同じ実装がある（3重の重複）。</p>
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
