package com.proquip.common.dto.procurement;

import java.io.Serializable;

/**
 * 承認リクエストデータ転送オブジェクト。
 *
 * <p>発注書または購買依頼に対する承認・却下アクションを表現する。
 * 承認ワークフローの各ステップで使用される。</p>
 *
 * @author ProQuip開発チーム
 */
public class ApprovalRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 発注書ID（発注書承認の場合） */
    private Long orderId;

    /** 購買依頼ID（購買依頼承認の場合） */
    private Long requisitionId;

    /** エンティティ種別（ORDER, REQUISITION） */
    private String entityType;

    /** コメント */
    private String comment;

    /** アクション（APPROVE, REJECT, DELEGATE, ESCALATE） */
    private String action;

    /** 委任先ユーザーID（DELEGATEアクションの場合） */
    private Long delegateToUserId;

    /**
     * デフォルトコンストラクタ。
     */
    public ApprovalRequestDto() {
    }

    // --- Getter / Setter ---

    /**
     * 発注書IDを返す。
     *
     * @return 発注書ID
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * 発注書IDを設定する。
     *
     * @param orderId 発注書ID
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * 購買依頼IDを返す。
     *
     * @return 購買依頼ID
     */
    public Long getRequisitionId() {
        return requisitionId;
    }

    /**
     * 購買依頼IDを設定する。
     *
     * @param requisitionId 購買依頼ID
     */
    public void setRequisitionId(Long requisitionId) {
        this.requisitionId = requisitionId;
    }

    /**
     * エンティティ種別を返す。
     *
     * @return エンティティ種別
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * エンティティ種別を設定する。
     *
     * @param entityType エンティティ種別
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * コメントを返す。
     *
     * @return コメント
     */
    public String getComment() {
        return comment;
    }

    /**
     * コメントを設定する。
     *
     * @param comment コメント
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * アクションを返す。
     *
     * @return アクション
     */
    public String getAction() {
        return action;
    }

    /**
     * アクションを設定する。
     *
     * @param action アクション
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 委任先ユーザーIDを返す。
     *
     * @return 委任先ユーザーID
     */
    public Long getDelegateToUserId() {
        return delegateToUserId;
    }

    /**
     * 委任先ユーザーIDを設定する。
     *
     * @param delegateToUserId 委任先ユーザーID
     */
    public void setDelegateToUserId(Long delegateToUserId) {
        this.delegateToUserId = delegateToUserId;
    }
}
