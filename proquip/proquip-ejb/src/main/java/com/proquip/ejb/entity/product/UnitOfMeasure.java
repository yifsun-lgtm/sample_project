package com.proquip.ejb.entity.product;

import com.proquip.ejb.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * 計量単位を表すエンティティ。
 *
 * <p>商品の数量管理に使用する単位（個、kg、m、箱 など）を定義する。
 * 基準単位への変換係数（conversionFactor）を保持し、
 * 単位間の変換を可能にする。</p>
 *
 * <p>技術的負債: {@code baseUnitId} が {@code Long} 型の生IDで保持されている。
 * 本来は自己参照の {@code @ManyToOne} リレーションとして定義すべきだが、
 * レガシー設計のまま残されている。この設計では参照整合性がJPAレベルで保証されない。</p>
 *
 * @author ProQuip開発チーム
 */
@Entity
@Table(name = "unit_of_measure")
@NamedQueries({
    @NamedQuery(
        name = "UnitOfMeasure.findByCode",
        query = "SELECT u FROM UnitOfMeasure u WHERE u.code = :code"
    ),
    @NamedQuery(
        name = "UnitOfMeasure.findAll",
        query = "SELECT u FROM UnitOfMeasure u ORDER BY u.name"
    ),
    @NamedQuery(
        name = "UnitOfMeasure.findBaseUnits",
        query = "SELECT u FROM UnitOfMeasure u WHERE u.baseUnitId IS NULL ORDER BY u.name"
    )
})
public class UnitOfMeasure extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 単位コード（例: "PCS", "KG", "M"） */
    @Column(name = "unit_code", nullable = false, unique = true, length = 20)
    private String code;

    /** 単位名（例: "個", "キログラム", "メートル"） */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 基準単位への変換係数。
     * 例: 1 kg = 1000 g の場合、g の conversionFactor は 0.001
     */
    @Column(name = "conversion_factor", precision = 18, scale = 8)
    private BigDecimal conversionFactor;

    /**
     * 基準単位のID。
     * 技術的負債: {@code @ManyToOne} リレーションではなく生IDで保持している。
     * 自己参照の外部キーとして設計すべきだったが、初期開発時に簡略化された。
     * nullの場合、この単位自体が基準単位であることを意味する。
     */
    @Column(name = "base_unit_id")
    private Long baseUnitId;

    /**
     * デフォルトコンストラクタ。
     */
    public UnitOfMeasure() {
        super();
    }

    // --- Getter / Setter ---

    /**
     * 単位コードを返す。
     *
     * @return 単位コード
     */
    public String getCode() {
        return code;
    }

    /**
     * 単位コードを設定する。
     *
     * @param code 単位コード
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 単位名を返す。
     *
     * @return 単位名
     */
    public String getName() {
        return name;
    }

    /**
     * 単位名を設定する。
     *
     * @param name 単位名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 基準単位への変換係数を返す。
     *
     * @return 変換係数
     */
    public BigDecimal getConversionFactor() {
        return conversionFactor;
    }

    /**
     * 基準単位への変換係数を設定する。
     *
     * @param conversionFactor 変換係数
     */
    public void setConversionFactor(BigDecimal conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    /**
     * 基準単位のIDを返す。
     * 技術的負債: 生IDを返す。エンティティ参照に移行予定。
     *
     * @return 基準単位のID（基準単位自体の場合はnull）
     */
    public Long getBaseUnitId() {
        return baseUnitId;
    }

    /**
     * 基準単位のIDを設定する。
     * 技術的負債: 生IDを受け取る。エンティティ参照に移行予定。
     *
     * @param baseUnitId 基準単位のID
     */
    public void setBaseUnitId(Long baseUnitId) {
        this.baseUnitId = baseUnitId;
    }

    @Override
    public String toString() {
        return "UnitOfMeasure{" +
                "id=" + getId() +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", conversionFactor=" + conversionFactor +
                ", baseUnitId=" + baseUnitId +
                '}';
    }
}
