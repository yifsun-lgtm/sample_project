package com.proquip.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ダッシュボードサマリデータ転送オブジェクト。
 *
 * <p>ダッシュボード画面に表示する集計情報をまとめて転送するためのクラス。
 * 各種件数、直近の発注情報、予算利用率などを含む。</p>
 *
 * <p>技術的負債 #8: クラス名のサフィックスが "DTO"（大文字）。
 * 内部クラスは "Dto" サフィックスを使用しており、同一クラス内でも不統一。</p>
 *
 * @author ProQuip開発チーム
 */
public class DashboardSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品総数 */
    private long totalProducts;

    /** 有効仕入先数 */
    private long activeSuppliers;

    /** 承認待ち発注数 */
    private long pendingOrders;

    /** 在庫不足品目数 */
    private long lowStockItems;

    /** 承認待ち件数（全種別合計） */
    private long pendingApprovals;

    /** 直近の発注一覧 */
    private List<RecentOrderDto> recentOrders = new ArrayList<>();

    /**
     * 予算利用率マップ。
     * キー: 部門名、値: 利用率（パーセンテージ）
     */
    private Map<String, BigDecimal> budgetUtilization = new HashMap<>();

    /**
     * デフォルトコンストラクタ。
     */
    public DashboardSummaryDTO() {
    }

    // --- Getter / Setter ---

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getActiveSuppliers() {
        return activeSuppliers;
    }

    public void setActiveSuppliers(long activeSuppliers) {
        this.activeSuppliers = activeSuppliers;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getLowStockItems() {
        return lowStockItems;
    }

    public void setLowStockItems(long lowStockItems) {
        this.lowStockItems = lowStockItems;
    }

    public long getPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(long pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
    }

    public List<RecentOrderDto> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<RecentOrderDto> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public Map<String, BigDecimal> getBudgetUtilization() {
        return budgetUtilization;
    }

    public void setBudgetUtilization(Map<String, BigDecimal> budgetUtilization) {
        this.budgetUtilization = budgetUtilization;
    }

    // --- 内部DTOクラス ---

    /**
     * 直近の発注情報DTO。
     *
     * <p>技術的負債 #8: 親クラスは "DTO" サフィックスだが、
     * この内部クラスは "Dto" サフィックスで命名不統一。</p>
     */
    public static class RecentOrderDto implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 発注ID */
        private Long id;

        /** 発注番号 */
        private String orderNumber;

        /** 仕入先名 */
        private String supplierName;

        /** 合計金額 */
        private BigDecimal totalAmount;

        /** ステータス */
        private String status;

        /** 発注日 */
        private Date orderDate;

        public RecentOrderDto() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public void setOrderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(Date orderDate) {
            this.orderDate = orderDate;
        }
    }
}
