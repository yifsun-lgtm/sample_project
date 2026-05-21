package com.proquip.ejb.service.base;

import com.proquip.common.exception.ValidationException;
import com.proquip.ejb.entity.base.BaseEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * バリデーション付きエンティティサービスの抽象クラス（レベル3/5）。
 * <p>
 * {@link AbstractAuditedEntityServiceBean}を拡張し、
 * エンティティの作成・更新時に自動バリデーションを行う。
 * サブクラスは{@link #validate(BaseEntity)}メソッドをオーバーライドして
 * エンティティ固有のバリデーションルールを実装する。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化（5階層のうちレベル3）】
 * Bean Validation (Jakarta Validation) を使えばこの階層は不要。
 * {@code @Valid}アノテーションとカスタムバリデータで同等の機能を実現できる。
 * さらに、バリデーションエラーの収集方式が独自実装であり、
 * {@code ConstraintViolation}との互換性がない。</p>
 *
 * @param <T> エンティティ型
 * @param <ID> ID型
 *
 * @author ProQuip開発チーム
 * @since 1.3.0
 */
public abstract class AbstractValidatedEntityServiceBean<T extends BaseEntity, ID extends Serializable>
        extends AbstractAuditedEntityServiceBean<T, ID> {

    /**
     * {@inheritDoc}
     *
     * <p>作成前にバリデーションを実行する。</p>
     */
    @Override
    public T create(T entity) {
        performValidation(entity);
        return super.create(entity);
    }

    /**
     * {@inheritDoc}
     *
     * <p>更新前にバリデーションを実行する。</p>
     */
    @Override
    public T update(T entity) {
        performValidation(entity);
        return super.update(entity);
    }

    /**
     * エンティティのバリデーションを実行する。
     *
     * <p>バリデーションエラーがある場合は{@link ValidationException}をスローする。</p>
     *
     * @param entity バリデーション対象のエンティティ
     * @throws ValidationException バリデーションエラー
     */
    private void performValidation(T entity) {
        List<String> errors = new ArrayList<String>();

        validate(entity, errors);

        if (!errors.isEmpty()) {
            // 技術的負債: 最初のエラーのみスロー。全エラーを返すべき。
            throw new ValidationException(getEntityName(), errors.get(0));
        }
    }

    /**
     * エンティティ固有のバリデーションを行う。
     *
     * <p>サブクラスでオーバーライドしてバリデーションルールを実装する。
     * エラーがある場合はerrorsリストにメッセージを追加する。</p>
     *
     * <p>【技術的負債】独自のエラー収集方式。Jakarta Bean Validationの
     * {@code ConstraintViolation}に移行すべき。</p>
     *
     * @param entity バリデーション対象
     * @param errors エラーメッセージの収集リスト
     */
    protected void validate(T entity, List<String> errors) {
        // デフォルトではnullチェックのみ
        if (entity == null) {
            errors.add(getEntityName() + "がnullです。");
        }
    }

    /**
     * 必須フィールドのチェックを行うヘルパーメソッド。
     *
     * @param value チェック対象の値
     * @param fieldName フィールド名
     * @param errors エラーメッセージの収集リスト
     */
    protected void requireNotNull(Object value, String fieldName, List<String> errors) {
        if (value == null) {
            errors.add(fieldName + "は必須です。");
        }
    }

    /**
     * 文字列の必須チェック（null・空文字チェック）を行うヘルパーメソッド。
     *
     * @param value チェック対象の文字列
     * @param fieldName フィールド名
     * @param errors エラーメッセージの収集リスト
     */
    protected void requireNotEmpty(String value, String fieldName, List<String> errors) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(fieldName + "は必須です。");
        }
    }
}
