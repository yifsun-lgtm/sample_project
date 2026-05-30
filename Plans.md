# ProQuip AWS OpenShift Deployment Plans.md

作成日: 2026-05-30

---

## Overview

ProQuip (Jakarta EE 10 / WildFly 30 / Angular 17 / PostgreSQL 15) を
AWS 上の既存 OpenShift クラスタ (Open Environment) へデプロイする。
環境: Dev/Staging のみ。DB は RDS、認証は OpenShift 上 Keycloak (RDS バックエンド)。
CI/CD は GitHub Actions でビルド・テスト・イメージプッシュ・デプロイを自動化する。

### Architecture

```
┌─────────────────────────────────────────────────────┐
│                    AWS Account                       │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │       OpenShift Cluster (Open Environment)    │   │
│  │                                               │   │
│  │  ┌─────────┐  ┌──────────┐  ┌────────────┐  │   │
│  │  │ nginx   │  │ wildfly  │  │  keycloak   │  │   │
│  │  │ (Route) │→ │ (Deploy) │  │  (Deploy)   │  │   │
│  │  └─────────┘  └────┬─────┘  └──────┬──────┘  │   │
│  │                     │               │         │   │
│  └─────────────────────┼───────────────┼─────────┘   │
│                        │               │             │
│  ┌─────────────────────▼───────────────▼─────────┐   │
│  │          Amazon RDS (PostgreSQL 15.7)          │   │
│  │     proquip DB    +    keycloak DB             │   │
│  └───────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
         ▲
         │ oc apply / image push
         │
┌────────┴────────────────────────────────────────────┐
│              GitHub Actions CI/CD                    │
│  PR → build → test → image push → deploy            │
└─────────────────────────────────────────────────────┘
```

---

## Phase 1: AWS / OpenShift 基盤準備

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | AWS CLI / oc CLI をローカルにインストール・設定 | `oc version` / `aws sts get-caller-identity` が成功する | - | cc:完了 |
| 1.2 | 既存 OpenShift クラスタへ `oc login` 接続確認 | `oc login` 成功、`oc get nodes` でノード一覧表示 | 1.1 | cc:完了 |
| 1.3 | OpenShift 内部レジストリ or ECR の利用可否を確認・設定 | イメージ push/pull が動作する registry が確定、`oc get imagestream` or `aws ecr` で確認 | 1.2 | cc:完了 |
| 1.4 | Amazon RDS PostgreSQL 15.18 インスタンス作成 (db.t3.medium, dev 環境) | `psql` で RDS エンドポイントへ接続成功、proquip + keycloak DB 作成済み | 1.1 | cc:完了 |

---

## Phase 2: GitHub Actions CI/CD パイプライン構築

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 2.1 | GitHub Secrets 設定 (OpenShift token/CA, registry URL, RDS endpoint/password) | Settings → Secrets に 6 Secrets 登録済み | 1.2, 1.3 | cc:完了 |
| 2.2 | CI ワークフロー作成: PR 時にバックエンド (Maven) ビルド + テスト | `.github/workflows/ci.yml` で PR に対して `mvn clean package` + `mvn test` が実行され、PR ステータス緑 | - | cc:完了 |
| 2.3 | CI ワークフロー拡張: PR 時にフロントエンド (Angular) ビルド + lint + テスト | `.github/workflows/ci.yml` で `npm ci && npm run lint && npm test && npm run build` が成功 | 2.2 | cc:完了 |
| 2.4 | CD ワークフロー作成: main マージ時に 3 コンテナイメージをビルド・プッシュ | `.github/workflows/cd.yml` で main push 時に 3 イメージが registry にプッシュされる | 2.2, 1.3 | cc:完了 |
| 2.5 | CD ワークフロー拡張: イメージプッシュ後に OpenShift へ自動デプロイ (dev 環境) | `.github/workflows/cd.yml` で `oc set image` + `oc rollout status` が成功 | 2.4, 1.2 | cc:完了 |
| 2.6 | Branch protection rule 設定 (main ブランチ: CI 必須、レビュー必須) | main ブランチに direct push 不可、CI パス + 1 approve 必須 | 2.2, 2.3 | cc:完了 |

---

## Phase 3: コンテナイメージの OpenShift 対応

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 3.1 | WildFly Dockerfile を OpenShift 対応に修正 (非 root 実行、arbitrary UID 対応) | `podman build` 成功、非 root ユーザーで起動確認 | - | cc:完了 |
| 3.2 | WildFly 設定を外部化 (DB 接続先を環境変数/ConfigMap 参照に変更) | `DB_HOST`, `DB_USER`, `DB_PASSWORD` 等の環境変数で RDS 接続可能 (既存 CLI 設定で対応済み) | 3.1 | cc:完了 |
| 3.3 | Keycloak Dockerfile を本番モード対応 (`start` + `--optimized`、レルム import 分離) | Keycloak が `start` モードで起動、RDS バックエンドに接続、レルム import Job 成功 | - | cc:完了 |
| 3.4 | nginx Dockerfile を OpenShift 対応 (非 root、ポート 8080 に変更) | nginx が非 root / port 8080 で起動、Angular SPA 配信成功 | - | cc:完了 |
| 3.5 | 3 イメージを registry にプッシュするビルドスクリプト作成 | `scripts/build-push.sh` で 3 イメージが registry にプッシュされる | 1.3, 3.1, 3.3, 3.4 | cc:完了 |

---

## Phase 4: OpenShift マニフェスト作成

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 4.1 | Namespace / Project 作成 (`proquip-dev`) | `oc get project proquip-dev` 成功 | 1.2 | cc:完了 |
| 4.2 | Registry pull secret 作成 (内部レジストリの場合は不要) | イメージ pull が動作すること | 4.1, 1.3 | cc:完了 |
| 4.3 | ConfigMap / Secret 作成 (DB 接続情報、Keycloak admin 資格情報) | `oc get configmap proquip-config` / `oc get secret proquip-secrets` 存在 | 4.1, 1.4 | cc:完了 |
| 4.4 | Flyway マイグレーション Job マニフェスト作成 | `oc create -f` で Job 実行、RDS 上の proquip DB にテーブル作成完了 | 4.2, 4.3 | cc:完了 |
| 4.5 | WildFly Deployment + Service マニフェスト作成 | Pod Running、`/api/health` が 200 返却、RDS 接続成功 | 4.2, 4.3, 4.4 | cc:完了 |
| 4.6 | Keycloak Deployment + Service マニフェスト作成 | Pod Running、Keycloak 管理画面アクセス可能、proquip レルム存在 | 4.2, 4.3 | cc:完了 |
| 4.7 | nginx Deployment + Service + Route マニフェスト作成 | Route 経由で Angular SPA 表示、`/api/*` が WildFly にプロキシされる | 4.5, 4.6 | cc:完了 |
| 4.8 | Keycloak レルム import Job マニフェスト作成 | proquip-realm.json がインポートされ、テストユーザー 5 名でログイン可能 | 4.6 | cc:完了 |

---

## Phase 5: デプロイ検証

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 5.1 | 全リソースを `oc apply` で一括デプロイ | 全 Pod が Running、全 Service にエンドポイント存在 | Phase 4 | cc:完了 |
| 5.2 | E2E スモークテスト (ログイン → ダッシュボード → 商品一覧 → 発注作成) | 5 テストユーザー全員でログイン成功、主要画面遷移正常 | 5.1 | cc:完了 |
| 5.3 | CI/CD E2E テスト: feature ブランチ PR → CI パス → main マージ → 自動デプロイ確認 | PR 作成から OpenShift 反映まで自動で完了、Pod が新イメージで起動 | 5.1, Phase 2 | cc:完了 |
| 5.4 | デプロイ手順書更新 | `docs/deployment/rosa-setup.md` を OpenShift Open Environment 向けに更新 | 5.2, 5.3 | cc:完了 |

---

## Completed

| Task | 内容 | Completed |
|------|------|-----------|
| 1.1 | AWS CLI / oc CLI インストール | 2026-05-30 (1m 23s) |
| 1.2 | OpenShift クラスタ接続 | 2026-05-30 (1m 17s) |
| 1.3 | OpenShift 内部レジストリ設定 | 2026-05-30 (8m 17s) |
| 1.4 | RDS PostgreSQL 15.18 作成 | 2026-05-30 (13m 47s) |
| 2.1 | GitHub Secrets 設定 | 2026-05-30 (9m 21s) |
| 2.2 | CI: バックエンド (Maven) | 2026-05-30 (5m 10s) |
| 2.3 | CI: フロントエンド (Angular) | 2026-05-30 (included in 2.2) |

## Archive

_None yet._
