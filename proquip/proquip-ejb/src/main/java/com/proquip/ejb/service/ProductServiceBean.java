package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.product.Category;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.product.ProductBundle;
import com.proquip.ejb.entity.product.ProductBundleItem;
import com.proquip.ejb.entity.product.ProductChangeLog;
import com.proquip.ejb.entity.product.ProductSpecification;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 商品管理サービスBean。
 * <p>
 * 商品マスタのCRUD操作、検索、カタログ管理、バンドル価格計算を提供する。
 * </p>
 *
 * <p>【技術的負債】
 * <ul>
 *   <li>N+1パターン：カタログ取得時に商品ごとに仕様・画像を個別ロード。</li>
 *   <li>StringBufferによるCSV構築。</li>
 *   <li>for-indexループの多用。</li>
 *   <li>マジックストリングによるステータス判定。</li>
 *   <li>商品インポート処理がこのクラスにも存在（ImportExportServiceBeanと重複）。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Stateless
public class ProductServiceBean {

    private static final Logger logger = Logger.getLogger(ProductServiceBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    @EJB
    private AuditServiceBean auditService;

    @EJB
    private NotificationServiceBean notificationService;

    // ========================================================================
    // CRUD操作
    // ========================================================================

    /**
     * 商品を新規作成する。
     *
     * @param product 商品エンティティ
     * @return 永続化された商品
     * @throws ValidationException バリデーションエラー
     */
    public Product createProduct(Product product) {
        if (product == null) {
            throw new ValidationException("product", "商品情報は必須です。");
        }

        // バリデーション
        validateProduct(product);

        // SKU重複チェック
        if (findBySku(product.getSku()) != null) {
            throw new ValidationException("sku",
                    "SKU「" + product.getSku() + "」は既に使用されています。");
        }

        if (product.getStatus() == null || product.getStatus().isEmpty()) {
            product.setStatus("DRAFT");
        }
        product.setCreatedBy(AppConstants.SYSTEM_USER);
        product.setUpdatedBy(AppConstants.SYSTEM_USER);

        em.persist(product);

        // 監査ログ
        auditService.logAction("Product", product.getId(), "CREATE",
                AppConstants.SYSTEM_USER, null, "商品作成: " + product.getName());

        logger.info("商品を作成しました。SKU: " + product.getSku());
        return product;
    }

    /**
     * 商品を更新する。
     *
     * @param product 更新する商品エンティティ
     * @return 更新後の商品
     */
    public Product updateProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new ValidationException("product", "商品情報とIDは必須です。");
        }

        Product existing = em.find(Product.class, product.getId());
        if (existing == null) {
            throw new EntityNotFoundException("Product", product.getId());
        }

        product.setUpdatedBy(AppConstants.SYSTEM_USER);
        Product merged = em.merge(product);

        // 監査ログ
        auditService.logAction("Product", merged.getId(), "UPDATE",
                AppConstants.SYSTEM_USER, null, "商品更新: " + merged.getName());

        return merged;
    }

    /**
     * IDで商品を取得する。
     *
     * @param productId 商品ID
     * @return 商品エンティティ
     * @throws EntityNotFoundException 商品が見つからない場合
     */
    public Product findById(Long productId) {
        if (productId == null) {
            throw new ValidationException("productId", "商品IDは必須です。");
        }

        List<Product> results = em.createQuery(
                "SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.manufacturer LEFT JOIN FETCH p.unit LEFT JOIN FETCH p.specifications WHERE p.id = :id",
                Product.class)
                .setParameter("id", productId)
                .getResultList();
        if (results.isEmpty()) {
            throw new EntityNotFoundException("Product", productId);
        }
        return results.get(0);
    }

    /**
     * SKUで商品を検索する。
     *
     * @param sku SKU
     * @return 商品エンティティ（見つからない場合はnull）
     */
    @SuppressWarnings("unchecked")
    public Product findBySku(String sku) {
        if (sku == null || sku.isEmpty()) {
            return null;
        }

        List<Product> results = em.createNamedQuery("Product.findBySku")
                .setParameter("sku", sku)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 有効な商品を全件取得する。
     *
     * <p>【技術的負債】ページネーション未対応。</p>
     *
     * @return 有効な商品のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Product> findActiveProducts() {
        return em.createNamedQuery("Product.findActiveProducts")
                .getResultList();
    }

    /**
     * ステータスで商品を検索する。
     *
     * @param status ステータス
     * @return 商品のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Product> findByStatus(String status) {
        if (status == null || status.isEmpty()) {
            return new ArrayList<Product>();
        }

        return em.createNamedQuery("Product.findByStatus")
                .setParameter("status", status)
                .getResultList();
    }

    // ========================================================================
    // 検索
    // ========================================================================

    /**
     * キーワードで商品を検索する。
     *
     * <p>商品名および説明文の部分一致検索を行う。</p>
     *
     * <p>【技術的負債】LIKE '%keyword%' による部分一致検索。
     * インデックスが効かず、大量データでパフォーマンスが劣化する。
     * 全文検索エンジン（Elasticsearch等）への移行を検討すべき。</p>
     *
     * @param keyword 検索キーワード
     * @return 検索結果の商品リスト
     */
    @SuppressWarnings("unchecked")
    public List<Product> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<Product>();
        }

        return em.createNamedQuery("Product.searchByName")
                .setParameter("keyword", keyword.trim())
                .getResultList();
    }

    /**
     * カテゴリで商品を検索する。
     *
     * @param categoryId カテゴリID
     * @return 商品のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Product> findByCategory(Long categoryId) {
        if (categoryId == null) {
            return new ArrayList<Product>();
        }

        return em.createNamedQuery("Product.findByCategory")
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    /**
     * 価格帯で商品を検索する。
     *
     * @param minPrice 最低価格
     * @param maxPrice 最高価格
     * @return 商品のリスト
     */
    @SuppressWarnings("unchecked")
    public List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null) {
            minPrice = BigDecimal.ZERO;
        }
        if (maxPrice == null) {
            maxPrice = new BigDecimal("999999999.99");
        }

        return em.createNamedQuery("Product.findByPriceRange")
                .setParameter("minPrice", minPrice)
                .setParameter("maxPrice", maxPrice)
                .getResultList();
    }

    /**
     * 複合条件で商品を検索する。
     *
     * <p>【技術的負債 #11】StringBufferによるJPQL構築。
     * Criteria APIに移行すべき。</p>
     *
     * @param keyword キーワード（nullの場合は条件に含めない）
     * @param categoryId カテゴリID（nullの場合は条件に含めない）
     * @param status ステータス（nullの場合は条件に含めない）
     * @param minPrice 最低価格（nullの場合は条件に含めない）
     * @param maxPrice 最高価格（nullの場合は条件に含めない）
     * @return 検索結果の商品リスト
     */
    @SuppressWarnings("unchecked")
    public List<Product> searchProducts(String keyword, Long categoryId,
                                        String status, BigDecimal minPrice,
                                        BigDecimal maxPrice) {
        // 技術的負債 #11: StringBufferによるJPQL構築
        StringBuffer jpql = new StringBuffer();
        jpql.append("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.manufacturer LEFT JOIN FETCH p.unit WHERE 1=1");

        if (keyword != null && !keyword.trim().isEmpty()) {
            jpql.append(" AND (p.name LIKE :keyword OR p.description LIKE :keyword OR p.sku LIKE :keyword)");
        }
        if (categoryId != null) {
            jpql.append(" AND p.category.id = :categoryId");
        }
        if (status != null && !status.isEmpty()) {
            jpql.append(" AND p.status = :status");
        }
        if (minPrice != null) {
            jpql.append(" AND p.unitPrice >= :minPrice");
        }
        if (maxPrice != null) {
            jpql.append(" AND p.unitPrice <= :maxPrice");
        }

        jpql.append(" ORDER BY p.name");

        Query query = em.createQuery(jpql.toString());

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.setParameter("keyword", "%" + keyword.trim() + "%");
        }
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (status != null && !status.isEmpty()) {
            query.setParameter("status", status);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }

        return query.getResultList();
    }

    // ========================================================================
    // カタログ管理
    // ========================================================================

    /**
     * 商品カタログ情報を取得する（仕様・画像込み）。
     *
     * <p>【技術的負債 #3】N+1パターン。商品ごとに仕様と画像を個別ロード。
     * JOIN FETCHで一括取得すべき。</p>
     *
     * @param productId 商品ID
     * @return カタログ情報のMap
     */
    public Map<String, Object> getProductCatalog(Long productId) {
        Map<String, Object> catalog = new HashMap<String, Object>();

        Product product = findById(productId);
        catalog.put("product", product);

        // 技術的負債 #3: N+1パターン — 仕様を個別ロード
        List<ProductSpecification> specs = product.getSpecifications();
        // 遅延ロードをトリガー
        if (specs != null) {
            catalog.put("specificationCount", specs.size());

            // 技術的負債 #6: for-indexループ
            List<Map<String, String>> specList = new ArrayList<Map<String, String>>();
            for (int i = 0; i < specs.size(); i++) {
                Map<String, String> specMap = new HashMap<String, String>();
                specMap.put("name", specs.get(i).getSpecName());
                specMap.put("value", specs.get(i).getSpecValue());
                specList.add(specMap);
            }
            catalog.put("specifications", specList);
        }

        // カテゴリ情報
        if (product.getCategory() != null) {
            catalog.put("categoryName", product.getCategory().getName());
            catalog.put("categoryCode", product.getCategory().getCode());
        }

        // 製造元情報
        if (product.getManufacturer() != null) {
            catalog.put("manufacturerName", product.getManufacturer().getName());
        }

        return catalog;
    }

    /**
     * 商品カタログを一括取得する。
     *
     * <p>【技術的負債 #3】N+1パターン。各商品ごとにgetProductCatalogを呼び出し。</p>
     *
     * @param categoryId カテゴリID（nullの場合は全カテゴリ）
     * @return カタログ情報のリスト
     */
    public List<Map<String, Object>> getCatalogByCategory(Long categoryId) {
        List<Product> products;
        if (categoryId != null) {
            products = findByCategory(categoryId);
        } else {
            products = findActiveProducts();
        }

        List<Map<String, Object>> catalogs = new ArrayList<Map<String, Object>>();

        // 技術的負債 #3: N+1パターン — 個別にカタログ情報を取得
        for (int i = 0; i < products.size(); i++) {
            try {
                Map<String, Object> catalog = getProductCatalog(products.get(i).getId());
                catalogs.add(catalog);
            } catch (Exception e) {
                // 技術的負債 #7: 個別エラーを握りつぶし
                logger.warning("カタログ情報取得エラー。商品ID: " + products.get(i).getId());
            }
        }

        return catalogs;
    }

    // ========================================================================
    // ステータス管理
    // ========================================================================

    /**
     * 商品ステータスを変更する。
     *
     * <p>【技術的負債 #14】マジックストリングによるステータス遷移チェック。
     * Enum型とステートマシンパターンに移行すべき。</p>
     *
     * @param productId 商品ID
     * @param newStatus 新しいステータス
     */
    public void changeStatus(Long productId, String newStatus) {
        Product product = findById(productId);
        String oldStatus = product.getStatus();

        // 技術的負債 #14: マジックストリングによる遷移チェック
        if ("DISCONTINUED".equals(oldStatus)) {
            throw new ValidationException("status",
                    "廃止された商品のステータスは変更できません。");
        }

        if ("ACTIVE".equals(newStatus) && !"PENDING".equals(oldStatus)
                && !"INACTIVE".equals(oldStatus)) {
            throw new ValidationException("status",
                    "現在のステータス「" + oldStatus + "」からACTIVEへの変更はできません。");
        }

        product.setStatus(newStatus);
        product.setUpdatedBy(AppConstants.SYSTEM_USER);

        // active field was removed from Product; status string is the sole indicator (DDL alignment)

        em.merge(product);

        // 変更ログの記録
        try {
            ProductChangeLog changeLog = new ProductChangeLog();
            changeLog.setProduct(product);
            changeLog.setChangeType("STATUS_CHANGE");
            changeLog.setFieldName("status");
            changeLog.setOldValue(oldStatus);
            changeLog.setNewValue(newStatus);
            // changedAt field was removed; BaseEntity.createdAt is used instead (DDL alignment)
            // changedBy is Long, not String; set to null for system actions
            changeLog.setChangedBy(null);
            em.persist(changeLog);
        } catch (Exception e) {
            // 技術的負債 #7: 変更ログ失敗を握りつぶし
            logger.warning("商品変更ログの記録に失敗しました。商品ID: " + productId);
        }

        auditService.logAction("Product", productId, "UPDATE",
                AppConstants.SYSTEM_USER, "status=" + oldStatus, "status=" + newStatus);

        logger.info("商品ステータスを変更しました。商品ID: " + productId
                + ", " + oldStatus + " → " + newStatus);
    }

    // ========================================================================
    // バンドル価格計算
    // ========================================================================

    /**
     * バンドル（セット商品）の価格を計算する。
     *
     * <p>【技術的負債 #3】N+1パターン。バンドル構成品目ごとに個別に
     * 商品エンティティを参照している。</p>
     *
     * @param bundleId バンドルID
     * @return バンドル価格情報のMap（totalPrice, discountedPrice, discountAmountを含む）
     */
    public Map<String, BigDecimal> calculateBundlePrice(Long bundleId) {
        Map<String, BigDecimal> priceInfo = new HashMap<String, BigDecimal>();

        if (bundleId == null) {
            priceInfo.put("totalPrice", BigDecimal.ZERO);
            priceInfo.put("discountedPrice", BigDecimal.ZERO);
            priceInfo.put("discountAmount", BigDecimal.ZERO);
            return priceInfo;
        }

        ProductBundle bundle = em.find(ProductBundle.class, bundleId);
        if (bundle == null) {
            throw new EntityNotFoundException("ProductBundle", bundleId);
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        // 技術的負債 #3: N+1パターン — バンドルアイテムごとに商品を参照
        List<ProductBundleItem> items = bundle.getBundleItems();
        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < items.size(); i++) {
            ProductBundleItem item = items.get(i);
            Product product = item.getProduct();

            if (product != null && product.getUnitPrice() != null) {
                BigDecimal qty = new BigDecimal(item.getQuantity());
                BigDecimal itemPrice = product.getUnitPrice().multiply(qty);
                totalPrice = totalPrice.add(itemPrice);
            }
        }

        priceInfo.put("totalPrice", totalPrice.setScale(2, RoundingMode.HALF_UP));

        // 割引適用
        BigDecimal discount = bundle.getDiscount();
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountRate = discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal discountAmount = totalPrice.multiply(discountRate)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountedPrice = totalPrice.subtract(discountAmount);

            priceInfo.put("discountAmount", discountAmount);
            priceInfo.put("discountedPrice", discountedPrice.setScale(2, RoundingMode.HALF_UP));
        } else {
            priceInfo.put("discountAmount", BigDecimal.ZERO);
            priceInfo.put("discountedPrice", totalPrice.setScale(2, RoundingMode.HALF_UP));
        }

        return priceInfo;
    }

    // ========================================================================
    // インポート/エクスポート
    // ========================================================================

    /**
     * 商品リストをCSV形式で出力する。
     *
     * <p>【技術的負債 #11】StringBufferでCSVを手動構築。
     * ImportExportServiceBeanのCSVエクスポート処理と重複。</p>
     *
     * @param products 出力対象の商品リスト
     * @return CSV文字列
     */
    public String exportProductsToCsv(List<Product> products) {
        // 技術的負債 #11: StringBufferによるCSV構築
        StringBuffer csv = new StringBuffer();

        // ヘッダ行
        csv.append("ID,SKU,商品名,ステータス,単価,最小発注数量,カテゴリ,有効");
        csv.append("\r\n");

        if (products == null || products.isEmpty()) {
            return csv.toString();
        }

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            csv.append(p.getId() != null ? p.getId() : "");
            csv.append(",");
            csv.append(p.getSku() != null ? p.getSku() : "");
            csv.append(",");
            // 技術的負債: CSVエスケープ処理がない（名前にカンマ含む場合に壊れる）
            csv.append(p.getName() != null ? p.getName() : "");
            csv.append(",");
            csv.append(p.getStatus() != null ? p.getStatus() : "");
            csv.append(",");
            csv.append(p.getUnitPrice() != null ? p.getUnitPrice() : "");
            csv.append(",");
            csv.append(p.getMinOrderQty() != null ? p.getMinOrderQty() : "");
            csv.append(",");
            // 技術的負債 #3: カテゴリ名取得でN+1クエリ発生
            try {
                csv.append(p.getCategory() != null ? p.getCategory().getName() : "");
            } catch (Exception e) {
                csv.append("");
            }
            csv.append(",");
            csv.append("ACTIVE".equals(p.getStatus()));
            csv.append("\r\n");
        }

        return csv.toString();
    }

    /**
     * 簡易的な商品一括インポートを行う。
     *
     * <p>【技術的負債】ImportExportServiceBeanの商品インポートと処理が重複。
     * このメソッドは簡易版として残されているが、本来は一本化すべき。</p>
     *
     * @param productDataList 商品データのリスト（各要素はMap: sku, name, unitPrice, categoryCode）
     * @return インポート結果のMap（successCount, failCount, errorsを含む）
     */
    public Map<String, Object> importProducts(List<Map<String, String>> productDataList) {
        Map<String, Object> result = new HashMap<String, Object>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<String>();

        if (productDataList == null || productDataList.isEmpty()) {
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("errors", errors);
            return result;
        }

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < productDataList.size(); i++) {
            Map<String, String> data = productDataList.get(i);
            int rowNumber = i + 1;

            try {
                String sku = data.get("sku");
                String name = data.get("name");
                String unitPriceStr = data.get("unitPrice");
                String categoryCode = data.get("categoryCode");

                if (sku == null || sku.isEmpty()) {
                    errors.add("行" + rowNumber + ": SKUは必須です。");
                    failCount++;
                    continue;
                }
                if (name == null || name.isEmpty()) {
                    errors.add("行" + rowNumber + ": 商品名は必須です。");
                    failCount++;
                    continue;
                }

                // 既存チェック
                Product existing = findBySku(sku);
                if (existing != null) {
                    errors.add("行" + rowNumber + ": SKU「" + sku + "」は既に存在します。");
                    failCount++;
                    continue;
                }

                Product product = new Product();
                product.setSku(sku);
                product.setName(name);

                if (unitPriceStr != null && !unitPriceStr.isEmpty()) {
                    try {
                        product.setUnitPrice(new BigDecimal(unitPriceStr));
                    } catch (NumberFormatException e) {
                        errors.add("行" + rowNumber + ": 単価の形式が不正です。");
                        failCount++;
                        continue;
                    }
                }

                // カテゴリの設定
                if (categoryCode != null && !categoryCode.isEmpty()) {
                    Category category = findCategoryByCode(categoryCode);
                    if (category != null) {
                        product.setCategory(category);
                    } else {
                        errors.add("行" + rowNumber + ": カテゴリコード「" + categoryCode + "」が見つかりません。");
                        // カテゴリなしで続行（エラーにはしない）
                    }
                }

                product.setStatus("DRAFT");
                product.setCreatedBy(AppConstants.SYSTEM_USER);
                product.setUpdatedBy(AppConstants.SYSTEM_USER);

                em.persist(product);
                successCount++;

            } catch (Exception e) {
                // 技術的負債 #7: 個別エラーを握りつぶし
                errors.add("行" + rowNumber + ": " + e.getMessage());
                failCount++;
            }
        }

        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);

        logger.info("商品インポート完了。成功: " + successCount + ", 失敗: " + failCount);
        return result;
    }

    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================

    /**
     * 商品バリデーション。
     */
    private void validateProduct(Product product) {
        if (product.getSku() == null || product.getSku().isEmpty()) {
            throw new ValidationException("sku", "SKUは必須です。");
        }
        if (product.getName() == null || product.getName().isEmpty()) {
            throw new ValidationException("name", "商品名は必須です。");
        }
        if (product.getUnitPrice() != null
                && product.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("unitPrice", "単価は0以上を指定してください。");
        }
    }

    /**
     * カテゴリコードでカテゴリを検索する。
     */
    @SuppressWarnings("unchecked")
    private Category findCategoryByCode(String code) {
        List<Category> results = em.createNamedQuery("Category.findByCode")
                .setParameter("code", code)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
