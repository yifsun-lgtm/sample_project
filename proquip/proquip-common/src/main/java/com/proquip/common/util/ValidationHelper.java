package com.proquip.common.util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 入力値のバリデーションに関するヘルパークラス。
 *
 * <p>メールアドレス、電話番号、郵便番号等の形式検証を行う。</p>
 *
 * <p>【技術的負債】このクラスには以下の問題がある：
 * <ul>
 *   <li>Bean Validation（Jakarta Validation）のアノテーションと機能が重複している</li>
 *   <li>メールアドレスのバリデーションがRFC 5322に準拠していない（簡易正規表現）</li>
 *   <li>一部のバリデーションはエンティティのアノテーションでも定義されており二重管理</li>
 *   <li>java.util.Dateを使用しておりjava.time APIに移行すべき</li>
 * </ul>
 * </p>
 *
 * <p>【対応方針】Jakarta Validationのカスタムバリデータに移行し、
 * このクラスは廃止予定。ただし移行完了まではレガシーコードが参照している。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public final class ValidationHelper {

    /**
     * メールアドレスの正規表現パターン。
     *
     * <p>【技術的負債】RFC 5322に準拠していない簡易パターン。
     * 以下のケースを正しく処理できない：
     * <ul>
     *   <li>引用符付きローカルパート（"user name"@example.com）</li>
     *   <li>IPアドレスリテラルドメイン（user@[192.168.1.1]）</li>
     *   <li>国際化ドメイン名（IDN）</li>
     *   <li>プラス記号付きアドレス（user+tag@example.com）は対応済み</li>
     * </ul>
     * </p>
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * 電話番号の正規表現パターン（国際形式・日本形式対応）。
     *
     * <p>ハイフンあり/なし、先頭の+（国番号）を許容する。</p>
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\-()\\s]{7,20}$"
    );

    /**
     * 日本の郵便番号パターン（XXX-XXXX形式）。
     */
    private static final Pattern JP_POSTAL_CODE_PATTERN = Pattern.compile(
            "^\\d{3}-\\d{4}$"
    );

    /**
     * 米国のZIPコードパターン（XXXXX または XXXXX-XXXX形式）。
     */
    private static final Pattern US_ZIP_CODE_PATTERN = Pattern.compile(
            "^\\d{5}(-\\d{4})?$"
    );

    /** インスタンス化を防止 */
    private ValidationHelper() {
    }

    /**
     * メールアドレスの形式を検証する。
     *
     * <p>【技術的負債】RFC 5322に完全準拠していない簡易検証。
     * Jakarta MailのInternetAddress.validate()やHibernate Validatorの
     * {@code @Email}アノテーションを使用すべき。
     * また、エンティティクラスの{@code @Email}アノテーションと二重管理。</p>
     *
     * @param email 検証対象のメールアドレス
     * @return 有効な形式の場合true
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 電話番号の形式を検証する。
     *
     * <p>国際形式および日本国内の各種形式に対応する。
     * 桁数の厳密な検証は行わない（7〜20桁の範囲チェックのみ）。</p>
     *
     * @param phone 検証対象の電話番号
     * @return 有効な形式の場合true
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * 郵便番号の形式を検証する。
     *
     * <p>現在、日本（JP）と米国（US）のみ対応。
     * その他の国コードの場合は常にtrueを返す（未実装）。</p>
     *
     * <p>【技術的負債】対応国が2か国のみで、他国は素通し。
     * 全世界の郵便番号形式を網羅する必要がある場合は、
     * 外部ライブラリの使用を検討すべき。</p>
     *
     * @param postalCode 検証対象の郵便番号
     * @param countryCode 国コード（ISO 3166-1 alpha-2）
     * @return 有効な形式の場合true。未対応の国コードの場合は常にtrue
     */
    public static boolean isValidPostalCode(String postalCode,
                                             String countryCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return false;
        }
        if (countryCode == null) {
            // 国コードが不明な場合は検証をスキップ（技術的負債）
            return true;
        }

        switch (countryCode.toUpperCase()) {
            case "JP":
                return JP_POSTAL_CODE_PATTERN
                        .matcher(postalCode.trim()).matches();
            case "US":
                return US_ZIP_CODE_PATTERN
                        .matcher(postalCode.trim()).matches();
            default:
                // 【技術的負債】未対応の国は常にtrueを返す
                return true;
        }
    }

    /**
     * 金額が正の値であることを検証する。
     *
     * <p>【技術的負債】Jakarta Validationの{@code @Positive}や
     * {@code @DecimalMin}アノテーションと機能が重複。</p>
     *
     * @param amount 検証対象の金額
     * @return 正の値（0より大きい）の場合true
     */
    public static boolean isPositiveAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 日付範囲の妥当性を検証する。
     *
     * <p>開始日が終了日より前であることを確認する。</p>
     *
     * <p>【技術的負債】java.util.Dateを使用。java.time.LocalDateに移行すべき。
     * また、DateUtils内にも同様の比較ロジックが存在し、責務が分散している。</p>
     *
     * @param from 開始日
     * @param to 終了日
     * @return 開始日が終了日より前（同日含む）の場合true
     */
    public static boolean isValidDateRange(Date from, Date to) {
        if (from == null || to == null) {
            return false;
        }
        return !from.after(to);
    }
}
