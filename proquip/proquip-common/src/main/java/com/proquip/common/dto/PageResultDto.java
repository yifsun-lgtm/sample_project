package com.proquip.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ページネーション結果の汎用ラッパーDTO。
 *
 * <p>一覧APIの応答で使用する汎用的なページネーションラッパー。
 * Spring Data互換のフィールド名（content, totalElements, number, size等）を使用する。</p>
 *
 * @param <T> 結果アイテムの型
 *
 * @author ProQuip開発チーム
 */
public class PageResultDto<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 結果アイテムのリスト */
    private List<T> content = new ArrayList<>();

    /** 総件数 */
    private long totalElements;

    /** 現在ページ（0始まり） */
    private int number;

    /** ページサイズ */
    private int size;

    /** 総ページ数 */
    private int totalPages;

    /** 最初のページかどうか */
    private boolean first;

    /** 最後のページかどうか */
    private boolean last;

    /** 結果が空かどうか */
    private boolean empty;

    /**
     * デフォルトコンストラクタ。
     */
    public PageResultDto() {
    }

    /**
     * 全パラメータを指定するコンストラクタ。
     *
     * <p>totalPages, first, last, empty はtotalElementsとsizeから自動計算される。</p>
     *
     * @param content       結果アイテムのリスト
     * @param totalElements 総件数
     * @param number        現在ページ（0始まり）
     * @param size          ページサイズ
     */
    public PageResultDto(List<T> content, long totalElements, int number, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.number = number;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.first = number == 0;
        this.last = number >= totalPages - 1;
        this.empty = content == null || content.isEmpty();
    }

    // --- Getter / Setter ---

    /**
     * 結果アイテムのリストを返す。
     *
     * @return 結果アイテムのリスト
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * 結果アイテムのリストを設定する。
     *
     * @param content 結果アイテムのリスト
     */
    public void setContent(List<T> content) {
        this.content = content;
    }

    /**
     * 総件数を返す。
     *
     * @return 総件数
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * 総件数を設定する。
     *
     * @param totalElements 総件数
     */
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    /**
     * 現在ページを返す。
     *
     * @return 現在ページ（0始まり）
     */
    public int getNumber() {
        return number;
    }

    /**
     * 現在ページを設定する。
     *
     * @param number 現在ページ（0始まり）
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * ページサイズを返す。
     *
     * @return ページサイズ
     */
    public int getSize() {
        return size;
    }

    /**
     * ページサイズを設定する。
     *
     * @param size ページサイズ
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 総ページ数を返す。
     *
     * @return 総ページ数
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 総ページ数を設定する。
     *
     * @param totalPages 総ページ数
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * 最初のページかどうかを返す。
     *
     * @return 最初のページの場合 {@code true}
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * 最初のページかどうかを設定する。
     *
     * @param first 最初のページの場合 {@code true}
     */
    public void setFirst(boolean first) {
        this.first = first;
    }

    /**
     * 最後のページかどうかを返す。
     *
     * @return 最後のページの場合 {@code true}
     */
    public boolean isLast() {
        return last;
    }

    /**
     * 最後のページかどうかを設定する。
     *
     * @param last 最後のページの場合 {@code true}
     */
    public void setLast(boolean last) {
        this.last = last;
    }

    /**
     * 結果が空かどうかを返す。
     *
     * @return 結果が空の場合 {@code true}
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * 結果が空かどうかを設定する。
     *
     * @param empty 結果が空の場合 {@code true}
     */
    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    /**
     * 次のページが存在するかどうかを返す。
     *
     * @return 次ページが存在する場合 {@code true}
     */
    public boolean hasNextPage() {
        return number < totalPages - 1;
    }

    /**
     * 前のページが存在するかどうかを返す。
     *
     * @return 前ページが存在する場合 {@code true}
     */
    public boolean hasPreviousPage() {
        return number > 0;
    }
}
