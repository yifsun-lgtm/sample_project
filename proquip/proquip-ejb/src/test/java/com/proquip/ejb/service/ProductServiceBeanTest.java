package com.proquip.ejb.service;

import com.proquip.common.exception.EntityNotFoundException;
import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.product.Category;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.product.ProductBundle;
import com.proquip.ejb.entity.product.ProductBundleItem;
import com.proquip.ejb.entity.product.ProductChangeLog;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductServiceBeanの単体テスト。
 *
 * <p>技術的負債 #13: カタログ管理・インポート処理のテストが欠如。
 * バンドル価格計算テストはハードコードされた価格に依存。</p>
 *
 * @author ProQuip開発チーム
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceBeanTest {

    @Mock
    private EntityManager em;

    @Mock
    private AuditServiceBean auditService;

    @Mock
    private NotificationServiceBean notificationService;

    @InjectMocks
    private ProductServiceBean service;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setSku("PRD-000001");
        testProduct.setName("テスト商品A");
        testProduct.setUnitPrice(new BigDecimal("1500.00"));
        testProduct.setStatus("PENDING");
        // active field was removed from Product; use status instead (DDL alignment)
    }

    // ========================================================================
    // 商品作成テスト
    // ========================================================================

    @Test
    @DisplayName("商品作成 - 正常系: 新規商品が正しく作成されること")
    void testCreateProduct_success() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setSku("NEW-000001");
        newProduct.setName("新規テスト商品");
        newProduct.setUnitPrice(new BigDecimal("2500.00"));

        // SKU重複チェック — 重複なし
        Query skuQuery = mock(Query.class);
        when(em.createNamedQuery("Product.findBySku")).thenReturn(skuQuery);
        when(skuQuery.setParameter(anyString(), any())).thenReturn(skuQuery);
        when(skuQuery.getResultList()).thenReturn(new ArrayList<>());

        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        Product result = service.createProduct(newProduct);

        // Assert
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        // active field was removed; status check is used instead (DDL alignment)
        verify(em).persist(newProduct);
    }

    @Test
    @DisplayName("商品作成 - 異常系: SKUが重複している場合はValidationExceptionがスローされること")
    void testCreateProduct_duplicateSku() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setSku("PRD-000001"); // 既存SKU
        newProduct.setName("重複SKUテスト");

        Query skuQuery = mock(Query.class);
        when(em.createNamedQuery("Product.findBySku")).thenReturn(skuQuery);
        when(skuQuery.setParameter(anyString(), any())).thenReturn(skuQuery);
        when(skuQuery.getResultList()).thenReturn(Arrays.asList(testProduct));

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.createProduct(newProduct);
        });
    }

    // ========================================================================
    // 商品更新テスト
    // ========================================================================

    @Test
    @DisplayName("商品更新 - 正常系: 商品情報が正しく更新されること")
    void testUpdateProduct_success() {
        // Arrange
        Product updateProduct = new Product();
        updateProduct.setId(1L);
        updateProduct.setName("更新後商品名");

        when(em.find(Product.class, 1L)).thenReturn(testProduct);
        when(em.merge(any(Product.class))).thenReturn(updateProduct);
        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        Product result = service.updateProduct(updateProduct);

        // Assert
        assertNotNull(result);
        verify(em).merge(any(Product.class));
    }

    // ========================================================================
    // 商品検索テスト
    // ========================================================================

    @Test
    @DisplayName("商品検索 - 正常系: キーワードで商品が検索できること")
    void testSearchProducts() {
        // Arrange
        Query searchQuery = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(searchQuery);
        when(searchQuery.setParameter(anyString(), any())).thenReturn(searchQuery);

        List<Product> expected = new ArrayList<>();
        expected.add(testProduct);
        when(searchQuery.getResultList()).thenReturn(expected);

        // Act
        List<Product> result = service.searchProducts("テスト", null, null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("テスト商品A", result.get(0).getName());
    }

    // ========================================================================
    // 商品詳細取得テスト
    // ========================================================================

    @Test
    @Disabled("Lazy Loading問題のため統合テスト環境で実行する必要あり")
    @DisplayName("商品詳細 - 正常系: 商品の全詳細情報が取得できること")
    void testGetProductWithFullDetails() {
        // Arrange
        when(em.find(Product.class, 1L)).thenReturn(testProduct);

        // Act
        Map<String, Object> catalog = service.getProductCatalog(1L);

        // Assert
        assertNotNull(catalog);
        assertNotNull(catalog.get("product"));
    }

    // ========================================================================
    // ステータス変更テスト
    // ========================================================================

    @Test
    @DisplayName("ステータス変更 - 正常系: PENDINGからACTIVEへ変更できること")
    void testUpdateProductStatus() {
        // Arrange
        when(em.find(Product.class, 1L)).thenReturn(testProduct);
        doNothing().when(auditService).logAction(anyString(), any(), anyString(), any(), any(), any());

        // Act
        service.changeStatus(1L, "ACTIVE");

        // Assert
        assertEquals("ACTIVE", testProduct.getStatus());
        assertEquals("ACTIVE", testProduct.getStatus());
        verify(em).merge(testProduct);
    }

    @Test
    @DisplayName("ステータス変更 - 異常系: DISCONTINUEDからの変更はエラー")
    void testUpdateProductStatus_fromDiscontinued() {
        // Arrange
        testProduct.setStatus("DISCONTINUED");
        when(em.find(Product.class, 1L)).thenReturn(testProduct);

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            service.changeStatus(1L, "ACTIVE");
        });
    }

    // ========================================================================
    // バンドル価格計算テスト
    // ========================================================================

    @Test
    @DisplayName("バンドル価格 - 正常系: バンドル商品の合計価格が正しく計算されること")
    void testCalculateBundlePrice() {
        // 技術的負債 #13: ハードコードされた価格に依存するテスト

        // Arrange
        Product product1 = new Product();
        product1.setUnitPrice(new BigDecimal("1000.00"));

        Product product2 = new Product();
        product2.setUnitPrice(new BigDecimal("2000.00"));

        ProductBundleItem bundleItem1 = new ProductBundleItem();
        bundleItem1.setProduct(product1);
        bundleItem1.setQuantity(2);

        ProductBundleItem bundleItem2 = new ProductBundleItem();
        bundleItem2.setProduct(product2);
        bundleItem2.setQuantity(3);

        ProductBundle bundle = new ProductBundle();
        bundle.setId(1L);
        bundle.setDiscount(new BigDecimal("10")); // 10%割引
        bundle.setBundleItems(new ArrayList<>(Arrays.asList(bundleItem1, bundleItem2)));

        when(em.find(ProductBundle.class, 1L)).thenReturn(bundle);

        // Act
        Map<String, BigDecimal> result = service.calculateBundlePrice(1L);

        // Assert
        // product1: 1000 * 2 = 2000
        // product2: 2000 * 3 = 6000
        // total: 8000
        // discount: 8000 * 0.10 = 800
        // discountedPrice: 8000 - 800 = 7200
        assertNotNull(result);
        assertEquals(new BigDecimal("8000.00"), result.get("totalPrice"));
        assertEquals(new BigDecimal("800.00"), result.get("discountAmount"));
        assertEquals(new BigDecimal("7200.00"), result.get("discountedPrice"));
    }

    // ========================================================================
    // コメントアウトされたテスト
    // ========================================================================

    // 商品インポートテスト — ImportExportServiceBeanとの重複のため保留
    // @Test
    // @DisplayName("商品インポート - 正常系: CSVデータから商品が正しくインポートされること")
    // void testImportProducts() {
    //     // Arrange
    //     List<Map<String, String>> importData = new ArrayList<>();
    //     Map<String, String> row1 = new HashMap<>();
    //     row1.put("sku", "IMP-000001");
    //     row1.put("name", "インポート商品1");
    //     row1.put("unitPrice", "3000");
    //     importData.add(row1);
    //
    //     Query skuQuery = mock(Query.class);
    //     when(em.createNamedQuery("Product.findBySku")).thenReturn(skuQuery);
    //     when(skuQuery.setParameter(anyString(), any())).thenReturn(skuQuery);
    //     when(skuQuery.getResultList()).thenReturn(new ArrayList<>());
    //
    //     // Act
    //     Map<String, Object> result = service.importProducts(importData);
    //
    //     // Assert
    //     assertEquals(1, result.get("successCount"));
    //     assertEquals(0, result.get("failCount"));
    // }

    // TODO: CSV出力テスト
    // @Test
    // void testExportProductsToCsv() {
    //     // CSV出力のテスト
    // }
}
