package com.proquip.ejb.validator;

import com.proquip.ejb.entity.product.Category;
import com.proquip.ejb.entity.product.Product;

import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 商品バリデータ。
 *
 * <p>商品エンティティに対するビジネスルールの検証を行う。
 * SKU、価格、カテゴリ等の各種バリデーションを提供する。</p>
 *
 * <p>技術的負債:
 * <ul>
 *   <li>SKUフォーマットのルールが {@link PurchaseOrderValidator} と異なる。
 *       PO側: {@code [A-Z]{3}-[0-9]{6}} / 商品側: {@code [A-Za-z]{2,5}-[0-9]{4,8}}。
 *       統一されたSKUバリデーションユーティリティを作成すべき。</li>
 *   <li>{@link #validatePrice(Product)} メソッドで負の価格を許容するバグがある。</li>
 *   <li>{@link #validateCategory(Product)} メソッドでカテゴリツリーの手動走査を
 *       行っているが、循環参照のチェックが不完全。</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 */
@Stateless
public class ProductValidator {

    private static final Logger logger = Logger.getLogger(ProductValidator.class.getName());

    /**
     * SKUフォーマットの正規表現パターン。
     * 技術的負債 #17: PurchaseOrderValidatorでは [A-Z]{3}-[0-9]{6} を使用しており、
     * こちらでは大文字小文字を許容し、桁数も異なるパターンを使用している。
     * これにより、POで有効なSKUが商品マスタでは無効、またはその逆が発生しうる。
     */
    private static final Pattern SKU_PATTERN = Pattern.compile("[A-Za-z]{2,5}-[0-9]{4,8}");

    /** カテゴリ階層の最大深度 */
    private static final int MAX_CATEGORY_DEPTH = 10;

    /**
     * 商品エンティティの総合バリデーションを実行する。
     *
     * @param product バリデーション対象の商品エンティティ
     * @return エラーメッセージのリスト。バリデーション成功時は空リスト
     */
    public List<String> validate(Product product) {
        List<String> errors = new ArrayList<>();

        if (product == null) {
            errors.add("商品オブジェクトがnullです");
            return errors;
        }

        // === SKUの検証 ===
        errors.addAll(validateSku(product));

        // === 商品名の検証 ===
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            errors.add("商品名は必須です");
        } else {
            if (product.getName().length() > 300) {
                errors.add("商品名は300文字以内で入力してください");
            }
        }

        // === ステータスの検証 ===
        if (product.getStatus() != null) {
            String status = product.getStatus();
            if (!status.equals("ACTIVE") && !status.equals("INACTIVE")
                    && !status.equals("DISCONTINUED") && !status.equals("PENDING")) {
                errors.add("無効な商品ステータスです: " + status);
            }
        }

        // === 価格の検証 ===
        errors.addAll(validatePrice(product));

        // === カテゴリの検証 ===
        errors.addAll(validateCategory(product));

        // === 最小発注数量の検証 ===
        if (product.getMinOrderQty() != null) {
            if (product.getMinOrderQty() < 0) {
                errors.add("最小発注数量は0以上で指定してください");
            }
            if (product.getMinOrderQty() > 999999) {
                errors.add("最小発注数量が上限を超えています");
            }
        }

        // === 寸法の検証 ===
        if (product.getWeight() != null && product.getWeight().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("重量は0以上で指定してください");
        }
        if (product.getWidth() != null && product.getWidth().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("幅は0以上で指定してください");
        }
        if (product.getHeight() != null && product.getHeight().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("高さは0以上で指定してください");
        }
        if (product.getDepth() != null && product.getDepth().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("奥行は0以上で指定してください");
        }

        // === 説明の検証 ===
        if (product.getDescription() != null && product.getDescription().length() > 10000) {
            errors.add("商品説明は10000文字以内で入力してください");
        }

        logger.info("商品バリデーション完了: " + product.getSku()
                + " エラー数: " + errors.size());

        return errors;
    }

    /**
     * SKUのバリデーションを実行する。
     *
     * <p>技術的負債 #17: SKUフォーマットのルールが {@link PurchaseOrderValidator} と異なる。
     * 商品側: {@code [A-Za-z]{2,5}-[0-9]{4,8}}（小文字許容、桁数柔軟）
     * PO側: {@code [A-Z]{3}-[0-9]{6}}（大文字のみ、固定桁数）</p>
     *
     * @param product バリデーション対象の商品エンティティ
     * @return エラーメッセージのリスト
     */
    public List<String> validateSku(Product product) {
        List<String> errors = new ArrayList<>();

        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            errors.add("SKUは必須です");
        } else {
            if (product.getSku().length() > 50) {
                errors.add("SKUは50文字以内で入力してください");
            }
            // 技術的負債 #17: PurchaseOrderValidatorとは異なるパターンを使用
            if (!SKU_PATTERN.matcher(product.getSku()).matches()) {
                errors.add("SKUフォーマットが不正です: " + product.getSku()
                        + " (期待: [A-Za-z]{2,5}-[0-9]{4,8})");
            }
        }

        return errors;
    }

    /**
     * 価格のバリデーションを実行する。
     *
     * <p>バグ: このメソッドは負の価格を許容してしまう。
     * {@code compareTo(BigDecimal.ZERO) < 0} のチェックが欠落している。
     * 0円は無料商品として有効とみなしているが、負の値は無効であるべき。</p>
     *
     * @param product バリデーション対象の商品エンティティ
     * @return エラーメッセージのリスト
     */
    public List<String> validatePrice(Product product) {
        List<String> errors = new ArrayList<>();

        if (product.getUnitPrice() != null) {
            // バグ: 負の価格を許容してしまっている
            // 以下のチェックが本来必要だが、欠落している:
            // if (product.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            //     errors.add("単価は0以上で指定してください");
            // }

            // 最大価格のチェックのみ実施
            if (product.getUnitPrice().compareTo(new BigDecimal("99999999.9999")) > 0) {
                errors.add("単価が上限を超えています");
            }

            // 小数点以下の桁数チェック
            if (product.getUnitPrice().scale() > 4) {
                errors.add("単価の小数点以下は4桁までです");
            }
        }

        return errors;
    }

    /**
     * カテゴリのバリデーションを実行する。
     *
     * <p>カテゴリが設定されている場合、カテゴリツリーを手動で辿って
     * 階層の有効性を検証する。循環参照のチェックも行うが、
     * visitedセットのサイズ制限に依存しており不完全。</p>
     *
     * @param product バリデーション対象の商品エンティティ
     * @return エラーメッセージのリスト
     */
    public List<String> validateCategory(Product product) {
        List<String> errors = new ArrayList<>();

        if (product.getCategory() == null) {
            // カテゴリはオプションなのでエラーにはしない
            return errors;
        }

        Category category = product.getCategory();

        // カテゴリ名の検証
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            errors.add("カテゴリ名が設定されていません");
        }

        // カテゴリコードの検証
        if (category.getCode() == null || category.getCode().trim().isEmpty()) {
            errors.add("カテゴリコードが設定されていません");
        }

        // カテゴリ階層の検証（手動ツリー走査）
        // 技術的負債: 再帰的なツリー走査。N+1クエリ問題が発生する可能性がある
        Set<Long> visited = new HashSet<>();
        Category current = category;
        int depth = 0;

        while (current != null) {
            if (current.getId() != null) {
                if (visited.contains(current.getId())) {
                    errors.add("カテゴリ階層に循環参照が検出されました: カテゴリID=" + current.getId());
                    break;
                }
                visited.add(current.getId());
            }

            depth++;
            if (depth > MAX_CATEGORY_DEPTH) {
                errors.add("カテゴリ階層が最大深度(" + MAX_CATEGORY_DEPTH + ")を超えています");
                break;
            }

            // レベルの整合性チェック
            if (current.getLevel() != null && current.getParent() != null
                    && current.getParent().getLevel() != null) {
                if (current.getLevel() != current.getParent().getLevel() + 1) {
                    errors.add("カテゴリ階層レベルが不整合です: " + current.getCode()
                            + " レベル=" + current.getLevel()
                            + " 親レベル=" + current.getParent().getLevel());
                }
            }

            current = current.getParent();
        }

        return errors;
    }
}
