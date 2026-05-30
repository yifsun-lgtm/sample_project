# ProQuip ROSA Deployment Guide

ProQuip を ROSA (Red Hat OpenShift Service on AWS) へデプロイするための手順書。
CI/CD は GitHub Actions でビルド・テスト・イメージプッシュ・デプロイを自動化する。

## 目次

- [前提条件](#前提条件)
- [Phase 1: AWS / ROSA 基盤準備](#phase-1-aws--rosa-基盤準備)
  - [1.1 CLI ツールインストール・設定](#11-cli-ツールインストール設定)
  - [1.2 ROSA サービス有効化 / クォータ確認](#12-rosa-サービス有効化--クォータ確認)
  - [1.3 ROSA クラスタ作成](#13-rosa-クラスタ作成)
  - [1.4 ECR リポジトリ作成](#14-ecr-リポジトリ作成)
  - [1.5 RDS PostgreSQL 作成](#15-rds-postgresql-作成)
- [Phase 2: GitHub Actions CI/CD パイプライン構築](#phase-2-github-actions-cicd-パイプライン構築)
  - [2.1 GitHub Secrets 設定](#21-github-secrets-設定)
  - [2.2 CI ワークフロー: バックエンド](#22-ci-ワークフロー-バックエンド)
  - [2.3 CI ワークフロー: フロントエンド](#23-ci-ワークフロー-フロントエンド)
  - [2.4 CD ワークフロー: ECR イメージビルド・プッシュ](#24-cd-ワークフロー-ecr-イメージビルドプッシュ)
  - [2.5 CD ワークフロー: ROSA 自動デプロイ](#25-cd-ワークフロー-rosa-自動デプロイ)
  - [2.6 Branch protection rule](#26-branch-protection-rule)
- [Phase 3: コンテナイメージの OpenShift 対応](#phase-3-コンテナイメージの-openshift-対応)
  - [3.1 WildFly Dockerfile 修正](#31-wildfly-dockerfile-修正)
  - [3.2 WildFly 設定外部化](#32-wildfly-設定外部化)
  - [3.3 Keycloak 本番モード対応](#33-keycloak-本番モード対応)
  - [3.4 nginx OpenShift 対応](#34-nginx-openshift-対応)
  - [3.5 ECR プッシュスクリプト](#35-ecr-プッシュスクリプト)
- [Phase 4: OpenShift マニフェスト作成](#phase-4-openshift-マニフェスト作成)
  - [4.1 Namespace / Project](#41-namespace--project)
  - [4.2 ECR pull secret](#42-ecr-pull-secret)
  - [4.3 ConfigMap / Secret](#43-configmap--secret)
  - [4.4 Flyway マイグレーション Job](#44-flyway-マイグレーション-job)
  - [4.5 WildFly Deployment](#45-wildfly-deployment)
  - [4.6 Keycloak Deployment](#46-keycloak-deployment)
  - [4.7 nginx Deployment + Route](#47-nginx-deployment--route)
  - [4.8 Keycloak レルム import Job](#48-keycloak-レルム-import-job)
- [Phase 5: デプロイ検証](#phase-5-デプロイ検証)
- [アーキテクチャ](#アーキテクチャ)
- [コスト概算](#コスト概算)

---

## 前提条件

| 項目 | 要件 |
|------|------|
| AWS アカウント | IAM ユーザー or SSO、AdministratorAccess または ROSA 用カスタムポリシー |
| Red Hat アカウント | 無料作成可。ROSA 利用規約への同意が必要 |
| AWS サポートプラン | Business 以上推奨 (Red Hat サポート連携のため) |
| ローカル環境 | macOS or Linux、インターネット接続、sudo 権限 |

---

## Phase 1: AWS / ROSA 基盤準備

### 1.1 CLI ツールインストール・設定

3 つの CLI を準備する: AWS CLI, ROSA CLI, OpenShift CLI (oc)。

#### AWS CLI v2

```bash
brew install awscli

aws configure
# → AWS Access Key ID: <your-key>
# → AWS Secret Access Key: <your-secret>
# → Default region: ap-northeast-1
# → Default output format: json
```

確認:

```bash
aws sts get-caller-identity
```

出力例:

```json
{
    "UserId": "AIDAXXXXXXXXXXXXXXXXX",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/your-user"
}
```

#### ROSA CLI

```bash
brew install rosa-cli
```

または Red Hat Console からダウンロード:
https://console.redhat.com/openshift/downloads#tool-rosa

ログイン:

```bash
rosa login --token=<トークン>
```

トークン取得先: https://console.redhat.com/openshift/token/rosa

確認:

```bash
rosa whoami
```

#### OpenShift CLI (oc)

```bash
rosa download oc
tar -xf openshift-client-*.tar.gz
sudo mv oc /usr/local/bin/
```

確認:

```bash
oc version
```

#### DoD チェック

以下 3 コマンドが全て成功すること:

```bash
aws sts get-caller-identity && rosa whoami && oc version
```

---

### 1.2 ROSA サービス有効化 / クォータ確認

#### ROSA 有効化

AWS Console → ROSA ページ → "Enable ROSA" ボタンをクリック。

またはCLIで検証:

```bash
rosa verify permissions
rosa verify quota
```

#### 確認項目 (AWS Console → ROSA ページ)

以下の 3 項目が全て緑チェックであること:

- AWS Marketplace permissions
- Service quotas meet requirements
- AWSServiceRoleForElasticLoadBalancing exists

#### ELB Service-Linked Role

```bash
aws iam get-role --role-name AWSServiceRoleForElasticLoadBalancing
```

存在しない場合:

```bash
aws iam create-service-linked-role \
  --aws-service-name elasticloadbalancing.amazonaws.com
```

#### 必要なサービスクォータ

| サービス | クォータ | 最小値 |
|---------|---------|-------|
| EC2 | Running On-Demand Standard instances | 8 vCPU |
| EC2 | EC2-VPC Elastic IPs | 1 |
| VPC | VPCs per Region | 1 空き |
| ELB | Network Load Balancers per Region | 1 |

クォータが不足している場合、AWS Console → Service Quotas から引き上げ申請する。
承認まで数時間〜1営業日かかる場合がある。

#### DoD チェック

```bash
rosa verify permissions  # PASS
rosa verify quota        # PASS
```

---

### 1.3 ROSA クラスタ作成

所要時間: 約 30-45 分。

#### アカウントロール作成

```bash
rosa create account-roles --mode auto --yes
```

#### クラスタ作成

```bash
rosa create cluster \
  --cluster-name proquip-dev \
  --sts \
  --mode auto \
  --region ap-northeast-1 \
  --compute-machine-type m5.xlarge \
  --replicas 2 \
  --machine-cidr 10.0.0.0/16 \
  --service-cidr 172.30.0.0/16 \
  --pod-cidr 10.128.0.0/14 \
  --yes
```

#### 構成選定理由

| パラメータ | 値 | 理由 |
|-----------|-----|------|
| compute-machine-type | m5.xlarge (4vCPU/16GB) | WildFly + Keycloak + nginx を余裕持って稼働できる最小構成 |
| replicas | 2 | Dev 環境の最小冗長構成。1 台だとノード障害時に全停止 |
| Single-AZ | (デフォルト) | Dev 環境のコスト削減。Production では Multi-AZ を推奨 |
| STS mode | yes | AWS 推奨のセキュアな認証方式。短期トークンベース |

#### 進捗確認

```bash
rosa describe cluster --cluster proquip-dev
rosa logs install --cluster proquip-dev --watch
```

#### Operator Roles / OIDC Provider 作成

クラスタ作成後に実行:

```bash
rosa create operator-roles --cluster proquip-dev --mode auto --yes
rosa create oidc-provider --cluster proquip-dev --mode auto --yes
```

#### 管理者ユーザー作成・ログイン

```bash
rosa create admin --cluster proquip-dev
# → oc login コマンドと一時パスワードが出力される
```

出力されたコマンドでログイン:

```bash
oc login https://api.proquip-dev.xxxx.p1.openshiftapps.com:6443 \
  --username cluster-admin \
  --password <出力されたパスワード>
```

#### DoD チェック

```bash
rosa describe cluster --cluster proquip-dev | grep -i status
# → Status: ready

oc get nodes
# → 2 ノード表示 (Ready 状態)
```

---

### 1.4 ECR リポジトリ作成

3 つのコンテナイメージ用プライベートレジストリを作成する。

#### 変数設定

```bash
REGION=ap-northeast-1
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
```

#### リポジトリ作成

```bash
for repo in proquip-wildfly proquip-nginx proquip-keycloak; do
  aws ecr create-repository \
    --repository-name "${repo}" \
    --region "${REGION}" \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256
done
```

#### ライフサイクルポリシー設定

古いイメージを自動削除し、最新 10 件を保持:

```bash
POLICY='{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Keep last 10 images",
      "selection": {
        "tagStatus": "any",
        "countType": "imageCountMoreThan",
        "countNumber": 10
      },
      "action": {
        "type": "expire"
      }
    }
  ]
}'

for repo in proquip-wildfly proquip-nginx proquip-keycloak; do
  aws ecr put-lifecycle-policy \
    --repository-name "${repo}" \
    --lifecycle-policy-text "${POLICY}"
done
```

#### ECR ログイン確認

```bash
aws ecr get-login-password --region "${REGION}" | \
  podman login --username AWS --password-stdin \
  "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
```

#### DoD チェック

```bash
aws ecr describe-repositories \
  --query 'repositories[?starts_with(repositoryName,`proquip-`)].repositoryName' \
  --output table
# → proquip-wildfly, proquip-nginx, proquip-keycloak の 3 行
```

---

### 1.5 RDS PostgreSQL 作成

proquip DB と keycloak DB を 1 つの RDS インスタンスでホストする。

#### VPC / サブネット情報取得

ROSA クラスタの VPC を使用する:

```bash
# ROSA クラスタの VPC ID を確認
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=*proquip-dev*" \
  --query 'Vpcs[0].VpcId' --output text)

echo "VPC: ${VPC_ID}"

# Private サブネット取得
SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
             "Name=tag:Name,Values=*private*" \
  --query 'Subnets[*].SubnetId' --output text | tr '\t' ' ')

echo "Subnets: ${SUBNET_IDS}"
```

#### セキュリティグループ作成

ROSA ワーカーノードからの PostgreSQL 接続を許可:

```bash
SG_ID=$(aws ec2 create-security-group \
  --group-name proquip-rds-sg \
  --description "ProQuip RDS access from ROSA" \
  --vpc-id "${VPC_ID}" \
  --query 'GroupId' --output text)

aws ec2 authorize-security-group-ingress \
  --group-id "${SG_ID}" \
  --protocol tcp \
  --port 5432 \
  --cidr 10.0.0.0/16

echo "Security Group: ${SG_ID}"
```

#### DB サブネットグループ作成

```bash
aws rds create-db-subnet-group \
  --db-subnet-group-name proquip-db-subnet \
  --db-subnet-group-description "ProQuip RDS subnets" \
  --subnet-ids ${SUBNET_IDS}
```

#### RDS インスタンス作成

```bash
aws rds create-db-instance \
  --db-instance-identifier proquip-dev-db \
  --db-instance-class db.t3.medium \
  --engine postgres \
  --engine-version 15.7 \
  --master-username proquip_admin \
  --master-user-password '<強力なパスワード>' \
  --allocated-storage 20 \
  --storage-type gp3 \
  --vpc-security-group-ids "${SG_ID}" \
  --db-subnet-group-name proquip-db-subnet \
  --no-publicly-accessible \
  --backup-retention-period 7 \
  --storage-encrypted \
  --deletion-protection
```

**重要**: `--master-user-password` は後で OpenShift Secret に格納する。安全な場所に記録すること。

#### 構成選定理由

| パラメータ | 値 | 理由 |
|-----------|-----|------|
| db.t3.medium | 2vCPU / 4GB | Dev 環境に十分。Production では r6g 系を推奨 |
| gp3 / 20GB | 汎用 SSD | Dev 環境の最小構成。追加 IOPS 不要 |
| Multi-AZ | No | Dev 環境のコスト削減。Production では有効化を推奨 |
| encryption | Yes | ストレージ暗号化をデフォルトで有効 |
| backup | 7 日 | 最低限の自動バックアップ |
| publicly-accessible | No | ROSA VPC 内からのみアクセス可 |

#### 待機・エンドポイント取得

```bash
# インスタンス起動待ち (約 10-15 分)
aws rds wait db-instance-available \
  --db-instance-identifier proquip-dev-db

# エンドポイント取得
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier proquip-dev-db \
  --query 'DBInstances[0].Endpoint.Address' --output text)

echo "RDS Endpoint: ${RDS_ENDPOINT}"
```

#### データベース作成

ROSA クラスタ内の一時 Pod から、または踏み台経由で実行:

```bash
# ROSA 内から一時 Pod で接続する場合
oc run psql-client --rm -it --restart=Never \
  --image=postgres:15.7-alpine \
  -- psql -h "${RDS_ENDPOINT}" -U proquip_admin -d postgres
```

SQL:

```sql
CREATE DATABASE proquip;
CREATE DATABASE keycloak;
GRANT ALL PRIVILEGES ON DATABASE proquip TO proquip_admin;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO proquip_admin;
```

#### DoD チェック

```bash
# RDS 接続確認 (ROSA Pod 内から)
oc run psql-check --rm -it --restart=Never \
  --image=postgres:15.7-alpine \
  -- psql -h "${RDS_ENDPOINT}" -U proquip_admin -d proquip -c '\l'
# → proquip, keycloak が表示される
```

---

## Phase 2: GitHub Actions CI/CD パイプライン構築

### 2.1 GitHub Secrets 設定

GitHub リポジトリの Settings → Secrets and variables → Actions に以下を登録する。

| Secret 名 | 値 | 用途 |
|-----------|-----|------|
| `AWS_ACCESS_KEY_ID` | IAM ユーザーの Access Key ID | ECR push / ROSA デプロイ用 |
| `AWS_SECRET_ACCESS_KEY` | IAM ユーザーの Secret Access Key | 同上 |
| `AWS_REGION` | `ap-northeast-1` | リージョン |
| `ECR_REGISTRY` | `<ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com` | ECR レジストリ URL |
| `ROSA_KUBECONFIG` | `oc config view --raw` の出力 (base64) | ROSA クラスタ接続用 |

IAM ポリシー (最小権限):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    }
  ]
}
```

ROSA kubeconfig の取得:

```bash
oc login https://api.proquip-dev.xxxx.p1.openshiftapps.com:6443 \
  --username cluster-admin --password <password>

oc config view --raw | base64 | pbcopy
# → GitHub Secrets に ROSA_KUBECONFIG として貼り付け
```

#### DoD チェック

GitHub Settings → Secrets に 5 つの Secret が登録済み。

---

### 2.2 CI ワークフロー: バックエンド

PR 作成・更新時に Maven ビルドとテストを実行する。

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
    branches: [main]

jobs:
  backend:
    name: Backend Build & Test
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: proquip/proquip-parent
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Build & Test
        run: mvn clean verify -B

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-results
          path: '**/target/surefire-reports/*.xml'

  frontend:
    name: Frontend Build & Test
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: proquip/proquip-frontend
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: npm
          cache-dependency-path: proquip/proquip-frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Lint
        run: npm run lint

      - name: Test
        run: npm test -- --no-watch --browsers=ChromeHeadless

      - name: Build
        run: npm run build
```

#### DoD チェック

PR を作成し、Actions タブで `CI` ワークフローが起動、backend + frontend の両ジョブが緑になること。

---

### 2.3 CI ワークフロー: フロントエンド

Task 2.2 の `ci.yml` に `frontend` ジョブとして統合済み (上記参照)。

追加の lint/test 設定が必要な場合:

```bash
# Angular テスト設定 (karma.conf.js)
# CI 用の ChromeHeadless ブラウザ設定を確認
cd proquip/proquip-frontend
npx ng test --no-watch --browsers=ChromeHeadless --code-coverage
```

#### DoD チェック

PR で `npm run lint` と `npm test -- --no-watch --browsers=ChromeHeadless` が CI 上で成功。

---

### 2.4 CD ワークフロー: ECR イメージビルド・プッシュ

main ブランチへのマージ時に 3 つのコンテナイメージをビルドして ECR にプッシュする。

```yaml
# .github/workflows/cd.yml
name: CD

on:
  push:
    branches: [main]

env:
  AWS_REGION: ${{ secrets.AWS_REGION }}

jobs:
  build-and-push:
    name: Build & Push Images
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to ECR
        id: ecr-login
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set image tag
        id: tag
        run: |
          SHORT_SHA=$(echo "${{ github.sha }}" | cut -c1-7)
          echo "tag=${SHORT_SHA}" >> "$GITHUB_OUTPUT"

      - name: Build & Push WildFly
        working-directory: proquip
        run: |
          docker build -f docker/wildfly/Dockerfile \
            -t ${{ secrets.ECR_REGISTRY }}/proquip-wildfly:${{ steps.tag.outputs.tag }} \
            -t ${{ secrets.ECR_REGISTRY }}/proquip-wildfly:latest .
          docker push ${{ secrets.ECR_REGISTRY }}/proquip-wildfly --all-tags

      - name: Build & Push nginx
        working-directory: proquip
        run: |
          docker build -f docker/nginx/Dockerfile \
            -t ${{ secrets.ECR_REGISTRY }}/proquip-nginx:${{ steps.tag.outputs.tag }} \
            -t ${{ secrets.ECR_REGISTRY }}/proquip-nginx:latest .
          docker push ${{ secrets.ECR_REGISTRY }}/proquip-nginx --all-tags

      - name: Build & Push Keycloak
        working-directory: proquip
        run: |
          docker build -f docker/keycloak/Dockerfile \
            -t ${{ secrets.ECR_REGISTRY }}/proquip-keycloak:${{ steps.tag.outputs.tag }} \
            -t ${{ secrets.ECR_REGISTRY }}/proquip-keycloak:latest .
          docker push ${{ secrets.ECR_REGISTRY }}/proquip-keycloak --all-tags

    outputs:
      image-tag: ${{ steps.tag.outputs.tag }}

  deploy:
    name: Deploy to ROSA
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Install oc CLI
        uses: redhat-actions/openshift-tools-installer@v1
        with:
          oc: '4'

      - name: Configure kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.ROSA_KUBECONFIG }}" | base64 -d > ~/.kube/config

      - name: Update images
        run: |
          REGISTRY="${{ secrets.ECR_REGISTRY }}"
          TAG="${{ needs.build-and-push.outputs.image-tag }}"
          NS="proquip-dev"

          oc set image deployment/proquip-wildfly \
            wildfly="${REGISTRY}/proquip-wildfly:${TAG}" -n "${NS}"
          oc set image deployment/proquip-nginx \
            nginx="${REGISTRY}/proquip-nginx:${TAG}" -n "${NS}"
          oc set image deployment/proquip-keycloak \
            keycloak="${REGISTRY}/proquip-keycloak:${TAG}" -n "${NS}"

      - name: Wait for rollout
        run: |
          NS="proquip-dev"
          oc rollout status deployment/proquip-wildfly -n "${NS}" --timeout=180s
          oc rollout status deployment/proquip-nginx -n "${NS}" --timeout=60s
          oc rollout status deployment/proquip-keycloak -n "${NS}" --timeout=180s

      - name: Smoke test
        run: |
          ROUTE=$(oc get route proquip -n proquip-dev -o jsonpath='{.spec.host}')
          STATUS=$(curl -s -o /dev/null -w '%{http_code}' "https://${ROUTE}/api/health" || true)
          if [ "${STATUS}" != "200" ]; then
            echo "Health check failed: HTTP ${STATUS}"
            exit 1
          fi
          echo "Health check passed: HTTP ${STATUS}"
```

#### DoD チェック

main への push 後、Actions で `CD` ワークフローが起動し:
1. 3 イメージが ECR にプッシュされる (commit SHA タグ + latest)
2. ROSA 上の Deployment が新イメージに更新される
3. スモークテストが HTTP 200 を返す

---

### 2.5 CD ワークフロー: ROSA 自動デプロイ

Task 2.4 の `cd.yml` に `deploy` ジョブとして統合済み (上記参照)。

デプロイ戦略:

| 項目 | 設定 |
|------|------|
| トリガー | main ブランチへの push |
| イメージタグ | commit SHA 短縮 (7 文字) + `latest` |
| デプロイ方法 | `oc set image` + `oc rollout status` |
| ヘルスチェック | `/api/health` に HTTP GET、200 で成功 |
| ロールバック | `oc rollout undo deployment/<name> -n proquip-dev` |

手動ロールバック手順:

```bash
# 直前のリビジョンに戻す
oc rollout undo deployment/proquip-wildfly -n proquip-dev
oc rollout undo deployment/proquip-nginx -n proquip-dev
oc rollout undo deployment/proquip-keycloak -n proquip-dev

# 特定リビジョンに戻す
oc rollout undo deployment/proquip-wildfly --to-revision=<N> -n proquip-dev
```

---

### 2.6 Branch protection rule

GitHub リポジトリの Settings → Branches → Add rule:

| 設定 | 値 |
|------|-----|
| Branch name pattern | `main` |
| Require a pull request before merging | Yes |
| Required approvals | 1 |
| Require status checks to pass | Yes |
| Required status checks | `Backend Build & Test`, `Frontend Build & Test` |
| Require branches to be up to date | Yes |
| Include administrators | Yes (推奨) |

```bash
# gh CLI で設定する場合
gh api repos/{owner}/{repo}/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["Backend Build & Test","Frontend Build & Test"]}' \
  --field required_pull_request_reviews='{"required_approving_review_count":1}' \
  --field enforce_admins=true \
  --field restrictions=null
```

#### DoD チェック

- main ブランチへの direct push が拒否される
- CI ステータスが赤の PR はマージ不可
- 1 approve がないとマージ不可

---

## Phase 3: コンテナイメージの OpenShift 対応

### 3.1 WildFly Dockerfile 修正

OpenShift はデフォルトで arbitrary UID (非 root) でコンテナを実行する。
WildFly Dockerfile を以下の方針で修正:

- `USER root` セクションの最小化
- ファイルパーミッションをグループ書き込み可 (`chmod g+rwx`) に変更
- 最終的に非 root ユーザーで実行

修正箇所 (`docker/wildfly/Dockerfile`):

```dockerfile
# Stage 2 末尾に追加
USER root
RUN chgrp -R 0 /opt/jboss/wildfly && \
    chmod -R g=u /opt/jboss/wildfly
USER 1001
```

確認:

```bash
cd proquip
podman build -f docker/wildfly/Dockerfile -t proquip-wildfly:test .
podman run --user 1001:0 proquip-wildfly:test
```

### 3.2 WildFly 設定外部化

ハードコードされた DB 接続情報を環境変数参照に変更する。

対象: `docker/wildfly/configure-wildfly.cli`

環境変数:

| 変数名 | 用途 | 例 |
|--------|------|-----|
| `PROQUIP_DB_HOST` | RDS エンドポイント | `proquip-dev-db.xxxxx.ap-northeast-1.rds.amazonaws.com` |
| `PROQUIP_DB_PORT` | ポート | `5432` |
| `PROQUIP_DB_NAME` | データベース名 | `proquip` |
| `PROQUIP_DB_USER` | ユーザー名 | `proquip_admin` |
| `PROQUIP_DB_PASSWORD` | パスワード | (Secret から注入) |

WildFly CLI 内でシステムプロパティとして参照:

```
/subsystem=datasources/data-source=ProQuipDS:write-attribute(name=connection-url, \
  value=jdbc:postgresql://${env.PROQUIP_DB_HOST:localhost}:${env.PROQUIP_DB_PORT:5432}/${env.PROQUIP_DB_NAME:proquip})
```

### 3.3 Keycloak 本番モード対応

現在の `start-dev` から本番モード (`start`) に変更:

```dockerfile
FROM quay.io/keycloak/keycloak:22.0.5 AS builder
ENV KC_DB=postgres
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:22.0.5
COPY --from=builder /opt/keycloak/ /opt/keycloak/
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
```

レルム import は Deployment の init container または別 Job で実行する (Task 4.8)。

環境変数:

| 変数名 | 用途 |
|--------|------|
| `KC_DB_URL` | `jdbc:postgresql://<RDS_ENDPOINT>:5432/keycloak` |
| `KC_DB_USERNAME` | DB ユーザー名 |
| `KC_DB_PASSWORD` | DB パスワード (Secret) |
| `KC_HOSTNAME` | Keycloak の外部 URL |
| `KC_PROXY` | `edge` (Route で TLS 終端) |

### 3.4 nginx OpenShift 対応

OpenShift では非 root かつ 1024 以上のポートが必要:

```dockerfile
# nginx.conf で listen ポートを変更
# listen 80; → listen 8080;

# Dockerfile に追加
RUN chgrp -R 0 /var/cache/nginx /var/run /var/log/nginx && \
    chmod -R g=u /var/cache/nginx /var/run /var/log/nginx && \
    sed -i 's/listen\s*80;/listen 8080;/' /etc/nginx/conf.d/proquip.conf

EXPOSE 8080
USER 1001
```

### 3.5 ECR プッシュスクリプト

`scripts/build-push-ecr.sh` を作成:

```bash
#!/usr/bin/env bash
set -euo pipefail

REGION="${AWS_REGION:-ap-northeast-1}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
TAG="${1:-latest}"

# ECR ログイン
aws ecr get-login-password --region "${REGION}" | \
  podman login --username AWS --password-stdin "${REGISTRY}"

cd "$(dirname "$0")/../proquip"

# ビルド & プッシュ
for svc in wildfly nginx keycloak; do
  IMAGE="${REGISTRY}/proquip-${svc}:${TAG}"
  echo "Building ${IMAGE}..."
  podman build -f "docker/${svc}/Dockerfile" -t "${IMAGE}" .
  podman push "${IMAGE}"
  echo "Pushed ${IMAGE}"
done

echo "All images pushed with tag: ${TAG}"
```

---

## Phase 4: OpenShift マニフェスト作成

マニフェストファイルの配置先: `k8s/` ディレクトリ

```
k8s/
├── 00-namespace.yaml
├── 01-ecr-pull-secret.yaml
├── 02-configmap.yaml
├── 03-secrets.yaml
├── 10-flyway-job.yaml
├── 20-wildfly-deployment.yaml
├── 21-wildfly-service.yaml
├── 30-keycloak-deployment.yaml
├── 31-keycloak-service.yaml
├── 38-keycloak-realm-import-job.yaml
├── 40-nginx-deployment.yaml
├── 41-nginx-service.yaml
└── 42-nginx-route.yaml
```

### 4.1 Namespace / Project

```yaml
# k8s/00-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: proquip-dev
  labels:
    app.kubernetes.io/part-of: proquip
```

```bash
oc apply -f k8s/00-namespace.yaml
oc project proquip-dev
```

### 4.2 ECR pull secret

ROSA から ECR のプライベートイメージを pull するための認証情報:

```bash
REGION=ap-northeast-1
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

oc create secret docker-registry ecr-pull-secret \
  --namespace proquip-dev \
  --docker-server="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com" \
  --docker-username=AWS \
  --docker-password="$(aws ecr get-login-password --region ${REGION})"

oc secrets link default ecr-pull-secret --for=pull -n proquip-dev
oc secrets link builder ecr-pull-secret -n proquip-dev
```

**注意**: ECR トークンは 12 時間で失効する。本番運用では CronJob でトークン更新を自動化するか、
ROSA の IAM Roles for Service Accounts (IRSA) を設定して自動認証にする。

### 4.3 ConfigMap / Secret

```yaml
# k8s/02-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: proquip-config
  namespace: proquip-dev
data:
  PROQUIP_DB_HOST: "proquip-dev-db.xxxxx.ap-northeast-1.rds.amazonaws.com"
  PROQUIP_DB_PORT: "5432"
  PROQUIP_DB_NAME: "proquip"
  KC_DB: "postgres"
  KC_DB_URL: "jdbc:postgresql://proquip-dev-db.xxxxx.ap-northeast-1.rds.amazonaws.com:5432/keycloak"
  KC_HOSTNAME_STRICT: "false"
  KC_PROXY: "edge"
  KC_HTTP_ENABLED: "true"
```

```yaml
# k8s/03-secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: proquip-secrets
  namespace: proquip-dev
type: Opaque
stringData:
  PROQUIP_DB_USER: "proquip_admin"
  PROQUIP_DB_PASSWORD: "<RDS パスワード>"
  KC_DB_USERNAME: "proquip_admin"
  KC_DB_PASSWORD: "<RDS パスワード>"
  KEYCLOAK_ADMIN: "admin"
  KEYCLOAK_ADMIN_PASSWORD: "<Keycloak 管理者パスワード>"
```

**重要**: `03-secrets.yaml` は git にコミットしないこと。`oc create secret` コマンドで直接作成するか、Sealed Secrets / External Secrets Operator を使用する。

### 4.4 Flyway マイグレーション Job

```yaml
# k8s/10-flyway-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: proquip-flyway-migrate
  namespace: proquip-dev
spec:
  backoffLimit: 3
  template:
    spec:
      restartPolicy: Never
      imagePullSecrets:
        - name: ecr-pull-secret
      containers:
        - name: flyway
          image: flyway/flyway:10.15.0
          args: ["migrate"]
          env:
            - name: FLYWAY_URL
              value: "jdbc:postgresql://$(PROQUIP_DB_HOST):$(PROQUIP_DB_PORT)/$(PROQUIP_DB_NAME)"
            - name: FLYWAY_USER
              valueFrom:
                secretKeyRef:
                  name: proquip-secrets
                  key: PROQUIP_DB_USER
            - name: FLYWAY_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: proquip-secrets
                  key: PROQUIP_DB_PASSWORD
          envFrom:
            - configMapRef:
                name: proquip-config
          volumeMounts:
            - name: migration-sql
              mountPath: /flyway/sql
              readOnly: true
      volumes:
        - name: migration-sql
          configMap:
            name: proquip-flyway-sql
```

マイグレーション SQL は ConfigMap として投入するか、WildFly イメージにバンドルする。
ファイル数が多い場合は専用の Flyway イメージをビルドする方が良い。

### 4.5 WildFly Deployment

```yaml
# k8s/20-wildfly-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: proquip-wildfly
  namespace: proquip-dev
  labels:
    app: proquip-wildfly
spec:
  replicas: 1
  selector:
    matchLabels:
      app: proquip-wildfly
  template:
    metadata:
      labels:
        app: proquip-wildfly
    spec:
      imagePullSecrets:
        - name: ecr-pull-secret
      containers:
        - name: wildfly
          image: <ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com/proquip-wildfly:latest
          ports:
            - containerPort: 8080
              name: http
            - containerPort: 9990
              name: management
          envFrom:
            - configMapRef:
                name: proquip-config
            - secretRef:
                name: proquip-secrets
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "1000m"
          readinessProbe:
            httpGet:
              path: /api/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /api/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
```

```yaml
# k8s/21-wildfly-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: proquip-wildfly
  namespace: proquip-dev
spec:
  selector:
    app: proquip-wildfly
  ports:
    - name: http
      port: 8080
      targetPort: 8080
```

### 4.6 Keycloak Deployment

```yaml
# k8s/30-keycloak-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: proquip-keycloak
  namespace: proquip-dev
  labels:
    app: proquip-keycloak
spec:
  replicas: 1
  selector:
    matchLabels:
      app: proquip-keycloak
  template:
    metadata:
      labels:
        app: proquip-keycloak
    spec:
      imagePullSecrets:
        - name: ecr-pull-secret
      containers:
        - name: keycloak
          image: <ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com/proquip-keycloak:latest
          ports:
            - containerPort: 8080
              name: http
          envFrom:
            - configMapRef:
                name: proquip-config
            - secretRef:
                name: proquip-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          readinessProbe:
            httpGet:
              path: /health/ready
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /health/live
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
```

```yaml
# k8s/31-keycloak-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: proquip-keycloak
  namespace: proquip-dev
spec:
  selector:
    app: proquip-keycloak
  ports:
    - name: http
      port: 8080
      targetPort: 8080
```

### 4.7 nginx Deployment + Route

```yaml
# k8s/40-nginx-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: proquip-nginx
  namespace: proquip-dev
  labels:
    app: proquip-nginx
spec:
  replicas: 1
  selector:
    matchLabels:
      app: proquip-nginx
  template:
    metadata:
      labels:
        app: proquip-nginx
    spec:
      imagePullSecrets:
        - name: ecr-pull-secret
      containers:
        - name: nginx
          image: <ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com/proquip-nginx:latest
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "256Mi"
              cpu: "200m"
          readinessProbe:
            httpGet:
              path: /proquip/health.json
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 10
```

```yaml
# k8s/41-nginx-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: proquip-nginx
  namespace: proquip-dev
spec:
  selector:
    app: proquip-nginx
  ports:
    - name: http
      port: 8080
      targetPort: 8080
```

```yaml
# k8s/42-nginx-route.yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: proquip
  namespace: proquip-dev
spec:
  to:
    kind: Service
    name: proquip-nginx
  port:
    targetPort: http
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
```

デプロイ後の URL 確認:

```bash
oc get route proquip -n proquip-dev -o jsonpath='{.spec.host}'
```

### 4.8 Keycloak レルム import Job

Keycloak 起動後に proquip レルムをインポートする Job:

```yaml
# k8s/38-keycloak-realm-import-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: proquip-keycloak-realm-import
  namespace: proquip-dev
spec:
  backoffLimit: 3
  template:
    spec:
      restartPolicy: Never
      imagePullSecrets:
        - name: ecr-pull-secret
      containers:
        - name: import
          image: <ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com/proquip-keycloak:latest
          command: ["/opt/keycloak/bin/kc.sh"]
          args: ["import", "--file=/opt/keycloak/data/import/proquip-realm.json"]
          envFrom:
            - configMapRef:
                name: proquip-config
            - secretRef:
                name: proquip-secrets
```

---

## Phase 5: デプロイ検証

### 一括デプロイ

```bash
# 順序通りに適用
oc apply -f k8s/00-namespace.yaml
oc apply -f k8s/02-configmap.yaml
# Secret は oc create secret で作成済みの前提
oc apply -f k8s/10-flyway-job.yaml
oc wait --for=condition=complete job/proquip-flyway-migrate -n proquip-dev --timeout=120s
oc apply -f k8s/30-keycloak-deployment.yaml -f k8s/31-keycloak-service.yaml
oc wait --for=condition=available deployment/proquip-keycloak -n proquip-dev --timeout=180s
oc apply -f k8s/38-keycloak-realm-import-job.yaml
oc wait --for=condition=complete job/proquip-keycloak-realm-import -n proquip-dev --timeout=120s
oc apply -f k8s/20-wildfly-deployment.yaml -f k8s/21-wildfly-service.yaml
oc wait --for=condition=available deployment/proquip-wildfly -n proquip-dev --timeout=180s
oc apply -f k8s/40-nginx-deployment.yaml -f k8s/41-nginx-service.yaml -f k8s/42-nginx-route.yaml
oc wait --for=condition=available deployment/proquip-nginx -n proquip-dev --timeout=60s
```

### スモークテスト

```bash
ROUTE_URL=$(oc get route proquip -n proquip-dev -o jsonpath='https://{.spec.host}')

# ヘルスチェック
curl -s "${ROUTE_URL}/api/health" | jq .

# フロントエンド
curl -s -o /dev/null -w '%{http_code}' "${ROUTE_URL}/"

# Keycloak
KC_URL=$(oc get route proquip-keycloak -n proquip-dev -o jsonpath='https://{.spec.host}' 2>/dev/null || echo "internal")
```

ブラウザで以下を確認:

1. `${ROUTE_URL}` にアクセス → ログイン画面表示
2. admin / admin123 でログイン → ダッシュボード表示
3. 商品一覧画面へ遷移 → データ表示
4. 発注作成画面へ遷移 → フォーム表示
5. 5 テストユーザー全員でログイン可能

### CI/CD E2E テスト

CI/CD パイプライン全体の動作確認:

```bash
# 1. feature ブランチ作成
git checkout -b test/cicd-e2e

# 2. 軽微な変更 (README にタイムスタンプ追加など)
echo "<!-- CI/CD test: $(date -u +%Y%m%dT%H%M%SZ) -->" >> proquip/README.md
git add proquip/README.md
git commit -m "test: CI/CD E2E verification"
git push -u origin test/cicd-e2e

# 3. PR 作成
gh pr create --title "test: CI/CD E2E verification" --body "CI/CD pipeline test"

# 4. CI 確認
gh pr checks  # Backend + Frontend が緑になるまで待機

# 5. マージ
gh pr merge --squash --delete-branch

# 6. CD 確認
gh run list --workflow=cd.yml --limit=1  # CD ワークフローが成功

# 7. ROSA 確認
oc get pods -n proquip-dev  # 全 Pod が Running
curl -s "https://$(oc get route proquip -n proquip-dev -o jsonpath='{.spec.host}')/api/health"
```

#### DoD チェック

PR 作成 → CI パス → main マージ → ECR プッシュ → ROSA デプロイ → ヘルスチェック 200 まで自動で完了。

---

## アーキテクチャ

```
┌─────────────────────────────────────────────────────┐
│                    AWS Account                       │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │              ROSA Cluster                     │   │
│  │              (proquip-dev namespace)           │   │
│  │                                               │   │
│  │  ┌─────────┐  ┌──────────┐  ┌────────────┐  │   │
│  │  │ nginx   │  │ wildfly  │  │  keycloak   │  │   │
│  │  │ (Route) │→ │ (Deploy) │  │  (Deploy)   │  │   │
│  │  │ :8080   │  │ :8080    │  │  :8080      │  │   │
│  │  └─────────┘  └────┬─────┘  └──────┬──────┘  │   │
│  │                     │               │         │   │
│  └─────────────────────┼───────────────┼─────────┘   │
│                        │               │             │
│  ┌─────────────────────▼───────────────▼─────────┐   │
│  │          Amazon RDS PostgreSQL 15.7            │   │
│  │        (db.t3.medium, Single-AZ, gp3)          │   │
│  │     proquip DB    +    keycloak DB             │   │
│  └───────────────────────────────────────────────┘   │
│                                                      │
│  ┌───────────────────────────────────────────────┐   │
│  │       Amazon ECR (3 private repositories)      │   │
│  │  proquip-wildfly / proquip-nginx / proquip-kc  │   │
│  └───────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
         ▲                              ▲
         │ oc set image                 │ docker push
         │                              │
┌────────┴──────────────────────────────┴─────────────┐
│              GitHub Actions CI/CD                    │
│                                                      │
│  PR → ci.yml (mvn verify + npm test + lint)          │
│  main push → cd.yml (build → ECR push → oc deploy)  │
│                                                      │
│  Repository: yifsun-lgtm/sample_project              │
└─────────────────────────────────────────────────────┘
```

---

## コスト概算

Dev/Staging 環境 (ap-northeast-1) の月額概算:

| リソース | スペック | 月額 (USD) |
|---------|---------|-----------|
| ROSA クラスタ料 | 管理フィー | ~$125 |
| EC2 (ワーカー) | m5.xlarge x2 | ~$360 |
| RDS PostgreSQL | db.t3.medium, Single-AZ | ~$50 |
| ECR ストレージ | ~1GB | ~$1 |
| NAT Gateway | 1 AZ | ~$35 |
| ELB (Router) | 1 NLB | ~$20 |
| **合計** | | **~$590/月** |

Production 昇格時の追加コスト:
- Multi-AZ RDS: +$50/月
- Multi-AZ ROSA (3 worker): +$180/月
- WAF / Shield: +$10-50/月
