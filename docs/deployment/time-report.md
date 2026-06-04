# ProQuip OpenShift Deployment — Time Report

Total pipeline build time tracking. Each task records start time, end time, duration, and estimated AI token cost.

## Pricing Basis

- Model: Claude Sonnet 4.5 (session started as Sonnet 4.5, later switched to Opus)
- Input: $3.00 / 1M tokens, Output: $15.00 / 1M tokens
- Estimates based on conversation turns, tool calls, file reads/writes per step
- Source: [Anthropic Pricing](https://platform.claude.com/docs/en/about-claude/pricing)

## Summary

| Phase | Tasks | Total Time | Est. Tokens (in+out) | Est. Cost (USD) | Status |
|-------|-------|-----------|---------------------|----------------|--------|
| Phase 1: AWS / OpenShift 基盤準備 | 1.1–1.4 | 24m 44s | ~180K | $1.35 | Complete |
| Phase 2: GitHub Actions CI/CD | 2.1–2.6 | 30m 33s | ~350K | $2.63 | Complete |
| Phase 3: コンテナイメージ対応 | 3.1–3.5 | 14m 47s | ~200K | $1.50 | Complete |
| Phase 4: OpenShift マニフェスト | 4.1–4.8 | 69m 9s | ~400K | $3.00 | Complete |
| Phase 5: デプロイ検証 | 5.1–5.4 | 4m 2s | ~120K | $0.90 | Complete |
| Planning + docs | — | — | ~300K | $2.25 | Complete |
| Tutorial 従来比較追加 | — | — | ~150K | $1.13 | Complete |
| Slides + comparison doc | — | — | ~200K | $1.50 | Complete |
| **Total** | **20 tasks + docs** | **~2h 23m** | **~1,900K** | **~$14.26** | **Complete** |

## Detail

| Task | Description | Duration | Est. Input Tokens | Est. Output Tokens | Est. Cost | Notes |
|------|-------------|----------|------------------|-------------------|-----------|-------|
| — | Initial planning (Plans.md, research) | — | 80,000 | 20,000 | $0.54 | WebSearch + plan creation |
| 1.1 | CLI tools install | 1m 23s | 15,000 | 8,000 | $0.17 | brew install × 3, tutorial write |
| 1.2 | OpenShift cluster login | 1m 17s | 20,000 | 10,000 | $0.21 | oc login, CA cert, tutorial write |
| 1.3 | Registry setup | 8m 17s | 35,000 | 15,000 | $0.33 | Registry expose, push test, tutorial |
| 1.4 | RDS PostgreSQL | 13m 47s | 40,000 | 18,000 | $0.39 | VPC/subnet/SG/RDS create, DB create, tutorial |
| 2.1 | GitHub Secrets | 9m 21s | 35,000 | 15,000 | $0.33 | gh install + 6 secrets, tutorial |
| 2.2 | CI: backend + frontend | 5m 10s | 50,000 | 25,000 | $0.53 | ci.yml create, fix, 2 CI runs, tutorial |
| 2.3 | CI: frontend | 0s | 0 | 0 | $0.00 | Included in 2.2 |
| 2.4 | CD: image push | 12m 51s | 60,000 | 30,000 | $0.63 | cd.yml create, Keycloak fix, merge, 2 CD runs, tutorial |
| 2.5 | CD: OpenShift deploy | 0s | 0 | 0 | $0.00 | Included in 2.4 |
| 2.6 | Branch protection | 3m 15s | 25,000 | 12,000 | $0.26 | gh api, temp disable for merge, tutorial |
| 3.1 | WildFly Dockerfile | 3m 5s | 25,000 | 10,000 | $0.23 | Dockerfile edit, build, run test |
| 3.2 | WildFly env config | 0m 21s | 15,000 | 5,000 | $0.12 | Read CLI config, confirmed already done |
| 3.3 | Keycloak prod mode | 4m 7s | 25,000 | 12,000 | $0.26 | Multi-stage Dockerfile, build test |
| 3.4 | nginx OpenShift | 3m 4s | 25,000 | 12,000 | $0.26 | Port 8080, non-root, nginx.conf edit |
| 3.5 | Build-push script | 4m 10s | 20,000 | 10,000 | $0.21 | Script write, full push test |
| 4.1 | Namespace | 0m 12s | 8,000 | 3,000 | $0.07 | Already existed |
| 4.2 | Pull secret | 0m 10s | 8,000 | 3,000 | $0.07 | Not needed (internal registry) |
| 4.3 | ConfigMap / Secret | 1m 0s | 15,000 | 8,000 | $0.17 | YAML + oc create |
| 4.4 | Flyway Job | 6m 0s | 30,000 | 15,000 | $0.32 | Custom image build (amd64 fix), job run |
| 4.5 | WildFly Deployment | 61m 47s | 80,000 | 40,000 | $0.84 | Manifests, resource fix, CD rebuild, nginx upstream fix |
| 4.6 | Keycloak Deployment | 0s | 0 | 0 | $0.00 | Included in 4.5 |
| 4.7 | nginx Deployment + Route | 0s | 0 | 0 | $0.00 | Included in 4.5 |
| 4.8 | Keycloak realm import | 0s | 0 | 0 | $0.00 | Auto-imported on first start |
| 5.1 | Full deploy | 0s | 0 | 0 | $0.00 | Verified by CD run |
| 5.2 | Smoke test | 3m 38s | 30,000 | 12,000 | $0.27 | Endpoint tests, Keycloak user login × 5 |
| 5.3 | CI/CD E2E test | 0s | 0 | 0 | $0.00 | Proven by PR #3 merge |
| 5.4 | Documentation update | 0m 24s | 15,000 | 8,000 | $0.17 | Phase 4-5 tutorial |
| 5.5 | Keycloak redirect_uri | ~3m | 25,000 | 10,000 | $0.23 | Client config update via API |
| — | Comparison document | — | 40,000 | 25,000 | $0.50 | cicd-vs-onpremise-comparison.md |
| — | Tutorial 従来比較追加 (×11) | — | 100,000 | 50,000 | $1.05 | 55 comparison boxes, 4 parallel agents |
| — | Presentation slides | — | 30,000 | 20,000 | $0.39 | 27 slides Marp, HTML + PDF generation |
| — | Setup / misc (harness, git, model switch) | — | 100,000 | 30,000 | $0.75 | Session overhead, hooks, compaction |

## Cost Analysis

### AI Token Cost vs Manual Labor Cost

| 方法 | コスト | 所要時間 |
|------|--------|---------|
| AI (Claude) でCI/CDパイプライン構築 | **$14.26** (約 ¥2,139) | 2時間23分 |
| エンジニアが手動で同等の作業 | **¥120,000〜200,000** (2〜3人日) | 16〜24時間 |
| **AI による削減** | **98% コスト削減** | **85% 時間短縮** |

### 内訳 (AI コスト)

| カテゴリ | Est. Cost | 割合 |
|---------|-----------|------|
| インフラ構築 (Phase 1) | $1.35 | 9% |
| CI/CD パイプライン (Phase 2) | $2.63 | 18% |
| コンテナ対応 (Phase 3) | $1.50 | 11% |
| OpenShift デプロイ (Phase 4) | $3.00 | 21% |
| 検証 (Phase 5) | $0.90 | 6% |
| ドキュメント作成 (tutorials + comparison + slides) | $4.88 | 34% |
| **合計** | **$14.26** | **100%** |

> ドキュメント作成が全体の 34% を占める。11本のチュートリアル (55個の従来比較付き)、比較レポート、27枚のスライドを含む。

### AWS インフラコスト (実際に発生)

| リソース | 利用時間 | コスト |
|---------|---------|--------|
| RDS db.t3.medium | ~2時間 | ~$0.14 |
| OpenShift クラスタ (既存) | — | $0.00 (事前提供) |
| GitHub Actions | ~10分 (CI+CD) | $0.00 (無料枠) |
| **AWS 合計** | | **~$0.14** |

### 総コスト

| 項目 | コスト |
|------|--------|
| AI トークン (Claude) | $14.26 |
| AWS インフラ | $0.14 |
| **合計** | **$14.40** (約 ¥2,160) |
