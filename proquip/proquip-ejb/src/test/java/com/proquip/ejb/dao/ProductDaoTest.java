package com.proquip.ejb.dao;

import com.proquip.ejb.entity.product.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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
 * ProductDaoの統合テスト。
 *
 * <p>技術的負債 #13: クラスレベルの@Disabledにより全テストが無効化されている。
 * 統合テスト環境（Arquillian/テスト用DB）の構築が完了していないため、
 * Criteria APIクエリの実行テストが一切行えない状態。</p>
 *
 * <p>モックベースのテストも一部記述されているが、Criteria APIのモックが
 * 過度に複雑で可読性が低く、実質的な検証になっていない。</p>
 *
 * @author ProQuip開発チーム
 */
@Disabled("統合テスト環境の構築待ち — Arquillian + H2のセットアップが必要")
@ExtendWith(MockitoExtension.class)
class ProductDaoTest {

    @Mock
    private EntityManager em;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private CriteriaQuery<Product> cq;

    @Mock
    private Root<Product> root;

    @Mock
    private TypedQuery<Product> typedQuery;

    @Mock
    private Predicate predicate;

    @InjectMocks
    private ProductDao dao;

    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setSku("PRD-000001");
        testProduct1.setName("テスト商品A");
        testProduct1.setUnitPrice(new BigDecimal("1000.00"));
        testProduct1.setStatus("ACTIVE");

        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setSku("PRD-000002");
        testProduct2.setName("テスト商品B");
        testProduct2.setUnitPrice(new BigDecimal("2500.00"));
        testProduct2.setStatus("ACTIVE");
    }

    // ========================================================================
    // カテゴリ検索テスト
    // ========================================================================

    @Test
    @DisplayName("カテゴリ検索 - 正常系: 指定カテゴリの商品が取得できること")
    void testFindByCategory() {
        // Arrange
        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Product.class)).thenReturn(cq);
        when(cq.from(Product.class)).thenReturn(root);
        when(cq.select(root)).thenReturn(cq);
        when(em.createQuery(cq)).thenReturn(typedQuery);

        List<Product> expected = new ArrayList<>(Arrays.asList(testProduct1, testProduct2));
        when(typedQuery.getResultList()).thenReturn(expected);

        // Act
        List<Product> result = dao.findByCategory(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========================================================================
    // キーワード検索テスト
    // ========================================================================

    @Test
    @DisplayName("キーワード検索 - 正常系: キーワードに一致する商品が取得できること")
    void testSearchByKeyword() {
        // Arrange
        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Product.class)).thenReturn(cq);
        when(cq.from(Product.class)).thenReturn(root);
        when(em.createQuery(cq)).thenReturn(typedQuery);

        List<Product> expected = new ArrayList<>(Arrays.asList(testProduct1));
        when(typedQuery.getResultList()).thenReturn(expected);

        // Act
        List<Product> result = dao.searchByKeyword("テスト商品A");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PRD-000001", result.get(0).getSku());
    }

    // ========================================================================
    // 有効商品取得テスト
    // ========================================================================

    @Test
    @DisplayName("有効商品取得 - 正常系: アクティブな商品のみ取得できること")
    void testFindActiveProducts() {
        // Arrange
        when(em.createNamedQuery("Product.findActiveProducts", Product.class)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Arrays.asList(testProduct1, testProduct2));

        // Act
        List<Product> result = dao.findActiveProducts();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ========================================================================
    // SKU検索テスト
    // ========================================================================

    @Test
    @DisplayName("SKU検索 - 正常系: SKUコードで商品が取得できること")
    void testFindBySkuCode() {
        // Arrange
        when(em.createNamedQuery("Product.findBySku", Product.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("sku", "PRD-000001")).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Arrays.asList(testProduct1));

        // Act
        Product result = dao.findBySkuCode("PRD-000001");

        // Assert
        assertNotNull(result);
        assertEquals("PRD-000001", result.getSku());
        assertEquals("テスト商品A", result.getName());
    }

    @Test
    @DisplayName("SKU検索 - 異常系: 存在しないSKUでnullが返ること")
    void testFindBySkuCode_notFound() {
        // Arrange
        when(em.createNamedQuery("Product.findBySku", Product.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("sku", "NONEXISTENT")).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(new ArrayList<>());

        // Act
        Product result = dao.findBySkuCode("NONEXISTENT");

        // Assert
        assertNull(result);
    }

    // ========================================================================
    // 複合条件検索テスト
    // ========================================================================

    @Test
    @DisplayName("複合条件検索 - 正常系: ステータスと価格帯で検索できること")
    void testFindByStatusAndPriceRange() {
        // Arrange
        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Product.class)).thenReturn(cq);
        when(cq.from(Product.class)).thenReturn(root);
        when(em.createQuery(cq)).thenReturn(typedQuery);

        List<Product> expected = new ArrayList<>(Arrays.asList(testProduct1));
        when(typedQuery.getResultList()).thenReturn(expected);

        // Act
        List<Product> result = dao.findByStatusAndPriceRange(
                "ACTIVE", new BigDecimal("500"), new BigDecimal("2000"));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
