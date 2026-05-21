# ProQuip - エンタープライズ調達・在庫管理システム

IT機器ディストリビューター向けの調達・在庫管理システムのサンプルプロジェクト。
AIリファクタリング検証用に、意図的な技術的負債を含む約10万ステップ規模のコードベース。

## システム概要

ProQuipは、IT機器の調達から在庫管理までを一元管理するエンタープライズシステムです。

### ドメイン領域（7つ）

| 領域 | エンティティ数 | 主な機能 |
|------|-------------|---------|
| 製品カタログ | 12 | 製品・カテゴリ・メーカー管理、製品バンドル、代替品 |
| サプライヤー管理 | 8 | 仕入先・連絡先・契約・評価・認証管理 |
| 調達管理 | 10 | 購買依頼→発注→承認ワークフロー→入荷→返品 |
| 在庫・倉庫管理 | 8 | 在庫管理、倉庫ゾーン、在庫移動、棚卸 |
| 価格・予算 | 6 | 価格表、税率、予算管理 |
| 組織・ユーザー | 6 | 部門（階層）、ユーザー、ロール・権限、委任 |
| 通知・システム | 4 | 通知テンプレート、監査ログ、システム設定、インポートジョブ |

### 画面一覧（47画面・10モジュール）

1. **ダッシュボード** — メインダッシュボード、マイタスク
2. **製品カタログ** — 一覧、詳細/編集（7タブ）、作成ウィザード、カテゴリ管理、バンドル管理
3. **サプライヤー** — 一覧、詳細（5タブ）、作成/編集、サプライヤー比較
4. **調達管理** — 購買依頼（一覧/作成/詳細）、発注（一覧/作成/詳細）、承認キュー、入荷処理、返品管理
5. **在庫管理** — 在庫概要、在庫詳細、在庫移動（作成/一覧）、棚卸、取引履歴
6. **倉庫管理** — 倉庫一覧、詳細、レイアウト表示
7. **価格管理** — 価格表管理、価格編集（インライン）、価格比較
8. **レポート** — 支出分析、在庫評価、サプライヤー実績、予算対実績
9. **管理者設定** — ユーザー管理、ロール/権限、システム設定、監査ログ、予算管理、マスタデータ管理、承認委譲管理、部門管理、通知設定
10. **インポート/エクスポート** — CSV取込/出力、カラムマッピング

## 技術スタック

| レイヤー | 技術 | バージョン |
|---------|------|-----------|
| コンテナ | Podman Compose | - |
| DB | PostgreSQL | 15.7 |
| 認証 | Keycloak (OIDC) | 22.0.5 |
| AP | WildFly (Jakarta EE 10) | 30.0.1 |
| ビルド | Maven (multi-module) | - |
| Java | OpenJDK | 17 |
| ORM | Hibernate | 6.4.9 |
| DBマイグレーション | Flyway | 10.15.0 |
| フロントエンド | Angular | 17.3.12 |
| FE認証 | keycloak-angular | 15.3.0 |
| リバースプロキシ | nginx | 1.25.4 |

**意図的にSpringを使わず**、純粋なJakarta EE（EJB, JPA, JAX-RS, CDI）で構築しています。

### Mavenモジュール構成

```
proquip-parent/     … 親POM（バージョン一元管理）
proquip-common/     … DTO, Mapper, ユーティリティ, 定数, 例外
proquip-ejb/        … JPAエンティティ, EJBサービス, DAO, Interceptor, Scheduler
proquip-web/        … JAX-RS REST API, フィルター, セキュリティ設定
proquip-ear/        … EARパッケージング
proquip-db/         … Flywayマイグレーション + シードデータ
proquip-frontend/   … Angular SPA
```

## 起動方法

### 前提条件

- Podman + Podman Compose（または Docker + Docker Compose）
- Java 17 (JDK)
- Maven 3.9+
- Node.js 18+ / npm 9+

### 1. インフラ起動

```bash
cd proquip
podman compose up -d
```

4つのコンテナが起動します：

| サービス | ポート | 用途 |
|---------|--------|------|
| proquip-postgres | 5432 | PostgreSQL データベース |
| proquip-keycloak | 8180 | Keycloak 認証サーバー |
| proquip-wildfly | 8080 (API), 9990 (管理) | WildFly アプリケーションサーバー |
| proquip-nginx | 4200 | nginx リバースプロキシ |

### 2. デフォルト認証情報

| サービス | ユーザー | パスワード |
|---------|---------|-----------|
| PostgreSQL | proquip | proquip |
| Keycloak 管理コンソール | admin | admin |
| WildFly 管理コンソール | admin | admin123! |

### テストユーザー（Keycloak）

| ユーザー名 | パスワード | ロール | 氏名 |
|-----------|-----------|--------|------|
| admin | admin123 | ADMIN | 管理太郎 |
| manager | manager123 | MANAGER | 部長花子 |
| buyer | buyer123 | BUYER | 購買次郎 |
| warehouse | warehouse123 | WAREHOUSE_STAFF | 倉庫三郎 |
| viewer | viewer123 | VIEWER | 閲覧四郎 |

### 3. バックエンドビルド＆デプロイ

```bash
# 全モジュールビルド
cd proquip-parent
mvn clean install

# DBマイグレーション実行
cd ../proquip-db
mvn flyway:migrate

# WildFlyへデプロイ
cd ../proquip-ear
mvn wildfly:deploy
```

#### ビルドプロファイル

```bash
mvn clean package -P dev          # 開発環境（デフォルト）
mvn clean package -P staging      # ステージング環境
mvn clean package -P production   # 本番環境
```

### 4. フロントエンド起動

```bash
cd proquip-frontend
npm install
npm start
```

開発サーバーが起動し、プロキシ経由でバックエンドに接続します：
- `/api/*` → `http://localhost:8080` (WildFly)
- `/auth/*` → `http://localhost:8180` (Keycloak)

### 5. アクセスURL

| URL | 用途 |
|-----|------|
| http://localhost:4200 | フロントエンド（nginx経由 or ng serve） |
| http://localhost:4200/api/health | バックエンド ヘルスチェック（nginx経由） |
| http://localhost:8180 | Keycloak 管理コンソール |
| http://localhost:9990 | WildFly 管理コンソール |

### コンテナ停止

```bash
podman compose down        # コンテナ停止
podman compose down -v     # コンテナ停止 + データ削除
```

## ディレクトリ構造

```
proquip/
├── README.md
├── podman-compose.yml
├── .env.example
├── .gitignore
├── docker/
│   ├── wildfly/        Dockerfile, standalone-custom.xml, CLI設定, PGモジュール
│   ├── keycloak/       Dockerfile, proquip-realm.json
│   ├── nginx/          Dockerfile, nginx.conf
│   └── postgres/       初期化SQL
├── proquip-parent/     親POM
├── proquip-common/     共通モジュール
│   └── src/main/java/com/proquip/common/
│       ├── constant/   定数・Enum (7ファイル)
│       ├── dto/        DTO (41ファイル, 命名不統一あり)
│       ├── exception/  例外クラス (7ファイル)
│       ├── mapper/     MapStruct + 手書きMapper (11ファイル)
│       └── util/       ユーティリティ (8ファイル, 技術的負債あり)
├── proquip-ejb/        EJBモジュール
│   └── src/main/java/com/proquip/ejb/
│       ├── dao/        DAO層 (10ファイル, パターン不統一)
│       ├── entity/     JPAエンティティ (56ファイル, 7パッケージ)
│       ├── event/      CDIイベント (4ファイル)
│       ├── interceptor/ CDIインターセプター (7ファイル)
│       ├── scheduler/  スケジューラー (5ファイル)
│       ├── service/    EJBサービス (20ファイル, God Class含む)
│       └── validator/  バリデーター (4ファイル)
├── proquip-web/        WEBモジュール (JAX-RS)
│   └── src/main/java/com/proquip/web/
│       ├── filter/     フィルター・ExceptionMapper (5ファイル)
│       └── resource/   RESTリソース (21ファイル)
├── proquip-ear/        EARパッケージング
├── proquip-db/         DBマイグレーション
│   └── src/main/resources/db/migration/
│       ├── V001〜V011  DDL, インデックス, ビュー, ストアドプロシージャ
│       └── V020〜V027  シードデータ (マスタ, 製品, サプライヤー, 在庫, 調達, 価格, システム)
└── proquip-frontend/   Angular SPA
    └── src/app/
        ├── core/       認証, レイアウト, インターセプター
        ├── shared/     共通コンポーネント, パイプ, ディレクティブ, モデル, サービス
        └── features/   10モジュール (dashboard, products, suppliers, procurement,
                        inventory, warehouses, pricing, reports, admin, import-export)
```

## コードベース規模

| カテゴリ | ファイル数 | LOC | 備考 |
|---------|----------|-----|------|
| Java (Backend) | ~190 | ~47,000 | エンティティ, サービス, DAO, REST |
| TypeScript (Frontend) | ~120 | ~20,600 | コンポーネント, サービス, テスト |
| HTML (Templates) | ~50 | ~7,800 | Angular テンプレート |
| SCSS/CSS (Styles) | ~50 | ~12,000 | コンポーネントスタイル |
| SQL | ~20 | ~13,700 | DDL + シードデータ |
| XML/JSON/Config | ~30 | ~4,400 | Maven POM, WildFly設定, i18n |
| **合計** | **~563** | **~105,500** | |

## 技術的負債パターン（20種）

このプロジェクトには、AIリファクタリング検証のために**意図的に**以下の技術的負債が埋め込まれています。

### 1. God Class（巨大クラス）

**該当ファイル**: `PurchaseOrderServiceBean.java` (1,963行, 40+メソッド, 15+依存)

発注に関するCRUD、承認、入荷、予算チェック、レポート、通知、CSV出力をすべて1クラスに集約。単一責任原則に違反。

### 2. Copy-Paste（コピペコード）

**該当ファイル**: `RequisitionServiceBean.java` と `PurchaseOrderServiceBean.java`

承認ワークフローのロジックが購買依頼と発注で重複。閾値やステータス文字列が微妙に異なる。バリデーター (`PurchaseOrderValidator.java` / `RequisitionValidator.java`) も同様にコピペ。

### 3. N+1 クエリ

**該当ファイル**: `ProductResource.java` (検索), `DashboardResource.java` (サマリ構築)

発注一覧で各行ごとにitems→product→categoryを遅延ロード。ダッシュボードで5つのAPIを逐次呼び出し。

### 4. ハードコードされたビジネスルール

**該当ファイル**: `PurchaseOrderServiceBean.java`, `LowStockAlertScheduler.java`

承認閾値（100万円→MANAGER、500万円→ADMIN）、税率、在庫アラート閾値、メール宛先がソースコードにリテラルで埋め込み。

### 5. 関心の混在（Mixed Concerns）

**該当ファイル**: `ProductResource.java` (EntityManagerを直接使用), `order-create.component.ts` (~200行のビジネスロジック)

RESTリソースにDB直接アクセス。Angularコンポーネントに価格計算・税計算・バリデーションロジック。

### 6. Pre-Java 8 スタイル

**該当ファイル**: `DateUtils.java`, `BaseEntity.java`, 各サービスビーン

`java.util.Date` / `Calendar` / `SimpleDateFormat`（スレッドセーフでない）、for-indexループ、`StringBuffer`によるJPQL構築。

### 7. 壊れたエラーハンドリング

**該当ファイル**: `PurchaseOrderServiceBean.java`, `AuditInterceptor.java`, `GlobalExceptionMapper.java`

空catch、`Exception`一括catch→null返却、HTTP 200でエラーボディ返却。

### 8. 命名不統一

**該当ファイル**: `dto/` パッケージ全体

`ProductDTO` vs `ProductDetailDto` vs `PurchaseOrderResponse` — DTO/Dto/Response の混在。`ServiceBean` / `Manager` / `Handler` / `Helper` の混在。

### 9. デッドコード

**該当ファイル**: `LegacyReportGenerator.java` (300行), `OldDataMigrationHelper.java` (200行), `old-dashboard.component.ts`

`@Deprecated` だが削除されていない。未使用のコンポーネント・クラスが残存。「念のため残す」コメント。

### 10. 過剰設計（Over-Engineering）

**該当ファイル**: `service/base/Abstract*EntityServiceBean.java` (5段階継承), `NotificationSenderFactory.java`

5段階の抽象クラス階層（誰も継承しない）。通知送信に2実装しかないのにAbstract Factoryパターン。

### 11. SQLインジェクション

**該当ファイル**: `PurchaseOrderDao.java` (`searchOrders()`), `ReportQueryHelper.java`

ユーザー入力を直接文字列結合してネイティブSQL/JPQLを構築。パラメータバインドを使用していない。

### 12. 密結合

**該当ファイル**: `SupplierResource.java`, `ReportResource.java`

RESTリソースがJPAエンティティを直接レスポンスとして返却。`List<Object[]>` や `HashMap` での戻り値。フロントエンドがDBカラム名に依存。

### 13. 脆弱なテスト

**該当ファイル**: `*Test.java`, `*.spec.ts` 全般

テストカバレッジ約15%。ハードコード日付 (`"2024-03-15"`)、`@Disabled` テスト多数、コメントアウトされたテスト。`xit()` だらけのフロントエンドspec。

### 14. マジック文字列

**該当ファイル**: `PurchaseOrderServiceBean.java`, `order-detail.component.html`

ステータス値が `"APPROVED"` / `"approved"` と大文字小文字不統一。Enumを使わず生文字列で比較。

### 15. モノリシックトランザクション

**該当ファイル**: `PurchaseOrderServiceBean.java` (`createOrder()`)

PO作成→明細作成→予算チェック→承認開始→通知送信を1トランザクションで実行。メール送信失敗でロールバック。

### 16. 手動JSON処理

**該当ファイル**: `JsonHelper.java`, `AuditLog.java`

Jacksonが使える環境で `toJson()` / `fromJson()` を手書き。日付形式が ISO / epoch / MM-dd-yyyy で不統一。

### 17. フロントエンド/バックエンド バリデーション不一致

**該当ファイル**: `custom-validators.ts` vs `PurchaseOrderValidator.java`

| ルール | フロントエンド | バックエンド |
|--------|-------------|------------|
| 金額上限 | 1,000,000 | 999,999.99 |
| SKUフォーマット | `[A-Za-z]{2,5}-[0-9]{4,8}` | `[A-Z]{3}-[0-9]{6}` |
| 最小金額 | なし | 1,000 (PO) / 5,000 (依頼) |

### 18. データアクセスパターン混在

**該当ファイル**: `dao/` パッケージ全体

`ProductDao` → Criteria API、`SupplierDao` → JPQL、`PurchaseOrderDao` → ネイティブSQL、`CustomQueryBuilder` → 独自ビルダー。4つのパターンが混在。

### 19. 環境結合

**該当ファイル**: `ImportJobCleanupScheduler.java`, `OldDataMigrationHelper.java`, `LowStockAlertScheduler.java`

ファイルパス `/opt/proquip/imports/` ハードコード。SMTPサーバー直書き。PostgreSQL固有のSQL。メール宛先リスト直書き。

### 20. 循環依存

**該当ファイル**: `PurchaseOrderServiceBean.java` ↔ `InventoryServiceBean.java`

発注サービスが在庫サービスを `@EJB` で注入し、在庫サービスも発注サービスを `@EJB` で注入。循環参照。

## 技術的負債の所在マップ

特に負債が集中しているファイル：

| ファイル | LOC | 含まれるパターン |
|---------|-----|----------------|
| `PurchaseOrderServiceBean.java` | 1,963 | #1, #2, #3, #4, #7, #14, #15, #20 |
| `order-create.component.ts` | ~450 | #5, #14, #17 |
| `PurchaseOrderDao.java` | ~210 | #11, #18 |
| `ReportQueryHelper.java` | ~200 | #11, #12 |
| `DateUtils.java` | ~200 | #6, #16 |
| `LegacyReportGenerator.java` | ~300 | #9 |
| `Abstract*EntityServiceBean.java` | 5ファイル | #10 |
| `RequisitionServiceBean.java` | ~600 | #2, #4, #14 |

## ライセンス

このプロジェクトはAIリファクタリング検証用のサンプルです。
