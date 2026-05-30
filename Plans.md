# ProQuip AWS/ROSA Deployment Plans.md

作成日: 2026-05-30

---

## Overview

ProQuip (Jakarta EE 10 / WildFly 30 / Angular 17 / PostgreSQL 15) を
ROSA (Red Hat OpenShift Service on AWS) へデプロイする。
環境: Dev/Staging のみ。DB は RDS、認証は OpenShift 上 Keycloak (RDS バックエンド)。
CI/CD は GitHub Actions でビルド・テスト・イメージプッシュ・デプロイを自動化する。

### Architecture

```
┌─────────────────────────────────────────────────────┐
│                    AWS Account                       │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │              ROSA Cluster                     │   │
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
│                                                      │
│  ┌───────────────────────────────────────────────┐   │
│  │          Amazon ECR (Container Registry)       │   │
│  └───────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
         ▲
         │ push images / oc apply
         │
┌────────┴────────────────────────────────────────────┐
│              GitHub Actions CI/CD                    │
│  PR → build → test → image push → deploy            │
└─────────────────────────────────────────────────────┘
```

---

## Phase 1: AWS / ROSA 基盤準備

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | AWS CLI / ROSA CLI / oc CLI をローカルにインストール・設定 | `rosa version` / `oc version` / `aws sts get-caller-identity` が全て成功する | - | cc:TODO |
| 1.2 | AWS アカウントで ROSA サービスを有効化、サービスクォータ確認 | AWS ROSA コンソールで緑チェック表示、ELB service-linked role 存在確認 | 1.1 | cc:TODO |
| 1.3 | ROSA クラスタ作成 (STS モード、Single-AZ、m5.xlarge x2 worker) | `rosa describe cluster` で status=ready、`oc login` でクラスタ接続成功 | 1.2 | cc:TODO |
| 1.4 | Amazon ECR リポジトリ作成 (proquip-wildfly, proquip-nginx, proquip-keycloak) | `aws ecr describe-repositories` で 3 リポジトリ確認 | 1.1 | cc:TODO |
| 1.5 | Amazon RDS PostgreSQL 15.7 インスタンス作成 (db.t3.medium, dev 環境) | `psql` で RDS エンドポイントへ接続成功、proquip + keycloak DB 作成済み | 1.2 | cc:TODO |

---

## Phase 2: GitHub Actions CI/CD パイプライン構築

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 2.1 | GitHub Secrets 設定 (AWS credentials, ECR registry, ROSA kubeconfig) | Settings → Secrets に `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `ECR_REGISTRY`, `ROSA_KUBECONFIG` が登録済み | 1.3, 1.4 | cc:TODO |
| 2.2 | CI ワークフロー作成: PR 時にバックエンド (Maven) ビルド + テスト | `.github/workflows/ci.yml` で PR に対して `mvn clean verify` が実行され、結果が PR ステータスに反映 | - | cc:TODO |
| 2.3 | CI ワークフロー拡張: PR 時にフロントエンド (Angular) ビルド + lint + テスト | `.github/workflows/ci.yml` で `npm ci && npm run lint && npm test -- --no-watch --browsers=ChromeHeadless` が成功 | 2.2 | cc:TODO |
| 2.4 | CD ワークフロー作成: main マージ時に 3 コンテナイメージを ECR にビルド・プッシュ | `.github/workflows/cd.yml` で main push 時に 3 イメージが ECR にプッシュされる (commit SHA タグ + latest) | 2.2, 1.4 | cc:TODO |
| 2.5 | CD ワークフロー拡張: ECR プッシュ後に ROSA へ自動デプロイ (dev 環境) | `.github/workflows/cd.yml` で `oc set image` + `oc rollout status` が成功、Pod が新イメージで起動 | 2.4, 1.3 | cc:TODO |
| 2.6 | Branch protection rule 設定 (main ブランチ: CI 必須、レビュー必須) | main ブランチに direct push 不可、CI パス + 1 approve 必須 | 2.2, 2.3 | cc:TODO |

---

## Phase 3: コンテナイメージの OpenShift 対応

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 3.1 | WildFly Dockerfile を OpenShift 対応に修正 (非 root 実行、arbitrary UID 対応) | `podman build` 成功、非 root ユーザーで起動確認 | - | cc:TODO |
| 3.2 | WildFly 設定を外部化 (DB 接続先を環境変数/ConfigMap 参照に変更) | `PROQUIP_DB_URL`, `PROQUIP_DB_USER`, `PROQUIP_DB_PASSWORD` 環境変数で RDS 接続可能 | 3.1 | cc:TODO |
| 3.3 | Keycloak Dockerfile を本番モード対応 (`start` + `--optimized`、レルム import 分離) | Keycloak が `start` モードで起動、RDS バックエンドに接続、レルム import Job 成功 | - | cc:TODO |
| 3.4 | nginx Dockerfile を OpenShift 対応 (非 root、ポート 8080 に変更) | nginx が非 root / port 8080 で起動、Angular SPA 配信成功 | - | cc:TODO |
| 3.5 | 3 イメージを ECR にプッシュするビルドスクリプト作成 | `scripts/build-push-ecr.sh` で 3 イメージが ECR にプッシュされる | 1.4, 3.1, 3.3, 3.4 | cc:TODO |

---

## Phase 4: OpenShift マニフェスト作成

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 4.1 | Namespace / Project 作成 (`proquip-dev`) | `oc get project proquip-dev` 成功 | 1.3 | cc:TODO |
| 4.2 | ECR pull secret 作成、ServiceAccount にリンク | `oc get secret ecr-pull-secret` 存在、default SA にリンク済み | 4.1, 1.4 | cc:TODO |
| 4.3 | ConfigMap / Secret 作成 (DB 接続情報、Keycloak admin 資格情報) | `oc get configmap proquip-config` / `oc get secret proquip-secrets` 存在 | 4.1, 1.5 | cc:TODO |
| 4.4 | Flyway マイグレーション Job マニフェスト作成 | `oc create -f` で Job 実行、RDS 上の proquip DB にテーブル作成完了 | 4.2, 4.3 | cc:TODO |
| 4.5 | WildFly Deployment + Service マニフェスト作成 | Pod Running、`/api/health` が 200 返却、RDS 接続成功 | 4.2, 4.3, 4.4 | cc:TODO |
| 4.6 | Keycloak Deployment + Service マニフェスト作成 | Pod Running、Keycloak 管理画面アクセス可能、proquip レルム存在 | 4.2, 4.3 | cc:TODO |
| 4.7 | nginx Deployment + Service + Route マニフェスト作成 | Route 経由で Angular SPA 表示、`/api/*` が WildFly にプロキシされる | 4.5, 4.6 | cc:TODO |
| 4.8 | Keycloak レルム import Job マニフェスト作成 | proquip-realm.json がインポートされ、テストユーザー 5 名でログイン可能 | 4.6 | cc:TODO |

---

## Phase 5: デプロイ検証

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 5.1 | 全リソースを `oc apply` で一括デプロイ | 全 Pod が Running、全 Service にエンドポイント存在 | Phase 4 | cc:TODO |
| 5.2 | E2E スモークテスト (ログイン → ダッシュボード → 商品一覧 → 発注作成) | 5 テストユーザー全員でログイン成功、主要画面遷移正常 | 5.1 | cc:TODO |
| 5.3 | CI/CD E2E テスト: feature ブランチ PR → CI パス → main マージ → 自動デプロイ確認 | PR 作成から ROSA 反映まで自動で完了、Pod が新イメージで起動 | 5.1, Phase 2 | cc:TODO |
| 5.4 | デプロイ手順書更新 | `docs/deployment/rosa-setup.md` に CI/CD セクション追加、手順に従い初回デプロイ可能 | 5.2, 5.3 | cc:TODO |

---

## Completed

_None yet._

## Archive

_None yet._
