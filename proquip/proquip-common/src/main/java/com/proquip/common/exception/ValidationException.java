package com.proquip.common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * バリデーションエラーが発生した場合にスローされる例外。
 *
 * <p>入力値の検証に失敗した場合に使用する。
 * 複数のバリデーション違反をまとめて保持できる。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** バリデーション違反メッセージのリスト */
    private final List<String> violations;

    /**
     * フィールド名とメッセージを指定して例外を生成する。
     *
     * @param fieldName バリデーションに失敗したフィールド名
     * @param message エラーメッセージ
     */
    public ValidationException(String fieldName, String message) {
        super("VALIDATION_ERROR", fieldName + ": " + message);
        this.violations = new ArrayList<>();
        this.violations.add(fieldName + ": " + message);
    }

    /**
     * 複数のバリデーション違反メッセージを指定して例外を生成する。
     *
     * @param violations バリデーション違反メッセージのリスト
     */
    public ValidationException(List<String> violations) {
        super("VALIDATION_ERROR",
                "バリデーションエラーが" + violations.size() + "件あります。");
        this.violations = new ArrayList<>(violations);
    }

    /**
     * バリデーション違反メッセージのリストを返す。
     *
     * @return バリデーション違反メッセージの不変リスト
     */
    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }
}
