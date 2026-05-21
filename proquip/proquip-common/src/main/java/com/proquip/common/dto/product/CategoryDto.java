package com.proquip.common.dto.product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * カテゴリデータ転送オブジェクト。
 *
 * <p>商品カテゴリの階層情報を表現する。親カテゴリ情報と子カテゴリ一覧を
 * 保持し、ツリー構造としてフロントエンドに提供する。</p>
 *
 * @author ProQuip開発チーム
 */
public class CategoryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** カテゴリID */
    private Long id;

    /** カテゴリ名 */
    private String name;

    /** 親カテゴリID（ルートカテゴリの場合はnull） */
    private Long parentId;

    /** 親カテゴリ名 */
    private String parentName;

    /** 階層レベル（ルート = 0） */
    private int level;

    /** 有効フラグ */
    private boolean active;

    /** 所属商品数 */
    private int productCount;

    /** 子カテゴリ一覧 */
    private List<CategoryDto> children = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public CategoryDto() {
    }

    // --- Getter / Setter ---

    /**
     * カテゴリIDを返す。
     *
     * @return カテゴリID
     */
    public Long getId() {
        return id;
    }

    /**
     * カテゴリIDを設定する。
     *
     * @param id カテゴリID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * カテゴリ名を返す。
     *
     * @return カテゴリ名
     */
    public String getName() {
        return name;
    }

    /**
     * カテゴリ名を設定する。
     *
     * @param name カテゴリ名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 親カテゴリIDを返す。
     *
     * @return 親カテゴリID（ルートの場合はnull）
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * 親カテゴリIDを設定する。
     *
     * @param parentId 親カテゴリID
     */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 親カテゴリ名を返す。
     *
     * @return 親カテゴリ名
     */
    public String getParentName() {
        return parentName;
    }

    /**
     * 親カテゴリ名を設定する。
     *
     * @param parentName 親カテゴリ名
     */
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    /**
     * 階層レベルを返す。
     *
     * @return 階層レベル（ルート = 0）
     */
    public int getLevel() {
        return level;
    }

    /**
     * 階層レベルを設定する。
     *
     * @param level 階層レベル
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * 有効フラグを返す。
     *
     * @return 有効な場合 true
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 有効フラグを設定する。
     *
     * @param active 有効な場合 true
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 所属商品数を返す。
     *
     * @return 商品数
     */
    public int getProductCount() {
        return productCount;
    }

    /**
     * 所属商品数を設定する。
     *
     * @param productCount 商品数
     */
    public void setProductCount(int productCount) {
        this.productCount = productCount;
    }

    /**
     * 子カテゴリ一覧を返す。
     *
     * @return 子カテゴリのリスト
     */
    public List<CategoryDto> getChildren() {
        return children;
    }

    /**
     * 子カテゴリ一覧を設定する。
     *
     * @param children 子カテゴリのリスト
     */
    public void setChildren(List<CategoryDto> children) {
        this.children = children;
    }
}
