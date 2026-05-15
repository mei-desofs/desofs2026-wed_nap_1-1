# Pipeline Automation-Phase 2, Sprint 1

## Table of Contents

1. [Branching Strategy & Team Workflow](#1-branching-strategy--team-workflow)
2. [Development Pipeline](#2-development-pipeline)
3. [CodeQL SAST Pipeline](#3-codeql-sast-pipeline)
4. [Release Pipeline](#4-release-pipeline)

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

## 4. Release Pipeline

**Workflow file:** [`.github/workflows/release-please.yml`](../../../../.github/workflows/release-please.yml)

**Tool:** [Release Please](https://github.com/googleapis/release-please)

**Purpose:** Automates versioned releases based on [Conventional Commits](https://www.conventionalcommits.org/). On merge to `main`, it:

1. Creates or updates a release PR with a generated `CHANGELOG.md` entry.
2. On merge of the release PR, tags the commit and creates a GitHub Release.

This ensures every production-bound artifact is versioned and traceable to the commits it includes.
