package com.proquip.ejb.integration;

import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;
import com.proquip.ejb.entity.procurement.GoodsReceipt;
import com.proquip.ejb.entity.procurement.GoodsReceiptItem;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.service.PurchaseOrderServiceBean;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 発注書の統合テスト。
 *
 * <p>WildFly上で実行するArquillian統合テスト。発注書のライフサイクル全体
 * （作成 → 提出 → 承認 → 入荷 → クローズ）を検証する。</p>
 *
 * <p>技術的負債 #13: 統合テストが未実装。このクラスはスケルトンのみで、
 * WildFlyの統合テスト環境が整備されるまで全テストが@Disabledとなっている。</p>
 *
 * <p>前提条件:</p>
 * <ul>
 *   <li>WildFly 32.0.1.Final が起動していること</li>
 *   <li>PostgreSQLテスト用データベースが構成済みであること</li>
 *   <li>テストデータ（サプライヤー、製品）が事前投入されていること</li>
 * </ul>
 *
 * @author ProQuip開発チーム
 */
@Disabled("統合テスト環境のセットアップ待ち")
@ExtendWith(ArquillianExtension.class)
public class PurchaseOrderIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Inject
    private PurchaseOrderServiceBean purchaseOrderService;

    /** テスト用サプライヤーID */
    private Long testSupplierId;

    /** テスト用製品ID */
    private Long testProductId;

    /**
     * テスト用WARアーカイブを作成する。
     *
     * <p>ShrinkWrapを使用してテスト用のデプロイメントアーカイブを構築する。
     * エンティティ、サービス、DAOクラスを含むミニマルなアーカイブ。</p>
     *
     * @return テスト用WebArchive
     */
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "proquip-integration-test.war")
            .addPackages(true, "com.proquip.ejb.entity")
            .addPackages(true, "com.proquip.ejb.service")
            .addPackages(true, "com.proquip.ejb.dao")
            .addPackages(true, "com.proquip.ejb.validator")
            .addPackages(true, "com.proquip.common.dto")
            .addPackages(true, "com.proquip.ejb.mapper")
            .addPackages(true, "com.proquip.common.exception")
            .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * 各テスト前にテストデータを準備する。
     *
     * @throws Exception トランザクション操作で例外が発生した場合
     */
    @BeforeEach
    void setUp() throws Exception {
        utx.begin();

        // テスト用サプライヤーの検索または作成
        List<Supplier> suppliers = em.createQuery(
            "SELECT s FROM Supplier s WHERE s.code = :code", Supplier.class)
            .setParameter("code", "TEST-SUP-001")
            .getResultList();

        if (suppliers.isEmpty()) {
            Supplier supplier = new Supplier();
            supplier.setCode("TEST-SUP-001");
            supplier.setName("統合テスト用サプライヤー株式会社");
            supplier.setStatus("ACTIVE");
            em.persist(supplier);
            em.flush();
            testSupplierId = supplier.getId();
        } else {
            testSupplierId = suppliers.get(0).getId();
        }

        // テスト用製品の検索または作成
        List<Product> products = em.createQuery(
            "SELECT p FROM Product p WHERE p.sku = :sku", Product.class)
            .setParameter("sku", "TEST-PRD-001")
            .getResultList();

        if (products.isEmpty()) {
            Product product = new Product();
            product.setSku("TEST-PRD-001");
            product.setName("統合テスト用製品A");
            product.setUnitPrice(new BigDecimal("5000.00"));
            product.setStatus("ACTIVE");
            em.persist(product);
            em.flush();
            testProductId = product.getId();
        } else {
            testProductId = products.get(0).getId();
        }

        utx.commit();
    }

    // ========================================================================
    // 発注書ライフサイクルテスト
    // ========================================================================

    @Test
    @DisplayName("発注書を新規作成できること")
    void shouldCreatePurchaseOrder() {
        // given: テスト用の発注データを準備
        // PurchaseOrder order = new PurchaseOrder();
        // order.setStatus("DRAFT");
        // order.setCurrency("JPY");
        // Supplier supplier = em.find(Supplier.class, testSupplierId);
        // order.setSupplier(supplier);
        // order.setOrderDate(new Date());
        //
        // PurchaseOrderItem item = new PurchaseOrderItem();
        // Product product = em.find(Product.class, testProductId);
        // item.setProduct(product);
        // item.setQuantity(10);
        // item.setUnitPrice(new BigDecimal("5000.00"));
        // item.setLineNumber(1);
        // order.getItems().add(item);

        // when: 発注書を作成
        // PurchaseOrder created = purchaseOrderService.createOrder(order);

        // then: 発注書が作成されていること
        // assertNotNull(created.getId());
        // assertNotNull(created.getPoNumber());
        // assertEquals("DRAFT", created.getStatus());
        // assertEquals(1, created.getItems().size());
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("発注書を提出して承認待ちステータスに遷移すること")
    void shouldSubmitOrderForApproval() {
        // given: DRAFT状態の発注書を作成
        // PurchaseOrder order = createTestOrder();

        // when: 承認依頼を提出
        // purchaseOrderService.submitForApproval(order.getId());

        // then: ステータスがPENDING_APPROVALに変更されていること
        // PurchaseOrder updated = em.find(PurchaseOrder.class, order.getId());
        // assertEquals("PENDING_APPROVAL", updated.getStatus());
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("発注書を承認してAPPROVEDステータスに遷移すること")
    void shouldApproveOrder() {
        // given: PENDING_APPROVAL状態の発注書
        // PurchaseOrder order = createTestOrder();
        // purchaseOrderService.submitForApproval(order.getId());

        // when: 承認
        // purchaseOrderService.approveOrder(order.getId(), "admin", "承認します");

        // then: ステータスがAPPROVEDに変更されていること
        // PurchaseOrder updated = em.find(PurchaseOrder.class, order.getId());
        // assertEquals("APPROVED", updated.getStatus());
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("発注書を却下してREJECTEDステータスに遷移すること")
    void shouldRejectOrder() {
        // given: PENDING_APPROVAL状態の発注書
        // PurchaseOrder order = createTestOrder();
        // purchaseOrderService.submitForApproval(order.getId());

        // when: 却下
        // purchaseOrderService.rejectOrder(order.getId(), "admin", "予算超過のため却下");

        // then: ステータスがREJECTEDに変更されていること
        // PurchaseOrder updated = em.find(PurchaseOrder.class, order.getId());
        // assertEquals("REJECTED", updated.getStatus());
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("入荷検収を登録して在庫が更新されること")
    void shouldReceiveGoodsAndUpdateInventory() {
        // given: APPROVED → ORDERED 状態の発注書
        // PurchaseOrder order = createAndApproveTestOrder();

        // when: 入荷検収を登録
        // GoodsReceipt receipt = new GoodsReceipt();
        // receipt.setPurchaseOrder(order);
        // receipt.setReceivedDate(new Date());
        //
        // GoodsReceiptItem receiptItem = new GoodsReceiptItem();
        // receiptItem.setPurchaseOrderItem(order.getItems().get(0));
        // receiptItem.setReceivedQuantity(10);
        // receiptItem.setAcceptedQuantity(10);
        // receipt.getItems().add(receiptItem);
        //
        // purchaseOrderService.receiveGoods(receipt);

        // then: 在庫が更新されていること
        // InventoryItem inventory = em.createQuery(
        //     "SELECT i FROM InventoryItem i WHERE i.product.id = :productId",
        //     InventoryItem.class)
        //     .setParameter("productId", testProductId)
        //     .getSingleResult();
        // assertTrue(inventory.getQuantityOnHand() >= 10);
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("全数入荷後に発注書がCLOSEDステータスに遷移すること")
    void shouldCloseOrderAfterFullReceipt() {
        // given: 全数入荷済みの発注書
        // PurchaseOrder order = createAndReceiveTestOrder();

        // when: クローズ処理
        // purchaseOrderService.closeOrder(order.getId());

        // then: ステータスがCLOSEDに変更されていること
        // PurchaseOrder updated = em.find(PurchaseOrder.class, order.getId());
        // assertEquals("CLOSED", updated.getStatus());
        fail("統合テスト環境のセットアップ待ち");
    }

    // ========================================================================
    // 予算チェックテスト
    // ========================================================================

    @Test
    @DisplayName("予算超過の発注書は承認時にエラーとなること")
    void shouldRejectOrderExceedingBudget() {
        // given: 予算上限を超える金額の発注書
        // PurchaseOrder order = createTestOrder();
        // order.setTotalAmount(new BigDecimal("99999999.99"));
        // em.merge(order);
        // purchaseOrderService.submitForApproval(order.getId());

        // when & then: 承認時にBusinessExceptionが発生すること
        // assertThrows(BusinessException.class, () -> {
        //     purchaseOrderService.approveOrder(order.getId(), "admin", "承認");
        // });
        fail("統合テスト環境のセットアップ待ち");
    }

    // ========================================================================
    // 通知テスト
    // ========================================================================

    @Test
    @DisplayName("発注書の承認依頼時に承認者に通知が送信されること")
    void shouldSendNotificationOnSubmit() {
        // given: DRAFT状態の発注書
        // PurchaseOrder order = createTestOrder();

        // when: 承認依頼を提出
        // purchaseOrderService.submitForApproval(order.getId());

        // then: 承認者への通知が作成されていること
        // Long notificationCount = em.createQuery(
        //     "SELECT COUNT(n) FROM Notification n WHERE n.referenceType = 'PurchaseOrder' AND n.referenceId = :orderId",
        //     Long.class)
        //     .setParameter("orderId", order.getId())
        //     .getSingleResult();
        // assertTrue(notificationCount > 0, "承認者への通知が作成されていること");
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("発注書の承認完了時に依頼者に通知が送信されること")
    void shouldSendNotificationOnApproval() {
        // given: PENDING_APPROVAL状態の発注書
        // PurchaseOrder order = createTestOrder();
        // purchaseOrderService.submitForApproval(order.getId());

        // when: 承認
        // purchaseOrderService.approveOrder(order.getId(), "admin", "承認します");

        // then: 依頼者への承認完了通知が作成されていること
        // Long notificationCount = em.createQuery(
        //     "SELECT COUNT(n) FROM Notification n WHERE n.type = 'INFO' AND n.referenceId = :orderId",
        //     Long.class)
        //     .setParameter("orderId", order.getId())
        //     .getSingleResult();
        // assertTrue(notificationCount > 0, "依頼者への承認完了通知が作成されていること");
        fail("統合テスト環境のセットアップ待ち");
    }
}
