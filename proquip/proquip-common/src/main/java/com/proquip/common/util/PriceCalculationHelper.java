package com.proquip.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 価格計算に関するヘルパークラス。
 *
 * <p>【技術的負債 - 重大】このクラスには以下の深刻な問題がある：
 * <ul>
 *   <li>税率が州ごとにif-elseチェーンでハードコードされている</li>
 *   <li>為替レートがハードコードされており、リアルタイムレートを参照していない</li>
 *   <li>ボリュームディスカウントの閾値がハードコードされている</li>
 *   <li>丸めモードが統一されていない（HALF_UPとHALF_DOWNが混在）</li>
 *   <li>ビジネスルールの外部化（DB・設定ファイル）がされていない</li>
 * </ul>
 * </p>
 *
 * <p>【対応方針】税率・為替レート・ディスカウントルール等はすべてDBまたは
 * 外部設定に移行し、このクラスは純粋な計算ロジックのみにすべき。</p>
 *
 * @author ProQuip開発チーム（初期実装: 2019年）
 * @since 1.0.0
 */
public final class PriceCalculationHelper {

    /** 金額計算の小数点以下桁数 */
    private static final int PRICE_SCALE = 2;

    /** 100（パーセンテージ計算用） */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** インスタンス化を防止 */
    private PriceCalculationHelper() {
    }

    /**
     * 小計を計算する。
     *
     * <p>小計 = 単価 x 数量 x (1 - 値引率/100)</p>
     *
     * @param unitPrice 単価
     * @param quantity 数量
     * @param discountPercent 値引率（%）。0〜100の範囲。nullの場合は0として扱う
     * @return 小計金額（小数第2位まで、HALF_UP丸め）
     * @throws IllegalArgumentException 単価がnullまたは数量が0以下の場合
     */
    public static BigDecimal calculateSubtotal(BigDecimal unitPrice, int quantity,
                                                BigDecimal discountPercent) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("単価がnullです。");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量は1以上を指定してください。");
        }

        BigDecimal qty = new BigDecimal(quantity);
        BigDecimal subtotal = unitPrice.multiply(qty);

        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountRate = BigDecimal.ONE.subtract(
                    discountPercent.divide(HUNDRED, 4, RoundingMode.HALF_UP));
            subtotal = subtotal.multiply(discountRate);
        }

        return subtotal.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 税額を計算する。
     *
     * <p>【技術的負債 - 重大】税率が州コードに基づくif-elseチェーンで
     * ハードコードされている。法改正のたびにコード変更・再デプロイが必要。
     * 本来はDBまたは外部APIで管理すべき。</p>
     *
     * <p>【注意】2024年の税率改正時に一部の州の税率を更新したが、
     * 古い税率で計算された過去データとの整合性確認が未実施。</p>
     *
     * @param amount 課税対象金額
     * @param stateCode 州コード（2文字の大文字）
     * @return 税額（小数第2位まで、HALF_UP丸め）
     */
    public static BigDecimal calculateTax(BigDecimal amount, String stateCode) {
        if (amount == null || stateCode == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal taxRate;

        // 【技術的負債】この巨大なif-elseチェーンはDBのマスタテーブルに移行すべき
        if ("CA".equals(stateCode)) {
            taxRate = new BigDecimal("0.0725");
        } else if ("NY".equals(stateCode)) {
            taxRate = new BigDecimal("0.08");
        } else if ("TX".equals(stateCode)) {
            taxRate = new BigDecimal("0.0625");
        } else if ("FL".equals(stateCode)) {
            taxRate = new BigDecimal("0.06");
        } else if ("IL".equals(stateCode)) {
            taxRate = new BigDecimal("0.0625");
        } else if ("PA".equals(stateCode)) {
            taxRate = new BigDecimal("0.06");
        } else if ("OH".equals(stateCode)) {
            taxRate = new BigDecimal("0.0575");
        } else if ("GA".equals(stateCode)) {
            taxRate = new BigDecimal("0.04");
        } else if ("NC".equals(stateCode)) {
            taxRate = new BigDecimal("0.0475");
        } else if ("MI".equals(stateCode)) {
            taxRate = new BigDecimal("0.06");
        } else if ("NJ".equals(stateCode)) {
            taxRate = new BigDecimal("0.06625");
        } else if ("VA".equals(stateCode)) {
            taxRate = new BigDecimal("0.053");
        } else if ("WA".equals(stateCode)) {
            taxRate = new BigDecimal("0.065");
        } else if ("AZ".equals(stateCode)) {
            taxRate = new BigDecimal("0.056");
        } else if ("MA".equals(stateCode)) {
            taxRate = new BigDecimal("0.0625");
        } else {
            // デフォルト税率（州が見つからない場合）
            // TODO: 不明な州コードの場合はエラーにすべきか要検討
            taxRate = new BigDecimal("0.05");
        }

        return amount.multiply(taxRate).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 明細リストの合計金額を計算する。
     *
     * <p>各明細はMap形式で、"subtotal"キーにBigDecimal型の小計を持つ想定。</p>
     *
     * <p>【技術的負債】for-indexループを使用しており、
     * 型安全でないMap操作をしている。専用のDTOクラスを使うべき。</p>
     *
     * @param items 明細リスト。各要素は"subtotal"キーを持つMap
     * @return 合計金額
     */
    @SuppressWarnings("unchecked")
    public static BigDecimal calculateTotal(List items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;

        // 【技術的負債】拡張forまたはStream APIを使うべき
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                Object subtotalObj = map.get("subtotal");
                if (subtotalObj instanceof BigDecimal) {
                    total = total.add((BigDecimal) subtotalObj);
                } else if (subtotalObj != null) {
                    // 文字列からの変換を試みる（レガシーデータ対応）
                    try {
                        total = total.add(new BigDecimal(subtotalObj.toString()));
                    } catch (NumberFormatException e) {
                        // 変換失敗時はスキップ（技術的負債：サイレント無視）
                    }
                }
            }
        }

        return total.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * ボリュームディスカウント率を適用した金額を計算する。
     *
     * <p>【技術的負債】ディスカウントのティア閾値がハードコードされている。
     * マスタテーブルまたは設定ファイルで管理すべき。</p>
     *
     * <p>ティア構成：
     * <ul>
     *   <li>100個未満: 割引なし</li>
     *   <li>100〜499個: 5%割引</li>
     *   <li>500〜999個: 10%割引</li>
     *   <li>1000個以上: 15%割引</li>
     * </ul>
     * </p>
     *
     * @param amount 割引前の金額
     * @param quantity 注文数量
     * @return 割引適用後の金額
     */
    public static BigDecimal applyVolumeDiscount(BigDecimal amount, int quantity) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountRate;

        // 【技術的負債】閾値をハードコード。pricing_tierテーブルに移行すべき。
        if (quantity >= 1000) {
            discountRate = new BigDecimal("0.15");
        } else if (quantity >= 500) {
            discountRate = new BigDecimal("0.10");
        } else if (quantity >= 100) {
            discountRate = new BigDecimal("0.05");
        } else {
            discountRate = BigDecimal.ZERO;
        }

        BigDecimal discountAmount = amount.multiply(discountRate);
        return amount.subtract(discountAmount)
                .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 通貨を変換する。
     *
     * <p>【技術的負債 - 重大】為替レートがハードコードされている。
     * リアルタイムの為替レートAPI（例: ECBやOpen Exchange Rates）を
     * 使用すべき。現在のレートは2024年1月時点のもの。</p>
     *
     * @param amount 変換元の金額
     * @param fromCurrency 変換元通貨コード（ISO 4217）
     * @param toCurrency 変換先通貨コード（ISO 4217）
     * @return 変換後の金額
     * @throws IllegalArgumentException サポートされていない通貨コードの場合
     */
    public static BigDecimal convertCurrency(BigDecimal amount,
                                              String fromCurrency,
                                              String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("通貨コードがnullです。");
        }
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        // まずUSDに変換し、その後目的通貨に変換する（クロスレート方式）
        BigDecimal amountInUsd = convertToUsd(amount, fromCurrency);
        return convertFromUsd(amountInUsd, toCurrency);
    }

    /**
     * 指定通貨からUSDに変換する（内部メソッド）。
     *
     * <p>【技術的負債】為替レートのハードコード。</p>
     */
    private static BigDecimal convertToUsd(BigDecimal amount, String currency) {
        BigDecimal rate;
        switch (currency) {
            case "USD":
                rate = BigDecimal.ONE;
                break;
            case "JPY":
                // 2024年1月時点の概算レート
                rate = new BigDecimal("0.0068");
                break;
            case "EUR":
                rate = new BigDecimal("1.10");
                break;
            case "GBP":
                rate = new BigDecimal("1.27");
                break;
            case "CNY":
                rate = new BigDecimal("0.14");
                break;
            default:
                throw new IllegalArgumentException(
                        "サポートされていない通貨コードです: " + currency);
        }
        return amount.multiply(rate).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * USDから指定通貨に変換する（内部メソッド）。
     *
     * <p>【技術的負債】convertToUsdと逆数でレート管理すべきだが、
     * 別途ハードコードしてしまっている。</p>
     */
    private static BigDecimal convertFromUsd(BigDecimal amountInUsd,
                                              String currency) {
        BigDecimal rate;
        switch (currency) {
            case "USD":
                rate = BigDecimal.ONE;
                break;
            case "JPY":
                rate = new BigDecimal("148.50");
                break;
            case "EUR":
                rate = new BigDecimal("0.91");
                break;
            case "GBP":
                rate = new BigDecimal("0.79");
                break;
            case "CNY":
                rate = new BigDecimal("7.18");
                break;
            default:
                throw new IllegalArgumentException(
                        "サポートされていない通貨コードです: " + currency);
        }
        return amountInUsd.multiply(rate).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 金額を丸める。
     *
     * <p>【技術的負債】丸めモードが呼び出し箇所によって異なる。
     * このメソッドはHALF_DOWNを使用しているが、calculateSubtotalやcalculateTaxは
     * HALF_UPを使用しており一貫性がない。統一すべき。</p>
     *
     * @param amount 丸め対象の金額
     * @return 小数第2位まで丸められた金額
     */
    public static BigDecimal roundPrice(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        // 【技術的負債】他のメソッドはHALF_UPなのに、ここだけHALF_DOWN
        // 元の開発者がなぜHALF_DOWNにしたか不明。変更すると既存データと不整合の恐れあり。
        return amount.setScale(PRICE_SCALE, RoundingMode.HALF_DOWN);
    }

    /**
     * 消費税込み金額を計算する（日本向け）。
     *
     * <p>【技術的負債】税率がAppConstantsと二重管理されている。</p>
     *
     * @param amount 税抜金額
     * @param reducedRate 軽減税率対象の場合true（8%）、通常税率の場合false（10%）
     * @return 税込金額
     */
    public static BigDecimal calculateJapaneseTax(BigDecimal amount,
                                                   boolean reducedRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        // 【技術的負債】AppConstants.DEFAULT_TAX_RATEおよびREDUCED_TAX_RATEと重複
        BigDecimal taxRate;
        if (reducedRate) {
            taxRate = new BigDecimal("0.08"); // 軽減税率
        } else {
            taxRate = new BigDecimal("0.10"); // 標準税率
        }

        BigDecimal tax = amount.multiply(taxRate)
                .setScale(0, RoundingMode.DOWN); // 消費税は切り捨て
        return amount.add(tax);
    }
}
