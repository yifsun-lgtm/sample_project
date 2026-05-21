package com.proquip.ejb.mapper;

import com.proquip.common.dto.PurchaseRequisitionDto;
import com.proquip.common.dto.PurchaseRequisitionDto.RequisitionItemDto;
import com.proquip.ejb.entity.procurement.PurchaseRequisition;
import com.proquip.ejb.entity.procurement.PurchaseRequisitionItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 購買依頼エンティティとDTO間の手書きマッパークラス。
 *
 * <p>技術的負債 #2: {@link PurchaseOrderMapper} からのコピペで作成された。
 * 構造が酷似しているにもかかわらず共通の基底クラスや
 * テンプレートメソッドパターンが適用されていない。</p>
 *
 * <p>技術的負債 #10: MapStructを使用せず、フィールドごとの手動マッピングを行っている。
 * {@link ProductMapper} や {@link SupplierMapper} がMapStructを使用しているのに対し、
 * このクラスは手書き実装になっている。</p>
 *
 * <p>手書きマッピングの問題点:</p>
 * <ul>
 *   <li>PurchaseOrderMapper との重複コードが多い</li>
 *   <li>新しいフィールドが追加された場合にマッピング漏れが発生しやすい</li>
 *   <li>コードが冗長で保守コストが高い</li>
 * </ul>
 *
 * // TODO: MapStructに移行し、PurchaseOrderMapperとの共通処理を整理すべき
 *
 * @author ProQuip開発チーム
 */
public class RequisitionMapper {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(RequisitionMapper.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public RequisitionMapper() {
    }

    /**
     * 購買依頼エンティティをPurchaseRequisitionDtoに変換する。
     *
     * <p>技術的負債: requestedBy はエンティティの requesterId（Long型の生ID）
     * から取得するため、ユーザー名に変換できない。呼び出し側で
     * UserProfileを検索して手動設定する必要がある。</p>
     *
     * <p>技術的負債: departmentName も同様にDepartmentエンティティを
     * 別途取得する必要がある。</p>
     *
     * @param entity 購買依頼エンティティ
     * @return 購買依頼DTO（entityがnullの場合はnull）
     */
    public PurchaseRequisitionDto toDto(PurchaseRequisition entity) {
        if (entity == null) {
            return null;
        }

        LOG.log(Level.FINE, "PurchaseRequisition -> PurchaseRequisitionDto 変換: id={0}", entity.getId());

        PurchaseRequisitionDto dto = new PurchaseRequisitionDto();

        // 基本フィールドのマッピング
        dto.setId(entity.getId());
        dto.setRequisitionNumber(entity.getReqNumber());
        dto.setStatus(entity.getStatus());
        // totalAmount, requestDate, urgency fields were removed from PurchaseRequisition (DDL alignment)
        // Use requiredDate and priority instead
        dto.setRequiredDate(entity.getRequiredDate());
        dto.setJustification(entity.getJustification());

        // 技術的負債: requestedBy はエンティティが requesterId（生のID）しか持たない
        // dto.setRequestedBy(...);

        // 技術的負債: departmentName はDepartmentエンティティを別途取得する必要がある
        // dto.setDepartmentName(...);

        // 明細のマッピング
        if (entity.getItems() != null) {
            List<RequisitionItemDto> itemDtos = new ArrayList<>();
            for (PurchaseRequisitionItem item : entity.getItems()) {
                itemDtos.add(toItemDto(item));
            }
            dto.setItems(itemDtos);
            dto.setItemCount(itemDtos.size());
        }

        return dto;
    }

    /**
     * 購買依頼明細エンティティをRequisitionItemDtoに変換する。
     *
     * @param item 購買依頼明細エンティティ
     * @return 購買依頼明細DTO
     */
    private RequisitionItemDto toItemDto(PurchaseRequisitionItem item) {
        if (item == null) {
            return null;
        }

        RequisitionItemDto dto = new RequisitionItemDto();
        // productId is accessed via the Product ManyToOne relation
        dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        dto.setQuantity(new BigDecimal(item.getQuantity()));
        dto.setEstimatedUnitPrice(item.getEstimatedUnitCost());
        // estimatedSubtotal is not stored; compute from quantity * unitCost
        if (item.getEstimatedUnitCost() != null && item.getQuantity() != null) {
            dto.setEstimatedSubtotal(item.getEstimatedUnitCost()
                    .multiply(new BigDecimal(item.getQuantity())));
        }

        // 技術的負債: productName はProductエンティティを別途取得する必要がある
        // dto.setProductName(...);

        return dto;
    }

    /**
     * PurchaseRequisitionDtoから購買依頼エンティティに変換する。
     *
     * <p>技術的負債 #2: PurchaseOrderMapper.toEntity() からのコピペ構造。
     * 依頼者の参照設定ができないため、呼び出し側で補完が必要。</p>
     *
     * @param dto 購買依頼DTO
     * @return 購買依頼エンティティ（dtoがnullの場合はnull）
     */
    public PurchaseRequisition toEntity(PurchaseRequisitionDto dto) {
        if (dto == null) {
            return null;
        }

        LOG.log(Level.FINE, "PurchaseRequisitionDto -> PurchaseRequisition 変換: id={0}", dto.getId());

        PurchaseRequisition entity = new PurchaseRequisition();

        entity.setId(dto.getId());
        entity.setReqNumber(dto.getRequisitionNumber());
        entity.setStatus(dto.getStatus());
        // totalAmount, requestDate, urgency fields were removed from PurchaseRequisition (DDL alignment)
        entity.setRequiredDate(dto.getRequiredDate());
        entity.setJustification(dto.getJustification());

        // 技術的負債: requesterId の設定ができない（DTOにはrequesterの名前しかない）
        // 技術的負債: departmentId の設定ができない（DTOにはdepartmentNameしかない）
        // 技術的負債: 明細のエンティティ変換も不完全

        if (dto.getItems() != null) {
            List<PurchaseRequisitionItem> items = new ArrayList<>();
            for (RequisitionItemDto itemDto : dto.getItems()) {
                PurchaseRequisitionItem item = new PurchaseRequisitionItem();
                // productId is set via Product ManyToOne; caller must resolve Product entity
                // item.setProduct(...);
                item.setQuantity(itemDto.getQuantity() != null ? itemDto.getQuantity().intValue() : 0);
                item.setEstimatedUnitCost(itemDto.getEstimatedUnitPrice());
                // 技術的負債: requisition（親）の参照設定ができない
                items.add(item);
            }
            entity.setItems(items);
        }

        return entity;
    }
}
