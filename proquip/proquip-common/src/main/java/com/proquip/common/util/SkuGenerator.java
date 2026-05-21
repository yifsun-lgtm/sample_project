package com.proquip.common.util;

import java.util.regex.Pattern;

/**
 * SKU（Stock Keeping Unit）コードの生成・検証を行うユーティリティクラス。
 *
 * <p>SKUコードの形式: {@code XXX-000000}（カテゴリコード3文字 + ハイフン + 連番6桁）</p>
 *
 * <p>【技術的負債】フロントエンド（JavaScript）側では異なるバリデーション正規表現を
 * 使用しており、バックエンドとフロントエンドでバリデーション結果が不一致になる
 * ケースが報告されている。FE側は {@code /^[A-Z]{2,4}-\d{4,8}$/} を使用。</p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public final class SkuGenerator {

    /** SKUコードの正規表現パターン（BE側）: 大文字英字3文字 + ハイフン + 数字6桁 */
    private static final Pattern SKU_PATTERN =
            Pattern.compile("^[A-Z]{3}-\\d{6}$");

    /** カテゴリコードの長さ */
    private static final int CATEGORY_CODE_LENGTH = 3;

    /** 連番部分の桁数 */
    private static final int SEQUENCE_DIGITS = 6;

    /** 連番の最大値 */
    private static final long MAX_SEQUENCE = 999999;

    /** インスタンス化を防止 */
    private SkuGenerator() {
    }

    /**
     * SKUコードを生成する。
     *
     * <p>カテゴリコードと連番からSKUコードを生成する。
     * カテゴリコードは大文字に変換され、3文字に切り詰められる。
     * 連番は6桁のゼロ埋めで出力される。</p>
     *
     * @param categoryCode カテゴリコード（英字3文字）
     * @param sequence 連番（0〜999999）
     * @return 生成されたSKUコード（例: "ELC-000042"）
     * @throws IllegalArgumentException カテゴリコードが不正、または連番が範囲外の場合
     */
    public static String generateSku(String categoryCode, long sequence) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "カテゴリコードがnullまたは空です。");
        }

        String code = categoryCode.trim().toUpperCase();

        if (code.length() < CATEGORY_CODE_LENGTH) {
            // 短い場合は右側をXで埋める（暫定仕様。本来はエラーにすべきかもしれない）
            code = StringUtils.padRight(code, CATEGORY_CODE_LENGTH, 'X');
        } else if (code.length() > CATEGORY_CODE_LENGTH) {
            code = code.substring(0, CATEGORY_CODE_LENGTH);
        }

        if (sequence < 0 || sequence > MAX_SEQUENCE) {
            throw new IllegalArgumentException(
                    "連番は0〜" + MAX_SEQUENCE + "の範囲で指定してください。: "
                            + sequence);
        }

        String seqStr = StringUtils.padLeft(
                String.valueOf(sequence), SEQUENCE_DIGITS, '0');

        return code + "-" + seqStr;
    }

    /**
     * SKUコードのフォーマットを検証する。
     *
     * <p>【技術的負債】このバリデーションはフロントエンド側のバリデーションと
     * 正規表現が異なる。BE側は厳密に「英字3文字-数字6桁」を要求するが、
     * FE側は「英字2〜4文字-数字4〜8桁」を許容している。
     * 統一する場合、既存データとの整合性確認が必要。</p>
     *
     * @param sku 検証対象のSKUコード
     * @return 有効な形式の場合true
     */
    public static boolean validateSku(String sku) {
        if (sku == null || sku.isEmpty()) {
            return false;
        }
        return SKU_PATTERN.matcher(sku).matches();
    }
}
