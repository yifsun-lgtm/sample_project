package com.proquip.ejb.entity.procurement;

import com.proquip.ejb.entity.base.AuditableEntity;
import com.proquip.ejb.entity.supplier.Supplier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 発注エンティティ。
 * <p>
 * サプライヤーへの発注情報を表す最も重要なエンティティの一つ。
 * 発注番号で一意に識別され、発注明細、ステータス履歴、入庫情報への関連を持つ。
 * </p>
 * <p>
 * 【技術的負債】
 * <ul>
 *   <li>supplier が EAGER フェッチになっており、一覧取得時に不要なJOINが発生する</li>
 *   <li>items が EAGER フェッチになっており、N+1問題を引き起こす</li>
 *   <li>buyerId が UserProfile へのリレーションではなく生のIDで保持されている</li>
 *   <li>currency が Currency エンティティへの参照ではなく文字列で管理されている</li>
 *   <li>NamedQuery の一部がデカルト積を発生させるLEFT JOIN FETCHを使用している</li>
 *   <li>@Column のネーミングに一貫性がない（snake_case とそうでないものが混在）</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "purchase_order")
@NamedQueries({
    @NamedQuery(
        name = "PurchaseOrder.findByPoNumber",
        query = "SELECT po FROM PurchaseOrder po WHERE po.poNumber = :poNumber"
    ),
    @NamedQuery(
        name = "PurchaseOrder.findByStatus",
        query = "SELECT po FROM PurchaseOrder po WHERE po.status = :status ORDER BY po.orderDate DESC"
    ),
    // 技術的負債: 全カラムを取得しているが、一覧画面では一部のみ必要
    @NamedQuery(
        name = "PurchaseOrder.findBySupplier",
        query = "SELECT po FROM PurchaseOrder po WHERE po.supplier.id = :supplierId ORDER BY po.orderDate DESC"
    ),
    // 技術的負債: LEFT JOIN FETCH でデカルト積が発生する可能性がある
    @NamedQuery(
        name = "PurchaseOrder.findWithAllDetails",
        query = "SELECT po FROM PurchaseOrder po " +
                "LEFT JOIN FETCH po.items " +
                "LEFT JOIN FETCH po.statusHistory " +
                "LEFT JOIN FETCH po.goodsReceipts " +
                "WHERE po.id = :id"
    ),
    @NamedQuery(
        name = "PurchaseOrder.findByDateRange",
        query = "SELECT po FROM PurchaseOrder po WHERE po.orderDate BETWEEN :startDate AND :endDate ORDER BY po.orderDate DESC"
    ),
    // 技術的負債: buyerIdが生のIDなのでJOINできず、サブクエリも使用していない
    @NamedQuery(
        name = "PurchaseOrder.findByBuyer",
        query = "SELECT po FROM PurchaseOrder po WHERE po.buyerId = :buyerId ORDER BY po.orderDate DESC"
    )
})
public class PurchaseOrder extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    /** 発注番号（一意制約） */
    @NotNull
    @Size(max = 30)
    @Column(name = "order_number", unique = true, nullable = false, length = 30)
    private String poNumber;

    /** 発注日 */
    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "order_date", nullable = false)
    private Date orderDate;

    /** 納品予定日 */
    @Temporal(TemporalType.DATE)
    @Column(name = "expected_delivery_date")
    private Date expectedDeliveryDate;

    /**
     * ステータス。
     * <p>
     * 技術的負債: Enum型を使用すべきだが、文字列で管理している。
     * 有効値: DRAFT, SUBMITTED, APPROVED, SENT, PARTIALLY_RECEIVED,
     *         RECEIVED, COMPLETED, CANCELLED
     * </p>
     */
    @NotNull
    @Size(max = 30)
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** 合計金額 */
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 通貨コード。
     * <p>
     * 技術的負債: Currency エンティティへの@ManyToOneリレーションを使用すべきだが、
     * 文字列で管理している。通貨マスタとの整合性が保証されない。
     * </p>
     */
    @Size(max = 3)
    @Column(name = "currency_code", length = 3)
    private String currency;

    /** 配送方法 */
    // 技術的負債: @Column の name 属性が未指定（他のフィールドとの一貫性がない）
    @Size(max = 50)
    @Column(length = 50)
    private String shippingMethod;

    /** 備考 */
    @Size(max = 2000)
    @Column(name = "notes", length = 2000)
    private String notes;

    /**
     * サプライヤーへの参照。
     * <p>
     * 技術的負債: EAGER フェッチが指定されており、発注一覧取得時に
     * 全サプライヤー情報が不必要にロードされる。LAZY に変更すべき。
     * </p>
     */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /**
     * 購入担当者のユーザーID。
     * <p>
     * 技術的負債: UserProfileへの@ManyToOneリレーションを使用すべきだが、
     * 生のIDで保持している。これによりJPQLでのJOINが不可能。
     * </p>
     */
    // 技術的負債: @Column の name 属性に snake_case を使っていない（一貫性なし）
    @Column(name = "ordered_by")
    private Long buyerId;

    /**
     * 発注明細のリスト。
     * <p>
     * 技術的負債: EAGER フェッチが指定されており、発注を取得するたびに
     * 全明細が即座にロードされ、N+1問題を引き起こす。
     * </p>
     */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<PurchaseOrderItem> items = new ArrayList<PurchaseOrderItem>();

    /** ステータス変更履歴のリスト */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("changedAt DESC")
    private List<PurchaseOrderStatusHistory> statusHistory = new ArrayList<PurchaseOrderStatusHistory>();

    /** 入庫情報のリスト */
    @OneToMany(mappedBy = "purchaseOrder", fetch = FetchType.LAZY)
    @OrderBy("receiptDate DESC")
    private List<GoodsReceipt> goodsReceipts = new ArrayList<GoodsReceipt>();

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseOrder() {
        super();
    }

    // --- Getter / Setter ---

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public Date getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(Date expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }

    public List<PurchaseOrderStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<PurchaseOrderStatusHistory> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public List<GoodsReceipt> getGoodsReceipts() {
        return goodsReceipts;
    }

    public void setGoodsReceipts(List<GoodsReceipt> goodsReceipts) {
        this.goodsReceipts = goodsReceipts;
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "poNumber='" + poNumber + '\'' +
                ", status='" + status + '\'' +
                ", orderDate=" + orderDate +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                '}';
    }
}
