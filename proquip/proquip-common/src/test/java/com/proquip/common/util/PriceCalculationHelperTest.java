package com.proquip.common.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriceCalculationHelperの単体テスト。
 *
 * <p>技術的負債 #13: テストが脆弱（fragile）である。
 * <ul>
 *   <li>ハードコードされた税率（CA: 7.25%, NY: 8%等）に依存するテスト</li>
 *   <li>丸めモードの不整合（HALF_UP vs HALF_DOWN）のテストが不十分</li>
 *   <li>ボリュームディスカウントの閾値がハードコードされており、
 *       閾値変更時にテストも修正が必要</li>
 *   <li>通貨変換のテストが@Disabled（為替レートがハードコードで不安定）</li>
 *   <li>calculateTotal（raw Listパラメータ）のテストが型安全でない</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
class PriceCalculationHelperTest {

    // ========================================================================
    // calculateSubtotal テスト
    // ========================================================================

    @Test
    @DisplayName("小計計算 - 正常系: 単価 x 数量で小計が計算されること")
    void testCalculateSubtotal_basic() {
        // Arrange
        BigDecimal unitPrice = new BigDecimal("1500.00");
        int quantity = 10;

        // Act
        BigDecimal result = PriceCalculationHelper.calculateSubtotal(
                unitPrice, quantity, null);

        // Assert
        assertEquals(new BigDecimal("15000.00"), result);
    }

    @Test
    @DisplayName("小計計算 - 正常系: 値引率適用で小計が計算されること")
    void testCalculateSubtotal_withDiscount() {
        // Arrange
        BigDecimal unitPrice = new BigDecimal("2000.00");
        int quantity = 5;
        BigDecimal discountPercent = new BigDecimal("10"); // 10%

        // Act
        BigDecimal result = PriceCalculationHelper.calculateSubtotal(
                unitPrice, quantity, discountPercent);

        // Assert — 2000 x 5 x (1 - 10/100) = 9000.00
        assertEquals(new BigDecimal("9000.00"), result);
    }

    @Test
    @DisplayName("小計計算 - 正常系: 値引率0%の場合は値引なしと同じ結果")
    void testCalculateSubtotal_zeroDiscount() {
        // Arrange
        BigDecimal unitPrice = new BigDecimal("1000.00");
        int quantity = 3;

        // Act
        BigDecimal withZero = PriceCalculationHelper.calculateSubtotal(
                unitPrice, quantity, BigDecimal.ZERO);
        BigDecimal withNull = PriceCalculationHelper.calculateSubtotal(
                unitPrice, quantity, null);

        // Assert
        assertEquals(withNull, withZero);
    }

    @Test
    @DisplayName("小計計算 - 異常系: 単価nullでIllegalArgumentExceptionがスローされること")
    void testCalculateSubtotal_nullUnitPrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            PriceCalculationHelper.calculateSubtotal(null, 10, null);
        });
    }

    @Test
    @DisplayName("小計計算 - 異常系: 数量0以下でIllegalArgumentExceptionがスローされること")
    void testCalculateSubtotal_zeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            PriceCalculationHelper.calculateSubtotal(new BigDecimal("1000"), 0, null);
        });
    }

    // ========================================================================
    // calculateTax テスト
    // 技術的負債 #13: ハードコードされた州別税率に依存するテスト
    // ========================================================================

    @Test
    @DisplayName("税額計算 - 正常系: CA州（7.25%）の税額が計算されること")
    void testCalculateTax_california() {
        // 技術的負債 #13: ハードコードされた税率 7.25% に依存
        BigDecimal amount = new BigDecimal("10000.00");

        BigDecimal tax = PriceCalculationHelper.calculateTax(amount, "CA");

        // 10000.00 x 0.0725 = 725.00
        assertEquals(new BigDecimal("725.00"), tax);
    }

    @Test
    @DisplayName("税額計算 - 正常系: NY州（8%）の税額が計算されること")
    void testCalculateTax_newYork() {
        // 技術的負債 #13: ハードコードされた税率 8% に依存
        BigDecimal amount = new BigDecimal("5000.00");

        BigDecimal tax = PriceCalculationHelper.calculateTax(amount, "NY");

        // 5000.00 x 0.08 = 400.00
        assertEquals(new BigDecimal("400.00"), tax);
    }

    @Test
    @DisplayName("税額計算 - 正常系: TX州（6.25%）の税額が計算されること")
    void testCalculateTax_texas() {
        BigDecimal amount = new BigDecimal("8000.00");

        BigDecimal tax = PriceCalculationHelper.calculateTax(amount, "TX");

        // 8000.00 x 0.0625 = 500.00
        assertEquals(new BigDecimal("500.00"), tax);
    }

    @Test
    @DisplayName("税額計算 - 正常系: 未知の州コードではデフォルト税率5%が適用されること")
    void testCalculateTax_unknownState() {
        // 技術的負債: 未知の州でもエラーにならずデフォルト5%が適用される。
        // 本来はエラーにすべきか要検討。
        BigDecimal amount = new BigDecimal("10000.00");

        BigDecimal tax = PriceCalculationHelper.calculateTax(amount, "XX");

        // 10000.00 x 0.05 = 500.00
        assertEquals(new BigDecimal("500.00"), tax);
    }

    @Test
    @DisplayName("税額計算 - 異常系: 金額nullの場合はZEROが返ること")
    void testCalculateTax_nullAmount() {
        assertEquals(BigDecimal.ZERO, PriceCalculationHelper.calculateTax(null, "CA"));
    }

    @Test
    @DisplayName("税額計算 - 異常系: 州コードnullの場合はZEROが返ること")
    void testCalculateTax_nullStateCode() {
        assertEquals(BigDecimal.ZERO,
                PriceCalculationHelper.calculateTax(new BigDecimal("1000"), null));
    }

    // ========================================================================
    // applyVolumeDiscount テスト
    // 技術的負債 #13: ハードコードされたティア閾値に依存するテスト
    // ========================================================================

    @Test
    @DisplayName("ボリュームディスカウント - 正常系: 100個未満は割引なし")
    void testApplyVolumeDiscount_noDiscount() {
        // 技術的負債 #13: 閾値100がハードコード
        BigDecimal amount = new BigDecimal("50000.00");

        BigDecimal result = PriceCalculationHelper.applyVolumeDiscount(amount, 99);

        assertEquals(new BigDecimal("50000.00"), result);
    }

    @Test
    @DisplayName("ボリュームディスカウント - 正常系: 100-499個は5%割引")
    void testApplyVolumeDiscount_tier1() {
        BigDecimal amount = new BigDecimal("100000.00");

        BigDecimal result = PriceCalculationHelper.applyVolumeDiscount(amount, 200);

        // 100000.00 x (1 - 0.05) = 95000.00
        assertEquals(new BigDecimal("95000.00"), result);
    }

    @Test
    @DisplayName("ボリュームディスカウント - 正常系: 500-999個は10%割引")
    void testApplyVolumeDiscount_tier2() {
        BigDecimal amount = new BigDecimal("100000.00");

        BigDecimal result = PriceCalculationHelper.applyVolumeDiscount(amount, 500);

        // 100000.00 x (1 - 0.10) = 90000.00
        assertEquals(new BigDecimal("90000.00"), result);
    }

    @Test
    @DisplayName("ボリュームディスカウント - 正常系: 1000個以上は15%割引")
    void testApplyVolumeDiscount_tier3() {
        BigDecimal amount = new BigDecimal("100000.00");

        BigDecimal result = PriceCalculationHelper.applyVolumeDiscount(amount, 1000);

        // 100000.00 x (1 - 0.15) = 85000.00
        assertEquals(new BigDecimal("85000.00"), result);
    }

    @Test
    @DisplayName("ボリュームディスカウント - 異常系: 金額nullの場合はZEROが返ること")
    void testApplyVolumeDiscount_nullAmount() {
        assertEquals(BigDecimal.ZERO,
                PriceCalculationHelper.applyVolumeDiscount(null, 100));
    }

    // ========================================================================
    // calculateJapaneseTax テスト
    // 技術的負債 #13: ハードコードされた税率に依存
    // ========================================================================

    @Test
    @DisplayName("消費税計算 - 正常系: 標準税率10%が適用されること")
    void testCalculateJapaneseTax_standard() {
        // 技術的負債 #13: AppConstants.DEFAULT_TAX_RATE と二重管理の税率
        BigDecimal amount = new BigDecimal("10000");

        BigDecimal result = PriceCalculationHelper.calculateJapaneseTax(amount, false);

        // 10000 + floor(10000 x 0.10) = 10000 + 1000 = 11000
        assertEquals(new BigDecimal("11000"), result);
    }

    @Test
    @DisplayName("消費税計算 - 正常系: 軽減税率8%が適用されること")
    void testCalculateJapaneseTax_reduced() {
        BigDecimal amount = new BigDecimal("10000");

        BigDecimal result = PriceCalculationHelper.calculateJapaneseTax(amount, true);

        // 10000 + floor(10000 x 0.08) = 10000 + 800 = 10800
        assertEquals(new BigDecimal("10800"), result);
    }

    @Test
    @DisplayName("消費税計算 - 正常系: 端数切り捨てが正しく動作すること")
    void testCalculateJapaneseTax_rounding() {
        // 消費税は切り捨て（RoundingMode.DOWN）
        BigDecimal amount = new BigDecimal("999");

        BigDecimal result = PriceCalculationHelper.calculateJapaneseTax(amount, false);

        // floor(999 x 0.10) = floor(99.9) = 99
        // 999 + 99 = 1098
        assertEquals(new BigDecimal("1098"), result);
    }

    @Test
    @DisplayName("消費税計算 - 異常系: 金額nullの場合はZEROが返ること")
    void testCalculateJapaneseTax_null() {
        assertEquals(BigDecimal.ZERO,
                PriceCalculationHelper.calculateJapaneseTax(null, false));
    }

    // ========================================================================
    // roundPrice テスト
    // 技術的負債: HALF_DOWN丸めモード（他メソッドのHALF_UPと不整合）
    // ========================================================================

    @Test
    @DisplayName("丸め - 正常系: HALF_DOWNで小数第2位まで丸められること")
    void testRoundPrice() {
        // 技術的負債: roundPriceはHALF_DOWNだが、
        // calculateSubtotalやcalculateTaxはHALF_UPを使用。不整合。
        BigDecimal amount = new BigDecimal("1234.5678");

        BigDecimal result = PriceCalculationHelper.roundPrice(amount);

        // HALF_DOWN: 1234.5678 → 1234.57（5678 > 5000なので切り上げ）
        assertEquals(new BigDecimal("1234.57"), result);
    }

    @Test
    @DisplayName("丸め - エッジケース: HALF_DOWNとHALF_UPで結果が異なる値")
    void testRoundPrice_halfDownVsHalfUp() {
        // 技術的負債: HALF_DOWNとHALF_UPの差が出るケース
        // 0.005 の場合: HALF_UP → 0.01, HALF_DOWN → 0.00
        BigDecimal amount = new BigDecimal("100.005");

        BigDecimal roundedDown = PriceCalculationHelper.roundPrice(amount);
        BigDecimal roundedUp = amount.setScale(2, RoundingMode.HALF_UP);

        // HALF_DOWN: 100.005 → 100.00（5は切り捨て）
        assertEquals(new BigDecimal("100.00"), roundedDown);
        // HALF_UP: 100.005 → 100.01（5は切り上げ）
        assertEquals(new BigDecimal("100.01"), roundedUp);

        // この不整合が実際の金額計算で問題を起こす可能性がある
        assertNotEquals(roundedDown, roundedUp);
    }

    @Test
    @DisplayName("丸め - 異常系: nullの場合はZEROが返ること")
    void testRoundPrice_null() {
        assertEquals(BigDecimal.ZERO, PriceCalculationHelper.roundPrice(null));
    }

    // ========================================================================
    // calculateTotal テスト（raw List パラメータ）
    // ========================================================================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("合計計算 - 正常系: 明細リストの合計が計算されること")
    void testCalculateTotal() {
        // 技術的負債: raw Listを使用した型安全でないAPI
        List items = new ArrayList();

        Map<String, Object> item1 = new HashMap<>();
        item1.put("subtotal", new BigDecimal("1000.00"));
        items.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("subtotal", new BigDecimal("2500.50"));
        items.add(item2);

        BigDecimal total = PriceCalculationHelper.calculateTotal(items);

        assertEquals(new BigDecimal("3500.50"), total);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("合計計算 - 正常系: 空リストの場合はZEROが返ること")
    void testCalculateTotal_emptyList() {
        assertEquals(BigDecimal.ZERO, PriceCalculationHelper.calculateTotal(new ArrayList()));
    }

    @Test
    @DisplayName("合計計算 - 異常系: nullの場合はZEROが返ること")
    void testCalculateTotal_null() {
        assertEquals(BigDecimal.ZERO, PriceCalculationHelper.calculateTotal(null));
    }

    // ========================================================================
    // 通貨変換テスト
    // 技術的負債 #13: 為替レートがハードコードされており不安定
    // ========================================================================

    @Test
    @Disabled("為替レートがハードコード（2024年1月時点）のため、将来的に不正確な値になる")
    @DisplayName("通貨変換 - 正常系: JPY→USDの変換が正しく行われること")
    void testConvertCurrency_jpyToUsd() {
        // 技術的負債: ハードコードされた為替レート（JPY→USD: 0.0068）に依存
        BigDecimal jpyAmount = new BigDecimal("148500.00");

        BigDecimal usdAmount = PriceCalculationHelper.convertCurrency(
                jpyAmount, "JPY", "USD");

        // 148500.00 x 0.0068 = 1009.80
        assertEquals(new BigDecimal("1009.80"), usdAmount);
    }

    @Test
    @Disabled("為替レートがハードコード（2024年1月時点）のため不安定")
    @DisplayName("通貨変換 - 正常系: 同一通貨の場合はそのまま返ること")
    void testConvertCurrency_sameCurrency() {
        BigDecimal amount = new BigDecimal("1000.00");

        BigDecimal result = PriceCalculationHelper.convertCurrency(amount, "USD", "USD");

        assertEquals(amount, result);
    }

    @Test
    @DisplayName("通貨変換 - 異常系: サポートされていない通貨でIllegalArgumentExceptionがスローされること")
    void testConvertCurrency_unsupportedCurrency() {
        assertThrows(IllegalArgumentException.class, () -> {
            PriceCalculationHelper.convertCurrency(
                    new BigDecimal("1000"), "USD", "KRW");
        });
    }

    // TODO: 以下のテストを追加する
    // TODO: calculateSubtotal — 小数点の多い単価でのHALF_UP丸めの検証
    // TODO: calculateTax — 全50州＋DCの税率テスト（現状は主要3州のみ）
    // TODO: calculateTotal — 文字列subtotalのフォールバック変換テスト
    // TODO: convertCurrency — EUR→JPY等のクロスレート精度検証
    // TODO: applyVolumeDiscount — ティア境界値（99→100、499→500）の網羅テスト
}
