package com.proquip.ejb.dao;

import com.proquip.ejb.entity.procurement.PurchaseOrder;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PurchaseOrderDaoの統合テスト。
 *
 * <p>技術的負債 #13: クラスレベルの@Disabledにより全テストが無効化されている。
 * ネイティブSQLを多用するDAOのため、テスト用データベースが必要だが未構築。</p>
 *
 * <p>注意: searchOrdersメソッドにSQLインジェクションの脆弱性あり。
 * テストケースにSQLインジェクションパターンが含まれているが、
 * 実行されていない（@Disabled）ため脆弱性は未検証のまま。</p>
 *
 * @author ProQuip開発チーム
 */
@Disabled("統合テスト環境の構築待ち — ネイティブSQLのテストにはH2 DBが必要")
@ExtendWith(MockitoExtension.class)
class PurchaseOrderDaoTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query nativeQuery;

    @Mock
    private TypedQuery<PurchaseOrder> typedQuery;

    @InjectMocks
    private PurchaseOrderDao dao;

    private PurchaseOrder testOrder1;
    private PurchaseOrder testOrder2;

    @BeforeEach
    void setUp() {
        testOrder1 = new PurchaseOrder();
        testOrder1.setId(1L);
        testOrder1.setPoNumber("PO-20240315-0001");
        testOrder1.setStatus("DRAFT");

        testOrder2 = new PurchaseOrder();
        testOrder2.setId(2L);
        testOrder2.setPoNumber("PO-20240316-0001");
        testOrder2.setStatus("APPROVED");
    }

    // ========================================================================
    // ステータス検索テスト
    // ========================================================================

    @Test
    @DisplayName("ステータス検索 - 正常系: 指定ステータスの発注が取得できること")
    void testFindByStatus() {
        // Arrange
        when(em.createNativeQuery(anyString(), eq(PurchaseOrder.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(Arrays.asList(testOrder1));

        // Act
        List<PurchaseOrder> result = dao.findByStatus("DRAFT");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DRAFT", result.get(0).getStatus());
    }

    // ========================================================================
    // 日付範囲検索テスト
    // ========================================================================

    @Test
    @DisplayName("日付範囲検索 - 正常系: 指定期間の発注が取得できること")
    void testFindByDateRange() throws Exception {
        // Arrange
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = sdf.parse("2024-01-01");
        Date endDate = sdf.parse("2024-12-31");

        when(em.createNativeQuery(anyString(), eq(PurchaseOrder.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(Arrays.asList(testOrder1, testOrder2));

        // Act
        List<PurchaseOrder> result = dao.findByDateRange(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========================================================================
    // SQLインジェクションテスト
    // 注意: searchOrdersメソッドにSQLインジェクションの脆弱性あり
    // ========================================================================

    @Test
    @DisplayName("検索 - セキュリティ: SQLインジェクションパターンが注入されないこと")
    void testSearchOrders_sqlInjection() {
        // 注意: このテストは@Disabled状態のクラスに含まれているため実行されない。
        // 実行された場合、searchOrdersメソッドの脆弱性により
        // SQLインジェクションが発生する可能性がある。

        // Arrange
        String maliciousInput = "'; DROP TABLE purchase_order; --";

        when(em.createNativeQuery(anyString(), eq(PurchaseOrder.class))).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act — 脆弱なsearchOrdersメソッドに悪意のある入力を渡す
        List<PurchaseOrder> result = dao.searchOrders(maliciousInput, null, null);

        // Assert — 本来はエラーまたは空結果を期待するが、
        // 脆弱性により予期しない動作をする可能性がある
        assertNotNull(result);
    }

    @Test
    @DisplayName("検索 - セキュリティ: UNION SELECTインジェクションパターン")
    void testSearchOrders_unionInjection() {
        // Arrange
        String maliciousInput = "' UNION SELECT * FROM user_profile WHERE '1'='1";

        when(em.createNativeQuery(anyString(), eq(PurchaseOrder.class))).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act
        List<PurchaseOrder> result = dao.searchOrders(null, maliciousInput, null);

        // Assert
        assertNotNull(result);
    }

    // ========================================================================
    // 発注番号検索テスト
    // ========================================================================

    @Test
    @DisplayName("発注番号検索 - 正常系: 発注番号で発注が取得できること")
    void testFindByPoNumber() {
        // Arrange
        when(em.createNativeQuery(anyString(), eq(PurchaseOrder.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(Arrays.asList(testOrder1));

        // Act
        PurchaseOrder result = dao.findByPoNumber("PO-20240315-0001");

        // Assert
        assertNotNull(result);
        assertEquals("PO-20240315-0001", result.getPoNumber());
    }

    @Test
    @DisplayName("発注番号検索 - 異常系: 存在しない発注番号でnullが返ること")
    void testFindByPoNumber_notFound() {
        // Arrange
        when(em.createNativeQuery(anyString(), eq(PurchaseOrder.class))).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyInt(), any())).thenReturn(nativeQuery);
        when(nativeQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act
        PurchaseOrder result = dao.findByPoNumber("PO-99999999-9999");

        // Assert
        assertNull(result);
    }

    // ========================================================================
    // 承認待ち検索テスト
    // ========================================================================

    @Test
    @DisplayName("承認待ち検索 - 正常系: SUBMITTED状態の発注が取得できること")
    void testFindPendingApproval() {
        // Arrange
        PurchaseOrder submittedOrder = new PurchaseOrder();
        submittedOrder.setId(3L);
        submittedOrder.setStatus("SUBMITTED");

        when(em.createQuery(anyString(), eq(PurchaseOrder.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Arrays.asList(submittedOrder));

        // Act
        List<PurchaseOrder> result = dao.findPendingApproval();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SUBMITTED", result.get(0).getStatus());
    }
}
