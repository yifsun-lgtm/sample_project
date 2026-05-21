package com.proquip.common.constant;

/**
 * アプリケーション全体で使用する定数クラス。
 *
 * <p>【技術的負債】このクラスには以下の問題がある：
 * <ul>
 *   <li>マジックナンバー・マジック文字列が散在している</li>
 *   <li>一部の定数は他クラスやプロパティファイルと重複定義されている</li>
 *   <li>設定ファイルに外出しすべき値がハードコードされている</li>
 *   <li>責務が曖昧で、何でも入れる「神定数クラス」と化している</li>
 * </ul>
 * </p>
 *
 * @author ProQuip開発チーム
 * @since 1.0.0
 */
public final class AppConstants {

    /** インスタンス化を防止 */
    private AppConstants() {
        // ユーティリティクラスのためインスタンス化不可
    }

    // ===== ファイルアップロード =====

    /** アップロードファイルの最大サイズ（MB） */
    public static final int MAX_UPLOAD_SIZE_MB = 10;

    /** アップロードファイルの最大サイズ（バイト） - 上の定数と冗長だが既存コードが参照している */
    public static final long MAX_UPLOAD_SIZE_BYTES = 10 * 1024 * 1024;

    // ===== ページネーション =====

    /** デフォルトのページサイズ */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 最大ページサイズ */
    public static final int MAX_PAGE_SIZE = 100;

    /** デフォルトのページ番号（0始まり） */
    public static final int DEFAULT_PAGE_NUMBER = 0;

    // ===== 日付フォーマット =====

    /** 日付フォーマット（日本形式） */
    public static final String DATE_FORMAT = "yyyy/MM/dd";

    /** 日時フォーマット（日本形式） */
    public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

    /** ISO 8601形式 - API用（後から追加されたが一部のAPIでしか使われていない） */
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    // ===== システムユーザー =====

    /** システムユーザー名（バッチ処理等で使用） */
    public static final String SYSTEM_USER = "SYSTEM";

    /** 移行用ユーザー名（データ移行時の作成者として使用） */
    public static final String MIGRATION_USER = "MIGRATION";

    // ===== ロール名 =====
    // 【技術的負債】web.xmlやJAAS設定にも同じ文字列が定義されている

    /** 管理者ロール */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 購買担当者ロール */
    public static final String ROLE_BUYER = "BUYER";

    /** 承認者ロール */
    public static final String ROLE_APPROVER = "APPROVER";

    /** 倉庫担当者ロール */
    public static final String ROLE_WAREHOUSE = "WAREHOUSE";

    /** 経理担当者ロール */
    public static final String ROLE_FINANCE = "FINANCE";

    /** 閲覧専用ロール */
    public static final String ROLE_VIEWER = "VIEWER";

    // ===== 設定キー =====
    // 【技術的負債】一部はDB上のsystem_configテーブルにも定義がある

    /** 承認不要の上限金額のキー */
    public static final String CONFIG_AUTO_APPROVE_LIMIT = "procurement.auto_approve_limit";

    /** デフォルトの消費税率のキー */
    public static final String CONFIG_DEFAULT_TAX_RATE = "pricing.default_tax_rate";

    /** 在庫警告閾値のキー */
    public static final String CONFIG_LOW_STOCK_THRESHOLD = "inventory.low_stock_threshold";

    /** メール通知有効/無効のキー */
    public static final String CONFIG_EMAIL_ENABLED = "notification.email_enabled";

    // ===== マジックナンバー =====
    // 【技術的負債】以下の定数は本来、設定ファイルまたはDBで管理すべき

    /** 消費税率（10%）- ハードコード。CONFIG_DEFAULT_TAX_RATEと二重管理 */
    public static final double DEFAULT_TAX_RATE = 0.10;

    /** 軽減税率（8%） */
    public static final double REDUCED_TAX_RATE = 0.08;

    /** 自動承認の上限金額（円） */
    public static final long AUTO_APPROVE_LIMIT = 50000;

    /** パスワードの最小文字数 */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /** ログインの最大試行回数 */
    public static final int MAX_LOGIN_ATTEMPTS = 5;

    /** セッションタイムアウト（分） */
    public static final int SESSION_TIMEOUT_MINUTES = 30;

    /** CSVインポート時の最大行数 */
    public static final int MAX_CSV_IMPORT_ROWS = 5000;

    /** SKUコードの長さ */
    public static final int SKU_CODE_LENGTH = 10;

    /** 発注番号のプレフィックス */
    public static final String PO_NUMBER_PREFIX = "PO";

    /** 購入依頼番号のプレフィックス */
    public static final String REQ_NUMBER_PREFIX = "REQ";

    /** 小数点以下の丸め桁数（金額計算用） */
    public static final int PRICE_SCALE = 2;

    // ===== HTTPステータス関連 =====
    // 【技術的負債】JAX-RSのResponse.Statusを使えば不要

    /** HTTPステータス200 */
    public static final int HTTP_OK = 200;

    /** HTTPステータス400 */
    public static final int HTTP_BAD_REQUEST = 400;

    /** HTTPステータス404 */
    public static final int HTTP_NOT_FOUND = 404;

    /** HTTPステータス500 */
    public static final int HTTP_INTERNAL_ERROR = 500;
}
