# ProQuip ROSA Deployment — Time Report

Total pipeline build time tracking. Each task records start time, end time, and duration.

## Summary

| Phase | Tasks | Total Time | Status |
|-------|-------|-----------|--------|
| Phase 1: AWS / OpenShift 基盤準備 | 1.1–1.4 | 24m 44s | Complete |
| Phase 2: GitHub Actions CI/CD | 2.1–2.6 | 30m 33s | Complete |
| Phase 3: コンテナイメージ対応 | 3.1–3.5 | — | Not started |
| Phase 4: OpenShift マニフェスト | 4.1–4.8 | — | Not started |
| Phase 5: デプロイ検証 | 5.1–5.4 | — | Not started |
| **Total** | **22** | **—** | |

## Detail

| Task | Description | Start | End | Duration | Notes |
|------|-------------|-------|-----|----------|-------|
| 1.1 | CLI tools install | 2026-05-30T06:42:06Z | 2026-05-30T06:43:29Z | 1m 23s | brew install awscli rosa-cli openshift-cli |
| 1.2 | OpenShift cluster login | 2026-05-30T11:56:38Z | 2026-05-30T11:57:55Z | 1m 17s | oc login with token + CA cert |
| 1.3 | Registry setup | 2026-05-30T12:00:15Z | 2026-05-30T12:08:32Z | 8m 17s | OpenShift internal registry — exposed default route, verified push/pull |
| 1.4 | RDS PostgreSQL | 2026-05-30T12:10:28Z | 2026-05-30T12:24:15Z | 13m 47s | RDS 15.18, db.t3.medium, proquip + keycloak DBs created |
| 1.5 | RDS PostgreSQL | — | — | — | |
| 2.1 | GitHub Secrets | 2026-05-30T12:29:59Z | 2026-05-30T12:39:20Z | 9m 21s | gh CLI install + 6 secrets set (OpenShift + RDS) |
| 2.2 | CI: backend | 2026-05-30T12:41:35Z | 2026-05-30T12:46:45Z | 5m 10s | ci.yml created, 1st run failed (intentional test debt), fixed, 2nd run green |
| 2.3 | CI: frontend | — | — | 0s | Included in 2.2 (same ci.yml, parallel job) |
| 2.4 | CD: image push | 2026-05-30T12:48:31Z | 2026-05-30T13:01:22Z | 12m 51s | cd.yml created, 1st run Keycloak context fix, 2nd run all 3 images green |
| 2.5 | CD: OpenShift deploy | — | — | 0s | Included in 2.4 (same cd.yml, deploy job) |
| 2.6 | Branch protection | 2026-05-30T13:04:27Z | 2026-05-30T13:07:42Z | 3m 15s | gh api branch protection: CI required + 1 approval + enforce admins |
| 3.1 | WildFly Dockerfile | — | — | — | |
| 3.2 | WildFly env config | — | — | — | |
| 3.3 | Keycloak prod mode | — | — | — | |
| 3.4 | nginx OpenShift | — | — | — | |
| 3.5 | ECR push script | — | — | — | |
| 4.1 | Namespace | — | — | — | |
| 4.2 | ECR pull secret | — | — | — | |
| 4.3 | ConfigMap / Secret | — | — | — | |
| 4.4 | Flyway Job | — | — | — | |
| 4.5 | WildFly Deployment | — | — | — | |
| 4.6 | Keycloak Deployment | — | — | — | |
| 4.7 | nginx Deployment + Route | — | — | — | |
| 4.8 | Keycloak realm import | — | — | — | |
| 5.1 | Full deploy | — | — | — | |
| 5.2 | Smoke test | — | — | — | |
| 5.3 | CI/CD E2E test | — | — | — | |
| 5.4 | Documentation update | — | — | — | |
