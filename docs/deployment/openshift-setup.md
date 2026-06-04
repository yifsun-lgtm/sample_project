# ProQuip OpenShift Deployment Guide

ProQuip を AWS 上の OpenShift (Open Environment) へデプロイするための手順書。
CI/CD は GitHub Actions でビルド・テスト・イメージプッシュ・デプロイを自動化する。

## 目次

- [前提条件](#前提条件)
- [Phase 1: AWS / OpenShift 基盤準備](#phase-1-aws--openshift-基盤準備)
- [Phase 2: GitHub Actions CI/CD パイプライン構築](#phase-2-github-actions-cicd-パイプライン構築)
- [Phase 3: コンテナイメージの OpenShift 対応](#phase-3-コンテナイメージの-openshift-対応)
- [Phase 4: OpenShift マニフェスト作成](#phase-4-openshift-マニフェスト作成)
- [Phase 5: デプロイ検証](#phase-5-デプロイ検証)
- [アーキテクチャ](#アーキテクチャ)
- [チュートリアル一覧](#チュートリアル一覧)
- [構築実績](#構築実績)

---

## 前提条件

| 項目 | 要件 |
|------|------|
| AWS アカウント | IAM ユーザー、アクセスキー発行済み |
| OpenShift クラスタ | 既存の OpenShift 4.x クラスタ (Open Environment 等) |
| OpenShift 認証情報 | API URL、認証トークン、CA 証明書 |
| GitHub リポジトリ | Admin 権限あり |
| ローカル環境 | macOS or Linux、Homebrew (macOS の場合) |

---

## Phase 1: AWS / OpenShift 基盤準備

| Task | 内容 | チュートリアル |
|------|------|-------------|
| 1.1 | AWS CLI / oc CLI インストール・設定 | [tutorials/1.1-cli-setup.md](tutorials/1.1-cli-setup.md) |
| 1.2 | 既存 OpenShift クラスタへ接続 (`oc login`) | [tutorials/1.2-openshift-login.md](tutorials/1.2-openshift-login.md) |
| 1.3 | OpenShift 内部レジストリの設定・公開 | [tutorials/1.3-registry-setup.md](tutorials/1.3-registry-setup.md) |
| 1.4 | Amazon RDS PostgreSQL の作成 | [tutorials/1.4-rds-postgresql.md](tutorials/1.4-rds-postgresql.md) |

### 必要な CLI ツール

```bash
brew install awscli openshift-cli
```

### OpenShift 接続

```bash
oc login \
  --server=https://api.your-cluster.example.com:6443 \
  --token=<your-token> \
  --certificate-authority=~/.kube/certs/your-cluster.ca.crt
```

### レジストリ公開

```bash
oc patch configs.imageregistry.operator.openshift.io/cluster \
  --type merge -p '{"spec":{"defaultRoute":true}}'
```

### RDS 作成

```bash
aws rds create-db-instance \
  --db-instance-identifier proquip-dev-db \
  --db-instance-class db.t3.medium \
  --engine postgres --engine-version 15.18 \
  --master-username proquip_admin \
  --master-user-password '<password>' \
  --allocated-storage 20 --storage-type gp3 \
  --vpc-security-group-ids <sg-id> \
  --db-subnet-group-name proquip-db-subnet \
  --no-publicly-accessible --storage-encrypted
```

---

## Phase 2: GitHub Actions CI/CD パイプライン構築

| Task | 内容 | チュートリアル |
|------|------|-------------|
| 2.1 | GitHub Secrets 設定 (OpenShift, RDS) | [tutorials/2.1-github-secrets.md](tutorials/2.1-github-secrets.md) |
| 2.2-2.3 | CI ワークフロー (Maven + Angular) | [tutorials/2.2-ci-workflow.md](tutorials/2.2-ci-workflow.md) |
| 2.4-2.5 | CD ワークフロー (イメージビルド + デプロイ) | [tutorials/2.4-cd-workflow.md](tutorials/2.4-cd-workflow.md) |
| 2.6 | Branch protection rule | [tutorials/2.6-branch-protection.md](tutorials/2.6-branch-protection.md) |

### GitHub Secrets (6件)

| Secret | 用途 |
|--------|------|
| `OPENSHIFT_SERVER` | OpenShift API URL |
| `OPENSHIFT_TOKEN` | 認証トークン |
| `OPENSHIFT_CA_CERT` | CA 証明書 (base64) |
| `REGISTRY_URL` | 内部レジストリの外部 URL |
| `RDS_ENDPOINT` | RDS 接続先 |
| `RDS_PASSWORD` | RDS パスワード |

### CI/CD ワークフロー

```
PR 作成 → ci.yml (Maven build + Angular build + lint + test)
main マージ → cd.yml (3 images build → push → OpenShift deploy)
```

---

## Phase 3: コンテナイメージの OpenShift 対応

| Task | 内容 | チュートリアル |
|------|------|-------------|
| 3.1-3.5 | Dockerfile 修正 + ビルドスクリプト | [tutorials/3.1-3.5-container-images.md](tutorials/3.1-3.5-container-images.md) |

### OpenShift 対応の要点

| イメージ | 変更内容 |
|---------|---------|
| WildFly | `chgrp -R 0` + `chmod -R g=u` + `USER 1001` |
| Keycloak | Multi-stage build, `start --optimized` (本番モード) |
| nginx | `listen 8080`, `USER 1001`, PID を `/tmp` に移動 |

### ビルド・プッシュ

```bash
bash scripts/build-push.sh <tag> <project>
```

---

## Phase 4: OpenShift マニフェスト作成

| Task | 内容 | チュートリアル |
|------|------|-------------|
| 4.1-5.4 | マニフェスト作成 + デプロイ + 検証 | [tutorials/4.1-5.4-openshift-deploy.md](tutorials/4.1-5.4-openshift-deploy.md) |

### マニフェスト一覧

```
k8s/
├── 00-namespace.yaml           # proquip-dev プロジェクト
├── 01-pull-secret.yaml         # (内部レジストリの場合は不要)
├── 02-configmap.yaml           # DB 接続先、Keycloak 設定
├── 03-secrets.yaml             # (oc CLI で作成、YAML にはテンプレートのみ)
├── 10-flyway-job.yaml          # DB マイグレーション Job
├── 20-wildfly-deployment.yaml  # WildFly Deployment + Service
├── 30-keycloak-deployment.yaml # Keycloak Deployment + Service
├── 38-keycloak-realm-import.yaml # レルムインポート Job
└── 40-nginx-deployment.yaml    # nginx Deployment + Service + Route
```

### デプロイ手順

```bash
# 1. ConfigMap / Secret
oc apply -f k8s/02-configmap.yaml
oc create secret generic proquip-secrets -n proquip-dev \
  --from-literal=DB_USER=proquip_admin \
  --from-literal=DB_PASSWORD='<password>' \
  --from-literal=KC_DB_USERNAME=proquip_admin \
  --from-literal=KC_DB_PASSWORD='<password>' \
  --from-literal=KEYCLOAK_ADMIN=admin \
  --from-literal=KEYCLOAK_ADMIN_PASSWORD='<password>'

# 2. Flyway マイグレーション
oc apply -f k8s/10-flyway-job.yaml
oc wait --for=condition=complete job/proquip-flyway-migrate -n proquip-dev --timeout=120s

# 3. アプリケーションデプロイ
oc apply -f k8s/30-keycloak-deployment.yaml
oc apply -f k8s/20-wildfly-deployment.yaml
oc apply -f k8s/40-nginx-deployment.yaml

# 4. 確認
oc get pods -n proquip-dev
oc get route proquip -n proquip-dev
```

---

## Phase 5: デプロイ検証

| Task | 内容 | チュートリアル |
|------|------|-------------|
| 5.5 | Keycloak redirect_uri 修正 | [tutorials/5.5-keycloak-redirect-uri.md](tutorials/5.5-keycloak-redirect-uri.md) |

### スモークテスト

```bash
ROUTE="https://$(oc get route proquip -n proquip-dev -o jsonpath='{.spec.host}')"

curl -sk "${ROUTE}/"                    # Frontend → 200
curl -sk "${ROUTE}/api/health"          # API → 200
curl -sk "${ROUTE}/health.json"         # nginx health → 200
curl -sk "${ROUTE}/realms/proquip/.well-known/openid-configuration"  # Keycloak → 200
```

### ユーザーログインテスト

```bash
curl -sk -X POST "${ROUTE}/realms/proquip/protocol/openid-connect/token" \
  -d "client_id=proquip-web" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123"
# → access_token が返れば成功
```

| ユーザー | パスワード | ロール |
|---------|----------|--------|
| admin | admin123 | ADMIN |
| manager | manager123 | MANAGER |
| buyer | buyer123 | BUYER |
| warehouse | warehouse123 | WAREHOUSE_STAFF |
| viewer | viewer123 | VIEWER |

---

## アーキテクチャ

```
┌─────────────────────────────────────────────────────┐
│                    AWS Account                       │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │       OpenShift Cluster (Open Environment)    │   │
│  │                                               │   │
│  │  ┌─────────┐  ┌──────────┐  ┌────────────┐  │   │
│  │  │ nginx   │  │ WildFly  │  │ Keycloak   │  │   │
│  │  │ (Route) │→ │ (API)    │  │ (認証)     │  │   │
│  │  │ :8080   │  │ :8080    │  │ :8080      │  │   │
│  │  └─────────┘  └────┬─────┘  └──────┬──────┘  │   │
│  │                     │               │         │   │
│  └─────────────────────┼───────────────┼─────────┘   │
│                        │               │             │
│  ┌─────────────────────▼───────────────▼─────────┐   │
│  │          Amazon RDS (PostgreSQL 15)            │   │
│  │     proquip DB    +    keycloak DB             │   │
│  └───────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
         ▲ oc set image / docker push
         │
┌────────┴────────────────────────────────────────────┐
│              GitHub Actions CI/CD                    │
│  PR → ci.yml (build + test)                          │
│  main → cd.yml (image build → push → deploy)         │
└─────────────────────────────────────────────────────┘
```

---

## チュートリアル一覧

全ステップのチュートリアルが `docs/deployment/tutorials/` にあります。
各チュートリアルには「従来との違い」セクションで、オンプレミスとの比較説明が含まれています。

| # | ファイル | 内容 | 対象者 |
|---|---------|------|--------|
| 1.1 | [1.1-cli-setup.md](tutorials/1.1-cli-setup.md) | CLI ツールのインストール | 全員 |
| 1.2 | [1.2-openshift-login.md](tutorials/1.2-openshift-login.md) | OpenShift クラスタ接続 | インフラ |
| 1.3 | [1.3-registry-setup.md](tutorials/1.3-registry-setup.md) | コンテナレジストリ設定 | インフラ |
| 1.4 | [1.4-rds-postgresql.md](tutorials/1.4-rds-postgresql.md) | RDS PostgreSQL 作成 | インフラ |
| 2.1 | [2.1-github-secrets.md](tutorials/2.1-github-secrets.md) | GitHub Secrets 設定 | DevOps |
| 2.2 | [2.2-ci-workflow.md](tutorials/2.2-ci-workflow.md) | CI ワークフロー作成 | 開発 |
| 2.4 | [2.4-cd-workflow.md](tutorials/2.4-cd-workflow.md) | CD ワークフロー作成 | DevOps |
| 2.6 | [2.6-branch-protection.md](tutorials/2.6-branch-protection.md) | Branch Protection 設定 | リーダー |
| 3.x | [3.1-3.5-container-images.md](tutorials/3.1-3.5-container-images.md) | Dockerfile OpenShift 対応 | 開発 |
| 4-5 | [4.1-5.4-openshift-deploy.md](tutorials/4.1-5.4-openshift-deploy.md) | マニフェスト作成・デプロイ | 全員 |
| 5.5 | [5.5-keycloak-redirect-uri.md](tutorials/5.5-keycloak-redirect-uri.md) | Keycloak redirect_uri 修正 | 開発 |

---

## 構築実績

| Phase | 内容 | 所要時間 |
|-------|------|---------|
| Phase 1 | AWS / OpenShift 基盤準備 | 24 分 44 秒 |
| Phase 2 | GitHub Actions CI/CD 構築 | 30 分 33 秒 |
| Phase 3 | コンテナイメージ対応 | 14 分 47 秒 |
| Phase 4 | OpenShift マニフェスト・デプロイ | 69 分 09 秒 |
| Phase 5 | デプロイ検証 | 4 分 02 秒 |
| **合計** | **ゼロから本番稼働まで** | **2 時間 23 分** |

詳細: [time-report.md](time-report.md)

---

## 関連ドキュメント

| ドキュメント | 内容 |
|------------|------|
| [time-report.md](time-report.md) | 全タスクの作業時間レポート |
| [cicd-vs-onpremise-comparison.md](cicd-vs-onpremise-comparison.md) | CI/CD vs オンプレミス比較 (3年間TCO) |
| [slides/cicd-solution-presentation.md](slides/cicd-solution-presentation.md) | クライアント向け提案スライド (30分) |
