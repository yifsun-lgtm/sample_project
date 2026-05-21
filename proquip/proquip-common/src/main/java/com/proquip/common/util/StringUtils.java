package com.proquip.common.util;

import java.util.Random;

/**
 * 文字列操作に関するユーティリティクラス。
 *
 * <p>【技術的負債】このクラスには以下の問題がある：
 * <ul>
 *   <li>Apache Commons Lang3のStringUtilsと機能が大幅に重複している</li>
 *   <li>StringBuilderではなくStringBufferを使用している箇所がある（不要な同期化）</li>
 *   <li>generateRandomStringでjava.util.Randomを使用（セキュリティ用途にはSecureRandomを使うべき）</li>
 *   <li>sanitizeメソッドのXSS対策が不完全</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public final class StringUtils {

    /** ランダム文字列生成用の文字セット */
    private static final String ALPHA_NUMERIC =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    // 【技術的負債】java.util.Randomはスレッド間でシードが予測可能。
    // セキュリティに関わる用途（トークン生成等）ではSecureRandomを使うべき。
    private static final Random RANDOM = new Random();

    /** インスタンス化を防止 */
    private StringUtils() {
    }

    /**
     * 文字列がnullまたは空文字列かどうかを判定する。
     *
     * <p>【技術的負債】Apache Commons Lang3のStringUtils.isEmpty()と同等。
     * ライブラリを使えば不要。</p>
     *
     * @param str 検査対象の文字列
     * @return nullまたは空文字列の場合true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * 文字列がnullでも空文字列でもないかどうかを判定する。
     *
     * @param str 検査対象の文字列
     * @return nullでも空文字列でもない場合true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 文字列がnullまたは空白文字のみかどうかを判定する。
     *
     * @param str 検査対象の文字列
     * @return nullまたは空白文字のみの場合true
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 文字列を指定した長さで切り詰める。
     *
     * @param str 対象文字列
     * @param maxLength 最大文字数
     * @return 切り詰められた文字列。maxLengthを超えない場合はそのまま返す
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * 文字列の左側を指定した文字でパディングする。
     *
     * <p>【技術的負債】StringBufferを使用しているが、
     * マルチスレッドでのアクセスはないためStringBuilderで十分。</p>
     *
     * @param str 対象文字列
     * @param length パディング後の全体長
     * @param padChar パディングに使用する文字
     * @return パディングされた文字列
     */
    public static String padLeft(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        // 【技術的負債】StringBufferを使用。StringBuilderに変更すべき。
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length - str.length(); i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * 文字列の右側を指定した文字でパディングする。
     *
     * <p>【技術的負債】StringBufferを使用。</p>
     *
     * @param str 対象文字列
     * @param length パディング後の全体長
     * @param padChar パディングに使用する文字
     * @return パディングされた文字列
     */
    public static String padRight(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuffer sb = new StringBuffer(str);
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    /**
     * キャメルケースをスネークケースに変換する。
     *
     * <p>例: "purchaseOrderId" → "purchase_order_id"</p>
     *
     * @param camelCase キャメルケースの文字列
     * @return スネークケースの文字列
     */
    public static String toSnakeCase(String camelCase) {
        if (isEmpty(camelCase)) {
            return camelCase;
        }
        // 大文字の前にアンダースコアを挿入し、全体を小文字に変換
        String result = camelCase.replaceAll("([a-z])([A-Z])", "$1_$2");
        return result.toLowerCase();
    }

    /**
     * スネークケースをキャメルケース（lower camel case）に変換する。
     *
     * <p>例: "purchase_order_id" → "purchaseOrderId"</p>
     *
     * @param snakeCase スネークケースの文字列
     * @return キャメルケースの文字列
     */
    public static String toCamelCase(String snakeCase) {
        if (isEmpty(snakeCase)) {
            return snakeCase;
        }
        String[] parts = snakeCase.split("_");
        StringBuffer sb = new StringBuffer(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    sb.append(parts[i].substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    /**
     * 指定した長さのランダム英数字文字列を生成する。
     *
     * <p>【技術的負債】java.util.Randomを使用しているため、
     * セキュリティトークンやパスワードリセットキーの生成には不適切。
     * そのような用途ではjava.security.SecureRandomを使用すること。</p>
     *
     * @param length 生成する文字列の長さ
     * @return ランダム英数字文字列
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHA_NUMERIC.length());
            sb.append(ALPHA_NUMERIC.charAt(index));
        }
        return sb.toString();
    }

    /**
     * メールアドレスの中間部分をマスクする。
     *
     * <p>例: "taro.yamada@example.com" → "ta***da@example.com"</p>
     *
     * @param email メールアドレス
     * @return マスクされたメールアドレス。不正な形式の場合はそのまま返す
     */
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 4) {
            // 短いローカルパートの場合は先頭1文字のみ残す
            return local.charAt(0) + "***@" + domain;
        }

        // 先頭2文字と末尾2文字を残してマスク
        return local.substring(0, 2) + "***"
                + local.substring(local.length() - 2) + "@" + domain;
    }

    /**
     * HTMLタグおよびスクリプト要素をサニタイズする。
     *
     * <p>【技術的負債 - セキュリティ】このメソッドは基本的なXSSベクターしか
     * 対処しておらず、以下のケースに対応していない：
     * <ul>
     *   <li>イベントハンドラ属性（onerror, onload等）</li>
     *   <li>data: URIスキーム</li>
     *   <li>CSS expressionインジェクション</li>
     *   <li>エンコードされた攻撃文字列</li>
     * </ul>
     * OWASPのHTMLサニタイザーライブラリを使用すべき。</p>
     *
     * @param input サニタイズ対象の文字列
     * @return サニタイズ済みの文字列
     */
    public static String sanitize(String input) {
        if (isEmpty(input)) {
            return input;
        }
        String result = input;
        result = result.replace("&", "&amp;");
        result = result.replace("<", "&lt;");
        result = result.replace(">", "&gt;");
        result = result.replace("\"", "&quot;");
        result = result.replace("'", "&#x27;");
        // TODO: javascript:プロトコル、data:URI、イベントハンドラ等は未対応
        return result;
    }
}
