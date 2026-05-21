package com.proquip.ejb.validator;

import com.proquip.ejb.entity.procurement.PurchaseOrder;
import com.proquip.ejb.entity.procurement.PurchaseOrderItem;

import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 発注バリデータ。
 *
 * <p>発注エンティティおよびその関連データに対するビジネスルールの検証を行う。
 * 金額、数量、日付、SKUフォーマット等の各種バリデーションを提供する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>{@link #validate(PurchaseOrder)} メソッドが200行を超えるネストされた
 *       if/else構造になっており、可読性・保守性が低い。</li>
 *   <li>ビジネスルール（最小金額、最大金額、最大明細数等）がハードコードされている。
 *       設定ファイルやデータベースから読み込むべき。</li>
 *   <li>SKUフォーマット: 大文字のみ許可 ({@code [A-Z]{3}-[0-9]{6}}) だが、
 *       フロントエンドでは小文字も許可しており不整合がある。</li>
 *   <li>最大金額が 999,999.99 だが、フロントエンドでは 1,000,000 まで入力可能。</li>
 *   <li>エラーメッセージが日本語と英語で混在している。</li>
 *   <li>戻り値が {@code List<String>} であり、型付きエラーオブジェクトを使用すべき。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class PurchaseOrderValidator {

    private static final Logger logger = Logger.getLogger(PurchaseOrderValidator.class.getName());

    /**
     * 最小発注金額（円）。
     * 技術的負債 #4: ハードコードされたビジネスルール。
     */
    private static final BigDecimal MIN_ORDER_AMOUNT = new BigDecimal("1000");

    /**
     * 最大発注金額（円）。
     * 技術的負債 #17: フロントエンドでは1,000,000まで入力可能だが、
     * バックエンドでは999,999.99までしか許可しない。境界値の不整合。
     */
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("999999.99");

    /** 1発注あたりの最大明細数 */
    private static final int MAX_ITEMS_PER_ORDER = 100;

    /**
     * SKUフォーマットの正規表現パターン。
     * 技術的負債 #17: 大文字英字3文字-数字6桁のみ許可。
     * フロントエンドでは小文字も許可しているため不整合が発生する。
     * ProductValidatorでは異なるパターンを使用している（さらなる不整合）。
     */
    private static final Pattern SKU_PATTERN = Pattern.compile("[A-Z]{3}-[0-9]{6}");

    /**
     * 発注エンティティの総合バリデーションを実行する。
     *
     * <p>技術的負債: このメソッドは200行を超えるネストされたif/else構造であり、
     * 循環的複雑度が非常に高い。個別のバリデーションメソッドに分割すべき。</p>
     *
     * @param order バリデーション対象の発注エンティティ
     * @return エラーメッセージのリスト。バリデーション成功時は空リスト
     */
    public List<String> validate(PurchaseOrder order) {
        // 技術的負債: 戻り値がList<String>。型付きエラー（ValidationErrorクラス等）を使用すべき
        List<String> errors = new ArrayList<>();

        // === Null チェック ===
        if (order == null) {
            errors.add("発注オブジェクトがnullです");
            return errors;
        }

        // === 発注番号の検証 ===
        if (order.getPoNumber() == null || order.getPoNumber().trim().isEmpty()) {
            // 技術的負債 #8: 日本語のエラーメッセージ
            errors.add("発注番号は必須です");
        } else {
            if (order.getPoNumber().length() > 30) {
                errors.add("発注番号は30文字以内で入力してください");
            }
            // 発注番号のフォーマットチェック
            if (!order.getPoNumber().matches("PO-[0-9]{8}-[0-9]{4}")) {
                // 技術的負債 #8: 英語のエラーメッセージ（日本語と混在）
                errors.add("Invalid PO number format. Expected: PO-YYYYMMDD-NNNN");
            }
        }

        // === ステータスの検証 ===
        if (order.getStatus() == null || order.getStatus().trim().isEmpty()) {
            errors.add("ステータスは必須です");
        } else {
            // ステータス値の妥当性チェック
            String status = order.getStatus();
            if (!status.equals("DRAFT") && !status.equals("SUBMITTED")
                    && !status.equals("APPROVED") && !status.equals("SENT")
                    && !status.equals("PARTIALLY_RECEIVED") && !status.equals("RECEIVED")
                    && !status.equals("COMPLETED") && !status.equals("CANCELLED")) {
                errors.add("無効なステータスです: " + status);
            }
        }

        // === サプライヤーの検証 ===
        if (order.getSupplier() == null) {
            errors.add("サプライヤーは必須です");
        } else {
            if (order.getSupplier().getId() == null) {
                // 技術的負債 #8: 英語のエラーメッセージ
                errors.add("Supplier must have a valid ID");
            }
            // サプライヤーがアクティブかチェック
            if (order.getSupplier().getStatus() != null
                    && !order.getSupplier().getStatus().equals("ACTIVE")) {
                errors.add("無効なサプライヤーが選択されています。アクティブなサプライヤーを選択してください");
            }
        }

        // === 金額の検証 ===
        errors.addAll(validateAmounts(order));

        // === 日付の検証 ===
        errors.addAll(validateDates(order));

        // === 明細の検証 ===
        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("発注明細が1件以上必要です");
        } else {
            if (order.getItems().size() > MAX_ITEMS_PER_ORDER) {
                errors.add("発注明細数が上限(" + MAX_ITEMS_PER_ORDER + "件)を超えています: "
                        + order.getItems().size() + "件");
            }
            errors.addAll(validateItems(order.getItems()));
        }

        // === 通貨の検証 ===
        if (order.getCurrency() == null || order.getCurrency().trim().isEmpty()) {
            errors.add("通貨コードは必須です");
        } else {
            if (order.getCurrency().length() != 3) {
                errors.add("通貨コードは3文字で入力してください");
            }
            // ハードコードされた通貨リスト（技術的負債 #4）
            if (!order.getCurrency().equals("JPY") && !order.getCurrency().equals("USD")
                    && !order.getCurrency().equals("EUR") && !order.getCurrency().equals("GBP")
                    && !order.getCurrency().equals("CNY")) {
                errors.add("サポートされていない通貨です: " + order.getCurrency());
            }
        }

        // === 購入担当者の検証 ===
        if (order.getBuyerId() == null) {
            // 技術的負債 #8: 英語のエラーメッセージ
            errors.add("Buyer ID is required");
        } else {
            if (order.getBuyerId() <= 0) {
                errors.add("購入担当者IDが不正です");
            }
        }

        // === 備考の検証 ===
        if (order.getNotes() != null && order.getNotes().length() > 2000) {
            errors.add("備考は2000文字以内で入力してください");
        }

        logger.info("発注バリデーション完了: " + order.getPoNumber()
                + " エラー数: " + errors.size());

        return errors;
    }

    /**
     * 発注明細リストのバリデーションを実行する。
     *
     * @param items バリデーション対象の発注明細リスト
     * @return エラーメッセージのリスト
     */
    public List<String> validateItems(List<PurchaseOrderItem> items) {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            PurchaseOrderItem item = items.get(i);
            String prefix = "明細行 " + (i + 1) + ": ";

            // lineNumber field was removed from PurchaseOrderItem (DDL alignment)
            // Line number validation is no longer applicable

            // 数量の検証
            if (item.getQuantity() == null) {
                errors.add(prefix + "数量は必須です");
            } else {
                if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(prefix + "数量は0より大きい値を指定してください");
                }
                if (item.getQuantity().scale() > 3) {
                    // 技術的負債 #8: 英語のエラーメッセージ
                    errors.add(prefix + "Quantity precision must be at most 3 decimal places");
                }
            }

            // 単価の検証
            if (item.getUnitPrice() == null) {
                errors.add(prefix + "単価は必須です");
            } else {
                if (item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(prefix + "単価は0以上で指定してください");
                }
                if (item.getUnitPrice().compareTo(new BigDecimal("99999999.99")) > 0) {
                    errors.add(prefix + "単価が上限を超えています");
                }
            }

            // 商品の検証
            if (item.getProduct() != null) {
                // SKUフォーマットの検証
                if (item.getProduct().getSku() != null) {
                    // 技術的負債 #17: PO側のSKUパターンとProduct側のSKUパターンが異なる
                    if (!SKU_PATTERN.matcher(item.getProduct().getSku()).matches()) {
                        errors.add(prefix + "SKUフォーマットが不正です: " + item.getProduct().getSku()
                                + " (期待: [A-Z]{3}-[0-9]{6})");
                    }
                }
            }

            // 税率の検証
            if (item.getTaxRate() != null) {
                if (item.getTaxRate().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(prefix + "税率は0以上で指定してください");
                }
                if (item.getTaxRate().compareTo(BigDecimal.ONE) > 0) {
                    errors.add(prefix + "税率は1.0以下で指定してください");
                }
            }

            // 割引率の検証
            if (item.getDiscount() != null) {
                if (item.getDiscount().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(prefix + "割引率は0以上で指定してください");
                }
                if (item.getDiscount().compareTo(BigDecimal.ONE) > 0) {
                    errors.add(prefix + "割引率は1.0以下で指定してください");
                }
            }

            // 小計の整合性チェック
            if (item.getSubtotal() != null && item.getQuantity() != null
                    && item.getUnitPrice() != null) {
                BigDecimal expectedSubtotal = item.getQuantity().multiply(item.getUnitPrice());
                if (item.getDiscount() != null) {
                    expectedSubtotal = expectedSubtotal.multiply(
                            BigDecimal.ONE.subtract(item.getDiscount()));
                }
                if (item.getTaxRate() != null) {
                    expectedSubtotal = expectedSubtotal.multiply(
                            BigDecimal.ONE.add(item.getTaxRate()));
                }
                // 丸め誤差を考慮して1円の差は許容
                if (item.getSubtotal().subtract(expectedSubtotal).abs()
                        .compareTo(BigDecimal.ONE) > 0) {
                    errors.add(prefix + "小計金額が計算値と一致しません");
                }
            }
        }

        return errors;
    }

    /**
     * 発注金額のバリデーションを実行する。
     *
     * <p>技術的負債 #17: 最大金額が999,999.99であるが、
     * フロントエンドでは1,000,000まで入力可能。境界値の不整合がある。</p>
     *
     * @param order バリデーション対象の発注エンティティ
     * @return エラーメッセージのリスト
     */
    public List<String> validateAmounts(PurchaseOrder order) {
        List<String> errors = new ArrayList<>();

        if (order.getTotalAmount() == null) {
            errors.add("合計金額は必須です");
        } else {
            // 技術的負債 #4: ハードコードされたビジネスルール
            if (order.getTotalAmount().compareTo(MIN_ORDER_AMOUNT) < 0) {
                errors.add("合計金額は" + MIN_ORDER_AMOUNT + "円以上で入力してください");
            }

            // 技術的負債 #17: フロントエンドとの不整合
            // FE: max=1,000,000 / BE: max=999,999.99
            if (order.getTotalAmount().compareTo(MAX_ORDER_AMOUNT) > 0) {
                // 技術的負債 #8: 英語のエラーメッセージ
                errors.add("Total amount exceeds maximum allowed: " + MAX_ORDER_AMOUNT);
            }

            // 合計金額と明細合計の整合性チェック
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                BigDecimal itemsTotal = BigDecimal.ZERO;
                for (PurchaseOrderItem item : order.getItems()) {
                    if (item.getSubtotal() != null) {
                        itemsTotal = itemsTotal.add(item.getSubtotal());
                    }
                }
                // 丸め誤差を考慮して10円の差は許容
                if (order.getTotalAmount().subtract(itemsTotal).abs()
                        .compareTo(new BigDecimal("10")) > 0) {
                    errors.add("合計金額と明細の小計合計が一致しません。合計: "
                            + order.getTotalAmount() + " 明細合計: " + itemsTotal);
                }
            }
        }

        return errors;
    }

    /**
     * 発注日付のバリデーションを実行する。
     *
     * <p>技術的負債: {@link java.util.Date} を使用した手動比較を行っている。
     * {@code java.time.LocalDate} に移行すべき。</p>
     *
     * @param order バリデーション対象の発注エンティティ
     * @return エラーメッセージのリスト
     */
    public List<String> validateDates(PurchaseOrder order) {
        List<String> errors = new ArrayList<>();

        // 技術的負債 #6: java.util.Dateを使用した手動比較
        Date now = new Date();

        // 発注日の検証
        if (order.getOrderDate() == null) {
            errors.add("発注日は必須です");
        } else {
            // ドラフト以外の場合、未来日は許可しない
            if (!"DRAFT".equals(order.getStatus())) {
                // 技術的負債 #6: Date.after()による手動比較
                if (order.getOrderDate().after(now)) {
                    errors.add("発注日に未来の日付は指定できません");
                }
            }

            // 1年以上前の日付は警告（エラーにはしない）
            long oneYearMillis = 365L * 24 * 60 * 60 * 1000;
            Date oneYearAgo = new Date(now.getTime() - oneYearMillis);
            if (order.getOrderDate().before(oneYearAgo)) {
                logger.warning("発注日が1年以上前です: " + order.getPoNumber()
                        + " 日付: " + order.getOrderDate());
            }
        }

        // 納品予定日の検証
        if (order.getExpectedDeliveryDate() != null) {
            if (order.getOrderDate() != null) {
                // 技術的負債 #6: Date.before()による手動比較
                if (order.getExpectedDeliveryDate().before(order.getOrderDate())) {
                    errors.add("納品予定日は発注日以降の日付を指定してください");
                }

                // 納品予定日が発注日から1年以上先の場合はエラー
                long oneYearMillis = 365L * 24 * 60 * 60 * 1000;
                Date maxDeliveryDate = new Date(order.getOrderDate().getTime() + oneYearMillis);
                if (order.getExpectedDeliveryDate().after(maxDeliveryDate)) {
                    // 技術的負債 #8: 英語のエラーメッセージ
                    errors.add("Expected delivery date cannot be more than 1 year from order date");
                }
            }

            // 完了済みの発注で納品予定日が未来の場合は警告
            if ("COMPLETED".equals(order.getStatus())
                    && order.getExpectedDeliveryDate().after(now)) {
                logger.warning("完了済み発注の納品予定日が未来です: " + order.getPoNumber());
            }
        }

        return errors;
    }
}
