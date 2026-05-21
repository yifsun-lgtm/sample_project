package com.proquip.ejb.service.base;

import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 検索機能付きエンティティサービスの抽象クラス（レベル4/5）。
 * <p>
 * {@link AbstractValidatedEntityServiceBean}を拡張し、
 * 動的な検索条件によるクエリ構築機能を追加する。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化（5階層のうちレベル4）】
 * 動的クエリ構築はCriteria API、QueryDSL、または
 * Spring Data JPAのSpecificationパターンで十分。
 * この抽象クラスの文字列連結によるJPQL構築は
 * 型安全性に欠け、保守が困難。</p>
 *
 * <p>【技術的負債 #11】StringBufferによるJPQL構築。
 * SQLインジェクションのリスクはパラメータバインドで軽減されているが、
 * コードの可読性が低い。</p>
 *
 * @param <T> エンティティ型
 * @param <ID> ID型
 *
 * @author ProQuip開発チーム
 * @since 1.3.0
 */
public abstract class AbstractSearchableEntityServiceBean<T extends BaseEntity, ID extends Serializable>
        extends AbstractValidatedEntityServiceBean<T, ID> {

    /**
     * 動的条件で検索する。
     *
     * <p>検索パラメータのMapを受け取り、動的にJPQLを構築して実行する。
     * パラメータ名はエンティティのフィールド名に対応する。</p>
     *
     * <p>【技術的負債 #11】StringBufferによるJPQL構築。
     * Criteria APIに移行すべき。</p>
     *
     * @param searchParams 検索パラメータ（キー: フィールド名、値: 検索値）
     * @return 検索結果のリスト
     */
    @SuppressWarnings("unchecked")
    public List<T> search(Map<String, Object> searchParams) {
        if (searchParams == null || searchParams.isEmpty()) {
            return findAll();
        }

        // 技術的負債 #11: StringBufferによるJPQL構築
        StringBuffer jpql = new StringBuffer();
        jpql.append("SELECT e FROM ");
        jpql.append(getEntityClass().getSimpleName());
        jpql.append(" e WHERE 1=1");

        // 動的条件の組み立て
        // 技術的負債 #6: for-indexループでMap.entrySetをイテレート
        List<Map.Entry<String, Object>> entries = new ArrayList<Map.Entry<String, Object>>(
                searchParams.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Object> entry = entries.get(i);
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            String paramName = "param" + i;

            if (value instanceof String) {
                // 文字列の場合はLIKE検索
                jpql.append(" AND e.");
                jpql.append(field);
                jpql.append(" LIKE :");
                jpql.append(paramName);
            } else {
                // その他の型は完全一致
                jpql.append(" AND e.");
                jpql.append(field);
                jpql.append(" = :");
                jpql.append(paramName);
            }
        }

        // ソート（デフォルトはID降順）
        String orderBy = getDefaultOrderBy();
        if (orderBy != null && !orderBy.isEmpty()) {
            jpql.append(" ORDER BY ");
            jpql.append(orderBy);
        }

        Query query = em.createQuery(jpql.toString());

        // パラメータのバインド
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Object> entry = entries.get(i);
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            String paramName = "param" + i;

            if (value instanceof String) {
                query.setParameter(paramName, "%" + value + "%");
            } else {
                query.setParameter(paramName, value);
            }
        }

        return query.getResultList();
    }

    /**
     * キーワードで検索する。
     *
     * <p>検索対象のフィールド一覧を{@link #getSearchableFields()}で取得し、
     * いずれかのフィールドに部分一致するエンティティを返す。</p>
     *
     * @param keyword 検索キーワード
     * @return 検索結果のリスト
     */
    @SuppressWarnings("unchecked")
    public List<T> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }

        List<String> fields = getSearchableFields();
        if (fields == null || fields.isEmpty()) {
            return findAll();
        }

        // 技術的負債 #11: StringBufferによるJPQL構築
        StringBuffer jpql = new StringBuffer();
        jpql.append("SELECT e FROM ");
        jpql.append(getEntityClass().getSimpleName());
        jpql.append(" e WHERE (");

        // 技術的負債 #6: for-indexループ
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                jpql.append(" OR ");
            }
            jpql.append("e.");
            jpql.append(fields.get(i));
            jpql.append(" LIKE :keyword");
        }
        jpql.append(")");

        String orderBy = getDefaultOrderBy();
        if (orderBy != null && !orderBy.isEmpty()) {
            jpql.append(" ORDER BY ");
            jpql.append(orderBy);
        }

        return em.createQuery(jpql.toString())
                .setParameter("keyword", "%" + keyword.trim() + "%")
                .getResultList();
    }

    /**
     * 検索対象のフィールド名リストを返す。
     *
     * <p>サブクラスでオーバーライドしてキーワード検索対象フィールドを指定する。
     * デフォルトでは空リスト（キーワード検索不可）。</p>
     *
     * @return 検索対象フィールド名のリスト
     */
    protected List<String> getSearchableFields() {
        return new ArrayList<String>();
    }

    /**
     * デフォルトのソート順を返す。
     *
     * <p>サブクラスでオーバーライドしてデフォルトのORDER BY句を指定する。</p>
     *
     * @return ORDER BY句の内容（例: "e.name ASC"）。nullの場合はソートなし。
     */
    protected String getDefaultOrderBy() {
        return "e.id DESC";
    }
}
