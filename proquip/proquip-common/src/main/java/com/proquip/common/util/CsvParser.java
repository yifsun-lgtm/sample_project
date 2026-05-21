package com.proquip.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV解析ユーティリティクラス。
 *
 * <p>CSVファイルの読み込み・解析を行う。ダブルクォートで囲まれたフィールドや
 * エスケープされた文字を処理する。</p>
 *
 * <p>【技術的負債】RFC 4180の一部のエッジケースに対応していない：
 * <ul>
 *   <li>フィールド内の改行（CRLF）を含むレコードの処理が不完全</li>
 *   <li>BOM（Byte Order Mark）付きUTF-8の自動検出に非対応</li>
 *   <li>大容量ファイルのストリーミング処理に最適化されていない</li>
 * </ul>
 * Apache Commons CSVやOpenCSVへの移行を検討すべき。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public final class CsvParser {

    /** デフォルトの区切り文字 */
    private static final char DEFAULT_DELIMITER = ',';

    /** クォート文字 */
    private static final char QUOTE_CHAR = '"';

    /** インスタンス化を防止 */
    private CsvParser() {
    }

    /**
     * InputStreamからCSVデータを読み込み、行ごとにフィールドの配列として返す。
     *
     * <p>先頭行はヘッダ行として扱われるが、本メソッドでは特別な処理はしない。
     * ヘッダの解釈は呼び出し元に委ねる。</p>
     *
     * <p>【技術的負債】全行をメモリに読み込むため、
     * 大容量ファイル（AppConstants.MAX_CSV_IMPORT_ROWS超）ではOOMの恐れがある。</p>
     *
     * @param inputStream CSVデータのInputStream
     * @return 各行のフィールド配列のリスト
     * @throws IOException 読み込みに失敗した場合
     */
    public static List<String[]> parse(InputStream inputStream) throws IOException {
        return parse(inputStream, DEFAULT_DELIMITER);
    }

    /**
     * 区切り文字を指定してCSVデータを読み込む。
     *
     * @param inputStream CSVデータのInputStream
     * @param delimiter 区切り文字
     * @return 各行のフィールド配列のリスト
     * @throws IOException 読み込みに失敗した場合
     */
    public static List<String[]> parse(InputStream inputStream,
                                        char delimiter) throws IOException {
        List<String[]> rows = new ArrayList<>();

        if (inputStream == null) {
            return rows;
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            // 空行はスキップ
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] fields = parseLine(line, delimiter);
            rows.add(fields);
        }

        // 【技術的負債】readerをcloseしていない。try-with-resourcesを使うべき。
        // ただしInputStreamの所有権は呼び出し元にあるため、ここでcloseすると
        // 問題になるケースがある。設計を見直す必要あり。

        return rows;
    }

    /**
     * CSV形式の1行を解析してフィールドの配列を返す。
     *
     * <p>ダブルクォートで囲まれたフィールド内のカンマや
     * エスケープされたダブルクォート（""）を処理する。</p>
     *
     * <p>【技術的負債】フィールド内にCR/LFを含むケースに非対応。
     * readLine()で行を読んでいるため、フィールド内の改行が行区切りとして扱われる。</p>
     *
     * @param line 解析対象の行文字列
     * @return フィールドの配列
     */
    public static String[] parseLine(String line) {
        return parseLine(line, DEFAULT_DELIMITER);
    }

    /**
     * 区切り文字を指定してCSV行を解析する。
     *
     * @param line 解析対象の行文字列
     * @param delimiter 区切り文字
     * @return フィールドの配列
     */
    public static String[] parseLine(String line, char delimiter) {
        if (line == null) {
            return new String[0];
        }

        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (inQuotes) {
                if (ch == QUOTE_CHAR) {
                    // 次の文字もクォートならエスケープされたクォート
                    if (i + 1 < line.length()
                            && line.charAt(i + 1) == QUOTE_CHAR) {
                        currentField.append(QUOTE_CHAR);
                        i++; // 次のクォートをスキップ
                    } else {
                        // クォート終了
                        inQuotes = false;
                    }
                } else {
                    currentField.append(ch);
                }
            } else {
                if (ch == QUOTE_CHAR) {
                    inQuotes = true;
                } else if (ch == delimiter) {
                    fields.add(currentField.toString());
                    currentField = new StringBuilder();
                } else {
                    currentField.append(ch);
                }
            }
        }

        // 最後のフィールドを追加
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * CSVフィールドとして出力する際のエスケープ処理を行う。
     *
     * <p>フィールドにカンマ、ダブルクォート、改行が含まれる場合、
     * フィールド全体をダブルクォートで囲み、
     * 内部のダブルクォートをエスケープする。</p>
     *
     * @param field エスケープ対象のフィールド値
     * @return エスケープ済みのフィールド文字列
     */
    public static String escape(String field) {
        if (field == null) {
            return "";
        }

        boolean needsQuoting = field.contains(",")
                || field.contains("\"")
                || field.contains("\n")
                || field.contains("\r");

        if (!needsQuoting) {
            return field;
        }

        // ダブルクォートをエスケープ（" → ""）し、全体をクォートで囲む
        String escaped = field.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
