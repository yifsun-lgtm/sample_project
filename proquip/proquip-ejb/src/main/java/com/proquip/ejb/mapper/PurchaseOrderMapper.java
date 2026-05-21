package com.proquip.ejb.mapper;

import com.proquip.common.dto.PurchaseOrderResponse;
import com.proquip.common.dto.PurchaseOrderResponse.PurchaseOrderItemResponse;
import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 発注エンティティとDTO間の手書きマッパークラス。
 *
 * <p>技術的負債 #10: MapStructを使用せず、フィールドごとの手動マッピングを行っている。
 * {@link ProductMapper} や {@link SupplierMapper} がMapStructを使用しているのに対し、
 * このクラスだけ手書き実装になっている。</p>
 *
 * <p>手書きマッピングの問題点:</p>
 * <ul>
 *   <li>新しいフィールドが追加された場合にマッピング漏れが発生しやすい</li>
 *   <li>コードが冗長で保守コストが高い</li>
 *   <li>コンパイル時のチェックが効かない</li>
 *   <li>一部のフィールドが意図的に/偶発的にマッピングされていない</li>
 * </ul>
 *
 * // TODO: MapStructに移行すべき
 *
 * @author ProQuip開発チーム
 */
public class PurchaseOrderMapper {

    /** ロガー */
    private static final Logger LOG = Logger.getLogger(PurchaseOrderMapper.class.getName());

    /**
     * デフォルトコンストラクタ。
     */
    public PurchaseOrderMapper() {
    }

    /**
     * 発注エンティティをPurchaseOrderResponseに変換する。
     *
     * <p>技術的負債: shippingMethod、notes、buyerId がマッピングされていない。
     * これが意図的な省略なのか、マッピング漏れなのか不明。</p>
     *
     * @param entity 発注エンティティ
     * @return 発注応答DTO（entityがnullの場合はnull）
     */
    public PurchaseOrderResponse toResponse(PurchaseOrder entity) {
        if (entity == null) {
            return null;
        }

        LOG.log(Level.FINE, "PurchaseOrder -> PurchaseOrderResponse 変換: id={0}", entity.getId());

        PurchaseOrderResponse response = new PurchaseOrderResponse();

        // 基本フィールドのマッピング
        response.setId(entity.getId());
        response.setOrderNumber(entity.getPoNumber());
        response.setStatus(entity.getStatus());
        response.setOrderDate(toUtilDate(entity.getOrderDate() != null ? entity.getOrderDate() : entity.getCreatedAt()));
        response.setExpectedDeliveryDate(toUtilDate(entity.getExpectedDeliveryDate()));
        response.setTotalAmount(entity.getTotalAmount());
        response.setCurrency(entity.getCurrency());

        // サプライヤー情報のマッピング
        if (entity.getSupplier() != null) {
            response.setSupplierId(entity.getSupplier().getId());
            response.setSupplierName(entity.getSupplier().getName());
        }

        // 技術的負債: shippingMethod がマッピングされていない
        // 技術的負債: notes がマッピングされていない
        // 技術的負債: buyerId がマッピングされていない

        // 明細のマッピング
        if (entity.getItems() != null) {
            List<PurchaseOrderItemResponse> itemResponses = new ArrayList<>();
            for (PurchaseOrderItem item : entity.getItems()) {
                itemResponses.add(toItemResponse(item));
            }
            response.setItems(itemResponses);
        }

        return response;
    }

    /**
     * 発注明細エンティティをPurchaseOrderItemResponseに変換する。
     *
     * @param item 発注明細エンティティ
     * @return 発注明細応答DTO
     */
    private PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item) {
        if (item == null) {
            return null;
        }

        PurchaseOrderItemResponse response = new PurchaseOrderItemResponse();
        response.setId(item.getId());
        // lineNumber field was removed from PurchaseOrderItem entity (DDL alignment)
        // response.setLineNumber(...);
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());
        response.setReceivedQuantity(item.getReceivedQuantity());
        response.setStatus(item.getStatus());

        // 製品情報のマッピング
        if (item.getProduct() != null) {
            response.setProductId(item.getProduct().getId());
            response.setProductName(item.getProduct().getName());
            response.setSkuCode(item.getProduct().getSku());
        }

        return response;
    }

    /**
     * PurchaseOrderResponseから発注エンティティに変換する。
     *
     * <p>技術的負債 #10: 逆マッピングもフィールドごとの手動実装。
     * サプライヤーの参照設定ができないため、呼び出し側で補完が必要。
     * また、エンティティのバージョンフィールドや監査フィールドが
     * 設定されないため、新規登録時のみ使用可能。</p>
     *
     * @param response 発注応答DTO
     * @return 発注エンティティ（responseがnullの場合はnull）
     */
    public PurchaseOrder toEntity(PurchaseOrderResponse response) {
        if (response == null) {
            return null;
        }

        LOG.log(Level.FINE, "PurchaseOrderResponse -> PurchaseOrder 変換: id={0}", response.getId());

        PurchaseOrder entity = new PurchaseOrder();

        entity.setId(response.getId());
        entity.setPoNumber(response.getOrderNumber());
        entity.setStatus(response.getStatus());
        entity.setOrderDate(response.getOrderDate());
        entity.setExpectedDeliveryDate(response.getExpectedDeliveryDate());
        entity.setTotalAmount(response.getTotalAmount());
        entity.setCurrency(response.getCurrency());

        // 技術的負債: サプライヤーオブジェクトの設定ができない（IDしかない）
        // 技術的負債: 明細のエンティティ変換も不完全
        // 技術的負債: shippingMethod、notes、buyerId が設定されない

        // 明細のマッピング（不完全）
        if (response.getItems() != null) {
            List<PurchaseOrderItem> items = new ArrayList<>();
            for (PurchaseOrderItemResponse itemResponse : response.getItems()) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setId(itemResponse.getId());
                // lineNumber field was removed from PurchaseOrderItem entity (DDL alignment)
                // item.setLineNumber(itemResponse.getLineNumber());
                item.setQuantity(itemResponse.getQuantity());
                item.setUnitPrice(itemResponse.getUnitPrice());
                item.setSubtotal(itemResponse.getSubtotal());
                item.setStatus(itemResponse.getStatus());
                // 技術的負債: product の参照設定ができない
                // 技術的負債: purchaseOrder（親）の参照設定ができない
                // 技術的負債: taxRate、discount が設定されない
                items.add(item);
            }
            entity.setItems(items);
        }

        return entity;
    }

    private java.util.Date toUtilDate(java.util.Date date) {
        return date != null ? new java.util.Date(date.getTime()) : null;
    }
}
