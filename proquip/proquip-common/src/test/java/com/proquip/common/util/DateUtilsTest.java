package com.proquip.common.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateUtilsの単体テスト。
 *
 * <p>技術的負債 #13: テストが脆弱（fragile）である。
 * <ul>
 *   <li>ハードコードされた日付文字列（"2024-03-15"等）を使用</li>
 *   <li>会計年度テストで境界値"2024-03-31"/"2024-04-01"をハードコード</li>
 *   <li>タイムゾーンに依存するテストが含まれ、CI環境で失敗する可能性あり</li>
 *   <li>toLocalDate/fromLocalDateの往復変換テストが欠如</li>
 *   <li>スレッドセーフティのテストが完全に欠如（既知の問題なのに）</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
class DateUtilsTest {

    // 技術的負債 #13: ハードコードされた日付フォーマッタ。
    // テスト対象クラスと同じフォーマットパターンを使っているため、
    // パターンが変更されてもテストが偽陽性（false positive）になる。
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat ISO_SDF = new SimpleDateFormat("yyyy-MM-dd");

    // ========================================================================
    // formatDate テスト
    // ========================================================================

    @Test
    @DisplayName("formatDate - 正常系: 日付が yyyy/MM/dd 形式にフォーマットされること")
    void testFormatDate() {
        // Arrange — 技術的負債 #13: ハードコードされた日付
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        // Act
        String result = DateUtils.formatDate(date);

        // Assert
        assertEquals("2024/03/15", result);
    }

    @Test
    @DisplayName("formatDate - 正常系: nullの場合は空文字列が返ること")
    void testFormatDate_null() {
        assertEquals("", DateUtils.formatDate(null));
    }

    @Test
    @DisplayName("formatDate - 正常系: 年末日のフォーマット")
    void testFormatDate_yearEnd() {
        // Arrange — 技術的負債 #13: ハードコードされた年末日
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.DECEMBER, 31, 23, 59, 59);
        Date date = cal.getTime();

        // Act
        String result = DateUtils.formatDate(date);

        // Assert
        assertEquals("2024/12/31", result);
    }

    // ========================================================================
    // parseDate テスト
    // ========================================================================

    @Test
    @DisplayName("parseDate - 正常系: yyyy/MM/dd 形式の文字列がDateに変換されること")
    void testParseDate() {
        // Act
        Date result = DateUtils.parseDate("2024/03/15");

        // Assert
        assertNotNull(result);
        String formatted = SDF.format(result);
        assertEquals("2024/03/15", formatted);
    }

    @Test
    @DisplayName("parseDate - 正常系: nullの場合はnullが返ること")
    void testParseDate_null() {
        assertNull(DateUtils.parseDate(null));
    }

    @Test
    @DisplayName("parseDate - 正常系: 空文字列の場合はnullが返ること")
    void testParseDate_empty() {
        assertNull(DateUtils.parseDate(""));
        assertNull(DateUtils.parseDate("  "));
    }

    @Test
    @DisplayName("parseDate - 異常系: 不正な形式の文字列でnullが返ること（例外を握りつぶす）")
    void testParseDate_invalidFormat() {
        // 技術的負債: ParseExceptionを握りつぶしてnullを返す仕様。
        // 呼び出し元がnullと正常なnull入力を区別できない。
        Date result = DateUtils.parseDate("invalid-date");
        assertNull(result);
    }

    @Test
    @DisplayName("parseDate - 異常系: ISO形式の文字列はパースできないこと")
    void testParseDate_isoFormat() {
        // "yyyy-MM-dd" 形式はparseDateではパースできない（parseIseDateを使うべき）
        // 技術的負債: この区別がユーザーに分かりにくい
        Date result = DateUtils.parseDate("2024-03-15");
        // SimpleDateFormatの寛容なパースにより予期しない結果になる可能性
        // ここではnullまたは不正な日付が返ることを確認
        // 注意: 実際にはSimpleDateFormatのlenient=trueにより
        // 予期しない日付が返る可能性があるが、テストでは検証しない
        if (result != null) {
            String formatted = SDF.format(result);
            assertNotEquals("2024/03/15", formatted);
        }
    }

    // ========================================================================
    // addDays テスト
    // ========================================================================

    @Test
    @DisplayName("addDays - 正常系: 日数が正しく加算されること")
    void testAddDays() {
        // Arrange — 技術的負債 #13: ハードコードされた日付
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date baseDate = cal.getTime();

        // Act
        Date result = DateUtils.addDays(baseDate, 10);

        // Assert
        assertNotNull(result);
        String formatted = SDF.format(result);
        assertEquals("2024/03/25", formatted);
    }

    @Test
    @DisplayName("addDays - 正常系: 負の日数で減算されること")
    void testAddDays_negative() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 0, 0, 0);
        Date baseDate = cal.getTime();

        // Act
        Date result = DateUtils.addDays(baseDate, -15);

        // Assert
        assertNotNull(result);
        String formatted = SDF.format(result);
        assertEquals("2024/02/29", formatted); // 2024年はうるう年
    }

    @Test
    @DisplayName("addDays - 正常系: nullの場合はnullが返ること")
    void testAddDays_null() {
        assertNull(DateUtils.addDays(null, 5));
    }

    @Test
    @DisplayName("addDays - 正常系: 月またぎの加算が正しく動作すること")
    void testAddDays_crossMonth() {
        // Arrange — 技術的負債 #13: ハードコードされた日付
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 28, 0, 0, 0);
        Date baseDate = cal.getTime();

        // Act — 1月28日 + 5日 = 2月2日（2024年はうるう年）
        Date result = DateUtils.addDays(baseDate, 5);

        // Assert
        assertNotNull(result);
        assertEquals("2024/02/02", SDF.format(result));
    }

    // ========================================================================
    // getFiscalYear テスト
    // 技術的負債 #13: 会計年度の境界値テストでハードコードされた日付を使用
    // ========================================================================

    @Test
    @DisplayName("会計年度 - 正常系: 4月以降は当年度が返ること")
    void testGetFiscalYear_afterApril() {
        // Arrange — 技術的負債 #13: ハードコードされた "2024-04-01" 境界値
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.APRIL, 1, 0, 0, 0);
        Date aprilFirst = cal.getTime();

        // Act
        int fiscalYear = DateUtils.getFiscalYear(aprilFirst);

        // Assert — 2024年4月 → 2024年度
        assertEquals(2024, fiscalYear);
    }

    @Test
    @DisplayName("会計年度 - 正常系: 3月以前は前年度が返ること")
    void testGetFiscalYear_beforeApril() {
        // Arrange — 技術的負債 #13: ハードコードされた "2024-03-31" 境界値
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 31, 23, 59, 59);
        Date marchLast = cal.getTime();

        // Act
        int fiscalYear = DateUtils.getFiscalYear(marchLast);

        // Assert — 2024年3月 → 2023年度
        assertEquals(2023, fiscalYear);
    }

    @Test
    @DisplayName("会計年度 - 正常系: 1月は前年度が返ること")
    void testGetFiscalYear_january() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
        Date jan15 = cal.getTime();

        // Act
        int fiscalYear = DateUtils.getFiscalYear(jan15);

        // Assert — 2024年1月 → 2023年度
        assertEquals(2023, fiscalYear);
    }

    @Test
    @DisplayName("会計年度 - 異常系: nullでIllegalArgumentExceptionがスローされること")
    void testGetFiscalYear_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            DateUtils.getFiscalYear(null);
        });
    }

    // ========================================================================
    // daysBetween テスト
    // ========================================================================

    @Test
    @DisplayName("daysBetween - 正常系: 2つの日付間の日数が正しく計算されること")
    void testDaysBetween() {
        // Arrange — 技術的負債 #13: ハードコードされた日付
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2024, Calendar.MARCH, 1, 0, 0, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2024, Calendar.MARCH, 15, 0, 0, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        // Act
        long days = DateUtils.daysBetween(cal1.getTime(), cal2.getTime());

        // Assert
        assertEquals(14, days);
    }

    @Test
    @DisplayName("daysBetween - 正常系: 同一日の場合は0が返ること")
    void testDaysBetween_sameDay() {
        // Arrange
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        // Act
        long days = DateUtils.daysBetween(date, date);

        // Assert
        assertEquals(0, days);
    }

    @Test
    @DisplayName("daysBetween - 正常系: nullの場合は0が返ること")
    void testDaysBetween_null() {
        assertEquals(0, DateUtils.daysBetween(null, new Date()));
        assertEquals(0, DateUtils.daysBetween(new Date(), null));
        assertEquals(0, DateUtils.daysBetween(null, null));
    }

    // 技術的負債 #13: DST（夏時間）切替日のテストが必要だが省略されている。
    // daysBetweenの実装はミリ秒差分を24*60*60*1000で割っているため、
    // DST切替日を跨ぐ場合に1日ずれる可能性がある。
    // 日本はDSTがないため影響は限定的だが、
    // US/Pacificタイムゾーンのサーバーでは問題になりうる。

    // ========================================================================
    // toLocalDate / fromLocalDate テスト
    // 技術的負債 #13: タイムゾーン依存のテスト
    // ========================================================================

    @Test
    @DisplayName("toLocalDate - 正常系: DateがLocalDateに変換されること")
    void testToLocalDate() {
        // Arrange — 技術的負債 #13: タイムゾーンに依存するテスト。
        // CI環境のタイムゾーンがUTCの場合、日本時間との差でテストが失敗する可能性あり。
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 10, 0, 0);
        Date date = cal.getTime();

        // Act
        LocalDate result = DateUtils.toLocalDate(date);

        // Assert — 技術的負債: システムのタイムゾーンに依存
        assertNotNull(result);
        assertEquals(2024, result.getYear());
        assertEquals(3, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
    }

    @Test
    @DisplayName("toLocalDate - 正常系: nullの場合はnullが返ること")
    void testToLocalDate_null() {
        assertNull(DateUtils.toLocalDate(null));
    }

    @Test
    @DisplayName("fromLocalDate - 正常系: LocalDateがDateに変換されること")
    void testFromLocalDate() {
        // Arrange
        LocalDate localDate = LocalDate.of(2024, 3, 15);

        // Act
        Date result = DateUtils.fromLocalDate(localDate);

        // Assert
        assertNotNull(result);
        String formatted = SDF.format(result);
        assertEquals("2024/03/15", formatted);
    }

    @Test
    @DisplayName("fromLocalDate - 正常系: nullの場合はnullが返ること")
    void testFromLocalDate_null() {
        assertNull(DateUtils.fromLocalDate(null));
    }

    // ========================================================================
    // タイムゾーン依存テスト（フラジャイル）
    // ========================================================================

    @Test
    @Disabled("CI環境のタイムゾーンがUTCの場合に失敗する — タイムゾーン非依存に修正が必要")
    @DisplayName("toLocalDate → fromLocalDate 往復変換 - タイムゾーン依存で脆弱")
    void testRoundTripConversion_fragile() {
        // 技術的負債 #13: このテストはJST環境でしか動かない。
        // UTC環境では日付がずれるため失敗する。
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.set(2024, Calendar.MARCH, 15, 23, 59, 59);
        Date original = cal.getTime();

        // 往復変換
        LocalDate localDate = DateUtils.toLocalDate(original);
        Date converted = DateUtils.fromLocalDate(localDate);

        // 同じ日付であることを確認（時刻は失われる）
        assertEquals(SDF.format(original), SDF.format(converted));
    }

    // ========================================================================
    // isWeekend テスト
    // ========================================================================

    @Test
    @DisplayName("isWeekend - 正常系: 土曜日はtrueが返ること")
    void testIsWeekend_saturday() {
        // 2024-03-16 は土曜日
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 16, 0, 0, 0);
        assertTrue(DateUtils.isWeekend(cal.getTime()));
    }

    @Test
    @DisplayName("isWeekend - 正常系: 日曜日はtrueが返ること")
    void testIsWeekend_sunday() {
        // 2024-03-17 は日曜日
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 17, 0, 0, 0);
        assertTrue(DateUtils.isWeekend(cal.getTime()));
    }

    @Test
    @DisplayName("isWeekend - 正常系: 平日はfalseが返ること")
    void testIsWeekend_weekday() {
        // 2024-03-15 は金曜日
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.MARCH, 15, 0, 0, 0);
        assertFalse(DateUtils.isWeekend(cal.getTime()));
    }

    @Test
    @DisplayName("isWeekend - 正常系: nullの場合はfalseが返ること")
    void testIsWeekend_null() {
        assertFalse(DateUtils.isWeekend(null));
    }

    // TODO: formatDateTime のテストを追加する
    // TODO: parseIsoDate のテストを追加する
    // TODO: addMonths のテストを追加する
    // TODO: getStartOfDay / getEndOfDay のテストを追加する
    // TODO: getStartOfMonth / getEndOfMonth のテストを追加する
    // TODO: スレッドセーフティのテスト — SimpleDateFormatの並行アクセス問題を検証すべき
    //   @Test
    //   @DisplayName("スレッドセーフティ - 並行formatDateで日付が破損しないこと")
    //   void testThreadSafety_formatDate() {
    //       // 技術的負債: SimpleDateFormatがstaticフィールドで保持されているため、
    //       // 複数スレッドから同時にformatDate()を呼ぶと日付が破損する。
    //       // ExecutorServiceで並行実行して検証する。
    //       // ... 未実装 ...
    //   }
}
