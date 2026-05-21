package com.proquip.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationHelperの単体テスト。
 *
 * <p>技術的負債 #13: テストカバレッジが不十分。
 * <ul>
 *   <li>メールアドレスのRFC 5322非準拠ケースのテストが不足</li>
 *   <li>郵便番号のバリデーションテストが日本(JP)のみ</li>
 *   <li>isValidDateRangeのテストが最低限のケースのみ</li>
 *   <li>SKUコードのバリデーションテストが欠如（PurchaseOrderValidatorとの整合性問題）</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
class ValidationHelperTest {

    // ========================================================================
    // isValidEmail テスト
    // ========================================================================

    @Test
    @DisplayName("メール検証 - 正常系: 標準的なメールアドレスが有効と判定されること")
    void testIsValidEmail() {
        assertTrue(ValidationHelper.isValidEmail("user@example.com"));
        assertTrue(ValidationHelper.isValidEmail("test.user@example.co.jp"));
        assertTrue(ValidationHelper.isValidEmail("user+tag@example.com"));
    }

    @Test
    @DisplayName("メール検証 - 異常系: 不正な形式のメールアドレスが無効と判定されること")
    void testIsValidEmail_invalid() {
        assertFalse(ValidationHelper.isValidEmail(null));
        assertFalse(ValidationHelper.isValidEmail(""));
        assertFalse(ValidationHelper.isValidEmail("   "));
        assertFalse(ValidationHelper.isValidEmail("userexample.com"));
        assertFalse(ValidationHelper.isValidEmail("user@"));
        assertFalse(ValidationHelper.isValidEmail("@example.com"));
    }

    @Test
    @DisplayName("メール検証 - エッジケース: RFC 5322非準拠パターン")
    void testIsValidEmail_rfc5322Edge() {
        // 技術的負債: RFC 5322に準拠していない簡易正規表現のため、
        // 以下のケースが本来の仕様と異なる動作をする可能性がある。

        // 引用符付きローカルパート — 本来は有効だが簡易パターンでは無効
        assertFalse(ValidationHelper.isValidEmail("\"user name\"@example.com"));

        // IPアドレスリテラルドメイン — 本来は有効だが未対応
        assertFalse(ValidationHelper.isValidEmail("user@[192.168.1.1]"));

        // ハイフン付きドメイン — 有効
        assertTrue(ValidationHelper.isValidEmail("user@my-domain.com"));
    }

    // ========================================================================
    // isValidPhone テスト
    // ========================================================================

    @Test
    @DisplayName("電話番号検証 - 正常系: 各種形式の電話番号が有効と判定されること")
    void testIsValidPhone() {
        // 日本の固定電話
        assertTrue(ValidationHelper.isValidPhone("03-1234-5678"));
        // 日本の携帯電話
        assertTrue(ValidationHelper.isValidPhone("090-1234-5678"));
        // 国際形式
        assertTrue(ValidationHelper.isValidPhone("+81-3-1234-5678"));
        // ハイフンなし
        assertTrue(ValidationHelper.isValidPhone("0312345678"));
    }

    @Test
    @DisplayName("電話番号検証 - 異常系: 不正な形式の電話番号が無効と判定されること")
    void testIsValidPhone_invalid() {
        assertFalse(ValidationHelper.isValidPhone(null));
        assertFalse(ValidationHelper.isValidPhone(""));
        assertFalse(ValidationHelper.isValidPhone("abc-defg-hijk"));
        // 短すぎる（7桁未満）
        assertFalse(ValidationHelper.isValidPhone("12345"));
    }

    // ========================================================================
    // isValidPostalCode テスト
    // ========================================================================

    @Test
    @DisplayName("郵便番号検証 - 正常系: 日本の郵便番号が有効と判定されること")
    void testIsValidPostalCode_japan() {
        assertTrue(ValidationHelper.isValidPostalCode("100-0001", "JP"));
        assertTrue(ValidationHelper.isValidPostalCode("999-9999", "JP"));
    }

    @Test
    @DisplayName("郵便番号検証 - 異常系: 不正な日本の郵便番号が無効と判定されること")
    void testIsValidPostalCode_japan_invalid() {
        // ハイフンなし
        assertFalse(ValidationHelper.isValidPostalCode("1000001", "JP"));
        // 桁数不足
        assertFalse(ValidationHelper.isValidPostalCode("100-001", "JP"));
    }

    @Test
    @DisplayName("郵便番号検証 - 正常系: 米国のZIPコードが有効と判定されること")
    void testIsValidPostalCode_us() {
        assertTrue(ValidationHelper.isValidPostalCode("10001", "US"));
        assertTrue(ValidationHelper.isValidPostalCode("10001-1234", "US"));
    }

    @Test
    @DisplayName("郵便番号検証 - エッジケース: 未対応の国コードは常にtrueが返ること")
    void testIsValidPostalCode_unknownCountry() {
        // 技術的負債: 未対応の国は素通し — セキュリティリスク
        assertTrue(ValidationHelper.isValidPostalCode("ANYTHING", "DE"));
        assertTrue(ValidationHelper.isValidPostalCode("invalid-format", "FR"));
    }

    @Test
    @DisplayName("郵便番号検証 - 異常系: 郵便番号nullの場合はfalseが返ること")
    void testIsValidPostalCode_null() {
        assertFalse(ValidationHelper.isValidPostalCode(null, "JP"));
    }

    @Test
    @DisplayName("郵便番号検証 - エッジケース: 国コードnullの場合はtrueが返ること")
    void testIsValidPostalCode_nullCountry() {
        // 技術的負債: 国コードがnullでも検証をスキップしてtrueを返す
        assertTrue(ValidationHelper.isValidPostalCode("12345", null));
    }

    // ========================================================================
    // isPositiveAmount テスト
    // ========================================================================

    @Test
    @DisplayName("正の金額検証 - 正常系: 正の値でtrueが返ること")
    void testIsPositiveAmount() {
        assertTrue(ValidationHelper.isPositiveAmount(new BigDecimal("1")));
        assertTrue(ValidationHelper.isPositiveAmount(new BigDecimal("0.01")));
        assertTrue(ValidationHelper.isPositiveAmount(new BigDecimal("999999.99")));
    }

    @Test
    @DisplayName("正の金額検証 - 異常系: 0以下の値でfalseが返ること")
    void testIsPositiveAmount_invalid() {
        assertFalse(ValidationHelper.isPositiveAmount(BigDecimal.ZERO));
        assertFalse(ValidationHelper.isPositiveAmount(new BigDecimal("-1")));
        assertFalse(ValidationHelper.isPositiveAmount(null));
    }

    // ========================================================================
    // isValidDateRange テスト
    // ========================================================================

    @Test
    @DisplayName("日付範囲検証 - 正常系: 開始日が終了日より前の場合にtrueが返ること")
    void testIsValidDateRange() {
        // Arrange
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2024, Calendar.JANUARY, 1);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2024, Calendar.DECEMBER, 31);

        // Act & Assert
        assertTrue(ValidationHelper.isValidDateRange(cal1.getTime(), cal2.getTime()));
    }

    @Test
    @DisplayName("日付範囲検証 - 正常系: 同一日の場合にtrueが返ること")
    void testIsValidDateRange_sameDay() {
        Date now = new Date();
        assertTrue(ValidationHelper.isValidDateRange(now, now));
    }

    @Test
    @DisplayName("日付範囲検証 - 異常系: 開始日が終了日より後の場合にfalseが返ること")
    void testIsValidDateRange_reversed() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2024, Calendar.DECEMBER, 31);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2024, Calendar.JANUARY, 1);

        assertFalse(ValidationHelper.isValidDateRange(cal1.getTime(), cal2.getTime()));
    }

    @Test
    @DisplayName("日付範囲検証 - 異常系: nullの場合にfalseが返ること")
    void testIsValidDateRange_null() {
        assertFalse(ValidationHelper.isValidDateRange(null, new Date()));
        assertFalse(ValidationHelper.isValidDateRange(new Date(), null));
        assertFalse(ValidationHelper.isValidDateRange(null, null));
    }

    // TODO: isValidEmail — 国際化ドメイン名(IDN)のテストを追加する
    // TODO: isValidPhone — 各国固有の電話番号形式テストを追加する
    // TODO: isValidPostalCode — ドイツ(DE)、フランス(FR)等の郵便番号形式を追加する
}
