package com.proquip.common.dto.common;

import java.io.Serializable;

/**
 * 検索条件の抽象基底クラス。
 *
 * <p>ページネーションおよびソート情報を保持する共通フィールドを提供する。
 * 各ドメイン固有の検索条件DTOはこのクラスを継承して、
 * 固有の絞り込み条件フィールドを追加する。</p>
 *
 * @author ProQuip開発チーム
 */
public abstract class SearchCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ページ番号（0始まり） */
    private int page = 0;

    /** 1ページあたりの件数 */
    private int pageSize = 20;

    /** ソート対象フィールド名 */
    private String sortBy;

    /** ソート方向（asc / desc） */
    private String sortDirection = "asc";

    /**
     * デフォルトコンストラクタ。
     */
    protected SearchCriteria() {
    }

    // --- Getter / Setter ---

    /**
     * ページ番号を返す。
     *
     * @return ページ番号（0始まり）
     */
    public int getPage() {
        return page;
    }

    /**
     * ページ番号を設定する。
     *
     * @param page ページ番号（0始まり）
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 1ページあたりの件数を返す。
     *
     * @return ページサイズ
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 1ページあたりの件数を設定する。
     *
     * @param pageSize ページサイズ
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * ソート対象フィールド名を返す。
     *
     * @return ソートフィールド名
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * ソート対象フィールド名を設定する。
     *
     * @param sortBy ソートフィールド名
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * ソート方向を返す。
     *
     * @return ソート方向（"asc" または "desc"）
     */
    public String getSortDirection() {
        return sortDirection;
    }

    /**
     * ソート方向を設定する。
     *
     * @param sortDirection ソート方向（"asc" または "desc"）
     */
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
