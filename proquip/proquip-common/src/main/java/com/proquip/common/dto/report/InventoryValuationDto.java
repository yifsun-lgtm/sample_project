package com.proquip.common.dto.report;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 在庫評価レポートデータ転送オブジェクト。
 *
 * <p>在庫の金額評価情報を保持する。倉庫別、カテゴリ別の
 * 在庫金額を集計して提供する。</p>
 *
 * @author ProQuip開発チーム
 */
public class InventoryValuationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** レポート生成日 */
    private String reportDate;

    /** 総在庫金額 */
    private BigDecimal totalValuation;

    /** 総在庫品目数 */
    private int totalItemCount;

    /** 総在庫数量 */
    private int totalQuantity;

    /** 倉庫別評価一覧 */
    private List<WarehouseValuation> warehouseBreakdown = new ArrayList<>();

    /** カテゴリ別評価一覧 */
    private List<CategoryValuation> categoryBreakdown = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryValuationDto() {
    }

    // --- Getter / Setter ---

    /**
     * レポート生成日を返す。
     *
     * @return レポート生成日
     */
    public String getReportDate() {
        return reportDate;
    }

    /**
     * レポート生成日を設定する。
     *
     * @param reportDate レポート生成日
     */
    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    /**
     * 総在庫金額を返す。
     *
     * @return 総在庫金額
     */
    public BigDecimal getTotalValuation() {
        return totalValuation;
    }

    /**
     * 総在庫金額を設定する。
     *
     * @param totalValuation 総在庫金額
     */
    public void setTotalValuation(BigDecimal totalValuation) {
        this.totalValuation = totalValuation;
    }

    /**
     * 総在庫品目数を返す。
     *
     * @return 品目数
     */
    public int getTotalItemCount() {
        return totalItemCount;
    }

    /**
     * 総在庫品目数を設定する。
     *
     * @param totalItemCount 品目数
     */
    public void setTotalItemCount(int totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    /**
     * 総在庫数量を返す。
     *
     * @return 総数量
     */
    public int getTotalQuantity() {
        return totalQuantity;
    }

    /**
     * 総在庫数量を設定する。
     *
     * @param totalQuantity 総数量
     */
    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    /**
     * 倉庫別評価一覧を返す。
     *
     * @return 倉庫別評価のリスト
     */
    public List<WarehouseValuation> getWarehouseBreakdown() {
        return warehouseBreakdown;
    }

    /**
     * 倉庫別評価一覧を設定する。
     *
     * @param warehouseBreakdown 倉庫別評価のリスト
     */
    public void setWarehouseBreakdown(List<WarehouseValuation> warehouseBreakdown) {
        this.warehouseBreakdown = warehouseBreakdown;
    }

    /**
     * カテゴリ別評価一覧を返す。
     *
     * @return カテゴリ別評価のリスト
     */
    public List<CategoryValuation> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    /**
     * カテゴリ別評価一覧を設定する。
     *
     * @param categoryBreakdown カテゴリ別評価のリスト
     */
    public void setCategoryBreakdown(List<CategoryValuation> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    /** 倉庫別評価内部クラス */
    public static class WarehouseValuation implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal valuation;
        private int itemCount;

        public WarehouseValuation() {}
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        public String getWarehouseName() { return warehouseName; }
        public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
        public BigDecimal getValuation() { return valuation; }
        public void setValuation(BigDecimal valuation) { this.valuation = valuation; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    }

    /** カテゴリ別評価内部クラス */
    public static class CategoryValuation implements Serializable {
        private static final long serialVersionUID = 1L;
        private String categoryName;
        private BigDecimal valuation;
        private int itemCount;
        private BigDecimal percentage;

        public CategoryValuation() {}
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public BigDecimal getValuation() { return valuation; }
        public void setValuation(BigDecimal valuation) { this.valuation = valuation; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    }
}
