# Pipeline Automation-Phase 2, Sprint 1

## Table of Contents

1. [Branching Strategy & Team Workflow](#1-branching-strategy--team-workflow)
2. [Development Pipeline](#2-development-pipeline)
3. [CodeQL SAST Pipeline](#3-codeql-sast-pipeline)
4. [Security Pipeline (DAST)](#4-security-pipeline-dast)
5. [Release Pipeline](#5-release-pipeline)

---

## 1. Branching Strategy & Team Workflow

Development follows a **feature-based Git workflow**:

- Each feature or fix is developed in a dedicated branch.
- Changes are integrated via **Pull Requests (PRs)** into `main`, triggering automated checks and requiring peer review.
- The `release-please` workflow automates changelog and release tag creation on merge to `main`.

_Benefits_: Traceability, clean collaboration, and early conflict detection.

---

## 2. Development Pipeline

**Workflow file:** [`.github/workflows/development.yml`](../../../../.github/workflows/development.yml)

**Triggers:** Every push to `main`.

**Steps:**

| Step | Tool | Purpose |
|---|---|---|
| Checkout | Git | Fetch source code |
| Setup JDK 21 | Temurin + Maven cache | Build environment |
| `mvn -B verify` | Maven | Compile + run all tests |
| Coverage report | JaCoCo 0.8.12 | Enforce >70% coverage gate |
| Static analysis | SpotBugs + FindSecBugs | Security bug patterns |
| Static analysis | PMD | Error-prone and security rule categories |
| Dependency check | OWASP Dependency-Check 11.1.1 | CVE scan-build fails on CVSS ≥ 9 |

Reports generated:
- `target/dependency-check-report.html`
- `target/dependency-check-report.json`
- `target/site/jacoco/`

---

## 3. CodeQL SAST Pipeline

**Workflow file:** [`.github/workflows/codeql.yml`](../../../../.github/workflows/codeql.yml)

**Triggers:** Every push and Pull Request to `main`.

**Steps:**

| Step | Purpose |
|---|---|
| Initialize CodeQL for Java | Set up analysis engine |
| `mvn compile -DskipTests` | Compile source for analysis |
| CodeQL analysis | Detect injection, path traversal, unsafe deserialization, cryptographic issues |
| Upload results | Results published to GitHub Security tab |

---

## 4. Security Pipeline (DAST)

**Workflow file:** [`.github/workflows/security.yml`](../../../../.github/workflows/security.yml)

**Triggers:** Invoked via `workflow_call` (reusable workflow, called from the development pipeline).

This pipeline performs four security checks in parallel:

### 4.1 Jobs Overview

| Job | Tool | Purpose |
|---|---|---|
| Secret Scan | GitLeaks 1.6.0 | Detect hardcoded secrets in the repository history |
| SAST | CodeQL (Java) | Static analysis for injection, path traversal, unsafe deserialization |
| SCA | OWASP Dependency-Check + Anchore SBOM | CVE scanning and Software Bill of Materials generation |
| DAST | OWASP ZAP 0.10.0 | Dynamic black-box testing against the running application |

### 4.2 DAST Job - OWASP ZAP

The DAST job performs dynamic application security testing against the live API:

1. **Build** - compiles the application JAR (skipping tests for speed)
2. **Start** - launches the Spring Boot app with a MySQL database and Auth0 configuration; waits up to 150 s for `/actuator/health` to respond
3. **Verify OpenAPI** - confirms `/v3/api-docs` is accessible (required for ZAP's API scan mode)
4. **Obtain Auth0 token** - requests a Machine-to-Machine (M2M) token via `client_credentials` grant; the token is masked in logs (`::add-mask::`)
5. **Verify token** - tests the token against `GET /api/movies` to ensure it's accepted
6. **Run ZAP** - executes `zaproxy/action-api-scan@v0.10.0` against the OpenAPI spec, injecting the Bearer token via ZAP's Replacer add-on so authenticated endpoints are exercised

#### ZAP Configuration

- **Scan mode:** OpenAPI-driven API scan
- **Authentication:** Bearer token injected into every request via ZAP Replacer
- **Fail threshold:** Build fails on any unignored alert (`fail_action: true`)
- **Suppressed rules** (informational / not applicable):

| Rule ID | Reason |
|---|---|
| 10015 | Cache-control headers not relevant for API |
| 10096 | Timestamp disclosure (informational) |
| 10036 | Server version leakage (Spring Boot doesn't expose by default) |
| 90005 | Sec-Fetch-Dest missing (browser-only header) |
| 10038 | CSP header set by filter but not on error pages |
| 10021 | X-Content-Type-Options set by filter |
| 40018 | SQL Injection false positive, JPA uses prepared statements; behavioural difference caused by input sanitizer, not injection |

---

## 5. Release Pipeline

**Workflow file:** [`.github/workflows/release-please.yml`](../../../../.github/workflows/release-please.yml)

**Tool:** [Release Please](https://github.com/googleapis/release-please)

**Purpose:** Automates versioned releases based on [Conventional Commits](https://www.conventionalcommits.org/). On merge to `main`, it:

1. Creates or updates a release PR with a generated `CHANGELOG.md` entry.
2. On merge of the release PR, tags the commit and creates a GitHub Release.

This ensures every production-bound artifact is versioned and traceable to the commits it includes.
