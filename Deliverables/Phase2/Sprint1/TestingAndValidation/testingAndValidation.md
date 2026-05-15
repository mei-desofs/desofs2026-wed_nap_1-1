# Testing and Validation

## Index

- [1. Purpose](#1-purpose)
- [2. Strategy Overview](#2-strategy-overview)
- [3. Testing Approach](#3-testing-approach)
    - [3.1 SAST - Static Analysis](#31-sast---static-analysis)
    - [3.2 SCA - Dependency Scanning](#32-sca---dependency-scanning)
    - [3.3 Functional & Unit Tests](#33-functional--unit-tests)
    - [3.4 DAST - Dynamic Testing](#34-dast---dynamic-testing)
    - [3.5 IAST - Instrumented Testing](#35-iast---instrumented-testing)
    - [3.6 Pen Testing](#36-pen-testing)
- [4. Functional Tests](#4-functional-tests)
- [5. Security Testing Results](#5-security-testing-results)
- [6. Results & Observations](#6-results--observations)
- [7. Tool Selection Justification Summary](#7-tool-selection-justification-summary)
- [8. References](#8-references)

## 1. Purpose

Document the test strategy, executed tests, and validation results for Sprint 1. This strategy integrates security testing throughout the development lifecycle (SAST, DAST, IAST, and SCA).

## 2. Strategy Overview

The testing strategy follows three core principles:

- **Early Detection**: Security testing integrated from the start
- **Automation**: Continuous security scanning in the CI/CD pipeline
- **Multiple Layers**: Different testing methodologies catch vulnerabilities at different stages

## 3. Testing Approach

### 3.1 SAST - Static Analysis (Development Phase)

Catches vulnerabilities before compilation.

| Aspect | CodeQL | SonarQube | Checkmarx |
|--------|--------|-----------|----------|
| Cost | Free (GitHub) | Free/Paid | Expensive |
| Java Support | Excellent | Excellent | Excellent |
| Setup Complexity | Simple | Simple | Complex |
| CI/CD Integration | Native GitHub | Good | Good |
| Centralized Results | GitHub Security tab | External server | External server |
| **Selection** | **Chosen** | Alternative | Alternative |

**Selected: CodeQL**
- Free and natively integrated with GitHub Actions
- Findings published directly to the **GitHub Security tab**, no external server or credentials required
- Excellent Java/Spring Boot support (detects injection, path traversal, unsafe deserialization, crypto issues)
- Runs on every push and PR, blocking merges when vulnerabilities are found
- Keeps SAST results, PR checks, and vulnerability alerts in one centralized place

> **Note:** CodeQL was chosen over SonarQube because it integrates natively with GitHub, findings are published directly to the **GitHub Security tab**, keeping static analysis results, PR checks, and vulnerability alerts in one centralized place without requiring an external server or additional credentials.

#### Local SAST (pre-push)

In addition to CodeQL in the CI pipeline, two Maven plugins run **locally during the build** to catch bugs before code is even pushed to the repository:

| Plugin | Purpose |
|--------|---------|
| **SpotBugs + FindSecBugs** | Detects common bug patterns and security-specific issues (SQL injection, XSS, path traversal, weak crypto) at bytecode level |
| **PMD** | Identifies code smells, dead code, and potential bugs via source-level static analysis rules |

Both plugins are configured in `pom.xml` and execute during `mvn verify`, so we can get immediate feedback on our local machine before creating a PR.

### 3.2 SCA - Dependency Scanning (Build Phase)

Third-party libraries often contain known vulnerabilities.

| Tool | Speed | Accuracy | Cost | Integration |
|------|-------|----------|------|-------------|
| OWASP Dependency-Check | Fast | Good | Free | Maven plugin |
| Snyk | Very Fast | Excellent | Freemium | GitHub/GitLab |
| Black Duck | Slow | Excellent | Expensive | Complex |

**Selected: OWASP Dependency-Check**
- Free and open-source
- Integrates directly into Maven build (`pom.xml`)
- Generates Software Bill of Materials (SBOM)
- No external accounts required

### 3.3 Functional & Unit Tests

- Security-focused test cases (authentication, authorization, input validation)
- Coverage target: >70%

### 3.4 DAST - Dynamic Testing

Tests the running application as an attacker would (black box).

| Tool | Ease of Use | Accuracy | Cost | Best For |
|------|-------------|----------|------|----------|
| OWASP ZAP | Easy | Good | Free | Quick scans, continuous integration |
| Burp Suite | Easy | Excellent | Free/Paid | Comprehensive testing, professional |
| Acunetix | Medium | Excellent | Expensive | Enterprise needs |

**Selected: OWASP ZAP**
- Free and open-source
- Active community and regular updates
- Excellent for CI/CD integration
- No licensing concerns

### 3.5 IAST - Instrumented Testing

> **TODO**  - IAST tooling not yet selected or integrated.

IAST instruments the application at runtime (via a JVM agent) and observes code execution paths as requests flow through, combining the depth of SAST with the realism of DAST. Unlike DAST (black box), IAST can pinpoint the exact vulnerable line of code.

| Tool | License | JVM Agent | Notes |
|------|---------|-----------|-------|
| Contrast Security | Commercial | Yes | Enterprise-grade, real-time feedback |
| Seeker (Synopsys) | Commercial | Yes | CI/CD integration |
| HCL AppScan | Commercial | Yes | Broad language support |

**Status:** Not yet implemented - planned for a future sprint.

### 3.6 Pen Testing

TODO

---

## 4. Functional Tests

### Test Scenarios

TBD

| Use Case | Test Cases | Security Focus |
|----------|-----------|-----------------|
|          |           |                 |

---

## 5. Security Testing Results

TBD

### SAST Results (CodeQL)

| Metric | Target | Status |
|--------|--------|--------|
|          |           |                 |


### SCA Results (Dependency-Check)

| Metric | Status |
|--------|--------|
| Total dependencies scanned | ~50+ transitive |
| Vulnerabilities remaining | 5 CVEs (accepted risk) |
| Build-breaking threshold | CVSS ≥ 9 |
| Build status | Pass |

#### Accepted Risk - Unfixable CVEs

The following CVEs have no available fix at the time of this sprint. They are documented as **accepted risk** with justification:

| Dependency | CVE | CVSS | Risk Justification |
|---|---|---|---|
| angus-activation 2.0.3 | CVE-2025-7962 | Low-Medium | No newer version available; managed by Spring Boot 3.5.14. Email activation library with minimal attack surface in our REST API context. |
| hibernate-validator 8.0.3 | CVE-2025-15104 | Medium | Latest 8.x release; version 9.x is incompatible with Spring Boot 3.5. Requires specific usage patterns not present in our codebase. |
| swagger-ui 5.32.2 (DOMPurify 3.3.2) | CVE-2026-41240 | Medium | Swagger UI is **disabled in production** (`springdoc.swagger-ui.enabled=false`). Only enabled in dev profile. The CVE requires using `ADD_TAGS` function + `FORBID_TAGS` simultaneously, a pattern Swagger UI does not use. |
| swagger-ui 5.32.2 (DOMPurify 3.3.2) | CVE-2026-41238 | Medium | Same as above, Swagger UI disabled in production; attack surface eliminated. |
| swagger-ui 5.32.2 (DOMPurify 3.3.2) | CVE-2026-41239 | Medium | Same as above, Swagger UI disabled in production; attack surface eliminated. |

**Mitigation controls in place:**
- Swagger UI and OpenAPI docs endpoints are disabled by default (`application.properties`)
- Only enabled via `application-dev.properties` in development profile
- Production deployments never expose the vulnerable Swagger UI frontend
- Dependencies will be re-scanned in future sprints as patches become available


### DAST Results (OWASP ZAP)

| Vulnerability Type | Status | Finding |
|-------------------|--------|---------|
|          |           |                 |

---

## 6. Results & Observations

### Overall Status

**Date**:
**Pass/Fail**: 

### Key Findings



### Issues Identified

| Severity | Count | Examples |
|----------|-------|----------|
| Critical |  | |
| High |  | |
| Medium |  | |

### Recommendations



---

## 7. Tool Selection Justification Summary

| Tool | Purpose | Why Selected |
|------|---------|--------------|
| **CodeQL** | SAST | Free, native GitHub integration, centralized Security tab |
| **Dependency-Check** | SCA | Free, Maven plugin, no external accounts |
| **OWASP ZAP** | DAST | Free, CI/CD friendly, active community |
| **Maven** | Build/Test | Already in use, integrated security plugins |

---

## 8. References

### Testing Tools
- [CodeQL](https://codeql.github.com/) - GitHub-native SAST engine, results in GitHub Security tab
- [OWASP Dependency-Check](https://github.com/jeremylong/DependencyCheck) - Maven Plugin for SCA
- [OWASP ZAP](https://www.zaproxy.org/) - Automated DAST scanning tool

### Security Standards
- [OWASP Top 10](https://owasp.org/www-project-top-ten/) - Web application security risks
- [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) - Application Security Verification Standard
- [STRIDE Threat Modeling](https://en.wikipedia.org/wiki/STRIDE_(security)) - Threat analysis methodology
