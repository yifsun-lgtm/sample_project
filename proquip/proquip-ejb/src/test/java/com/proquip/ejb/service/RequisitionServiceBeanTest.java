package com.proquip.ejb.service;

import com.proquip.common.exception.ApprovalException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseRequisition;
import com.proquip.ejb.entity.procurement.PurchaseRequisitionItem;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;

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
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RequisitionServiceBeanの単体テスト。
 *
 * <p>技術的負債 #2 / #13: PurchaseOrderServiceBeanTestとほぼ同一構造のテスト。
 * コピペで作成されたことが明白。承認テストの金額閾値が
 * PurchaseOrderServiceBeanTestと異なる値を使用しており不整合。</p>
 *
 * @author ProQuip開発チーム
 */
@ExtendWith(MockitoExtension.class)
class RequisitionServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private AuditServiceBean auditService;

    @Mock
    private NotificationServiceBean notificationService;

    @Mock
    private PurchaseOrderServiceBean purchaseOrderService;

    @InjectMocks
    private RequisitionServiceBean service;

    private PurchaseRequisition testRequisition;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setSku("PRD-000001");
        testProduct.setName("テスト商品A");
        testProduct.setUnitPrice(new BigDecimal("5000.00"));

        testRequisition = createTestRequisition();
    }

    /**
     * テスト用の購買依頼オブジェクトを作成する。
     * 技術的負債 #2: PurchaseOrderServiceBeanTestのcreateTestOrderと類似のヘルパー。
     */
    private PurchaseRequisition createTestRequisition() {
        PurchaseRequisition req = new PurchaseRequisition();
        req.setId(1L);
        req.setReqNumber("REQ-20240320-0001");
        req.setStatus("DRAFT");
        req.setRequesterId(1L);
        // requestDate was removed from PurchaseRequisition (DDL alignment)

        List<PurchaseRequisitionItem> items = new ArrayList<>();
        PurchaseRequisitionItem item = new PurchaseRequisitionItem();
        item.setProduct(testProduct);
        item.setQuantity(10);
        item.setEstimatedUnitCost(new BigDecimal("5000.00"));
        item.setRequisition(req);
        items.add(item);

        req.setItems(items);
        return req;
    }

    // ========================================================================
    // 購買依頼作成テスト
    // ========================================================================

    @Test
    @DisplayName("購買依頼作成 - 正常系: 購買依頼が正しく作成されること")
    void testCreateRequisition_success() {
        // Arrange
        PurchaseRequisition newReq = new PurchaseRequisition();
        newReq.setRequesterId(1L);

        List<PurchaseRequisitionItem> items = new ArrayList<>();
        PurchaseRequisitionItem item = new PurchaseRequisitionItem();
        item.setProduct(testProduct);
        item.setQuantity(5);
        items.add(item);
        newReq.setItems(items);

        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        PurchaseRequisition result = service.createRequisition(newReq);

        // Assert
        assertNotNull(result);
        assertEquals("DRAFT", result.getStatus());
        assertNotNull(result.getReqNumber());
        verify(em).persist(newReq);
    }

    @Test
    @DisplayName("購買依頼作成 - 異常系: null入力でValidationExceptionがスローされること")
    void testCreateRequisition_nullInput() {
        assertThrows(ValidationException.class, () -> {
            service.createRequisition(null);
        });
    }

    @Test
    @DisplayName("購買依頼作成 - 異常系: 依頼者ID未指定でValidationExceptionがスローされること")
    void testCreateRequisition_noRequester() {
        // Arrange
        PurchaseRequisition req = new PurchaseRequisition();
        // requesterId未設定

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.createRequisition(req);
        });
    }

    // ========================================================================
    // 承認申請テスト
    // ========================================================================

    @Test
    @DisplayName("承認申請 - 正常系: DRAFT→SUBMITTEDに遷移すること")
    void testSubmitForApproval() {
        // Arrange
        when(em.find(PurchaseRequisition.class, 1L)).thenReturn(testRequisition);
        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        service.submitForApproval(1L);

        // Assert
        assertEquals("SUBMITTED", testRequisition.getStatus());
        verify(em).merge(testRequisition);
    }

    @Test
    @DisplayName("承認申請 - 異常系: DRAFT以外の状態からは承認申請できないこと")
    void testSubmitForApproval_invalidStatus() {
        // Arrange
        testRequisition.setStatus("APPROVED");
        when(em.find(PurchaseRequisition.class, 1L)).thenReturn(testRequisition);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.submitForApproval(1L);
        });
    }

    // ========================================================================
    // 承認テスト
    // 技術的負債 #2: PurchaseOrderServiceBeanTestのテストとほぼ同一だが、
    // 金額閾値が微妙に異なる（ここでは500万円でADMIN、POテストでは別の値を使用）
    // ========================================================================

    @Test
    @DisplayName("購買依頼承認 - 正常系: MANAGER権限で50万円以下の依頼を承認できること")
    void testApproveRequisition() {
        // 技術的負債 #13: ハードコードされた閾値
        // PurchaseOrderServiceBeanTestでは50000円閾値をテストしているが、
        // ここでは500000円を使用 — 不整合

        // Arrange
        testRequisition.setStatus("SUBMITTED");
        when(em.find(PurchaseRequisition.class, 1L)).thenReturn(testRequisition);
        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act — APPROVERロールで承認（50000円 < 1000000円なのでAPPROVER権限で十分）
        service.approveRequisition(1L, 100L, "APPROVER");

        // Assert
        assertEquals("APPROVED", testRequisition.getStatus());
    }

    @Test
    @DisplayName("購買依頼承認 - 異常系: SUBMITTED以外の状態は承認できないこと")
    void testApproveRequisition_invalidStatus() {
        // Arrange
        testRequisition.setStatus("DRAFT");
        when(em.find(PurchaseRequisition.class, 1L)).thenReturn(testRequisition);

        // Act & Assert
        assertThrows(ApprovalException.class, () -> {
            service.approveRequisition(1L, 100L, "ADMIN");
        });
    }

    // ========================================================================
    // 発注変換テスト
    // ========================================================================

    @Test
    @DisplayName("発注変換 - 正常系: 承認済み購買依頼が発注に変換されること")
    void testConvertToOrder() {
        // Arrange
        testRequisition.setStatus("APPROVED");
        when(em.find(PurchaseRequisition.class, 1L)).thenReturn(testRequisition);

        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("変換先サプライヤー");
        supplier.setStatus("ACTIVE");
        when(em.find(Supplier.class, 1L)).thenReturn(supplier);

        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        PurchaseOrder result = service.convertToOrder(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("DRAFT", result.getStatus());
        assertEquals("JPY", result.getCurrency());
        assertEquals("CONVERTED", testRequisition.getStatus());
        verify(em).persist(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("発注変換 - 異常系: 未承認の購買依頼は変換できないこと")
    void testConvertToOrder_notApproved() {
        // Arrange
        testRequisition.setStatus("SUBMITTED");
        when(em.find(PurchaseRequisition.class, 1L)).thenReturn(testRequisition);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.convertToOrder(1L, 1L);
        });
    }

    @Test
    @DisplayName("発注変換 - 異常系: サプライヤーID未指定でValidationExceptionがスローされること")
    void testConvertToOrder_noSupplier() {
        // Arrange
        testRequisition.setStatus("APPROVED");
        when(em.find(PurchaseRequisition.class, 1L)).thenReturn(testRequisition);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.convertToOrder(1L, null);
        });
    }
}
