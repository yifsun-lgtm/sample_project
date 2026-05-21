package com.proquip.ejb.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * カスタムクエリビルダーユーティリティ。
 *
 * <p>SQLクエリを流暢（Fluent）APIで構築するためのユーティリティクラス。
 * EntityManagerをラップし、StringBuilderを使ってSQL文字列を手動で組み立てる。</p>
 *
 * <p>技術的負債 #9: 一部のDAOで使用されているが、DAO自身が持つクエリロジックと重複しており、
 * 実質的にデッドコードに近い状態。また、Criteria APIの劣化版とも言える。</p>
 *
 * <p>技術的負債 #18: 独自のクエリ構築手段を提供しており、
 * Criteria API・JPQL・ネイティブSQLに加えて4つ目のクエリ方式を追加してしまっている。</p>
 *
 * <p>技術的負債: {@link #or(String, String, Object)} メソッドは正しく実装されておらず、
 * 意図した結果を返さない。</p>
 *
 * @author ProQuip開発チーム
 */
public class CustomQueryBuilder {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(CustomQueryBuilder.class.getName());

    /** SELECT句 */
    private String selectClause;

    /** FROM句 */
    private String fromClause;

    /** WHERE条件のリスト */
    private final List<String> whereConditions = new ArrayList<>();

    /** ORDER BY句 */
    private String orderByClause;

    /** パラメータマップ */
    private final Map<String, Object> parameters = new HashMap<>();

    /** パラメータカウンター */
    private int paramCounter = 0;

    /** EntityManagerへの参照 */
    private final EntityManager entityManager;

    /**
     * コンストラクタ。
     *
     * @param entityManager 使用するEntityManager
     */
    public CustomQueryBuilder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * SELECT句を設定する。
     *
     * <p>技術的負債: エスケープ処理やバリデーションが一切行われていない。</p>
     *
     * @param columns 取得するカラム（例: "*", "p.id, p.name"）
     * @return このビルダーインスタンス
     */
    public CustomQueryBuilder select(String columns) {
        this.selectClause = "SELECT " + columns;
        return this;
    }

    /**
     * FROM句を設定する。
     *
     * @param table テーブル名（エイリアス含む、例: "product p"）
     * @return このビルダーインスタンス
     */
    public CustomQueryBuilder from(String table) {
        this.fromClause = " FROM " + table;
        return this;
    }

    /**
     * WHERE条件を追加する（AND条件）。
     *
     * <p>パラメータバインディングを使用してSQL文を構築する。</p>
     *
     * @param column   カラム名
     * @param operator 演算子（例: "=", "LIKE", ">="）
     * @param value    パラメータ値
     * @return このビルダーインスタンス
     */
    public CustomQueryBuilder where(String column, String operator, Object value) {
        String paramName = "param" + (++paramCounter);
        whereConditions.add(column + " " + operator + " :" + paramName);
        parameters.put(paramName, value);
        return this;
    }

    /**
     * AND条件を追加する。
     *
     * <p>{@link #where(String, String, Object)} と同じ動作だが、
     * 意味的にAND条件を明示するために用意されている。</p>
     *
     * @param column   カラム名
     * @param operator 演算子
     * @param value    パラメータ値
     * @return このビルダーインスタンス
     */
    public CustomQueryBuilder and(String column, String operator, Object value) {
        // 技術的負債: where() と全く同じ実装で冗長
        return where(column, operator, value);
    }

    /**
     * OR条件を追加する。
     *
     * <p><strong>警告: このメソッドは正しく動作しません。</strong></p>
     *
     * <p>技術的負債: OR条件の実装が不完全。単にAND条件として追加されてしまい、
     * 意図したOR条件にならない。修正が必要だが、使用箇所の影響範囲が
     * 不明のため放置されている。</p>
     *
     * @param column   カラム名
     * @param operator 演算子
     * @param value    パラメータ値
     * @return このビルダーインスタンス
     */
    public CustomQueryBuilder or(String column, String operator, Object value) {
        // 技術的負債: OR条件として機能していない
        // 本来は直前の条件とOR結合すべきだが、AND条件として追加されてしまう
        LOG.log(Level.WARNING, "CustomQueryBuilder.or() は正しく動作しません。AND条件として扱われます。");
        String paramName = "param" + (++paramCounter);
        whereConditions.add(column + " " + operator + " :" + paramName);
        parameters.put(paramName, value);
        return this;
    }

    /**
     * ORDER BY句を設定する。
     *
     * @param orderBy ソート条件（例: "p.name ASC"）
     * @return このビルダーインスタンス
     */
    public CustomQueryBuilder orderBy(String orderBy) {
        this.orderByClause = " ORDER BY " + orderBy;
        return this;
    }

    /**
     * SQL文字列を構築する。
     *
     * <p>設定されたSELECT、FROM、WHERE、ORDER BY句を結合してSQL文を生成する。</p>
     *
     * @return 構築されたSQL文字列
     */
    public String build() {
        StringBuilder sb = new StringBuilder();

        if (selectClause == null) {
            sb.append("SELECT *");
        } else {
            sb.append(selectClause);
        }

        if (fromClause == null) {
            throw new IllegalStateException("FROM句が設定されていません。");
        }
        sb.append(fromClause);

        if (!whereConditions.isEmpty()) {
            sb.append(" WHERE ");
            // 技術的負債: OR条件が追加されても全てANDで結合される
            sb.append(String.join(" AND ", whereConditions));
        }

        if (orderByClause != null) {
            sb.append(orderByClause);
        }

        String sql = sb.toString();
        LOG.log(Level.FINE, "構築されたSQL: {0}", sql);
        return sql;
    }

    /**
     * 構築されたクエリを実行し、結果を返す。
     *
     * <p>技術的負債: 戻り値が {@code List<Object[]>} であり、
     * 型安全性が確保されていない。呼び出し側でキャストが必要。</p>
     *
     * @return クエリ結果のリスト（各行は {@code Object[]}）
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> execute() {
        String sql = build();
        Query query = entityManager.createNativeQuery(sql);

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        return query.getResultList();
    }

    /**
     * 構築されたクエリを実行し、指定されたエンティティクラスにマッピングする。
     *
     * @param entityClass マッピング先のエンティティクラス
     * @param <E>         エンティティの型
     * @return クエリ結果のリスト
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> execute(Class<E> entityClass) {
        String sql = build();
        Query query = entityManager.createNativeQuery(sql, entityClass);

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        return query.getResultList();
    }

    /**
     * ビルダーの状態をリセットする。
     *
     * @return このビルダーインスタンス
     */
    public CustomQueryBuilder reset() {
        selectClause = null;
        fromClause = null;
        whereConditions.clear();
        orderByClause = null;
        parameters.clear();
        paramCounter = 0;
        return this;
    }
}
