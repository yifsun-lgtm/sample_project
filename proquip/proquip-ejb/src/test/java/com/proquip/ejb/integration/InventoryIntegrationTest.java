package com.proquip.ejb.integration;

import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.inventory.StockTransfer;
import com.proquip.ejb.entity.inventory.Warehouse;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.service.InventoryServiceBean;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 在庫管理の統合テスト。
 *
 * <p>WildFly上で実行するArquillian統合テスト。在庫の追加・削除、
 * 倉庫間移動、棚卸し処理を検証する。</p>
 *
 * <p>技術的負債 #13: 統合テストが未実装。WildFlyの統合テスト環境が
 * 整備されるまで全テストが@Disabledとなっている。</p>
 *
 * @author ProQuip開発チーム
 */
@Disabled("統合テスト環境のセットアップ待ち")
@ExtendWith(ArquillianExtension.class)
public class InventoryIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Inject
    private InventoryServiceBean inventoryService;

    /** テスト用倉庫A ID */
    private Long warehouseAId;

    /** テスト用倉庫B ID */
    private Long warehouseBId;

    /** テスト用製品ID */
    private Long testProductId;

    /**
     * テスト用WARアーカイブを作成する。
     *
     * @return テスト用WebArchive
     */
    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "proquip-inventory-test.war")
            .addPackages(true, "com.proquip.ejb.entity")
            .addPackages(true, "com.proquip.ejb.service")
            .addPackages(true, "com.proquip.ejb.dao")
            .addPackages(true, "com.proquip.common.dto")
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

        // テスト用倉庫Aの準備
        List<Warehouse> warehousesA = em.createQuery(
            "SELECT w FROM Warehouse w WHERE w.code = :code", Warehouse.class)
            .setParameter("code", "TEST-WH-A")
            .getResultList();
        if (warehousesA.isEmpty()) {
            Warehouse wh = new Warehouse();
            wh.setCode("TEST-WH-A");
            wh.setName("統合テスト倉庫A");
            wh.setActive(true);
            em.persist(wh);
            em.flush();
            warehouseAId = wh.getId();
        } else {
            warehouseAId = warehousesA.get(0).getId();
        }

        // テスト用倉庫Bの準備
        List<Warehouse> warehousesB = em.createQuery(
            "SELECT w FROM Warehouse w WHERE w.code = :code", Warehouse.class)
            .setParameter("code", "TEST-WH-B")
            .getResultList();
        if (warehousesB.isEmpty()) {
            Warehouse wh = new Warehouse();
            wh.setCode("TEST-WH-B");
            wh.setName("統合テスト倉庫B");
            wh.setActive(true);
            em.persist(wh);
            em.flush();
            warehouseBId = wh.getId();
        } else {
            warehouseBId = warehousesB.get(0).getId();
        }

        // テスト用製品の準備
        List<Product> products = em.createQuery(
            "SELECT p FROM Product p WHERE p.sku = :sku", Product.class)
            .setParameter("sku", "TEST-INV-001")
            .getResultList();
        if (products.isEmpty()) {
            Product product = new Product();
            product.setSku("TEST-INV-001");
            product.setName("在庫テスト用製品");
            product.setUnitPrice(new BigDecimal("3000.00"));
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
    // 在庫追加・削除テスト
    // ========================================================================

    @Test
    @DisplayName("在庫を追加できること")
    void shouldAddStock() {
        // given: 倉庫Aに対する在庫追加リクエスト
        // when: 在庫を追加
        // inventoryService.addStock(warehouseAId, testProductId, 100, "初期在庫投入");
        // then: 在庫数が100になっていること
        // InventoryItem item = findInventoryItem(warehouseAId, testProductId);
        // assertEquals(100, item.getQuantityOnHand());
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("在庫を削除（出庫）できること")
    void shouldRemoveStock() {
        // given: 倉庫Aに在庫100がある状態
        // inventoryService.addStock(warehouseAId, testProductId, 100, "テストデータ");
        // when: 50個出庫
        // inventoryService.removeStock(warehouseAId, testProductId, 50, "出庫テスト");
        // then: 在庫数が50になっていること
        // InventoryItem item = findInventoryItem(warehouseAId, testProductId);
        // assertEquals(50, item.getQuantityOnHand());
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("在庫数以上の出庫はエラーとなること")
    void shouldRejectOverdraw() {
        // given: 倉庫Aに在庫10がある状態
        // inventoryService.addStock(warehouseAId, testProductId, 10, "テストデータ");
        // when & then: 20個出庫しようとするとBusinessExceptionが発生
        // assertThrows(BusinessException.class, () -> {
        //     inventoryService.removeStock(warehouseAId, testProductId, 20, "超過出庫テスト");
        // });
        fail("統合テスト環境のセットアップ待ち");
    }

    // ========================================================================
    // 倉庫間移動テスト
    // ========================================================================

    @Test
    @DisplayName("倉庫間の在庫移動ができること")
    void shouldTransferBetweenWarehouses() {
        // given: 倉庫Aに在庫100がある状態
        // inventoryService.addStock(warehouseAId, testProductId, 100, "テストデータ");
        // when: 倉庫Aから倉庫Bへ30個移動
        // inventoryService.transferStock(warehouseAId, warehouseBId, testProductId, 30, "移動テスト");
        // then: 倉庫Aが70、倉庫Bが30になっていること
        // InventoryItem itemA = findInventoryItem(warehouseAId, testProductId);
        // InventoryItem itemB = findInventoryItem(warehouseBId, testProductId);
        // assertEquals(70, itemA.getQuantityOnHand());
        // assertEquals(30, itemB.getQuantityOnHand());
        fail("統合テスト環境のセットアップ待ち");
    }

    @Test
    @DisplayName("同一倉庫への移動はエラーとなること")
    void shouldRejectTransferToSameWarehouse() {
        // given: 倉庫Aに在庫100がある状態
        // when & then: 同一倉庫への移動はValidationException
        // assertThrows(ValidationException.class, () -> {
        //     inventoryService.transferStock(warehouseAId, warehouseAId, testProductId, 10, "自己移動");
        // });
        fail("統合テスト環境のセットアップ待ち");
    }

    // ========================================================================
    // 棚卸しテスト
    // ========================================================================

    @Test
    @DisplayName("棚卸しで差異が検出されること")
    void shouldDetectDiscrepancyDuringCount() {
        // given: 倉庫Aにシステム上在庫100がある状態
        // inventoryService.addStock(warehouseAId, testProductId, 100, "テストデータ");
        // when: 実数カウント95を記録
        // InventoryCount count = inventoryService.startCount(warehouseAId);
        // inventoryService.recordCount(count.getId(), testProductId, 95);
        // inventoryService.finalizeCount(count.getId());
        // then: 差異-5が記録されていること
        // InventoryItem item = findInventoryItem(warehouseAId, testProductId);
        // assertEquals(95, item.getQuantityOnHand(), "棚卸し後の在庫数が実数に更新されること");
        fail("統合テスト環境のセットアップ待ち");
    }
}
