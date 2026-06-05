# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ProQuip is an enterprise procurement and inventory management system sample project for IT equipment distributors. This is a ~100K LOC codebase intentionally containing technical debt patterns for AI refactoring verification.

**IMPORTANT**: This codebase intentionally contains 20 types of technical debt patterns (God Classes, N+1 queries, SQL injection vulnerabilities, circular dependencies, etc.). Do not "fix" these patterns unless explicitly asked - they exist for refactoring verification purposes.

## Technology Stack

### Backend (Jakarta EE 10)
- **Application Server**: WildFly 30.0.1 
- **Java**: OpenJDK 17
- **Framework**: Pure Jakarta EE (EJB, JPA, JAX-RS, CDI) - **Intentionally NOT using Spring**
- **ORM**: Hibernate 6.4.9
- **Database**: PostgreSQL 15.7
- **Migration**: Flyway 10.15.0
- **Build**: Maven (multi-module)

### Frontend
- **Framework**: Angular 17.3.12
- **Auth**: keycloak-angular 15.3.0

### Infrastructure
- **Container**: Podman Compose (or Docker Compose)
- **Auth**: Keycloak 22.0.5 (OIDC)
- **Reverse Proxy**: nginx 1.25.4

## Development Commands

### Full Stack (Container-based)

All commands should be run from the `proquip/` directory.

```bash
# Build and start all services
cd proquip
podman compose -f podman-compose.yml up -d --build

# Check service status
podman compose -f podman-compose.yml ps

# View logs
podman logs proquip-wildfly --tail 50
podman logs proquip-migration  # Check migration status
podman logs proquip-nginx --tail 20

# Stop services
podman compose -f podman-compose.yml down

# Stop and remove volumes (full reset)
podman compose -f podman-compose.yml down -v
```

**Note**: First build takes several minutes due to Maven and npm dependency resolution. No local Java, Maven, or Node.js installation required - everything builds in containers.

### Backend Development

```bash
# Build all modules (from proquip-parent/)
cd proquip/proquip-parent
mvn clean install

# Build specific module
mvn clean install -pl :proquip-ejb

# Run unit tests
mvn test

# Run integration tests
mvn verify

# Skip tests during build
mvn clean install -DskipTests

# Run Flyway migrations (requires local PostgreSQL)
cd proquip/proquip-db
mvn flyway:migrate -Pdev

# Deploy to running WildFly
cd proquip/proquip-ear
mvn wildfly:deploy
```

### Frontend Development

```bash
cd proquip/proquip-frontend

# Install dependencies
npm install

# Development server with hot reload (requires backend services running)
npm start
# Access at http://localhost:4200
# Proxies /api/* → localhost:8080 (WildFly)
# Proxies /auth/* → localhost:8180 (Keycloak)

# Build for production
npm run build

# Run tests
npm test

# Lint
npm run lint
```

### Access URLs

| URL | Service |
|-----|---------|
| http://localhost:4200 | Frontend (nginx) |
| http://localhost:4200/api/health | Backend health check |
| http://localhost:8180 | Keycloak admin console (admin/admin) |
| http://localhost:9990 | WildFly admin console (admin/admin123!) |

### Test Users

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| manager | manager123 | MANAGER |
| buyer | buyer123 | BUYER |
| warehouse | warehouse123 | WAREHOUSE_STAFF |
| viewer | viewer123 | VIEWER |

## Architecture Overview

### Maven Module Structure

```
proquip-parent/     → Parent POM (version management, ~450 lines)
proquip-common/     → DTOs, Mappers, Utils, Constants, Exceptions
proquip-ejb/        → JPA Entities, EJB Services, DAOs, Interceptors, Schedulers
proquip-web/        → JAX-RS REST APIs, Filters, Security
proquip-ear/        → EAR packaging (bundles EJB + WAR)
proquip-db/         → Flyway migrations + seed data
proquip-frontend/   → Angular SPA
```

**Dependency Flow**: `proquip-ear` packages `proquip-web` (WAR) + `proquip-ejb` (EJB-JAR). Both WAR and EJB depend on `proquip-common`.

### Backend Package Structure

```
com.proquip.common/
  ├── constant/      → Enums, Constants (7 files)
  ├── dto/           → Data Transfer Objects (41 files, naming inconsistent)
  ├── exception/     → Custom exceptions (7 files)
  ├── mapper/        → MapStruct + manual mappers (11 files)
  └── util/          → Utilities (8 files, contains technical debt)

com.proquip.ejb/
  ├── dao/           → Data Access Layer (10 files, mixed patterns)
  ├── entity/        → JPA Entities (56 files across 7 domain packages)
  ├── event/         → CDI Events (4 files)
  ├── interceptor/   → CDI Interceptors (7 files)
  ├── scheduler/     → Scheduled jobs (5 files)
  ├── service/       → Business logic (20 files, contains God Classes)
  └── validator/     → Business validators (4 files)

com.proquip.web/
  ├── filter/        → Servlet filters, Exception mappers (5 files)
  └── resource/      → JAX-RS REST endpoints (21 files)
```

### Frontend Structure

```
src/app/
  ├── core/          → Auth, layout, interceptors, guards
  ├── shared/        → Common components, pipes, directives, models, services
  └── features/      → 10 feature modules (dashboard, products, suppliers,
                       procurement, inventory, warehouses, pricing, reports,
                       admin, import-export)
```

### Domain Model (7 Domains)

| Domain | Entities | Key Features |
|--------|----------|--------------|
| Product Catalog | 12 | Product, Category, Manufacturer, Bundles, Alternatives |
| Supplier Management | 8 | Suppliers, Contacts, Contracts, Certifications |
| Procurement | 10 | Purchase Requisition → PO → Approval Workflow → Receipt → Returns |
| Inventory & Warehouse | 8 | Stock, Warehouse Zones, Transfers, Stock Counts |
| Pricing & Budget | 6 | Price Lists, Tax Rates, Budget Management |
| Organization & Users | 6 | Departments (hierarchical), Users, Roles, Delegation |
| Notifications & System | 4 | Templates, Audit Logs, Settings, Import Jobs |

## Key Development Rules

### Dependencies
**CRITICAL**: When installing OSS dependencies, explicitly specify stable versions - NEVER use `latest`.

```bash
# ✅ Good
npm install lodash@4.17.21
mvn install -Dversion=2.17.1

# ❌ Bad
npm install lodash@latest
mvn install
```

### Framework Choice
This project **intentionally uses Jakarta EE** (EJB, CDI, JAX-RS), not Spring. Do not suggest Spring alternatives unless explicitly requested.

### Data Access Patterns
Four different patterns are used (intentionally inconsistent):
- `ProductDao` → Criteria API
- `SupplierDao` → JPQL
- `PurchaseOrderDao` → Native SQL
- Custom query builders → Proprietary DSL

When asked to query data, match the existing pattern in that DAO class.

### Testing
Current test coverage is ~15% with many `@Disabled` tests. This is intentional - do not enable or fix disabled tests unless specifically asked.

## Known Technical Debt Hotspots

These files contain intentional technical debt patterns - **do not refactor** unless explicitly requested:

### Critical Hotspots
- **`PurchaseOrderServiceBean.java`** (1,963 LOC) - God Class, N+1 queries, hardcoded business rules, SQL injection, circular dependency with `InventoryServiceBean`
- **`order-create.component.ts`** (~450 LOC) - Mixed concerns, business logic in component
- **`PurchaseOrderDao.java`** - SQL injection via string concatenation
- **`ReportQueryHelper.java`** - SQL injection, tight coupling
- **`DateUtils.java`** - Pre-Java 8 style (`Date`, `Calendar`, `SimpleDateFormat`)

### Specific Patterns to Preserve
1. **God Class**: `PurchaseOrderServiceBean.java` handles CRUD, approval, receiving, budget, reports, notifications, CSV - all in one class
2. **Copy-Paste**: Approval workflow duplicated between `RequisitionServiceBean` and `PurchaseOrderServiceBean`
3. **N+1**: `ProductResource.java` search, `DashboardResource.java` summary building
4. **Hardcoded Rules**: Approval thresholds (¥1M → MANAGER, ¥5M → ADMIN), tax rates, alert thresholds in source code
5. **SQL Injection**: `PurchaseOrderDao.searchOrders()`, `ReportQueryHelper` - user input directly concatenated into SQL
6. **Validation Mismatch**: Frontend vs Backend validation rules differ (see README.md §17)
7. **Circular Dependency**: `PurchaseOrderServiceBean` ⟷ `InventoryServiceBean` via `@EJB`

## Database Migration

Flyway scripts are in `proquip-db/src/main/resources/db/migration/`:
- `V001-V011`: DDL, indexes, views, stored procedures
- `V020-V027`: Seed data (master data, products, suppliers, inventory, procurement, pricing, system)

When creating new migrations:
- Use sequential version numbers (next available is V028+)
- Place DDL changes before seed data
- Test in container first: `podman logs proquip-migration`

## Common Gotchas

1. **EAR Deployment**: Changes to `proquip-ejb` or `proquip-web` require rebuilding the entire EAR. Use `mvn clean install` from `proquip-parent/`.

2. **Keycloak Auth**: Frontend auth requires proper CORS configuration. Verify `proquip-realm.json` includes `http://localhost:4200` in allowed origins.

3. **Hot Reload**: For frontend hot reload during development, run only backend services in containers and use `npm start` locally.

4. **Database Reset**: Use `podman compose down -v` to fully reset the database. Migrations will re-run on next startup.

5. **WildFly Logs**: Application logs go to console, not files. Use `podman logs proquip-wildfly -f` to tail.

6. **Date Handling**: The codebase uses legacy `java.util.Date`. This is intentional technical debt (#6) - do not modernize to `java.time` unless specifically requested.

7. **String Concatenation SQL**: SQL injection vulnerabilities in `PurchaseOrderDao` and `ReportQueryHelper` are intentional - do not fix with prepared statements unless asked.

## Project Structure Reference

47 screens across 10 modules:
1. Dashboard (2 screens)
2. Product Catalog (7 screens + wizard)
3. Suppliers (4 screens)
4. Procurement (9 screens: requisitions, POs, approvals, receiving, returns)
5. Inventory (5 screens)
6. Warehouses (3 screens)
7. Pricing (3 screens)
8. Reports (4 screens)
9. Admin (10 screens)
10. Import/Export (2 screens)

See `proquip/README.md` for complete technical debt catalog and detailed system documentation.


## /make-slide
When the user types "/make-slide", read `.claude/skills/make-slide/SKILL.md` and follow the presentation creation workflow. Browse themes at https://make-slide.vercel.app
