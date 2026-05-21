package com.proquip.common.dto.common;

import java.io.Serializable;

/**
 * ドロップダウン等の選択肢を表す汎用DTOクラス。
 *
 * <p>値（value）とラベル（label）のペアを保持し、
 * カテゴリ選択、ステータス選択、倉庫選択などのUI部品で使用される。</p>
 *
 * @author ProQuip開発チーム
 */
public class SelectOptionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 値 */
    private String value;

    /** 表示ラベル */
    private String label;

    /**
     * デフォルトコンストラクタ。
     */
    public SelectOptionDto() {
    }

    /**
     * 値とラベルを指定するコンストラクタ。
     *
     * @param value 値
     * @param label 表示ラベル
     */
    public SelectOptionDto(String value, String label) {
        this.value = value;
        this.label = label;
    }

    // --- Getter / Setter ---

    /**
     * 値を返す。
     *
     * @return 値
     */
    public String getValue() {
        return value;
    }

    /**
     * 値を設定する。
     *
     * @param value 値
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * 表示ラベルを返す。
     *
     * @return 表示ラベル
     */
    public String getLabel() {
        return label;
    }

    /**
     * 表示ラベルを設定する。
     *
     * @param label 表示ラベル
     */
    public void setLabel(String label) {
        this.label = label;
    }
}
