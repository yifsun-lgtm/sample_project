package com.proquip.ejb.service;

import com.proquip.common.constant.AppConstants;
import com.proquip.common.exception.BusinessException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.InsufficientStockException;
import com.proquip.common.exception.ValidationException;
import com.proquip.common.util.DateUtils;
import com.proquip.ejb.entity.inventory.InventoryCount;
import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.inventory.InventoryTransaction;
import com.proquip.ejb.entity.inventory.StorageLocation;
import com.proquip.ejb.entity.inventory.Warehouse;
import com.proquip.ejb.entity.inventory.WarehouseZone;
import com.proquip.ejb.entity.product.Product;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 在庫管理サービスBean。
 * <p>
 * 在庫の入出庫、移動、棚卸、在庫レベル監視などの業務ロジックを提供する。
 * </p>
 *
 * <p>【技術的負債 #20 - 循環依存】PurchaseOrderServiceBeanとの相互参照が存在する。
 * 入庫処理時にPOServiceから呼ばれ、逆に在庫不足時にPOServiceの自動発注を
 * トリガーする設計になっている（実装は不完全）。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
@Stateless
public class InventoryServiceBean {

    private static final Logger logger = Logger.getLogger(InventoryServiceBean.class.getName());

    // 技術的負債 #20: 循環依存
    @EJB
    private PurchaseOrderServiceBean purchaseOrderService;

    @EJB
    private NotificationServiceBean notificationService;

    @EJB
    private AuditServiceBean auditService;

    // 技術的負債: DAOがあるのにEntityManagerも直接使用
    @PersistenceContext
    private EntityManager em;

    // ========================================================================
    // 入出庫処理
    // ========================================================================

    /**
     * 在庫を追加する（入庫処理）。
     *
     * <p>指定された商品・倉庫の在庫数量を増加させ、在庫トランザクションを記録する。</p>
     *
     * @param productId 商品ID
     * @param warehouseId 倉庫ID
     * @param quantity 追加数量
     * @param referenceType 参照種別（例: "PURCHASE_ORDER", "ADJUSTMENT"）
     * @param referenceId 参照ID
     * @throws EntityNotFoundException 商品または倉庫が見つからない場合
     */
    public void addStock(Long productId, Long warehouseId, int quantity,
                         String referenceType, Long referenceId) {
        if (productId == null || warehouseId == null) {
            throw new ValidationException("productId/warehouseId", "商品IDと倉庫IDは必須です。");
        }
        if (quantity <= 0) {
            throw new ValidationException("quantity", "追加数量は1以上を指定してください。");
        }

        // 商品の存在チェック
        Product product = em.find(Product.class, productId);
        if (product == null) {
            throw new EntityNotFoundException("Product", productId);
        }

        // 倉庫の存在チェック
        Warehouse warehouse = em.find(Warehouse.class, warehouseId);
        if (warehouse == null) {
            throw new EntityNotFoundException("Warehouse", warehouseId);
        }

        // 在庫品目の取得または作成
        InventoryItem item = findOrCreateInventoryItem(productId, warehouseId);

        // 数量の更新
        int currentQty = item.getQuantityOnHand() != null ? item.getQuantityOnHand() : 0;
        item.setQuantityOnHand(currentQty + quantity);

        // 発注中数量の減算（入庫の場合）
        if ("PURCHASE_ORDER".equals(referenceType)) {
            int onOrder = item.getQuantityOnOrder() != null ? item.getQuantityOnOrder() : 0;
            int newOnOrder = onOrder - quantity;
            if (newOnOrder < 0) {
                newOnOrder = 0;
            }
            item.setQuantityOnOrder(newOnOrder);
        }

        em.merge(item);

        // トランザクション記録
        createInventoryTransaction(item, "IN", quantity,
                referenceType + ":" + referenceId, "入庫処理");

        logger.info("在庫追加完了。商品ID: " + productId + ", 倉庫ID: " + warehouseId
                + ", 数量: " + quantity);
    }

    /**
     * 在庫を減少させる（出庫処理）。
     *
     * @param productId 商品ID
     * @param warehouseId 倉庫ID
     * @param quantity 減少数量
     * @param reason 出庫理由
     * @throws InsufficientStockException 在庫不足の場合
     */
    public void removeStock(Long productId, Long warehouseId, int quantity, String reason) {
        if (productId == null || warehouseId == null) {
            throw new ValidationException("productId/warehouseId", "商品IDと倉庫IDは必須です。");
        }
        if (quantity <= 0) {
            throw new ValidationException("quantity", "出庫数量は1以上を指定してください。");
        }

        InventoryItem item = findInventoryItem(productId, warehouseId);
        if (item == null) {
            throw new InsufficientStockException(productId, quantity, 0);
        }

        int available = item.getQuantityOnHand() - item.getQuantityReserved();
        if (available < quantity) {
            throw new InsufficientStockException(productId, quantity, available);
        }

        item.setQuantityOnHand(item.getQuantityOnHand() - quantity);
        em.merge(item);

        // トランザクション記録
        createInventoryTransaction(item, "OUT", -quantity, null,
                "出庫: " + (reason != null ? reason : ""));

        // 在庫低下チェック
        checkAndNotifyLowStock(item);

        logger.info("在庫減少完了。商品ID: " + productId + ", 数量: " + quantity);
    }

    /**
     * 倉庫間で在庫を移動する。
     *
     * @param fromWarehouseId 移動元倉庫ID
     * @param toWarehouseId 移動先倉庫ID
     * @param productId 商品ID
     * @param quantity 移動数量
     * @throws InsufficientStockException 移動元の在庫不足
     */
    public void transferStock(Long fromWarehouseId, Long toWarehouseId,
                              Long productId, int quantity) {
        if (fromWarehouseId == null || toWarehouseId == null || productId == null) {
            throw new ValidationException("parameter", "移動元倉庫ID、移動先倉庫ID、商品IDは必須です。");
        }
        if (quantity <= 0) {
            throw new ValidationException("quantity", "移動数量は1以上を指定してください。");
        }
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new ValidationException("warehouseId", "移動元と移動先が同一です。");
        }

        // 移動元の在庫チェック
        InventoryItem fromItem = findInventoryItem(productId, fromWarehouseId);
        if (fromItem == null) {
            throw new InsufficientStockException(productId, quantity, 0);
        }

        int available = fromItem.getQuantityOnHand() - fromItem.getQuantityReserved();
        if (available < quantity) {
            throw new InsufficientStockException(productId, quantity, available);
        }

        // 移動元から減算
        fromItem.setQuantityOnHand(fromItem.getQuantityOnHand() - quantity);
        em.merge(fromItem);

        // 移動先に加算
        InventoryItem toItem = findOrCreateInventoryItem(productId, toWarehouseId);
        int toCurrentQty = toItem.getQuantityOnHand() != null ? toItem.getQuantityOnHand() : 0;
        toItem.setQuantityOnHand(toCurrentQty + quantity);
        em.merge(toItem);

        // トランザクション記録
        createInventoryTransaction(fromItem, "TRANSFER", -quantity,
                "TO:" + toWarehouseId, "倉庫間移動（出庫）");
        createInventoryTransaction(toItem, "TRANSFER", quantity,
                "FROM:" + fromWarehouseId, "倉庫間移動（入庫）");

        logger.info("在庫移動完了。商品ID: " + productId + ", 数量: " + quantity
                + ", 移動元: " + fromWarehouseId + " → 移動先: " + toWarehouseId);
    }

    // ========================================================================
    // 在庫レベル照会
    // ========================================================================

    /**
     * 特定の商品・倉庫の在庫レベルを取得する。
     *
     * @param productId 商品ID
     * @param warehouseId 倉庫ID
     * @return 在庫品目（存在しない場合はnull）
     */
    public InventoryItem getStockLevel(Long productId, Long warehouseId) {
        return findInventoryItem(productId, warehouseId);
    }

    /**
     * 特定の商品の全倉庫にわたる在庫レベルを取得する。
     *
     * @param productId 商品ID
     * @return 在庫品目のリスト
     */
    @SuppressWarnings("unchecked")
    public List<InventoryItem> getAllStockLevels(Long productId) {
        if (productId == null) {
            return new ArrayList<InventoryItem>();
        }

        return em.createQuery(
                "SELECT ii FROM InventoryItem ii WHERE ii.product.id = :productId")
                .setParameter("productId", productId)
                .getResultList();
    }

    // ========================================================================
    // 在庫低下チェック
    // ========================================================================

    /**
     * 全品目の在庫低下チェックを実行する。
     *
     * <p>再発注点を下回っている品目を検出し、通知を送信する。
     * スケジューラーからの定期実行を想定。</p>
     *
     * <p>【技術的負債】@Schedule アノテーションが未設定。
     * 現在は手動呼び出しのみ。</p>
     */
    @SuppressWarnings("unchecked")
    public void checkLowStock() {
        logger.info("在庫低下チェックを開始します。");

        List<InventoryItem> lowStockItems = em.createNamedQuery("InventoryItem.findBelowReorderPoint")
                .getResultList();

        int count = 0;
        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < lowStockItems.size(); i++) {
            InventoryItem item = lowStockItems.get(i);
            try {
                // 通知送信
                // reorderPoint field was removed from InventoryItem (DDL alignment)
                String title = "在庫低下警告: " + item.getProduct().getName();
                String message = "倉庫「" + item.getWarehouse().getName() + "」の"
                        + "商品「" + item.getProduct().getName() + "」の在庫が低下しています。"
                        + " 現在在庫: " + item.getQuantityOnHand();

                // 技術的負債: 通知先のユーザーIDがハードコード（倉庫担当者に送るべき）
                notificationService.sendNotification(
                        1L, title, message, "WARNING",
                        "InventoryItem", item.getId());
                count++;
            } catch (Exception e) {
                // 技術的負債 #7: 個別通知失敗を握りつぶし
                logger.warning("在庫低下通知の送信に失敗: 品目ID=" + item.getId());
            }
        }

        logger.info("在庫低下チェック完了。検出件数: " + count);
    }

    /**
     * 特定の倉庫で在庫が低下している品目を取得する。
     *
     * @param warehouseId 倉庫ID
     * @return 在庫低下品目のリスト
     */
    @SuppressWarnings("unchecked")
    public List<InventoryItem> getLowStockItems(Long warehouseId) {
        if (warehouseId == null) {
            return new ArrayList<InventoryItem>();
        }

        // 技術的負債: DAOを経由せずEntityManager直接クエリ
        return em.createQuery(
                "SELECT ii FROM InventoryItem ii " +
                "WHERE ii.warehouse.id = :warehouseId " +
                "AND ii.quantityOnHand <= ii.reorderPoint " +
                "ORDER BY ii.product.name")
                .setParameter("warehouseId", warehouseId)
                .getResultList();
    }

    // ========================================================================
    // 棚卸処理
    // ========================================================================

    /**
     * 棚卸を作成する。
     *
     * @param warehouseId 対象倉庫ID
     * @param productIds 対象商品IDのリスト（nullの場合は全商品）
     * @return 作成された棚卸エンティティ
     */
    public InventoryCount createInventoryCount(Long warehouseId, List<Long> productIds) {
        if (warehouseId == null) {
            throw new ValidationException("warehouseId", "倉庫IDは必須です。");
        }

        Warehouse warehouse = em.find(Warehouse.class, warehouseId);
        if (warehouse == null) {
            throw new EntityNotFoundException("Warehouse", warehouseId);
        }

        // 棚卸番号の採番
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String countNumber = "IC-" + dateStr + "-" + String.format("%04d", (int) (Math.random() * 9999));

        InventoryCount count = new InventoryCount();
        count.setCountNumber(countNumber);
        count.setCountDate(new Date());
        count.setStatus("PLANNED");
        count.setType(productIds == null ? "FULL" : "SPOT");
        count.setWarehouseId(warehouseId);

        em.persist(count);

        logger.info("棚卸が作成されました。棚卸番号: " + countNumber);
        return count;
    }

    /**
     * 棚卸結果を処理する。
     *
     * <p>実際の数量と帳簿上の数量を比較し、差異がある場合は在庫を調整する。</p>
     *
     * @param countId 棚卸ID
     * @param actualQuantities 実際の数量マップ（キー: 商品ID、値: 実数量）
     */
    public void processInventoryCount(Long countId, Map<Long, Integer> actualQuantities) {
        if (countId == null) {
            throw new ValidationException("countId", "棚卸IDは必須です。");
        }

        InventoryCount count = em.find(InventoryCount.class, countId);
        if (count == null) {
            throw new EntityNotFoundException("InventoryCount", countId);
        }

        if (!"PLANNED".equals(count.getStatus()) && !"IN_PROGRESS".equals(count.getStatus())) {
            throw new BusinessException("INV_001",
                    "計画中または実施中の棚卸のみ処理できます。現在のステータス: " + count.getStatus());
        }

        count.setStatus("IN_PROGRESS");
        em.merge(count);

        if (actualQuantities == null || actualQuantities.isEmpty()) {
            return;
        }

        Long warehouseId = count.getWarehouseId();

        // 技術的負債 #6: for-indexループでMap.entrySetをイテレート
        List<Map.Entry<Long, Integer>> entries = new ArrayList<Map.Entry<Long, Integer>>(
                actualQuantities.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<Long, Integer> entry = entries.get(i);
            Long productId = entry.getKey();
            int actualQty = entry.getValue();

            try {
                InventoryItem item = findInventoryItem(productId, warehouseId);
                if (item == null) {
                    // 帳簿上に存在しない品目が実地で見つかった場合
                    if (actualQty > 0) {
                        item = findOrCreateInventoryItem(productId, warehouseId);
                        item.setQuantityOnHand(actualQty);
                        // lastCountDate field was removed from InventoryItem (DDL alignment)
                        em.merge(item);
                        createInventoryTransaction(item, "ADJUST", actualQty,
                                "IC:" + count.getCountNumber(), "棚卸差異（新規発見）");
                    }
                    continue;
                }

                int bookQty = item.getQuantityOnHand();
                int difference = actualQty - bookQty;

                if (difference != 0) {
                    // 差異がある場合は調整
                    item.setQuantityOnHand(actualQty);
                    // lastCountDate field was removed from InventoryItem (DDL alignment)
                    em.merge(item);

                    createInventoryTransaction(item, "ADJUST", difference,
                            "IC:" + count.getCountNumber(),
                            "棚卸差異調整。帳簿: " + bookQty + ", 実数: " + actualQty);

                    logger.info("棚卸差異検出。商品ID: " + productId
                            + ", 帳簿: " + bookQty + ", 実数: " + actualQty);
                } else {
                    // 差異なしでも最終棚卸日は更新
                    // lastCountDate field was removed from InventoryItem (DDL alignment)
                    em.merge(item);
                }
            } catch (Exception e) {
                // 技術的負債 #7: 個別品目のエラーを握りつぶし
                logger.log(Level.WARNING,
                        "棚卸処理エラー。商品ID: " + productId, e);
            }
        }

        // 棚卸完了
        count.setStatus("COMPLETED");
        em.merge(count);

        logger.info("棚卸処理が完了しました。棚卸番号: " + count.getCountNumber());
    }

    // ========================================================================
    // トランザクション履歴
    // ========================================================================

    /**
     * 在庫トランザクション履歴を取得する。
     *
     * @param productId 商品ID
     * @param from 開始日
     * @param to 終了日
     * @return トランザクション履歴のリスト
     */
    @SuppressWarnings("unchecked")
    public List<InventoryTransaction> getInventoryTransactionHistory(Long productId,
                                                                     Date from, Date to) {
        if (productId == null) {
            return new ArrayList<InventoryTransaction>();
        }

        // 技術的負債 #11: 文字列連結によるJPQL構築
        StringBuffer jpql = new StringBuffer();
        // inventoryItem ManyToOne was removed; use productId field directly (DDL alignment)
        jpql.append("SELECT it FROM InventoryTransaction it ");
        jpql.append("WHERE it.productId = :productId");

        if (from != null) {
            jpql.append(" AND it.transactionDate >= :fromDate");
        }
        if (to != null) {
            jpql.append(" AND it.transactionDate <= :toDate");
        }
        jpql.append(" ORDER BY it.transactionDate DESC");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("productId", productId);
        if (from != null) {
            query.setParameter("fromDate", from);
        }
        if (to != null) {
            query.setParameter("toDate", to);
        }

        return query.getResultList();
    }

    // ========================================================================
    // 在庫評価・レポート
    // ========================================================================

    /**
     * 倉庫の在庫評価額を計算する。
     *
     * <p>【技術的負債】BigDecimalの丸め処理に一貫性がない。
     * HALF_UPとHALF_DOWNが混在。</p>
     *
     * @param warehouseId 倉庫ID
     * @return 在庫評価額の合計
     */
    @SuppressWarnings("unchecked")
    public BigDecimal calculateInventoryValuation(Long warehouseId) {
        if (warehouseId == null) {
            return BigDecimal.ZERO;
        }

        List<InventoryItem> items = em.createNamedQuery("InventoryItem.findByWarehouse")
                .setParameter("warehouseId", warehouseId)
                .getResultList();

        BigDecimal totalValue = BigDecimal.ZERO;

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            try {
                Product product = item.getProduct();
                if (product != null && product.getUnitPrice() != null) {
                    BigDecimal qty = new BigDecimal(item.getQuantityOnHand());
                    BigDecimal value = product.getUnitPrice().multiply(qty);
                    // 技術的負債: HALF_DOWN使用（他はHALF_UP）
                    totalValue = totalValue.add(value.setScale(2, RoundingMode.HALF_DOWN));
                }
            } catch (Exception e) {
                // 技術的負債 #7: 個別計算エラーを握りつぶし
                logger.warning("在庫評価計算エラー。品目ID: " + item.getId());
            }
        }

        return totalValue.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 倉庫の利用状況を取得する。
     *
     * @param warehouseId 倉庫ID
     * @return 利用状況のMap（totalCapacity, usedCapacity, utilizationRateを含む）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWarehouseUtilization(Long warehouseId) {
        Map<String, Object> result = new HashMap<String, Object>();

        if (warehouseId == null) {
            return result;
        }

        Warehouse warehouse = em.find(Warehouse.class, warehouseId);
        if (warehouse == null) {
            return result;
        }

        int totalCapacity = warehouse.getCapacity() != null ? warehouse.getCapacity() : 0;

        // 在庫品目数のカウント
        Long itemCount = (Long) em.createQuery(
                "SELECT COUNT(ii) FROM InventoryItem ii WHERE ii.warehouse.id = :warehouseId " +
                "AND ii.quantityOnHand > 0")
                .setParameter("warehouseId", warehouseId)
                .getSingleResult();

        // 総在庫数量
        Object totalQtyObj = em.createQuery(
                "SELECT SUM(ii.quantityOnHand) FROM InventoryItem ii " +
                "WHERE ii.warehouse.id = :warehouseId")
                .setParameter("warehouseId", warehouseId)
                .getSingleResult();
        long totalQuantity = totalQtyObj != null ? ((Number) totalQtyObj).longValue() : 0;

        result.put("warehouseName", warehouse.getName());
        result.put("warehouseCode", warehouse.getCode());
        result.put("totalCapacity", totalCapacity);
        result.put("itemCount", itemCount);
        result.put("totalQuantity", totalQuantity);

        if (totalCapacity > 0) {
            double utilizationRate = (double) totalQuantity / totalCapacity * 100;
            result.put("utilizationRate", Math.round(utilizationRate * 100.0) / 100.0);
        } else {
            result.put("utilizationRate", 0.0);
        }

        return result;
    }

    /**
     * 利用可能な保管ロケーションを検索する。
     *
     * @param warehouseId 倉庫ID
     * @param productId 商品ID
     * @return 保管ロケーションのリスト
     */
    @SuppressWarnings("unchecked")
    public List<StorageLocation> findAvailableStorageLocation(Long warehouseId, Long productId) {
        if (warehouseId == null) {
            return new ArrayList<StorageLocation>();
        }

        // 技術的負債: 空きロケーションの判定が不完全（重量・容積チェックなし）
        return em.createQuery(
                "SELECT sl FROM StorageLocation sl " +
                "WHERE sl.zone.warehouse.id = :warehouseId " +
                "ORDER BY sl.code")
                .setParameter("warehouseId", warehouseId)
                .getResultList();
    }

    // ========================================================================
    // 在庫引当
    // ========================================================================

    /**
     * 在庫を引き当てる（予約する）。
     *
     * @param productId 商品ID
     * @param warehouseId 倉庫ID
     * @param quantity 引当数量
     * @param orderId 発注ID
     * @throws InsufficientStockException 在庫不足
     */
    public void reserveStock(Long productId, Long warehouseId, int quantity, Long orderId) {
        if (quantity <= 0) {
            throw new ValidationException("quantity", "引当数量は1以上を指定してください。");
        }

        InventoryItem item = findInventoryItem(productId, warehouseId);
        if (item == null) {
            throw new InsufficientStockException(productId, quantity, 0);
        }

        int available = item.getQuantityOnHand() - item.getQuantityReserved();
        if (available < quantity) {
            throw new InsufficientStockException(productId, quantity, available);
        }

        item.setQuantityReserved(item.getQuantityReserved() + quantity);
        em.merge(item);

        logger.info("在庫引当完了。商品ID: " + productId + ", 数量: " + quantity
                + ", 発注ID: " + orderId);
    }

    /**
     * 引当済み在庫を解放する。
     *
     * @param productId 商品ID
     * @param warehouseId 倉庫ID
     * @param quantity 解放数量
     */
    public void releaseReservedStock(Long productId, Long warehouseId, int quantity) {
        if (quantity <= 0) {
            return;
        }

        InventoryItem item = findInventoryItem(productId, warehouseId);
        if (item == null) {
            return;
        }

        int reserved = item.getQuantityReserved();
        int newReserved = reserved - quantity;
        if (newReserved < 0) {
            newReserved = 0;
        }
        item.setQuantityReserved(newReserved);
        em.merge(item);

        logger.info("在庫引当解放。商品ID: " + productId + ", 数量: " + quantity);
    }

    /**
     * 在庫移動レポートを取得する。
     *
     * <p>【技術的負債 #12】List<Object[]>を返却しており、型安全でない。</p>
     *
     * @param warehouseId 倉庫ID
     * @param from 開始日
     * @param to 終了日
     * @return 移動レポートデータ
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> getStockMovementReport(Long warehouseId, Date from, Date to) {
        if (warehouseId == null) {
            return new ArrayList<Object[]>();
        }

        // 技術的負債: ネイティブSQL
        String sql = "SELECT p.name, it.transaction_type, SUM(it.quantity), COUNT(*) " +
                "FROM inventory_transaction it " +
                "JOIN inventory_item ii ON ii.id = it.inventory_item_id " +
                "JOIN product p ON p.id = ii.product_id " +
                "WHERE ii.warehouse_id = ?1 " +
                "AND it.transaction_date BETWEEN ?2 AND ?3 " +
                "GROUP BY p.name, it.transaction_type " +
                "ORDER BY p.name, it.transaction_type";

        return em.createNativeQuery(sql)
                .setParameter(1, warehouseId)
                .setParameter(2, from)
                .setParameter(3, to)
                .getResultList();
    }

    // ========================================================================
    // プライベートヘルパーメソッド
    // ========================================================================

    /**
     * 在庫品目を検索する。
     */
    @SuppressWarnings("unchecked")
    private InventoryItem findInventoryItem(Long productId, Long warehouseId) {
        List<InventoryItem> items = em.createNamedQuery("InventoryItem.findByProductAndWarehouse")
                .setParameter("productId", productId)
                .setParameter("warehouseId", warehouseId)
                .getResultList();

        if (items != null && !items.isEmpty()) {
            return items.get(0);
        }
        return null;
    }

    /**
     * 在庫品目を検索し、存在しない場合は新規作成する。
     */
    private InventoryItem findOrCreateInventoryItem(Long productId, Long warehouseId) {
        InventoryItem item = findInventoryItem(productId, warehouseId);
        if (item != null) {
            return item;
        }

        // 新規作成
        Product product = em.find(Product.class, productId);
        Warehouse warehouse = em.find(Warehouse.class, warehouseId);

        item = new InventoryItem();
        item.setProduct(product);
        item.setWarehouse(warehouse);
        item.setQuantityOnHand(0);
        item.setQuantityReserved(0);
        item.setQuantityOnOrder(0);

        em.persist(item);
        em.flush();

        return item;
    }

    /**
     * 在庫トランザクションを記録する。
     */
    private void createInventoryTransaction(InventoryItem item, String type,
                                            int quantity, String reference, String notes) {
        InventoryTransaction tx = new InventoryTransaction();
        // inventoryItem ManyToOne was removed; use productId/warehouseId instead (DDL alignment)
        tx.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        tx.setWarehouseId(item.getWarehouse() != null ? item.getWarehouse().getId() : null);
        tx.setTransactionType(type);
        tx.setQuantity(quantity);
        // reference field was split into referenceType/referenceId (DDL alignment)
        tx.setReferenceType(reference);
        tx.setTransactionDate(new Date());
        tx.setNotes(notes);

        em.persist(tx);
    }

    /**
     * 在庫低下時の通知チェック。
     */
    private void checkAndNotifyLowStock(InventoryItem item) {
        // reorderPoint field was removed from InventoryItem (DDL alignment).
        // Simple zero-stock check instead.
        if (item.getQuantityOnHand() != null && item.getQuantityOnHand() <= 0) {
            try {
                String title = "在庫低下: " + item.getProduct().getName();
                String message = "在庫がゼロ以下になりました。"
                        + " 現在在庫: " + item.getQuantityOnHand();

                notificationService.sendNotification(
                        1L, title, message, "WARNING",
                        "InventoryItem", item.getId());
            } catch (Exception e) {
                // 技術的負債 #7: 通知失敗を握りつぶし
                logger.warning("在庫低下通知の送信に失敗しました。");
            }
        }
    }
}
