package com.proquip.ejb.mapper;

import com.proquip.common.dto.BudgetDto;
import com.proquip.ejb.entity.pricing.Budget;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 予算エンティティとDTO間の手書きマッパークラス。
 *
 * <p>技術的負債 #10: MapStructを使用せず、手動マッピングを行っている。
 * {@link ProductMapper} や {@link SupplierMapper} がMapStructを使用しているのに対し、
 * このクラスは {@link PurchaseOrderMapper} と同様に手書き実装になっている。</p>
 *
 * <p>手書きにした理由は、remainingAmount の計算ロジックが含まれるため。
 * しかしMapStructの @AfterMapping でも同様のことは実現可能であり、
 * 手書きにする正当な理由にはならない。</p>
 *
 * @author ProQuip開発チーム
 */
public class BudgetMapper {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(BudgetMapper.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public BudgetMapper() {
    }

    /**
     * 予算エンティティをBudgetDtoに変換する。
     *
     * <p>remainingAmount はエンティティの totalAmount から spentAmount を
     * 減算して計算する。spentAmount が null の場合は totalAmount がそのまま
     * 残額となる。</p>
     *
     * <p>技術的負債: departmentName はエンティティが departmentId（生のID）
     * しか持たないため設定できない。呼び出し側で別途Departmentを検索して
     * 手動設定する必要がある。</p>
     *
     * @param entity 予算エンティティ
     * @return 予算DTO（entityがnullの場合はnull）
     */
    public BudgetDto toDto(Budget entity) {
        if (entity == null) {
            return null;
        }

        LOG.log(Level.FINE, "Budget -> BudgetDto 変換: id={0}", entity.getId());

        BudgetDto dto = new BudgetDto();

        dto.setId(entity.getId());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setFiscalYear(entity.getFiscalYear());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setUsedAmount(entity.getSpentAmount());
        dto.setStatus(entity.getStatus());

        // 技術的負債: departmentName はDepartmentエンティティを
        // 別途取得しないと設定できない
        // dto.setDepartmentName(...);

        // remainingAmount の手動計算
        BigDecimal total = entity.getTotalAmount();
        BigDecimal spent = entity.getSpentAmount();
        if (total != null) {
            if (spent != null) {
                dto.setRemainingAmount(total.subtract(spent));
            } else {
                dto.setRemainingAmount(total);
            }
        } else {
            dto.setRemainingAmount(BigDecimal.ZERO);
        }

        return dto;
    }

    /**
     * BudgetDtoから予算エンティティに変換する。
     *
     * <p>技術的負債: remainingAmount は計算値のため、
     * エンティティ側には対応するフィールドがない。
     * usedAmount は spentAmount にマッピングする。</p>
     *
     * @param dto 予算DTO
     * @return 予算エンティティ（dtoがnullの場合はnull）
     */
    public Budget toEntity(BudgetDto dto) {
        if (dto == null) {
            return null;
        }

        LOG.log(Level.FINE, "BudgetDto -> Budget 変換: id={0}", dto.getId());

        Budget entity = new Budget();

        entity.setId(dto.getId());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setFiscalYear(dto.getFiscalYear());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setSpentAmount(dto.getUsedAmount());
        entity.setStatus(dto.getStatus());

        // 技術的負債: name フィールドがDTOにない（departmentNameとは別）
        // 技術的負債: allocatedAmount がDTOにない
        // 技術的負債: lineItems がDTOにない

        return entity;
    }
}
