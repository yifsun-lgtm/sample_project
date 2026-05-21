package com.proquip.common.dto.inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 棚卸しデータ転送オブジェクト。
 *
 * <p>棚卸し（サイクルカウント）の情報を保持する。倉庫を指定し、
 * 各在庫品目の予想数量と実数量を記録する明細を持つ。</p>
 *
 * @author ProQuip開発チーム
 */
public class InventoryCountDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 棚卸しID */
    private Long countId;

    /** 棚卸し番号 */
    private String countNumber;

    /** 倉庫ID */
    private Long warehouseId;

    /** 倉庫名 */
    private String warehouseName;

    /** ステータス（PLANNED, IN_PROGRESS, COMPLETED, CANCELLED） */
    private String status;

    /** 計画日 */
    private Date plannedDate;

    /** 完了日 */
    private Date completedDate;

    /** 実施者 */
    private String conductedBy;

    /** 差異のある品目数 */
    private int discrepancyCount;

    /** 棚卸し明細一覧 */
    private List<CountItemDto> items = new ArrayList<>();

    /**
     * デフォルトコンストラクタ。
     */
    public InventoryCountDto() {
    }

    // --- Getter / Setter ---

    /**
     * 棚卸しIDを返す。
     *
     * @return 棚卸しID
     */
    public Long getCountId() {
        return countId;
    }

    /**
     * 棚卸しIDを設定する。
     *
     * @param countId 棚卸しID
     */
    public void setCountId(Long countId) {
        this.countId = countId;
    }

    /**
     * 棚卸し番号を返す。
     *
     * @return 棚卸し番号
     */
    public String getCountNumber() {
        return countNumber;
    }

    /**
     * 棚卸し番号を設定する。
     *
     * @param countNumber 棚卸し番号
     */
    public void setCountNumber(String countNumber) {
        this.countNumber = countNumber;
    }

    /**
     * 倉庫IDを返す。
     *
     * @return 倉庫ID
     */
    public Long getWarehouseId() {
        return warehouseId;
    }

    /**
     * 倉庫IDを設定する。
     *
     * @param warehouseId 倉庫ID
     */
    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    /**
     * 倉庫名を返す。
     *
     * @return 倉庫名
     */
    public String getWarehouseName() {
        return warehouseName;
    }

    /**
     * 倉庫名を設定する。
     *
     * @param warehouseName 倉庫名
     */
    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    /**
     * ステータスを返す。
     *
     * @return ステータス
     */
    public String getStatus() {
        return status;
    }

    /**
     * ステータスを設定する。
     *
     * @param status ステータス
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 計画日を返す。
     *
     * @return 計画日
     */
    public Date getPlannedDate() {
        return plannedDate;
    }

    /**
     * 計画日を設定する。
     *
     * @param plannedDate 計画日
     */
    public void setPlannedDate(Date plannedDate) {
        this.plannedDate = plannedDate;
    }

    /**
     * 完了日を返す。
     *
     * @return 完了日
     */
    public Date getCompletedDate() {
        return completedDate;
    }

    /**
     * 完了日を設定する。
     *
     * @param completedDate 完了日
     */
    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    /**
     * 実施者を返す。
     *
     * @return 実施者名
     */
    public String getConductedBy() {
        return conductedBy;
    }

    /**
     * 実施者を設定する。
     *
     * @param conductedBy 実施者名
     */
    public void setConductedBy(String conductedBy) {
        this.conductedBy = conductedBy;
    }

    /**
     * 差異のある品目数を返す。
     *
     * @return 差異品目数
     */
    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    /**
     * 差異のある品目数を設定する。
     *
     * @param discrepancyCount 差異品目数
     */
    public void setDiscrepancyCount(int discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    /**
     * 棚卸し明細一覧を返す。
     *
     * @return 棚卸し明細のリスト
     */
    public List<CountItemDto> getItems() {
        return items;
    }

    /**
     * 棚卸し明細一覧を設定する。
     *
     * @param items 棚卸し明細のリスト
     */
    public void setItems(List<CountItemDto> items) {
        this.items = items;
    }

    /**
     * 棚卸し明細の内部DTO。
     */
    public static class CountItemDto implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 在庫品目ID */
        private Long itemId;

        /** 製品名 */
        private String productName;

        /** SKUコード */
        private String skuCode;

        /** 予想数量（システム上の在庫数） */
        private Integer expectedQuantity;

        /** 実数量（実地カウント数） */
        private Integer actualQuantity;

        /** 差異 */
        private Integer discrepancy;

        /** 調整理由 */
        private String adjustmentReason;

        public CountItemDto() {
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getSkuCode() {
            return skuCode;
        }

        public void setSkuCode(String skuCode) {
            this.skuCode = skuCode;
        }

        public Integer getExpectedQuantity() {
            return expectedQuantity;
        }

        public void setExpectedQuantity(Integer expectedQuantity) {
            this.expectedQuantity = expectedQuantity;
        }

        public Integer getActualQuantity() {
            return actualQuantity;
        }

        public void setActualQuantity(Integer actualQuantity) {
            this.actualQuantity = actualQuantity;
        }

        public Integer getDiscrepancy() {
            return discrepancy;
        }

        public void setDiscrepancy(Integer discrepancy) {
            this.discrepancy = discrepancy;
        }

        public String getAdjustmentReason() {
            return adjustmentReason;
        }

        public void setAdjustmentReason(String adjustmentReason) {
            this.adjustmentReason = adjustmentReason;
        }
    }
}
