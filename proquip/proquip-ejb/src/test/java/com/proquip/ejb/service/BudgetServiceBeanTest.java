package com.proquip.ejb.service;

import com.proquip.common.exception.BusinessException;
import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.pricing.Budget;
import com.proquip.ejb.entity.pricing.BudgetLineItem;

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
 * BudgetServiceBeanの単体テスト。
 *
 * <p>技術的負債 #13: 予算消化・戻し、利用状況レポートのテストが欠如。
 * 年度クローズのテストは@Disabledになっている。</p>
 *
 * @author ProQuip開発チーム
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private AuditServiceBean auditService;

    @Mock
    private NotificationServiceBean notificationService;

    @InjectMocks
    private BudgetServiceBean service;

    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setName("2024年度 IT部門予算");
        testBudget.setFiscalYear(2024);
        testBudget.setDepartmentId(10L);
        testBudget.setTotalAmount(new BigDecimal("10000000"));
        testBudget.setAllocatedAmount(new BigDecimal("5000000"));
        testBudget.setSpentAmount(new BigDecimal("3000000"));
        testBudget.setStatus("ACTIVE");
        testBudget.setLineItems(new ArrayList<>());
    }

    // ========================================================================
    // 予算配賦テスト
    // ========================================================================

    @Test
    @DisplayName("予算配賦 - 正常系: 予算明細が正しく追加されること")
    void testAllocateBudget() {
        // Arrange
        when(em.find(Budget.class, 1L)).thenReturn(testBudget);
        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // allocatedAmount=5000000, totalAmount=10000000 → remaining=5000000
        // 配賦額: 1000000 → OK

        // Act
        BudgetLineItem result = service.allocateBudget(
                1L, "サーバー購入費", new BigDecimal("1000000"), null);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("1000000"), result.getAllocatedAmount());
        assertEquals(new BigDecimal("6000000"), testBudget.getAllocatedAmount());
        verify(em).persist(any(BudgetLineItem.class));
    }

    @Test
    @DisplayName("予算配賦 - 異常系: 残高超過でBusinessExceptionがスローされること")
    void testAllocateBudget_exceedsRemaining() {
        // Arrange
        when(em.find(Budget.class, 1L)).thenReturn(testBudget);

        // remaining = 10000000 - 5000000 = 5000000
        // 配賦額: 6000000 → 超過

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            service.allocateBudget(1L, "大型投資", new BigDecimal("6000000"), null);
        });
    }

    // ========================================================================
    // 予算チェックテスト
    // ========================================================================

    @Test
    @DisplayName("予算チェック - 正常系: 十分な残高がある場合にtrueが返ること")
    void testCheckAvailability_sufficient() {
        // Arrange
        when(em.find(Budget.class, 1L)).thenReturn(testBudget);

        // spentAmount=3000000, totalAmount=10000000 → remaining=7000000
        // requestedAmount=5000000 → OK

        // Act
        boolean result = service.checkBudgetAvailability(1L, new BigDecimal("5000000"));

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("予算チェック - 異常系: 残高不足の場合にfalseが返ること")
    void testCheckAvailability_exceeded() {
        // Arrange
        when(em.find(Budget.class, 1L)).thenReturn(testBudget);

        // remaining=7000000, requestedAmount=8000000 → 不足

        // Act
        boolean result = service.checkBudgetAvailability(1L, new BigDecimal("8000000"));

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("予算チェック - 異常系: ACTIVE以外のステータスではfalseが返ること")
    void testCheckAvailability_inactiveStatus() {
        // Arrange
        testBudget.setStatus("FROZEN");
        when(em.find(Budget.class, 1L)).thenReturn(testBudget);

        // Act
        boolean result = service.checkBudgetAvailability(1L, new BigDecimal("1000"));

        // Assert
        assertFalse(result);
    }

    // ========================================================================
    // 予算引当テスト
    // ========================================================================

    @Test
    @Disabled("楽観ロックのテスト環境が未整備")
    @DisplayName("予算引当 - 正常系: 消化額が正しく加算されること")
    void testReserveAmount() {
        // Arrange
        when(em.find(Budget.class, 1L)).thenReturn(testBudget);

        // Act
        service.spendBudget(1L, new BigDecimal("500000"), "PO-20240315-0001");

        // Assert
        assertEquals(new BigDecimal("3500000"), testBudget.getSpentAmount());
        verify(em).merge(testBudget);
    }

    // ========================================================================
    // 年度クローズテスト
    // ========================================================================

    @Test
    @DisplayName("年度クローズ - 正常系: 指定年度の全予算がCLOSEDに変更されること")
    void testCloseFiscalYear() {
        // Arrange
        Budget budget1 = new Budget();
        budget1.setId(1L);
        budget1.setStatus("ACTIVE");
        budget1.setFiscalYear(2023);

        Budget budget2 = new Budget();
        budget2.setId(2L);
        budget2.setStatus("FROZEN");
        budget2.setFiscalYear(2023);

        Budget budget3 = new Budget();
        budget3.setId(3L);
        budget3.setStatus("CLOSED"); // 既にクローズ済み
        budget3.setFiscalYear(2023);

        Query yearQuery = mock(Query.class);
        when(em.createNamedQuery("Budget.findByFiscalYear")).thenReturn(yearQuery);
        when(yearQuery.setParameter(anyString(), any())).thenReturn(yearQuery);
        when(yearQuery.getResultList()).thenReturn(Arrays.asList(budget1, budget2, budget3));

        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        int closedCount = service.closeFiscalYear(2023);

        // Assert — 既にCLOSEDの1件を除く2件がクローズされること
        assertEquals(2, closedCount);
        assertEquals("CLOSED", budget1.getStatus());
        assertEquals("CLOSED", budget2.getStatus());
    }
}
