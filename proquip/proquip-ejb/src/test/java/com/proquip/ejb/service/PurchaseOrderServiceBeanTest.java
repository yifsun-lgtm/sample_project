package com.proquip.ejb.service;

import com.proquip.common.exception.ApprovalException;
import com.proquip.common.exception.BusinessException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.inventory.InventoryItem;
import com.proquip.ejb.entity.organization.Department;
import com.proquip.ejb.entity.organization.UserProfile;
import com.proquip.ejb.entity.pricing.Budget;
import com.proquip.ejb.entity.procurement.ApprovalStep;
import com.proquip.ejb.entity.procurement.ApprovalWorkflow;
import com.proquip.ejb.entity.procurement.GoodsReceipt;
import com.proquip.ejb.entity.procurement.GoodsReceiptItem;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;
import com.proquip.ejb.entity.procurement.PurchaseOrderStatusHistory;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;

import jakarta.ejb.SessionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PurchaseOrderServiceBeanの単体テスト。
 *
 * <p>技術的負債 #13: テストカバレッジが約15%しかない。
 * ハードコードされた日付、@Disabledテスト、コメントアウトされたテストが多数存在。
 * 一部のテストは実装の内部詳細をverifyしており脆弱。</p>
 *
 * @author ProQuip開発チーム
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceBeanTest {

    @Mock
    private InventoryServiceBean inventoryService;

    @Mock
    private SupplierServiceBean supplierService;

    @Mock
    private BudgetServiceBean budgetService;

    @Mock
    private NotificationServiceBean notificationService;

    @Mock
    private AuditServiceBean auditService;

    @Mock
    private EntityManager em;

    @Mock
    private SessionContext sessionContext;

    @Mock
    private Query query;

    @Mock
    private TypedQuery<Object> typedQuery;

    @Mock
    private Principal principal;

    @InjectMocks
    private PurchaseOrderServiceBean service;

    // テスト用フィクスチャ
    private PurchaseOrder testOrder;
    private Supplier testSupplier;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // テストデータの初期化
        testSupplier = new Supplier();
        testSupplier.setId(1L);
        testSupplier.setName("テストサプライヤー株式会社");
        testSupplier.setCode("SUP-001");
        testSupplier.setStatus("ACTIVE");

        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setSku("PRD-000001");
        testProduct.setName("テスト商品A");
        testProduct.setUnitPrice(new BigDecimal("1000.00"));

        testOrder = createTestOrder();
    }

    /**
     * テスト用の発注オブジェクトを作成する。
     * 技術的負債: 日付が"2024-03-15"にハードコードされている。
     */
    private PurchaseOrder createTestOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplier(testSupplier);
        order.setCurrency("JPY");
        order.setBuyerId(1L);

        // 技術的負債 #13: ハードコードされた日付 — テストが特定の日付に依存
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            order.setOrderDate(sdf.parse("2024-03-15"));
            order.setExpectedDeliveryDate(sdf.parse("2024-04-15"));
        } catch (Exception e) {
            order.setOrderDate(new Date());
        }

        // 明細の作成
        List<PurchaseOrderItem> items = new ArrayList<>();
        PurchaseOrderItem item1 = new PurchaseOrderItem();
        item1.setProduct(testProduct);
        item1.setQuantity(new BigDecimal("10"));
        item1.setUnitPrice(new BigDecimal("1000.00"));
        item1.setTaxRate(new BigDecimal("0.10"));
        items.add(item1);

        PurchaseOrderItem item2 = new PurchaseOrderItem();
        item2.setProduct(testProduct);
        item2.setQuantity(new BigDecimal("5"));
        item2.setUnitPrice(new BigDecimal("2000.00"));
        item2.setTaxRate(new BigDecimal("0.10"));
        items.add(item2);

        order.setItems(items);

        return order;
    }

    // ========================================================================
    // 発注作成テスト
    // ========================================================================

    @Test
    @DisplayName("発注作成 - 正常系: 基本的な発注作成が成功すること")
    void testCreateOrder_success() {
        // 技術的負債 #13: テスト日付がハードコードされている
        // 2024-03-15以降に実行すると意味が変わる可能性がある

        // Arrange
        when(em.find(Supplier.class, 1L)).thenReturn(testSupplier);

        // セッションコンテキストのモック設定
        when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testuser");

        // 発注番号採番のネイティブクエリモック
        Query nativeQuery = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);

        // 監査ログのモック（例外が発生しないようにする）
        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        PurchaseOrder result = service.createOrder(testOrder);

        // Assert
        assertNotNull(result);
        assertEquals("JPY", result.getCurrency());
        assertNotNull(result.getPoNumber());
        // 技術的負債 #13: 内部実装の詳細をverify — createOrderの内部動作に依存
        verify(em).persist(any(PurchaseOrder.class));
        verify(em).flush();
    }

    @Test
    @DisplayName("発注作成 - 異常系: null入力で例外がスローされること")
    void testCreateOrder_nullInput() {
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.createOrder(null);
        });
    }

    @Test
    @Disabled("予算チェックのロジック変更に伴い一時無効化")
    @DisplayName("発注作成 - 異常系: 予算超過時にBusinessExceptionがスローされること")
    void testCreateOrder_budgetExceeded() {
        // Arrange
        when(em.find(Supplier.class, 1L)).thenReturn(testSupplier);
        when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testuser");

        Query nativeQuery = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);

        // 予算チェック用のモック
        UserProfile buyer = new UserProfile();
        buyer.setId(1L);
        Department dept = new Department();
        dept.setId(10L);
        buyer.setDepartment(dept);
        when(em.find(UserProfile.class, 1L)).thenReturn(buyer);

        // 予算不足をシミュレート
        Query budgetQuery = mock(Query.class);
        when(em.createNamedQuery("Budget.findByDepartmentAndYear")).thenReturn(budgetQuery);
        when(budgetQuery.setParameter(anyString(), any())).thenReturn(budgetQuery);
        Budget budget = new Budget();
        budget.setTotalAmount(new BigDecimal("1000"));
        budget.setSpentAmount(new BigDecimal("999"));
        when(budgetQuery.getResultList()).thenReturn(Arrays.asList(budget));

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            service.createOrder(testOrder);
        });
    }

    // ========================================================================
    // 発注検索テスト
    // ========================================================================

    @Test
    @DisplayName("発注検索 - 正常系: IDで発注が見つかること")
    void testFindOrderById_found() {
        // Arrange
        PurchaseOrder expected = new PurchaseOrder();
        expected.setId(1L);
        expected.setPoNumber("PO-20240315-0001");
        expected.setStatus("DRAFT");
        when(em.find(PurchaseOrder.class, 1L)).thenReturn(expected);

        // Act
        PurchaseOrder result = service.findOrderById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PO-20240315-0001", result.getPoNumber());
    }

    @Test
    @DisplayName("発注検索 - 異常系: 存在しないIDでEntityNotFoundExceptionがスローされること")
    void testFindOrderById_notFound() {
        // Arrange
        when(em.find(PurchaseOrder.class, 999L)).thenReturn(null);

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            service.findOrderById(999L);
        });
    }

    // ========================================================================
    // 承認テスト
    // ========================================================================

    @Test
    @DisplayName("発注承認 - 正常系: 承認が成功すること")
    void testApproveOrder_success() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setPoNumber("PO-20240315-0001");
        order.setStatus("SUBMITTED");
        // 技術的負債 #13: ハードコードされた承認閾値（50000円）に依存
        order.setTotalAmount(new BigDecimal("30000"));
        order.setBuyerId(1L);

        when(em.find(PurchaseOrder.class, 1L)).thenReturn(order);
        when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("manager_user");

        // ワークフローのモック
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setStatus("PENDING");
        ApprovalStep step = new ApprovalStep();
        step.setId(10L);
        step.setStepOrder(1);
        step.setApproverId(0L);
        step.setComments("MANAGER");
        step.setStatus("PENDING");
        step.setWorkflow(workflow);
        workflow.setSteps(new ArrayList<>(Arrays.asList(step)));

        Query workflowQuery = mock(Query.class);
        when(em.createNamedQuery("ApprovalWorkflow.findByEntity")).thenReturn(workflowQuery);
        when(workflowQuery.setParameter(anyString(), any())).thenReturn(workflowQuery);
        when(workflowQuery.getResultList()).thenReturn(Arrays.asList(workflow));

        // ユーザー情報のモック
        Query userQuery = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(userQuery);
        when(userQuery.setParameter(anyString(), any())).thenReturn(userQuery);
        when(userQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act
        service.approveOrder(1L, 10L, "承認します");

        // Assert — 内部動作の検証（技術的負債: 実装詳細に依存）
        verify(em).merge(any(ApprovalStep.class));
    }

    @Test
    @DisplayName("発注承認 - 異常系: 権限不足で承認できないこと")
    void testApproveOrder_insufficientAuthority() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(2L);
        order.setPoNumber("PO-20240316-0001");
        order.setStatus("DRAFT"); // SUBMITTED以外のステータス

        when(em.find(PurchaseOrder.class, 2L)).thenReturn(order);

        // Act & Assert
        assertThrows(ApprovalException.class, () -> {
            service.approveOrder(2L, null, "承認します");
        });
    }

    @Test
    @Disabled("ステータスチェックの実装待ち")
    @DisplayName("発注承認 - 異常系: 既に承認済みの発注は再承認できないこと")
    void testApproveOrder_alreadyApproved() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(3L);
        order.setStatus("APPROVED");

        when(em.find(PurchaseOrder.class, 3L)).thenReturn(order);

        // Act & Assert
        assertThrows(ApprovalException.class, () -> {
            service.approveOrder(3L, null, "再承認テスト");
        });
    }

    // ========================================================================
    // 金額計算テスト
    // ========================================================================

    @Test
    @DisplayName("合計金額計算 - 正常系: 明細の合計が正しく計算されること")
    void testCalculateOrderTotal() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setCurrency("JPY");

        List<PurchaseOrderItem> items = new ArrayList<>();

        PurchaseOrderItem item1 = new PurchaseOrderItem();
        item1.setUnitPrice(new BigDecimal("1000"));
        item1.setQuantity(new BigDecimal("5"));
        item1.setTaxRate(new BigDecimal("0.10"));
        items.add(item1);

        PurchaseOrderItem item2 = new PurchaseOrderItem();
        item2.setUnitPrice(new BigDecimal("2000"));
        item2.setQuantity(new BigDecimal("3"));
        item2.setTaxRate(new BigDecimal("0.10"));
        items.add(item2);

        order.setItems(items);

        // Act
        BigDecimal total = service.calculateOrderTotal(order);

        // Assert
        // item1: 1000*5 = 5000, tax: 500, subtotal: 5500
        // item2: 2000*3 = 6000, tax: 600, subtotal: 6600
        // total: 12100
        assertNotNull(total);
        assertEquals(new BigDecimal("12100.00"), total);
    }

    @Test
    @DisplayName("合計金額計算 - 正常系: 税込計算で10%税率が正しく適用されること")
    void testCalculateOrderTotal_withTax() {
        // 技術的負債 #13: 税率10%がハードコードされた前提
        // 税率変更時にテストも更新が必要

        PurchaseOrder order = new PurchaseOrder();
        order.setCurrency("JPY");

        List<PurchaseOrderItem> items = new ArrayList<>();
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setUnitPrice(new BigDecimal("10000"));
        item.setQuantity(new BigDecimal("1"));
        // taxRateがnullの場合、JPYだとデフォルト10%が適用される
        item.setTaxRate(null);
        items.add(item);
        order.setItems(items);

        // Act
        BigDecimal total = service.calculateOrderTotal(order);

        // Assert
        // 10000 + 10000*0.10 = 11000
        assertEquals(new BigDecimal("11000.00"), total);
    }

    // ========================================================================
    // 検索テスト（コメントアウト）
    // ========================================================================

    // TODO: 検索条件の仕様変更後に修正
    // @Test
    // @DisplayName("発注検索 - 正常系: 検索条件に一致する発注が返されること")
    // void testSearchOrders() {
    //     // Arrange
    //     List<PurchaseOrder> expected = new ArrayList<>();
    //     PurchaseOrder po1 = new PurchaseOrder();
    //     po1.setId(1L);
    //     po1.setPoNumber("PO-20240315-0001");
    //     expected.add(po1);
    //
    //     Query searchQuery = mock(Query.class);
    //     when(em.createQuery(anyString())).thenReturn(searchQuery);
    //     when(searchQuery.setParameter(anyString(), any())).thenReturn(searchQuery);
    //     when(searchQuery.setFirstResult(anyInt())).thenReturn(searchQuery);
    //     when(searchQuery.setMaxResults(anyInt())).thenReturn(searchQuery);
    //     when(searchQuery.getResultList()).thenReturn(expected);
    //
    //     // Act
    //     List<PurchaseOrder> result = service.searchOrders(
    //         "PO-2024", "DRAFT", null, null, null, 0, 20);
    //
    //     // Assert
    //     assertNotNull(result);
    //     assertEquals(1, result.size());
    //     assertEquals("PO-20240315-0001", result.get(0).getPoNumber());
    // }

    // ========================================================================
    // 入庫処理テスト
    // ========================================================================

    @Test
    @DisplayName("入庫処理 - 正常系: 全量入庫が正しく処理されること")
    void testProcessGoodsReceipt_fullReceipt() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setStatus("APPROVED");
        order.setBuyerId(1L);

        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setId(10L);
        poItem.setQuantity(new BigDecimal("100"));
        poItem.setReceivedQuantity(BigDecimal.ZERO);
        poItem.setStatus("PENDING");
        poItem.setProduct(testProduct);
        order.setItems(new ArrayList<>(Arrays.asList(poItem)));

        when(em.find(PurchaseOrder.class, 1L)).thenReturn(order);
        when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("warehouse_user");

        // 入庫番号採番
        Query nativeQuery = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);

        // 入庫明細
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setWarehouseId(1L);

        GoodsReceiptItem receiptItem = new GoodsReceiptItem();
        receiptItem.setPurchaseOrderItemId(10L);
        receiptItem.setReceivedQuantity(100);
        receiptItem.setAcceptedQuantity(100);
        receipt.setReceiptItems(new ArrayList<>(Arrays.asList(receiptItem)));

        when(em.find(PurchaseOrderItem.class, 10L)).thenReturn(poItem);

        // Act
        GoodsReceipt result = service.processGoodsReceipt(1L, receipt);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getReceiptNumber());
        verify(em).persist(any(GoodsReceipt.class));
    }

    @Test
    @Disabled("部分入庫のステータス遷移テスト修正中")
    @DisplayName("入庫処理 - 正常系: 部分入庫でPARTIALLY_RECEIVEDステータスに変更されること")
    void testProcessGoodsReceipt_partialReceipt() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(2L);
        order.setStatus("APPROVED");

        PurchaseOrderItem poItem = new PurchaseOrderItem();
        poItem.setId(20L);
        poItem.setQuantity(new BigDecimal("100"));
        poItem.setReceivedQuantity(BigDecimal.ZERO);
        poItem.setStatus("PENDING");
        order.setItems(new ArrayList<>(Arrays.asList(poItem)));

        when(em.find(PurchaseOrder.class, 2L)).thenReturn(order);

        GoodsReceipt receipt = new GoodsReceipt();
        GoodsReceiptItem receiptItem = new GoodsReceiptItem();
        receiptItem.setPurchaseOrderItemId(20L);
        receiptItem.setReceivedQuantity(50); // 半分のみ入庫
        receipt.setReceiptItems(new ArrayList<>(Arrays.asList(receiptItem)));

        // Act
        GoodsReceipt result = service.processGoodsReceipt(2L, receipt);

        // Assert
        assertEquals("PARTIALLY_RECEIVED", order.getStatus());
    }

    // ========================================================================
    // バリデーションテスト
    // ========================================================================

    @Test
    @DisplayName("バリデーション - 正常系: 有効な発注がバリデーションを通過すること")
    void testValidateOrder() {
        // Act & Assert — 例外がスローされないことを確認
        assertDoesNotThrow(() -> {
            service.validateOrder(testOrder);
        });
    }

    @Test
    @DisplayName("バリデーション - 異常系: サプライヤー未指定でValidationExceptionがスローされること")
    void testValidateOrder_noSupplier() {
        // Arrange
        testOrder.setSupplier(null);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.validateOrder(testOrder);
        });
    }

    @Test
    @DisplayName("バリデーション - 異常系: 明細なしでValidationExceptionがスローされること")
    void testValidateOrder_noItems() {
        // Arrange
        testOrder.setItems(new ArrayList<>());

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.validateOrder(testOrder);
        });
    }

    // ========================================================================
    // キャンセルテスト
    // ========================================================================

    @Test
    @DisplayName("発注キャンセル - 正常系: DRAFT状態の発注がキャンセルできること")
    void testCancelOrder() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setPoNumber("PO-20240315-0001");
        order.setStatus("DRAFT");
        order.setBuyerId(1L);
        order.setTotalAmount(new BigDecimal("10000"));

        when(em.find(PurchaseOrder.class, 1L)).thenReturn(order);
        when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testuser");

        // Act
        service.cancelOrder(1L, "テストキャンセル");

        // Assert
        assertEquals("CANCELLED", order.getStatus());
        assertTrue(order.getNotes().contains("キャンセル理由"));
    }

    @Test
    @DisplayName("発注キャンセル - 異常系: COMPLETED状態の発注はキャンセルできないこと")
    void testCancelOrder_completed() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(2L);
        order.setStatus("COMPLETED");

        when(em.find(PurchaseOrder.class, 2L)).thenReturn(order);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            service.cancelOrder(2L, "キャンセル試行");
        });
    }

    // ========================================================================
    // 明細金額計算テスト
    // ========================================================================

    @Test
    @DisplayName("明細金額計算 - 正常系: 単価x数量が正しく計算されること")
    void testCalculateItemAmount_basic() {
        // Arrange
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setUnitPrice(new BigDecimal("1500"));
        item.setQuantity(new BigDecimal("10"));

        // Act
        BigDecimal amount = service.calculateItemAmount(item);

        // Assert
        assertEquals(new BigDecimal("15000.00"), amount);
    }

    @Test
    @DisplayName("明細金額計算 - 正常系: 割引が正しく適用されること")
    void testCalculateItemAmount_withDiscount() {
        // Arrange
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setUnitPrice(new BigDecimal("1000"));
        item.setQuantity(new BigDecimal("10"));
        item.setDiscount(new BigDecimal("0.10")); // 10%割引

        // Act
        BigDecimal amount = service.calculateItemAmount(item);

        // Assert
        // 1000 * 10 * (1 - 0.10) = 9000
        assertEquals(new BigDecimal("9000.00"), amount);
    }

    @Test
    @DisplayName("明細金額計算 - 境界値: nullアイテムの場合ZEROが返ること")
    void testCalculateItemAmount_null() {
        assertEquals(BigDecimal.ZERO, service.calculateItemAmount(null));
    }

    // ========================================================================
    // ステータス更新テスト
    // ========================================================================

    @Test
    @DisplayName("ステータス更新 - 異常系: COMPLETED状態からの変更は不可")
    void testUpdateStatus_fromCompleted() {
        // Arrange
        PurchaseOrder order = new PurchaseOrder();
        order.setId(1L);
        order.setStatus("COMPLETED");
        when(em.find(PurchaseOrder.class, 1L)).thenReturn(order);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            service.updateStatus(1L, "DRAFT");
        });
    }

    // ========================================================================
    // コメントアウトされたテスト群
    // 技術的負債 #13: 仕様変更や不安定さを理由に放置されたテスト
    // ========================================================================

    // TODO: findOrders のページネーションテスト
    // @Test
    // void testFindOrders_pagination() {
    //     // ページネーションの境界値テスト
    //     // 負のページ番号、0サイズ、MAX_PAGE_SIZE超過のケース
    // }

    // TODO: submitForApproval のテスト
    // @Test
    // void testSubmitForApproval_success() {
    //     PurchaseOrder order = new PurchaseOrder();
    //     order.setId(1L);
    //     order.setStatus("DRAFT");
    //     order.setTotalAmount(new BigDecimal("100000"));
    //     when(em.find(PurchaseOrder.class, 1L)).thenReturn(order);
    //     when(sessionContext.getCallerPrincipal()).thenReturn(principal);
    //     when(principal.getName()).thenReturn("buyer_user");
    //
    //     service.submitForApproval(1L);
    //
    //     assertEquals("SUBMITTED", order.getStatus());
    //     verify(em).persist(any(ApprovalWorkflow.class));
    // }

    // TODO: rejectOrder のテスト
    // @Test
    // void testRejectOrder_success() {
    //     // 却下処理のテスト
    // }

    // TODO: exportOrderToCsv のテスト
    // @Test
    // void testExportOrderToCsv() {
    //     // CSV出力のテスト — エスケープ処理の検証
    // }

    // TODO: getPreferredSuppliers のテスト
    // @Test
    // void testGetPreferredSuppliers() {
    //     // サプライヤーソートのテスト
    // }

    // TODO: compareSupplierPrices のN+1パターンテスト
    // @Test
    // void testCompareSupplierPrices() {
    //     // 価格比較テスト
    // }

    // TODO: 自動承認のテスト（50000円以下）
    // @Test
    // void testCreateOrder_autoApproval() {
    //     // 50000円以下で自動承認されることを確認
    // }
}
