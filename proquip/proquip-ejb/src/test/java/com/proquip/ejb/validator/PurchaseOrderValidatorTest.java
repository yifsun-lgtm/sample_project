package com.proquip.ejb.validator;

import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;
import com.proquip.ejb.entity.product.Product;
import com.proquip.ejb.entity.supplier.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PurchaseOrderValidatorの単体テスト。
 *
 * <p>技術的負債 #13: テストが脆弱（fragile）で不十分。
 * <ul>
 *   <li>ハードコードされた金額上限（999,999.99）に依存するテスト</li>
 *   <li>SKUフォーマット検証のテストがPurchaseOrderValidator側のパターンのみ検証
 *       （ProductValidator側との不整合は未テスト）</li>
 *   <li>エッジケーステスト（日付検証の境界値、通貨コード検証等）が@Disabled</li>
 *   <li>validateItems、validateAmounts、validateDatesの個別テストが不足</li>
 *   <li>テスト用のヘルパーメソッドが冗長（createValidOrder内でフィールドを手動設定）</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
class PurchaseOrderValidatorTest {

    private PurchaseOrderValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PurchaseOrderValidator();
    }

    /**
     * テスト用の有効な発注オブジェクトを作成する。
     *
     * <p>技術的負債 #13: テストデータのセットアップが冗長。
     * TestDataBuilder パターンを使用すべき。</p>
     */
    private PurchaseOrder createValidOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setPoNumber("PO-20240315-0001");
        order.setStatus("DRAFT");
        order.setCurrency("JPY");
        order.setBuyerId(1L);
        order.setOrderDate(new Date());
        order.setTotalAmount(new BigDecimal("50000.00"));

        // サプライヤー
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setStatus("ACTIVE");
        order.setSupplier(supplier);

        // 明細
        List<PurchaseOrderItem> items = new ArrayList<>();
        PurchaseOrderItem item = new PurchaseOrderItem();
        // lineNumber was removed from PurchaseOrderItem (DDL alignment)
        item.setQuantity(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("5000.00"));
        item.setSubtotal(new BigDecimal("50000.00"));

        Product product = new Product();
        product.setSku("PRD-000001");
        item.setProduct(product);

        items.add(item);
        order.setItems(items);

        return order;
    }

    // ========================================================================
    // validate - 正常系テスト
    // ========================================================================

    @Test
    @DisplayName("バリデーション - 正常系: 有効な発注でエラーが0件であること")
    void testValidate_validOrder() {
        // Arrange
        PurchaseOrder order = createValidOrder();

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        assertNotNull(errors);
        assertTrue(errors.isEmpty(),
                "有効な発注でエラーが発生: " + String.join(", ", errors));
    }

    @Test
    @DisplayName("バリデーション - 異常系: nullの場合はエラーが返ること")
    void testValidate_nullOrder() {
        // Act
        List<String> errors = validator.validate(null);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("null"));
    }

    // ========================================================================
    // 金額バリデーションテスト
    // 技術的負債 #13 / #17: ハードコードされた金額閾値
    // ========================================================================

    @Test
    @DisplayName("金額検証 - 異常系: 最小金額（1,000円）未満でエラーが返ること")
    void testValidate_amountTooLow() {
        // Arrange — 技術的負債 #4: ハードコードされた MIN_ORDER_AMOUNT = 1000
        PurchaseOrder order = createValidOrder();
        order.setTotalAmount(new BigDecimal("999.99"));

        // 明細の小計も合わせる
        order.getItems().get(0).setSubtotal(new BigDecimal("999.99"));

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        assertFalse(errors.isEmpty());
        boolean hasAmountError = errors.stream()
                .anyMatch(e -> e.contains("1000") || e.contains("以上"));
        assertTrue(hasAmountError, "最小金額エラーが含まれていない: " + errors);
    }

    @Test
    @DisplayName("金額検証 - 異常系: 最大金額（999,999.99円）超過でエラーが返ること")
    void testValidate_amountTooHigh() {
        // Arrange — 技術的負債 #17: MAX_ORDER_AMOUNT = 999999.99
        // フロントエンドでは1,000,000まで入力可能（不整合）
        PurchaseOrder order = createValidOrder();
        order.setTotalAmount(new BigDecimal("1000000.00"));

        // 明細の小計も合わせる
        order.getItems().get(0).setSubtotal(new BigDecimal("1000000.00"));
        order.getItems().get(0).setQuantity(new BigDecimal("200"));
        order.getItems().get(0).setUnitPrice(new BigDecimal("5000.00"));

        // Act
        List<String> errors = validator.validate(order);

        // Assert — 技術的負債 #17: 999,999.99のハードコード
        assertFalse(errors.isEmpty());
        boolean hasMaxAmountError = errors.stream()
                .anyMatch(e -> e.contains("999999.99") || e.contains("exceeds") || e.contains("maximum"));
        assertTrue(hasMaxAmountError,
                "最大金額超過エラーが含まれていない: " + errors);
    }

    @Test
    @DisplayName("金額検証 - 境界値: ちょうど最小金額で通過すること")
    void testValidate_amountExactMinimum() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.getItems().get(0).setSubtotal(new BigDecimal("1000.00"));
        order.getItems().get(0).setQuantity(new BigDecimal("1"));
        order.getItems().get(0).setUnitPrice(new BigDecimal("1000.00"));

        // Act
        List<String> errors = validator.validate(order);

        // Assert — 金額関連のエラーがないことを確認
        boolean hasAmountError = errors.stream()
                .anyMatch(e -> e.contains("金額") || e.contains("amount"));
        assertFalse(hasAmountError,
                "最小金額ちょうどでエラーが発生: " + errors);
    }

    // ========================================================================
    // SKUフォーマットテスト
    // 技術的負債 #17: バックエンドとフロントエンドでSKUパターンが異なる
    // ========================================================================

    @Test
    @DisplayName("SKU検証 - 正常系: 有効なSKUフォーマットでエラーがないこと")
    void testValidate_validSku() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        // SKU_PATTERN = [A-Z]{3}-[0-9]{6}
        order.getItems().get(0).getProduct().setSku("ABC-123456");

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasSkuError = errors.stream()
                .anyMatch(e -> e.contains("SKU"));
        assertFalse(hasSkuError, "有効なSKUでエラーが発生: " + errors);
    }

    @Test
    @DisplayName("SKU検証 - 異常系: 不正なSKUフォーマットでエラーが返ること")
    void testValidate_invalidSku() {
        // Arrange — 技術的負債 #17: 大文字のみ許可だがフロントエンドは小文字も許可
        PurchaseOrder order = createValidOrder();
        order.getItems().get(0).getProduct().setSku("abc-123456"); // 小文字

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasSkuError = errors.stream()
                .anyMatch(e -> e.contains("SKU"));
        assertTrue(hasSkuError, "不正なSKUでエラーが検出されない: " + errors);
    }

    @Test
    @DisplayName("SKU検証 - 異常系: 桁数不足のSKUでエラーが返ること")
    void testValidate_skuTooShort() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.getItems().get(0).getProduct().setSku("AB-12345");

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasSkuError = errors.stream()
                .anyMatch(e -> e.contains("SKU"));
        assertTrue(hasSkuError, "短いSKUでエラーが検出されない: " + errors);
    }

    // ========================================================================
    // 明細数テスト
    // ========================================================================

    @Test
    @DisplayName("明細数検証 - 異常系: 明細が0件でエラーが返ること")
    void testValidate_noItems() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setItems(new ArrayList<>());

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasItemError = errors.stream()
                .anyMatch(e -> e.contains("明細") || e.contains("1件以上"));
        assertTrue(hasItemError, "明細0件でエラーが検出されない: " + errors);
    }

    @Test
    @DisplayName("明細数検証 - 異常系: 明細が100件を超える場合にエラーが返ること")
    void testValidate_tooManyItems() {
        // Arrange — MAX_ITEMS_PER_ORDER = 100
        PurchaseOrder order = createValidOrder();
        List<PurchaseOrderItem> manyItems = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            // lineNumber was removed from PurchaseOrderItem (DDL alignment)
            item.setQuantity(new BigDecimal("1"));
            item.setUnitPrice(new BigDecimal("500.00"));
            item.setSubtotal(new BigDecimal("500.00"));

            Product product = new Product();
            product.setSku("PRD-" + String.format("%06d", i + 1));
            item.setProduct(product);

            manyItems.add(item);
        }
        order.setItems(manyItems);

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasItemCountError = errors.stream()
                .anyMatch(e -> e.contains("上限") || e.contains("100"));
        assertTrue(hasItemCountError, "明細数上限超過エラーが検出されない: " + errors);
    }

    // ========================================================================
    // サプライヤー検証テスト
    // ========================================================================

    @Test
    @DisplayName("サプライヤー検証 - 異常系: サプライヤー未設定でエラーが返ること")
    void testValidate_noSupplier() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setSupplier(null);

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasSupplierError = errors.stream()
                .anyMatch(e -> e.contains("サプライヤー") || e.contains("Supplier"));
        assertTrue(hasSupplierError, "サプライヤー未設定エラーが検出されない: " + errors);
    }

    @Test
    @DisplayName("サプライヤー検証 - 異常系: 非アクティブなサプライヤーでエラーが返ること")
    void testValidate_inactiveSupplier() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.getSupplier().setStatus("INACTIVE");

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasSupplierStatusError = errors.stream()
                .anyMatch(e -> e.contains("アクティブ") || e.contains("無効"));
        assertTrue(hasSupplierStatusError,
                "非アクティブサプライヤーエラーが検出されない: " + errors);
    }

    // ========================================================================
    // 日付検証テスト
    // ========================================================================

    @Test
    @DisplayName("日付検証 - 異常系: 発注日未設定でエラーが返ること")
    void testValidate_noOrderDate() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setOrderDate(null);

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasDateError = errors.stream()
                .anyMatch(e -> e.contains("発注日") || e.contains("必須"));
        assertTrue(hasDateError, "発注日未設定エラーが検出されない: " + errors);
    }

    @Test
    @Disabled("Date.after(now)の検証が微妙なタイミング差で不安定 — Clock注入パターンに要変更")
    @DisplayName("日付検証 - 異常系: SUBMITTED状態で未来日の発注日はエラーが返ること")
    void testValidate_futureDateForSubmitted() {
        // 技術的負債 #13: new Date()との比較が不安定。
        // テスト実行のタイミングによって結果が変わる可能性がある。

        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setStatus("SUBMITTED");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        order.setOrderDate(cal.getTime());

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasFutureDateError = errors.stream()
                .anyMatch(e -> e.contains("未来") || e.contains("future"));
        assertTrue(hasFutureDateError, "未来日エラーが検出されない: " + errors);
    }

    @Test
    @Disabled("納品予定日の1年制限テストが不安定 — ミリ秒単位の日付計算が脆弱")
    @DisplayName("日付検証 - 異常系: 納品予定日が発注日から1年以上先の場合エラー")
    void testValidate_deliveryDateTooFar() {
        // 技術的負債 #13: ミリ秒ベースの日付計算（365 * 24 * 60 * 60 * 1000）が
        // うるう年を考慮していない。

        // Arrange
        PurchaseOrder order = createValidOrder();
        Calendar cal = Calendar.getInstance();
        order.setOrderDate(cal.getTime());

        cal.add(Calendar.YEAR, 1);
        cal.add(Calendar.DAY_OF_MONTH, 2); // 1年+2日（確実に超過）
        order.setExpectedDeliveryDate(cal.getTime());

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasDeliveryDateError = errors.stream()
                .anyMatch(e -> e.contains("1 year") || e.contains("delivery"));
        assertTrue(hasDeliveryDateError,
                "納品予定日超過エラーが検出されない: " + errors);
    }

    // ========================================================================
    // 発注番号フォーマットテスト
    // ========================================================================

    @Test
    @DisplayName("発注番号検証 - 異常系: 不正な発注番号フォーマットでエラーが返ること")
    void testValidate_invalidPoNumberFormat() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setPoNumber("INVALID-FORMAT");

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasPoNumberError = errors.stream()
                .anyMatch(e -> e.contains("PO number") || e.contains("format"));
        assertTrue(hasPoNumberError,
                "不正な発注番号フォーマットエラーが検出されない: " + errors);
    }

    @Test
    @DisplayName("発注番号検証 - 異常系: 発注番号未設定でエラーが返ること")
    void testValidate_noPoNumber() {
        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setPoNumber(null);

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasPoNumberError = errors.stream()
                .anyMatch(e -> e.contains("発注番号") || e.contains("必須"));
        assertTrue(hasPoNumberError,
                "発注番号未設定エラーが検出されない: " + errors);
    }

    // ========================================================================
    // 通貨コード検証テスト
    // ========================================================================

    @Test
    @Disabled("サポート通貨リストがハードコードされており、通貨追加時にテスト修正が必要")
    @DisplayName("通貨検証 - 異常系: サポート外の通貨コードでエラーが返ること")
    void testValidate_unsupportedCurrency() {
        // 技術的負債 #4: サポート通貨がハードコード（JPY, USD, EUR, GBP, CNY）

        // Arrange
        PurchaseOrder order = createValidOrder();
        order.setCurrency("KRW"); // 韓国ウォン — 未サポート

        // Act
        List<String> errors = validator.validate(order);

        // Assert
        boolean hasCurrencyError = errors.stream()
                .anyMatch(e -> e.contains("通貨") || e.contains("サポート"));
        assertTrue(hasCurrencyError,
                "未サポート通貨エラーが検出されない: " + errors);
    }

    // TODO: validateItems の個別テストを追加する
    //   - 数量が0以下
    //   - 単価がnull
    //   - 税率が1.0超過
    //   - 割引率が負の値
    //   - 小計の整合性チェック（丸め誤差1円以内の許容）

    // TODO: validateAmounts の個別テストを追加する
    //   - 合計金額null
    //   - 合計金額と明細小計の不一致（10円超過）

    // TODO: validateDates の個別テストを追加する
    //   - 納品予定日が発注日より前
    //   - 完了済み発注で納品予定日が未来（警告のみ）
}
