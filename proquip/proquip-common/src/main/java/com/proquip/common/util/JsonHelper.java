package com.proquip.common.util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON操作に関するヘルパークラス。
 *
 * <p>【技術的負債 - 重大】このクラスには以下の深刻な問題がある：
 * <ul>
 *   <li>JacksonやJakarta JSON-Pを使わず、手動でJSON文字列を構築・解析している</li>
 *   <li>手動パースはエッジケースに弱く、不正なJSONで容易に壊れる</li>
 *   <li>ネストしたオブジェクトや配列の完全なパースに対応していない</li>
 *   <li>文字列連結でJSONを構築しており、エスケープ漏れのリスクがある</li>
 * </ul>
 * </p>
 *
 * <p>【対応方針】Jakarta JSON-B (Yasson) またはJacksonに全面移行すべき。
 * 当面はこのクラスを経由した簡易的なJSON操作のみサポートする。</p>
 *
 * @author ProQuip開発チーム（初期実装: 2019年、監査ログ対応: 2022年）
 * @since 1.0.0
 */
public final class JsonHelper {

    /** インスタンス化を防止 */
    private JsonHelper() {
    }

    /**
     * オブジェクトをJSON文字列に変換する。
     *
     * <p>【技術的負債】型ごとにswitch的な分岐で手動変換しており、
     * 対応していない型はtoString()でフォールバックする。
     * Jacksonの ObjectMapper.writeValueAsString() に置き換えるべき。</p>
     *
     * @param obj 変換対象のオブジェクト
     * @return JSON文字列
     */
    @SuppressWarnings("unchecked")
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        }

        if (obj instanceof Number) {
            return obj.toString();
        }

        if (obj instanceof Boolean) {
            return obj.toString();
        }

        if (obj instanceof Date) {
            // 日付は文字列として出力（DateUtils依存）
            return "\"" + DateUtils.formatDateTime((Date) obj) + "\"";
        }

        if (obj instanceof Map) {
            return toJsonMap((Map<String, Object>) obj);
        }

        if (obj instanceof List) {
            return toJsonList((List<Object>) obj);
        }

        // その他の型はtoString()をそのまま文字列値として出力
        // 【技術的負債】本来はリフレクションでフィールドを走査すべき
        return "\"" + escapeJsonString(obj.toString()) + "\"";
    }

    /**
     * MapをJSON文字列に変換する。
     *
     * <p>【技術的負債】StringBuilderで手動構築。ネストが深いと
     * スタックオーバーフローの可能性もある（再帰呼び出しの制限なし）。</p>
     *
     * @param map 変換対象のMap
     * @return JSON文字列
     */
    public static String toJsonMap(Map<String, Object> map) {
        if (map == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"");
            sb.append(escapeJsonString(entry.getKey()));
            sb.append("\":");
            sb.append(toJson(entry.getValue()));
            first = false;
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * ListをJSON配列文字列に変換する。
     *
     * @param list 変換対象のList
     * @return JSON配列文字列
     */
    private static String toJsonList(List<Object> list) {
        if (list == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(list.get(i)));
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * JSON文字列をMapに変換する。
     *
     * <p>【技術的負債 - 重大】文字を1つずつ読み取る手動パーサー。
     * 以下の制限がある：
     * <ul>
     *   <li>ネストしたオブジェクトや配列は文字列として扱われる</li>
     *   <li>Unicodeエスケープシーケンス（{@code \\uXXXX}）に非対応</li>
     *   <li>不正なJSONに対して予測不能な動作をする</li>
     *   <li>大きなJSONではパフォーマンスが極端に低下する</li>
     * </ul>
     * </p>
     *
     * @param json パース対象のJSON文字列（フラットなオブジェクトのみ対応）
     * @return パース結果のMap。パース失敗時は空のMap
     */
    public static Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> result = new HashMap<>();

        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        String trimmed = json.trim();

        // 先頭と末尾の波括弧を除去
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        } else {
            return result;
        }

        if (trimmed.isEmpty()) {
            return result;
        }

        // 簡易的なキー・バリュー解析
        // 【技術的負債】この解析はカンマを含む文字列値があると壊れる可能性がある
        int pos = 0;
        while (pos < trimmed.length()) {
            // キーの開始引用符を探す
            int keyStart = trimmed.indexOf('"', pos);
            if (keyStart < 0) break;

            int keyEnd = trimmed.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;

            String key = trimmed.substring(keyStart + 1, keyEnd);

            // コロンを探す
            int colonPos = trimmed.indexOf(':', keyEnd + 1);
            if (colonPos < 0) break;

            // 値を解析
            int valueStart = colonPos + 1;
            while (valueStart < trimmed.length()
                    && trimmed.charAt(valueStart) == ' ') {
                valueStart++;
            }

            if (valueStart >= trimmed.length()) break;

            Object value;
            int valueEnd;

            char firstChar = trimmed.charAt(valueStart);
            if (firstChar == '"') {
                // 文字列値
                valueEnd = findClosingQuote(trimmed, valueStart + 1);
                if (valueEnd < 0) break;
                value = unescapeJsonString(
                        trimmed.substring(valueStart + 1, valueEnd));
                valueEnd++; // 閉じ引用符の後に移動
            } else if (firstChar == 'n'
                    && trimmed.startsWith("null", valueStart)) {
                value = null;
                valueEnd = valueStart + 4;
            } else if (firstChar == 't'
                    && trimmed.startsWith("true", valueStart)) {
                value = Boolean.TRUE;
                valueEnd = valueStart + 4;
            } else if (firstChar == 'f'
                    && trimmed.startsWith("false", valueStart)) {
                value = Boolean.FALSE;
                valueEnd = valueStart + 5;
            } else {
                // 数値と仮定
                valueEnd = valueStart;
                while (valueEnd < trimmed.length()
                        && trimmed.charAt(valueEnd) != ','
                        && trimmed.charAt(valueEnd) != '}') {
                    valueEnd++;
                }
                String numStr = trimmed.substring(valueStart, valueEnd).trim();
                try {
                    if (numStr.contains(".")) {
                        value = new BigDecimal(numStr);
                    } else {
                        value = Long.parseLong(numStr);
                    }
                } catch (NumberFormatException e) {
                    value = numStr; // パース失敗時は文字列として保持
                }
            }

            result.put(key, value);

            // 次のカンマまたは終端へ移動
            pos = valueEnd;
            int nextComma = trimmed.indexOf(',', pos);
            if (nextComma < 0) break;
            pos = nextComma + 1;
        }

        return result;
    }

    /**
     * エスケープされた閉じ引用符の位置を探す。
     */
    private static int findClosingQuote(String str, int startPos) {
        for (int i = startPos; i < str.length(); i++) {
            if (str.charAt(i) == '\\') {
                i++; // エスケープされた文字をスキップ
                continue;
            }
            if (str.charAt(i) == '"') {
                return i;
            }
        }
        return -1;
    }

    /**
     * JSON文字列のエスケープ処理を行う。
     *
     * <p>ダブルクォート、バックスラッシュ、制御文字をエスケープする。</p>
     *
     * @param str エスケープ対象の文字列
     * @return エスケープ済みの文字列
     */
    public static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // 制御文字（U+0000〜U+001F）はUnicodeエスケープ
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * JSONエスケープシーケンスを元に戻す。
     */
    private static String unescapeJsonString(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    default:
                        sb.append(ch);
                        break;
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * 監査ログ用のJSON文字列を構築する。
     *
     * <p>フィールドの変更前後の値を記録するためのJSON文字列を生成する。</p>
     *
     * <p>【技術的負債】文字列連結でJSONを構築しており、
     * 値にダブルクォートが含まれるとJSONが壊れる可能性がある。
     * toJsonMapメソッドを使うべき。</p>
     *
     * @param fieldName 変更されたフィールド名
     * @param oldValue 変更前の値
     * @param newValue 変更後の値
     * @return 監査ログ用JSON文字列
     */
    public static String buildAuditJson(String fieldName,
                                         String oldValue,
                                         String newValue) {
        // 【技術的負債】文字列連結。escapeJsonStringを通していないケースがある。
        // 実際に本番でエスケープ漏れのバグが報告されたが、暫定対応のまま。
        return "{\"field\":\"" + escapeJsonString(fieldName) + "\""
                + ",\"oldValue\":\"" + (oldValue != null ? escapeJsonString(oldValue) : "") + "\""
                + ",\"newValue\":\"" + (newValue != null ? escapeJsonString(newValue) : "") + "\""
                + ",\"timestamp\":\"" + DateUtils.formatDateTime(new Date()) + "\""
                + "}";
    }
}
