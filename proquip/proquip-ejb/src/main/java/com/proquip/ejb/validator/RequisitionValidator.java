package com.proquip.ejb.validator;

import com.proquip.ejb.entity.procurement.PurchaseRequisition;
import com.proquip.ejb.entity.procurement.PurchaseRequisitionItem;

import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 購買依頼バリデータ。
 *
 * <p>購買依頼エンティティに対するビジネスルールの検証を行う。</p>
 *
 * <p>技術的負債 #2: このクラスは {@link PurchaseOrderValidator} からコピー＆ペーストで
 * 作成されている。構造がほぼ同一であるにもかかわらず、閾値やルールに微妙な違いがあり、
 * 共通のバリデーション基底クラスやユーティリティに統合すべき。</p>
 *
 * <p>差異:
 * <ul>
 *   <li>最小金額: PO=1,000円 / 購買依頼=5,000円（不整合の可能性）</li>
 *   <li>最大明細数: PO=100件 / 購買依頼=50件</li>
 *   <li>一部のバリデーションメソッドはPOバリデータの完全なコピー</li>
 *   <li>他のメソッドは微妙に異なるロジックを含む</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class RequisitionValidator {

    private static final Logger logger = Logger.getLogger(RequisitionValidator.class.getName());

    /**
     * 最小依頼金額（円）。
     * 技術的負債 #2: PurchaseOrderValidatorでは1,000円だが、ここでは5,000円。
     * 意図的な差異なのかコピー時の修正ミスなのか不明。
     */
    private static final BigDecimal MIN_REQUISITION_AMOUNT = new BigDecimal("5000");

    /** 最大依頼金額（円） */
    private static final BigDecimal MAX_REQUISITION_AMOUNT = new BigDecimal("999999.99");

    /**
     * 1依頼あたりの最大明細数。
     * 技術的負債 #2: PurchaseOrderValidatorでは100件だが、ここでは50件。
     */
    private static final int MAX_ITEMS_PER_REQUISITION = 50;

    /**
     * SKUフォーマットの正規表現パターン。
     * 技術的負債 #2: PurchaseOrderValidatorからのコピー。
     * PO側と同じ [A-Z]{3}-[0-9]{6} を使用しているが、
     * ProductValidator側は [A-Za-z]{2,5}-[0-9]{4,8} を使用。
     */
    private static final Pattern SKU_PATTERN = Pattern.compile("[A-Z]{3}-[0-9]{6}");

    /**
     * 購買依頼エンティティの総合バリデーションを実行する。
     *
     * <p>技術的負債 #2: PurchaseOrderValidator.validate()とほぼ同一の構造。
     * 共通化すべきだが、コピー＆ペーストで作成されている。</p>
     *
     * @param req バリデーション対象の購買依頼エンティティ
     * @return エラーメッセージのリスト。バリデーション成功時は空リスト
     */
    public List<String> validate(PurchaseRequisition req) {
        List<String> errors = new ArrayList<>();

        // === Null チェック ===
        // 技術的負債 #2: PurchaseOrderValidator.validate()と同一のパターン
        if (req == null) {
            errors.add("購買依頼オブジェクトがnullです");
            return errors;
        }

        // === 購買依頼番号の検証 ===
        if (req.getReqNumber() == null || req.getReqNumber().trim().isEmpty()) {
            errors.add("購買依頼番号は必須です");
        } else {
            if (req.getReqNumber().length() > 30) {
                errors.add("購買依頼番号は30文字以内で入力してください");
            }
            // 購買依頼番号のフォーマットチェック
            if (!req.getReqNumber().matches("REQ-[0-9]{8}-[0-9]{4}")) {
                errors.add("購買依頼番号のフォーマットが不正です。期待: REQ-YYYYMMDD-NNNN");
            }
        }

        // === ステータスの検証 ===
        // 技術的負債 #2: PurchaseOrderValidatorと同一のパターンだが、ステータス値が異なる
        if (req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            errors.add("ステータスは必須です");
        } else {
            String status = req.getStatus();
            if (!status.equals("DRAFT") && !status.equals("SUBMITTED")
                    && !status.equals("APPROVED") && !status.equals("REJECTED")
                    && !status.equals("CANCELLED") && !status.equals("CONVERTED")) {
                errors.add("無効なステータスです: " + status);
            }
        }

        // === 優先度の検証 ===
        if (req.getPriority() != null) {
            String priority = req.getPriority();
            if (!priority.equals("LOW") && !priority.equals("NORMAL")
                    && !priority.equals("HIGH") && !priority.equals("URGENT")) {
                errors.add("無効な優先度です: " + priority);
            }
        }

        // === 依頼者IDの検証 ===
        // 技術的負債 #2: PurchaseOrderValidatorのbuyerIdチェックのコピー
        if (req.getRequesterId() == null) {
            errors.add("依頼者IDは必須です");
        } else {
            if (req.getRequesterId() <= 0) {
                errors.add("依頼者IDが不正です");
            }
        }

        // === 日付の検証 ===
        errors.addAll(validateDates(req));

        // === 明細の検証 ===
        if (req.getItems() == null || req.getItems().isEmpty()) {
            errors.add("購買依頼明細が1件以上必要です");
        } else {
            if (req.getItems().size() > MAX_ITEMS_PER_REQUISITION) {
                errors.add("購買依頼明細数が上限(" + MAX_ITEMS_PER_REQUISITION + "件)を超えています: "
                        + req.getItems().size() + "件");
            }
            errors.addAll(validateItems(req.getItems()));
        }

        // === 金額の検証 ===
        errors.addAll(validateAmounts(req));

        // === 正当性理由の検証 ===
        if (req.getJustification() != null && req.getJustification().length() > 1000) {
            errors.add("正当性理由は1000文字以内で入力してください");
        }

        // ドラフト以外の場合は正当性理由が必須
        if (req.getStatus() != null && !req.getStatus().equals("DRAFT")) {
            if (req.getJustification() == null || req.getJustification().trim().isEmpty()) {
                errors.add("ドラフト以外のステータスでは正当性理由の入力が必要です");
            }
        }

        logger.info("購買依頼バリデーション完了: " + req.getReqNumber()
                + " エラー数: " + errors.size());

        return errors;
    }

    /**
     * 購買依頼明細リストのバリデーションを実行する。
     *
     * <p>技術的負債 #2: PurchaseOrderValidator.validateItems()のコピー。
     * 一部フィールド名が異なるだけで構造は同一。</p>
     *
     * @param items バリデーション対象の購買依頼明細リスト
     * @return エラーメッセージのリスト
     */
    public List<String> validateItems(List<PurchaseRequisitionItem> items) {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            PurchaseRequisitionItem item = items.get(i);
            String prefix = "明細 " + (i + 1) + ": ";

            // 数量の検証（PurchaseOrderValidatorからのコピー）
            if (item.getQuantity() == null) {
                errors.add(prefix + "数量は必須です");
            } else {
                if (item.getQuantity() <= 0) {
                    errors.add(prefix + "数量は0より大きい値を指定してください");
                }
                // 技術的負債 #2: PO側はBigDecimalだが、こちらはInteger。型が異なる
                if (item.getQuantity() > 999999) {
                    errors.add(prefix + "数量が上限を超えています");
                }
            }

            // 見積単価の検証
            if (item.getEstimatedUnitCost() != null) {
                if (item.getEstimatedUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(prefix + "見積単価は0以上で指定してください");
                }
                if (item.getEstimatedUnitCost().compareTo(new BigDecimal("99999999.99")) > 0) {
                    errors.add(prefix + "見積単価が上限を超えています");
                }
            }

            // 商品の検証
            if (item.getProduct() != null) {
                // SKUフォーマットの検証（PurchaseOrderValidatorからのコピー）
                if (item.getProduct().getSku() != null) {
                    if (!SKU_PATTERN.matcher(item.getProduct().getSku()).matches()) {
                        errors.add(prefix + "SKUフォーマットが不正です: " + item.getProduct().getSku()
                                + " (期待: [A-Z]{3}-[0-9]{6})");
                    }
                }
                // 非アクティブ商品のチェック
                // active field was removed from Product; use status instead (DDL alignment)
                if (!"ACTIVE".equals(item.getProduct().getStatus())) {
                    errors.add(prefix + "非アクティブな商品が指定されています: "
                            + item.getProduct().getSku());
                }
            }

            // 備考の検証
            if (item.getNotes() != null && item.getNotes().length() > 500) {
                errors.add(prefix + "備考は500文字以内で入力してください");
            }
        }

        return errors;
    }

    /**
     * 購買依頼の金額バリデーションを実行する。
     *
     * <p>技術的負債 #2: PurchaseOrderValidator.validateAmounts()からのコピーだが、
     * 最小金額が異なる（PO: 1,000円 / 購買依頼: 5,000円）。
     * この差異が意図的なものかは不明。</p>
     *
     * @param req バリデーション対象の購買依頼エンティティ
     * @return エラーメッセージのリスト
     */
    public List<String> validateAmounts(PurchaseRequisition req) {
        List<String> errors = new ArrayList<>();

        // 見積合計金額を計算
        BigDecimal estimatedTotal = BigDecimal.ZERO;
        if (req.getItems() != null) {
            for (PurchaseRequisitionItem item : req.getItems()) {
                if (item.getEstimatedUnitCost() != null && item.getQuantity() != null) {
                    BigDecimal itemTotal = item.getEstimatedUnitCost()
                            .multiply(new BigDecimal(item.getQuantity()));
                    estimatedTotal = estimatedTotal.add(itemTotal);
                }
            }
        }

        // 技術的負債 #2: PurchaseOrderValidatorでは1,000円だが、ここでは5,000円
        if (estimatedTotal.compareTo(BigDecimal.ZERO) > 0
                && estimatedTotal.compareTo(MIN_REQUISITION_AMOUNT) < 0) {
            errors.add("見積合計金額は" + MIN_REQUISITION_AMOUNT + "円以上で入力してください");
        }

        if (estimatedTotal.compareTo(MAX_REQUISITION_AMOUNT) > 0) {
            errors.add("見積合計金額が上限(" + MAX_REQUISITION_AMOUNT + "円)を超えています");
        }

        return errors;
    }

    /**
     * 購買依頼の日付バリデーションを実行する。
     *
     * <p>技術的負債 #2: PurchaseOrderValidator.validateDates()とほぼ同一の構造だが、
     * フィールド名が requestDate / requiredDate に変更されている。
     * 技術的負債 #6: java.util.Dateを使用した手動比較。</p>
     *
     * @param req バリデーション対象の購買依頼エンティティ
     * @return エラーメッセージのリスト
     */
    public List<String> validateDates(PurchaseRequisition req) {
        List<String> errors = new ArrayList<>();

        // 技術的負債 #6: java.util.Dateを使用した手動比較
        Date now = new Date();

        // requestDate field was removed from PurchaseRequisition (DDL alignment).
        // Validation now uses requiredDate and createdAt instead.

        // 必要期日の検証
        if (req.getRequiredDate() != null) {
            // 必要期日が過去6ヶ月以上前の場合はエラー
            long sixMonthsMillis = 180L * 24 * 60 * 60 * 1000;
            Date sixMonthsAgo = new Date(now.getTime() - sixMonthsMillis);
            if (req.getRequiredDate().before(sixMonthsAgo)) {
                errors.add("必要期日が6ヶ月以上前です");
            }

            // 必要期日が現在日から2年以上先の場合はエラー
            long twoYearsMillis = 730L * 24 * 60 * 60 * 1000;
            Date maxRequiredDate = new Date(now.getTime() + twoYearsMillis);
            if (req.getRequiredDate().after(maxRequiredDate)) {
                errors.add("必要期日は現在日から2年以内の日付を指定してください");
            }

            // 過去の必要期日チェック
            if (req.getRequiredDate().before(now)
                    && !"CANCELLED".equals(req.getStatus())
                    && !"CONVERTED".equals(req.getStatus())) {
                logger.warning("必要期日が過去日です: " + req.getReqNumber()
                        + " 期日: " + req.getRequiredDate());
            }
        }

        return errors;
    }
}
