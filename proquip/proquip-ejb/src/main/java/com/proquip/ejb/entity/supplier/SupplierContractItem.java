package com.proquip.ejb.entity.supplier;

import com.proquip.ejb.entity.base.BaseEntity;
import com.proquip.ejb.entity.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * 仕入先契約明細を表すエンティティ。
 *
 * <p>仕入先契約（{@link SupplierContract}）に紐付く個別商品の
 * 合意価格やボリュームディスカウント条件を管理する。</p>
 *
 * <p>{@code volumeThreshold} を超える数量を発注した場合に、
 * {@code volumeDiscount} が適用される。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "supplier_contract_item")
@NamedQueries({
    @NamedQuery(
        name = "SupplierContractItem.findByContract",
        query = "SELECT sci FROM SupplierContractItem sci WHERE sci.contract.id = :contractId"
    ),
    @NamedQuery(
        name = "SupplierContractItem.findByProduct",
        query = "SELECT sci FROM SupplierContractItem sci WHERE sci.product.id = :productId"
    )
})
public class SupplierContractItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 合意価格 */
    @Column(name = "agreed_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal agreedPrice;

    /** 所属契約 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private SupplierContract contract;

    /** 対象商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * デフォルトコンストラクタ。
     */
    public SupplierContractItem() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 合意価格を返す。
     *
     * @return 合意価格
     */
    public BigDecimal getAgreedPrice() {
        return agreedPrice;
    }

    /**
     * 合意価格を設定する。
     *
     * @param agreedPrice 合意価格
     */
    public void setAgreedPrice(BigDecimal agreedPrice) {
        this.agreedPrice = agreedPrice;
    }

    /**
     * 所属契約を返す。
     *
     * @return 契約
     */
    public SupplierContract getContract() {
        return contract;
    }

    /**
     * 所属契約を設定する。
     *
     * @param contract 契約
     */
    public void setContract(SupplierContract contract) {
        this.contract = contract;
    }

    /**
     * 対象商品を返す。
     *
     * @return 商品
     */
    public Product getProduct() {
        return product;
    }

    /**
     * 対象商品を設定する。
     *
     * @param product 商品
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "SupplierContractItem{" +
                "id=" + getId() +
                ", agreedPrice=" + agreedPrice +
                '}';
    }
}
