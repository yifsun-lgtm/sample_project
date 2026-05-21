package com.proquip.common.exception;

/**
 * データインポート処理中のエラー時にスローされる例外。
 *
 * <p>CSVやExcelファイルのインポート処理で、特定の行・列にエラーが
 * 発生した場合に使用する。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public class ImportException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** エラーが発生した行番号（1始まり、ヘッダ行含む） */
    private final int rowNumber;

    /** エラーが発生した列名 */
    private final String columnName;

    /**
     * 行番号・列名・メッセージを指定して例外を生成する。
     *
     * @param rowNumber エラーが発生した行番号
     * @param columnName エラーが発生した列名
     * @param message エラーメッセージ
     */
    public ImportException(int rowNumber, String columnName, String message) {
        super("IMPORT_ERROR",
                "インポートエラー（" + rowNumber + "行目、列: "
                        + columnName + "）: " + message);
        this.rowNumber = rowNumber;
        this.columnName = columnName;
    }

    /**
     * エラーが発生した行番号を返す。
     *
     * @return 行番号
     */
    public int getRowNumber() {
        return rowNumber;
    }

    /**
     * エラーが発生した列名を返す。
     *
     * @return 列名
     */
    public String getColumnName() {
        return columnName;
    }
}
