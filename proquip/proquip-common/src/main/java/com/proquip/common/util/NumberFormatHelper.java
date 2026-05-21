package com.proquip.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 数値フォーマットに関するヘルパークラス。
 *
 * <p>通貨・パーセンテージ等の数値をロケールに応じた書式で
 * フォーマットする機能を提供する。</p>
 *
 * <p>【技術的負債】ロケール処理が不完全で、一部のロケールでは
 * 意図しないフォーマットになる。また、parseNumberがNumberFormatExceptionを
 * 握りつぶしてnullを返す設計はDateUtils.parseDateと同じ問題を持つ。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public final class NumberFormatHelper {

    /** インスタンス化を防止 */
    private NumberFormatHelper() {
    }

    /**
     * 金額を通貨形式にフォーマットする。
     *
     * <p>【技術的負債】ロケール処理が不十分。以下の問題がある：
     * <ul>
     *   <li>"ja"以外のロケール文字列の解析が雑（国コード未対応）</li>
     *   <li>通貨記号の配置がロケールのデフォルトに依存</li>
     *   <li>千の位区切りの制御ができない</li>
     * </ul>
     * </p>
     *
     * @param amount フォーマット対象の金額
     * @param localeStr ロケール文字列（例: "ja", "en"）。nullの場合はデフォルトロケール
     * @return フォーマットされた通貨文字列。amountがnullの場合は"0"
     */
    public static String formatCurrency(BigDecimal amount, String localeStr) {
        if (amount == null) {
            return "0";
        }

        Locale locale;
        if (localeStr == null || localeStr.isEmpty()) {
            locale = Locale.getDefault();
        } else if ("ja".equals(localeStr)) {
            locale = Locale.JAPAN;
        } else if ("en".equals(localeStr)) {
            locale = Locale.US;
        } else {
            // 【技術的負債】雑なロケール解析。Locale.forLanguageTag()を使うべき。
            locale = new Locale(localeStr);
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        return formatter.format(amount);
    }

    /**
     * 数値をパーセンテージ形式にフォーマットする。
     *
     * <p>例: 0.156 → "15.6%"</p>
     *
     * @param value フォーマット対象の値（0.0〜1.0の範囲を想定）
     * @return フォーマットされたパーセンテージ文字列。valueがnullの場合は"0%"
     */
    public static String formatPercent(BigDecimal value) {
        if (value == null) {
            return "0%";
        }

        BigDecimal percentValue = value.multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
        return percentValue.toPlainString() + "%";
    }

    /**
     * 数値文字列をBigDecimalに変換する。
     *
     * <p>【技術的負債】NumberFormatExceptionをキャッチしてnullを返しており、
     * DateUtils.parseDateと同じサイレント失敗の問題を持つ。
     * 呼び出し元でnullチェックを忘れるとNPEになる。</p>
     *
     * @param numberStr 数値文字列（カンマ区切りも許容）
     * @return 変換されたBigDecimal。変換失敗時はnull
     */
    public static BigDecimal parseNumber(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty()) {
            return null;
        }

        try {
            // カンマ区切りを除去してからパース
            String cleaned = numberStr.trim().replace(",", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            // 【技術的負債】例外を握りつぶしてnullを返す
            return null;
        }
    }

    /**
     * 数値を3桁カンマ区切りでフォーマットする。
     *
     * @param amount フォーマット対象の数値
     * @return カンマ区切りの文字列。amountがnullの場合は"0"
     */
    public static String formatWithCommas(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        DecimalFormat formatter = new DecimalFormat("#,##0.##");
        return formatter.format(amount);
    }
}
