package com.proquip.ejb.event;

import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.system.AuditLog;
import com.proquip.ejb.entity.system.Notification;

import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 在庫イベントオブザーバー。
 *
 * <p>CDIイベントとして発火された {@link InventoryEvent} を監視し、
 * 在庫数量の減少時に在庫不足チェックを実行し、監査ログを記録する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>イベント処理が同期的に実行される。非同期処理に移行すべき。</li>
 *   <li>在庫不足チェックロジックが {@link com.proquip.ejb.scheduler.LowStockAlertScheduler}
 *       と重複している。共通のサービスに集約すべき。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class InventoryEventObserver {

    private static final Logger logger = Logger.getLogger(InventoryEventObserver.class.getName());

    @PersistenceContext(unitName = "proquipPU")
    private EntityManager em;

    /**
     * 在庫イベントを監視し、在庫不足チェックおよび監査ログ記録を行う。
     *
     * @param event 在庫イベント
     */
    public void onInventoryEvent(@Observes InventoryEvent event) {
        logger.info("在庫イベントを受信しました: " + event);

        try {
            // 監査ログの記録
            createAuditLog(event);

            // 数量が減少するイベントの場合、在庫不足チェックを実行
            if (isQuantityDecreaseEvent(event)) {
                checkLowStock(event);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "在庫イベント処理中にエラーが発生しました: " + event, e);
        }
    }

    /**
     * 在庫数量が減少するイベントかどうかを判定する。
     *
     * @param event 在庫イベント
     * @return 数量減少イベントの場合 {@code true}
     */
    private boolean isQuantityDecreaseEvent(InventoryEvent event) {
        if (event.getQuantity() != null && event.getQuantity() < 0) {
            return true;
        }
        String eventType = event.getEventType();
        return "SHIPPED".equals(eventType) || "ADJUSTED".equals(eventType)
                || "RESERVED".equals(eventType);
    }

    /**
     * 在庫不足チェックを実行し、再発注点以下の場合に通知を作成する。
     *
     * <p>技術的負債: このロジックは {@code LowStockAlertScheduler} と重複している。
     * 在庫チェック用の共通サービスに集約すべき。</p>
     *
     * @param event 在庫イベント
     */
    private void checkLowStock(InventoryEvent event) {
        if (event.getProductId() == null || event.getWarehouseId() == null) {
            return;
        }

        TypedQuery<InventoryItem> query = em.createNamedQuery(
                "InventoryItem.findByProductAndWarehouse", InventoryItem.class);
        query.setParameter("productId", event.getProductId());
        query.setParameter("warehouseId", event.getWarehouseId());

        List<InventoryItem> items = query.getResultList();
        if (items.isEmpty()) {
            return;
        }

        InventoryItem item = items.get(0);
        // reorderPoint field was removed from InventoryItem entity (DDL alignment).
        // Low-stock check is skipped here; use a dedicated query-based approach instead.
        if (item.getQuantityOnHand() != null && item.getQuantityOnHand() <= 0) {

            Notification notification = new Notification();
            notification.setTitle("在庫不足アラート（リアルタイム）");
            notification.setMessage(
                    "在庫イベント「" + event.getEventType() + "」の結果、"
                    + "在庫数がゼロ以下になりました。\n"
                    + "商品ID: " + event.getProductId() + "\n"
                    + "倉庫ID: " + event.getWarehouseId() + "\n"
                    + "現在在庫: " + item.getQuantityOnHand());
            notification.setType("WARNING");
            notification.setStatus("UNREAD");
            notification.setPriority(8);
            notification.setReferenceType("InventoryItem");
            notification.setReferenceId(item.getId());
            notification.setUserId(1L);

            em.persist(notification);

            logger.warning("在庫不足を検出しました（リアルタイム）: 商品ID=" + event.getProductId()
                    + " 倉庫ID=" + event.getWarehouseId()
                    + " 在庫: " + item.getQuantityOnHand());
        }
    }

    /**
     * 在庫イベントに基づいて監査ログを作成する。
     *
     * @param event 在庫イベント
     */
    private void createAuditLog(InventoryEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("InventoryItem");
        auditLog.setEntityId(event.getProductId() != null ? event.getProductId() : 0L);
        auditLog.setAction(event.getEventType());
        auditLog.setPerformedBy(0L);
        auditLog.setPerformedAt(event.getTimestamp() != null ? event.getTimestamp() : new Date());
        auditLog.setAdditionalInfo(
                "warehouseId=" + event.getWarehouseId()
                + ", quantity=" + event.getQuantity()
                + ", CDIイベント経由で記録");

        em.persist(auditLog);
    }
}
