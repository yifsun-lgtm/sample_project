package com.proquip.ejb.service;

import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.supplier.Supplier;
import com.proquip.ejb.entity.supplier.SupplierContract;
import com.proquip.ejb.entity.supplier.SupplierRating;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SupplierServiceBeanの単体テスト。
 *
 * <p>技術的負債 #13: 契約管理、CSV出力、納品実績のテストが欠如。
 * 評価ロジックのテストは見直し中で@Disabledになっている。</p>
 *
 * @author ProQuip開発チーム
 */
@ExtendWith(MockitoExtension.class)
class SupplierServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private AuditServiceBean auditService;

    @Mock
    private NotificationServiceBean notificationService;

    @InjectMocks
    private SupplierServiceBean service;

    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        testSupplier = new Supplier();
        testSupplier.setId(1L);
        testSupplier.setCode("SUP-001");
        testSupplier.setName("テストサプライヤー株式会社");
        testSupplier.setStatus("ACTIVE");
        // rating and paymentTermDays fields were removed from Supplier (DDL alignment)
    }

    // ========================================================================
    // 仕入先作成テスト
    // ========================================================================

    @Test
    @DisplayName("仕入先作成 - 正常系: 新規仕入先が正しく作成されること")
    void testCreateSupplier_success() {
        // Arrange
        Supplier newSupplier = new Supplier();
        newSupplier.setCode("SUP-NEW");
        newSupplier.setName("新規サプライヤー");

        // コード重複チェック — 重複なし
        Query codeQuery = mock(Query.class);
        when(em.createNamedQuery("Supplier.findByCode")).thenReturn(codeQuery);
        when(codeQuery.setParameter(anyString(), any())).thenReturn(codeQuery);
        when(codeQuery.getResultList()).thenReturn(new ArrayList<>());

        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        Supplier result = service.createSupplier(newSupplier);

        // Assert
        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.getStatus());
        verify(em).persist(newSupplier);
    }

    @Test
    @DisplayName("仕入先作成 - 異常系: null入力でValidationExceptionがスローされること")
    void testCreateSupplier_null() {
        assertThrows(ValidationException.class, () -> {
            service.createSupplier(null);
        });
    }

    // ========================================================================
    // 仕入先検索テスト
    // ========================================================================

    @Test
    @DisplayName("仕入先検索 - 正常系: ACTIVEステータスの仕入先が取得できること")
    void testFindActiveSuppliers() {
        // Arrange
        Query statusQuery = mock(Query.class);
        when(em.createNamedQuery("Supplier.findByStatus")).thenReturn(statusQuery);
        when(statusQuery.setParameter(anyString(), any())).thenReturn(statusQuery);

        List<Supplier> expected = new ArrayList<>();
        expected.add(testSupplier);
        when(statusQuery.getResultList()).thenReturn(expected);

        // Act
        List<Supplier> result = service.findByStatus("ACTIVE");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("テストサプライヤー株式会社", result.get(0).getName());
    }

    // ========================================================================
    // 評価テスト
    // ========================================================================

    @Test
    @DisplayName("仕入先評価 - 正常系: 評価が正しく登録されること")
    void testRateSupplier() {
        // Arrange
        when(em.find(Supplier.class, 1L)).thenReturn(testSupplier);

        // 既存評価の取得（平均計算用）
        Query ratingQuery = mock(Query.class);
        when(em.createNamedQuery("SupplierRating.findBySupplier")).thenReturn(ratingQuery);
        when(ratingQuery.setParameter(anyString(), any())).thenReturn(ratingQuery);
        when(ratingQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act
        SupplierRating result = service.addRating(
                1L,
                new BigDecimal("4.00"),
                new BigDecimal("3.50"),
                new BigDecimal("4.50"),
                new BigDecimal("4.00"),
                "良好なサプライヤー",
                100L);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("4.00"), result.getOverallScore());
        verify(em).persist(any(SupplierRating.class));
    }

    @Test
    @DisplayName("仕入先評価 - 異常系: スコア範囲外でValidationExceptionがスローされること")
    void testRateSupplier_invalidScore() {
        // Arrange
        when(em.find(Supplier.class, 1L)).thenReturn(testSupplier);

        // Act & Assert — 品質スコアが範囲外（6.00 > 5.00）
        assertThrows(ValidationException.class, () -> {
            service.addRating(1L,
                    new BigDecimal("6.00"), // 範囲外
                    new BigDecimal("3.00"),
                    new BigDecimal("3.00"),
                    new BigDecimal("3.00"),
                    "テスト", 100L);
        });
    }

    @Test
    @Disabled("評価ロジック見直し中 — 加重平均の導入検討")
    @DisplayName("仕入先評価 - 正常系: 総合評価が正しく再計算されること")
    void testEvaluateSupplier() {
        // Arrange
        when(em.find(Supplier.class, 1L)).thenReturn(testSupplier);

        // 既存評価の取得
        SupplierRating rating1 = new SupplierRating();
        rating1.setOverallScore(new BigDecimal("4.00"));
        SupplierRating rating2 = new SupplierRating();
        rating2.setOverallScore(new BigDecimal("3.00"));

        Query ratingQuery = mock(Query.class);
        when(em.createNamedQuery("SupplierRating.findBySupplier")).thenReturn(ratingQuery);
        when(ratingQuery.setParameter(anyString(), any())).thenReturn(ratingQuery);
        when(ratingQuery.getResultList()).thenReturn(Arrays.asList(rating1, rating2));

        // Act
        SupplierRating newRating = service.addRating(1L,
                new BigDecimal("5.00"),
                new BigDecimal("4.00"),
                new BigDecimal("3.00"),
                new BigDecimal("4.00"),
                "再評価", 100L);

        // Assert — 全評価の平均が仕入先の総合評価に反映されること
        // rating1(4.00) + rating2(3.00) + newRating(4.00) / 3 = 3.67
        // rating field was removed from Supplier (DDL alignment); rating is on SupplierRating entity
    }

    // ========================================================================
    // パフォーマンスレポートテスト
    // ========================================================================

    @Test
    @DisplayName("パフォーマンスレポート - 正常系: レポートが正しく生成されること")
    void testGetSupplierPerformanceReport() {
        // Arrange
        when(em.find(Supplier.class, 1L)).thenReturn(testSupplier);

        // 発注件数クエリ
        Query countQuery = mock(Query.class);
        Query totalQuery = mock(Query.class);
        Query ratingQuery = mock(Query.class);
        Query contractQuery = mock(Query.class);
        Query completedQuery = mock(Query.class);

        when(em.createQuery(contains("COUNT(po)"))).thenReturn(countQuery);
        when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(10L).thenReturn(8L);

        when(em.createQuery(contains("SUM(po.totalAmount)"))).thenReturn(totalQuery);
        when(totalQuery.setParameter(anyString(), any())).thenReturn(totalQuery);
        when(totalQuery.getSingleResult()).thenReturn(new BigDecimal("5000000"));

        when(em.createNamedQuery("SupplierRating.findLatestBySupplier")).thenReturn(ratingQuery);
        when(ratingQuery.setParameter(anyString(), any())).thenReturn(ratingQuery);
        when(ratingQuery.setMaxResults(anyInt())).thenReturn(ratingQuery);
        when(ratingQuery.getResultList()).thenReturn(new ArrayList<>());

        when(em.createQuery(contains("sc.status = 'ACTIVE'"))).thenReturn(contractQuery);
        when(contractQuery.setParameter(anyString(), any())).thenReturn(contractQuery);
        when(contractQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> report = service.getPerformanceReport(1L);

        // Assert
        assertNotNull(report);
        assertEquals("テストサプライヤー株式会社", report.get("supplierName"));
        assertEquals("SUP-001", report.get("supplierCode"));
        assertEquals(10L, report.get("totalOrders"));
    }
}
