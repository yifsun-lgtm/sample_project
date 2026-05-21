package com.proquip.ejb.service;

import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.InsufficientStockException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.inventory.InventoryTransaction;
import com.proquip.ejb.entity.inventory.Warehouse;
import com.proquip.ejb.entity.product.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InventoryServiceBeanの単体テスト。
 *
 * <p>技術的負債 #13: テストカバレッジが低い。
 * 棚卸処理、トランザクション履歴、在庫評価のテストが存在しない。
 * 在庫低下チェックのテストはハードコードされた再発注点に依存。</p>
 *
 * @author ProQuip開発チーム
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceBeanTest {

    @Mock
    private PurchaseOrderServiceBean purchaseOrderService;

    @Mock
    private NotificationServiceBean notificationService;

    @Mock
    private AuditServiceBean auditService;

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private InventoryServiceBean service;

    // テスト用フィクスチャ
    private Product testProduct;
    private Warehouse testWarehouse;
    private InventoryItem testInventoryItem;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setSku("PRD-000001");
        testProduct.setName("テスト商品A");
        testProduct.setUnitPrice(new BigDecimal("1000.00"));

        testWarehouse = new Warehouse();
        testWarehouse.setId(1L);
        testWarehouse.setName("東京倉庫");
        testWarehouse.setCode("WH-TKY");

        testInventoryItem = new InventoryItem();
        testInventoryItem.setId(1L);
        testInventoryItem.setProduct(testProduct);
        testInventoryItem.setWarehouse(testWarehouse);
        testInventoryItem.setQuantityOnHand(100);
        testInventoryItem.setQuantityReserved(10);
        testInventoryItem.setQuantityOnOrder(20);
        // reorderPoint was removed from InventoryItem (DDL alignment)
    }

    // ========================================================================
    // 入庫テスト
    // ========================================================================

    @Test
    @DisplayName("入庫 - 正常系: 在庫追加が成功すること")
    void testAddStock_success() {
        // Arrange
        when(em.find(Product.class, 100L)).thenReturn(testProduct);
        when(em.find(Warehouse.class, 1L)).thenReturn(testWarehouse);

        // findOrCreateInventoryItemのモック — 既存の在庫品目を返す
        Query namedQuery = mock(Query.class);
        when(em.createNamedQuery("InventoryItem.findByProductAndWarehouse")).thenReturn(namedQuery);
        when(namedQuery.setParameter(anyString(), any())).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(Arrays.asList(testInventoryItem));

        // Act
        service.addStock(100L, 1L, 50, "PURCHASE_ORDER", 1L);

        // Assert
        assertEquals(150, testInventoryItem.getQuantityOnHand());
        verify(em).merge(testInventoryItem);
        verify(em).persist(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("入庫 - 異常系: 数量が0以下の場合はValidationExceptionがスローされること")
    void testAddStock_invalidQuantity() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.addStock(100L, 1L, 0, "ADJUSTMENT", null);
        });

        assertThrows(ValidationException.class, () -> {
            service.addStock(100L, 1L, -5, "ADJUSTMENT", null);
        });
    }

    @Test
    @DisplayName("入庫 - 異常系: 存在しない商品IDでEntityNotFoundExceptionがスローされること")
    void testAddStock_productNotFound() {
        // Arrange
        when(em.find(Product.class, 999L)).thenReturn(null);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            service.addStock(999L, 1L, 10, "ADJUSTMENT", null);
        });
    }

    // ========================================================================
    // 出庫テスト
    // ========================================================================

    @Test
    @DisplayName("出庫 - 正常系: 在庫から正しく減算されること")
    void testRemoveStock_success() {
        // Arrange
        Query namedQuery = mock(Query.class);
        when(em.createNamedQuery("InventoryItem.findByProductAndWarehouse")).thenReturn(namedQuery);
        when(namedQuery.setParameter(anyString(), any())).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(Arrays.asList(testInventoryItem));

        // quantityOnHand=100, quantityReserved=10 → available=90
        // 50個出庫 → quantityOnHand=50
        int beforeQty = testInventoryItem.getQuantityOnHand();

        // Act
        service.removeStock(100L, 1L, 50, "テスト出庫");

        // Assert
        assertEquals(beforeQty - 50, testInventoryItem.getQuantityOnHand());
        verify(em).merge(testInventoryItem);
    }

    @Test
    @DisplayName("出庫 - 異常系: 在庫不足の場合はInsufficientStockExceptionがスローされること")
    void testRemoveStock_insufficientStock() {
        // Arrange
        Query namedQuery = mock(Query.class);
        when(em.createNamedQuery("InventoryItem.findByProductAndWarehouse")).thenReturn(namedQuery);
        when(namedQuery.setParameter(anyString(), any())).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(Arrays.asList(testInventoryItem));

        // quantityOnHand=100, quantityReserved=10 → available=90
        // 95個出庫を試行 → 不足

        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> {
            service.removeStock(100L, 1L, 95, "大量出庫テスト");
        });
    }

    // ========================================================================
    // 倉庫間移動テスト
    // ========================================================================

    @Test
    @Disabled("倉庫間移動のバリデーション修正中")
    @DisplayName("在庫移動 - 正常系: 倉庫間で在庫が正しく移動されること")
    void testTransferStock_success() {
        // Arrange
        Warehouse toWarehouse = new Warehouse();
        toWarehouse.setId(2L);
        toWarehouse.setName("大阪倉庫");

        InventoryItem toItem = new InventoryItem();
        toItem.setId(2L);
        toItem.setProduct(testProduct);
        toItem.setWarehouse(toWarehouse);
        toItem.setQuantityOnHand(30);
        toItem.setQuantityReserved(0);

        // 移動元の在庫品目
        Query fromQuery = mock(Query.class);
        when(em.createNamedQuery("InventoryItem.findByProductAndWarehouse")).thenReturn(fromQuery);
        when(fromQuery.setParameter(anyString(), any())).thenReturn(fromQuery);
        // 1回目: 移動元、2回目: 移動先
        when(fromQuery.getResultList())
                .thenReturn(Arrays.asList(testInventoryItem))
                .thenReturn(Arrays.asList(toItem));

        // Act
        service.transferStock(1L, 2L, 100L, 20);

        // Assert
        assertEquals(80, testInventoryItem.getQuantityOnHand()); // 100 - 20
        assertEquals(50, toItem.getQuantityOnHand()); // 30 + 20
    }

    // ========================================================================
    // 在庫レベル照会テスト
    // ========================================================================

    @Test
    @DisplayName("在庫レベル照会 - 正常系: 在庫情報が正しく返されること")
    void testGetStockLevel() {
        // Arrange
        Query namedQuery = mock(Query.class);
        when(em.createNamedQuery("InventoryItem.findByProductAndWarehouse")).thenReturn(namedQuery);
        when(namedQuery.setParameter(anyString(), any())).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(Arrays.asList(testInventoryItem));

        // Act
        InventoryItem result = service.getStockLevel(100L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getQuantityOnHand());
        assertEquals(10, result.getQuantityReserved());
    }

    // ========================================================================
    // 在庫低下チェックテスト
    // ========================================================================

    @Test
    @DisplayName("在庫低下チェック - 正常系: 再発注点を下回る品目が検出されること")
    void testCheckLowStock() {
        // 技術的負債 #13: ハードコードされた再発注点値に依存
        // reorderPoint=15, quantityOnHand=10 → 低下検出

        InventoryItem lowStockItem = new InventoryItem();
        lowStockItem.setId(5L);
        lowStockItem.setProduct(testProduct);
        lowStockItem.setWarehouse(testWarehouse);
        lowStockItem.setQuantityOnHand(10);
        // reorderPoint was removed from InventoryItem (DDL alignment)

        Query namedQuery = mock(Query.class);
        when(em.createNamedQuery("InventoryItem.findBelowReorderPoint")).thenReturn(namedQuery);
        when(namedQuery.getResultList()).thenReturn(Arrays.asList(lowStockItem));

        // Act
        service.checkLowStock();

        // Assert — 通知が送信されたことを確認
        verify(notificationService).sendNotification(
                eq(1L), anyString(), anyString(), eq("WARNING"),
                eq("InventoryItem"), eq(5L));
    }

    // ========================================================================
    // コメントアウトされたテスト
    // ========================================================================

    // 在庫評価テスト — BigDecimalの丸め問題で失敗するためコメントアウト
    // @Test
    // @DisplayName("在庫評価 - 倉庫の在庫評価額が正しく計算されること")
    // void testCalculateInventoryValuation() {
    //     // Arrange
    //     Query namedQuery = mock(Query.class);
    //     when(em.createNamedQuery("InventoryItem.findByWarehouse")).thenReturn(namedQuery);
    //     when(namedQuery.setParameter(anyString(), any())).thenReturn(namedQuery);
    //
    //     InventoryItem item1 = new InventoryItem();
    //     item1.setProduct(testProduct); // unitPrice=1000
    //     item1.setQuantityOnHand(50);
    //
    //     Product product2 = new Product();
    //     product2.setUnitPrice(new BigDecimal("2500.50"));
    //     InventoryItem item2 = new InventoryItem();
    //     item2.setProduct(product2);
    //     item2.setQuantityOnHand(30);
    //
    //     when(namedQuery.getResultList()).thenReturn(Arrays.asList(item1, item2));
    //
    //     // Act
    //     BigDecimal total = service.calculateInventoryValuation(1L);
    //
    //     // Assert
    //     // item1: 1000 * 50 = 50000
    //     // item2: 2500.50 * 30 = 75015
    //     // total: 125015.00
    //     // 技術的負債: HALF_DOWNとHALF_UPの混在で微妙にずれる可能性
    //     assertEquals(new BigDecimal("125015.00"), total);
    // }
}
