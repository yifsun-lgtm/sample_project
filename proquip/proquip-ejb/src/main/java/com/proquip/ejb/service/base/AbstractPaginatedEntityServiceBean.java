package com.proquip.ejb.service.base;

import com.proquip.common.constant.AppConstants;
import com.proquip.ejb.entity.base.BaseEntity;

import jakarta.persistence.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ページネーション付きエンティティサービスの抽象クラス（レベル5/5）。
 * <p>
 * {@link AbstractSearchableEntityServiceBean}を拡張し、
 * ページネーション付きのリスト取得・検索機能を追加する。
 * </p>
 *
 * <p>【技術的負債 #10 - 過度な抽象化（5階層のうちレベル5）】
 * 5階層の継承は明らかにオーバーエンジニアリング。
 * この階層の全機能は、以下のいずれかで実現可能：
 * <ul>
 *   <li>Spring Data JPAのPagingAndSortingRepository</li>
 *   <li>Jakarta Data（Jakarta EE 11以降）</li>
 *   <li>単純なユーティリティメソッドとコンポジション</li>
 * </ul>
 * さらに、この階層を実際に継承しているサービスBeanは存在しない
 * （全サービスBeanがEntityManagerを直接使用）。</p>
 *
 * <p>【技術的負債 #12】ページネーション結果をMap<String, Object>で返却しており、
 * 型安全でない。専用のPageレコード/クラスを作成すべき。</p>
 *
 * @param <T> エンティティ型
 * @param <ID> ID型
 *
 * @author ProQuip開発チーム
 * @since 1.3.0
 */
public abstract class AbstractPaginatedEntityServiceBean<T extends BaseEntity, ID extends Serializable>
        extends AbstractSearchableEntityServiceBean<T, ID> {

    /**
     * ページネーション付きで全エンティティを取得する。
     *
     * <p>【技術的負債 #12】Map<String, Object>で結果を返却。
     * Page<T>のような型安全なクラスに移行すべき。</p>
     *
     * @param page ページ番号（0始まり）
     * @param size ページサイズ
     * @return ページネーション結果（items, totalCount, page, size, totalPagesを含む）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findPaginated(int page, int size) {
        // パラメータの正規化
        if (page < 0) {
            page = AppConstants.DEFAULT_PAGE_NUMBER;
        }
        if (size <= 0 || size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.DEFAULT_PAGE_SIZE;
        }

        Map<String, Object> result = new HashMap<String, Object>();

        // 総件数の取得
        long totalCount = count();

        // データの取得
        // 技術的負債 #11: 文字列連結によるJPQL
        String orderBy = getDefaultOrderBy();
        String jpql = "SELECT e FROM " + getEntityClass().getSimpleName() + " e";
        if (orderBy != null && !orderBy.isEmpty()) {
            jpql += " ORDER BY " + orderBy;
        }

        List<T> items = em.createQuery(jpql)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        // ページ情報の計算
        int totalPages = (int) Math.ceil((double) totalCount / size);

        result.put("items", items);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);
        result.put("hasNext", page < totalPages - 1);
        result.put("hasPrevious", page > 0);

        return result;
    }

    /**
     * ページネーション付きで検索する。
     *
     * @param searchParams 検索パラメータ
     * @param page ページ番号（0始まり）
     * @param size ページサイズ
     * @return ページネーション付き検索結果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> searchPaginated(Map<String, Object> searchParams,
                                                int page, int size) {
        if (page < 0) {
            page = AppConstants.DEFAULT_PAGE_NUMBER;
        }
        if (size <= 0 || size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.DEFAULT_PAGE_SIZE;
        }

        Map<String, Object> result = new HashMap<String, Object>();

        if (searchParams == null || searchParams.isEmpty()) {
            return findPaginated(page, size);
        }

        // 検索条件のJPQL構築
        // 技術的負債 #11: StringBufferによるJPQL構築（親クラスからのコピペ要素あり）
        StringBuffer jpql = new StringBuffer();
        jpql.append("SELECT e FROM ");
        jpql.append(getEntityClass().getSimpleName());
        jpql.append(" e WHERE 1=1");

        StringBuffer countJpql = new StringBuffer();
        countJpql.append("SELECT COUNT(e) FROM ");
        countJpql.append(getEntityClass().getSimpleName());
        countJpql.append(" e WHERE 1=1");

        // 技術的負債 #6: for-indexループ
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
            String condition;

            if (value instanceof String) {
                condition = " AND e." + field + " LIKE :" + paramName;
            } else {
                condition = " AND e." + field + " = :" + paramName;
            }

            jpql.append(condition);
            countJpql.append(condition);
        }

        // ソート
        String orderBy = getDefaultOrderBy();
        if (orderBy != null && !orderBy.isEmpty()) {
            jpql.append(" ORDER BY ");
            jpql.append(orderBy);
        }

        // 件数取得
        Query countQuery = em.createQuery(countJpql.toString());
        Query dataQuery = em.createQuery(jpql.toString());

        // パラメータバインド
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Object> entry = entries.get(i);
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            String paramName = "param" + i;

            if (value instanceof String) {
                String likeValue = "%" + value + "%";
                countQuery.setParameter(paramName, likeValue);
                dataQuery.setParameter(paramName, likeValue);
            } else {
                countQuery.setParameter(paramName, value);
                dataQuery.setParameter(paramName, value);
            }
        }

        long totalCount = (Long) countQuery.getSingleResult();
        List<T> items = dataQuery
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        int totalPages = (int) Math.ceil((double) totalCount / size);

        result.put("items", items);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);
        result.put("hasNext", page < totalPages - 1);
        result.put("hasPrevious", page > 0);

        return result;
    }

    /**
     * ページネーション付きでキーワード検索する。
     *
     * @param keyword 検索キーワード
     * @param page ページ番号（0始まり）
     * @param size ページサイズ
     * @return ページネーション付き検索結果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> searchByKeywordPaginated(String keyword, int page, int size) {
        if (page < 0) {
            page = AppConstants.DEFAULT_PAGE_NUMBER;
        }
        if (size <= 0 || size > AppConstants.MAX_PAGE_SIZE) {
            size = AppConstants.DEFAULT_PAGE_SIZE;
        }

        Map<String, Object> result = new HashMap<String, Object>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return findPaginated(page, size);
        }

        List<String> fields = getSearchableFields();
        if (fields == null || fields.isEmpty()) {
            return findPaginated(page, size);
        }

        // 技術的負債 #11: StringBufferによるJPQL構築
        StringBuffer wherePart = new StringBuffer();
        wherePart.append("(");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                wherePart.append(" OR ");
            }
            wherePart.append("e.");
            wherePart.append(fields.get(i));
            wherePart.append(" LIKE :keyword");
        }
        wherePart.append(")");

        String entityName = getEntityClass().getSimpleName();

        // 件数取得
        String countJpql = "SELECT COUNT(e) FROM " + entityName + " e WHERE " + wherePart;
        long totalCount = (Long) em.createQuery(countJpql)
                .setParameter("keyword", "%" + keyword.trim() + "%")
                .getSingleResult();

        // データ取得
        String dataJpql = "SELECT e FROM " + entityName + " e WHERE " + wherePart;
        String orderBy = getDefaultOrderBy();
        if (orderBy != null && !orderBy.isEmpty()) {
            dataJpql += " ORDER BY " + orderBy;
        }

        List<T> items = em.createQuery(dataJpql)
                .setParameter("keyword", "%" + keyword.trim() + "%")
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        int totalPages = (int) Math.ceil((double) totalCount / size);

        result.put("items", items);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);
        result.put("hasNext", page < totalPages - 1);
        result.put("hasPrevious", page > 0);

        return result;
    }
}
