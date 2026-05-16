# Code Review & Release Strategy

Our team follows a **branch-based workflow** supported by Continuous Integration (CI) and release automation. The goal of this strategy is to ensure that any code reaching the `main` branch is functional, secure and properly reviewed, minimising the introduction of regressions or vulnerabilities.

Below we describe the lifecycle of a code change, from development through to official release.

## Table of Contents

1. [Development in Isolated Branches](#1-development-in-isolated-branches)
2. [Creating the Pull Request (PR)](#2-creating-the-pull-request-pr)
3. [Automated Validation (CI & Security)](#3-automated-validation-ci--security)
4. [Code Review (Peer Review)](#4-code-review-peer-review)
5. [Promotion to Production (main -> prod)](#5-promotion-to-production-main---prod)
6. [Official Release and Automation (release-please)](#6-official-release-and-automation-release-please)

---

## 1. Development in Isolated Branches

- Each new feature, bug fix or maintenance task is developed in a separate branch.
- Whenever new commits are pushed to these feature branches, the `Feature Checks` workflow is triggered automatically.
- This preliminary step builds the project on Java 21 and runs unit tests and basic static analyses (SpotBugs), ensuring the base code is healthy before it is proposed for integration.

## 2. Creating the Pull Request (PR)

- When work on the branch is complete, the developer opens a Pull Request to get feedback and merge the change into `main`.
- The PR title and final commits must follow **Conventional Commits** (for example: `feat:`, `fix:`, `chore:`). This is required for the release tooling.

## 3. Automated Validation (CI & Security)

Opening or updating a PR against `main` triggers main validation workflows (`Development CI` and `Security Pipeline`). Before any human review, GitHub Actions runs the following checks automatically:

- **Build & Test:** compile the source (Maven) and run all automated tests.
- **Secret Scan:** detect exposed credentials using GitLeaks.
- **SAST (Static Application Security Testing):** analyse source for vulnerabilities using CodeQL and FindSecBugs.
- **SCA (Software Composition Analysis):** check third-party dependencies for known vulnerabilities using OWASP Dependency-Check and generate an SBOM.
- **DAST (Dynamic Application Security Testing):** run dynamic API scans against a deployed test instance using OWASP ZAP.

## 4. Code Review (Peer Review)

- After pipelines complete, a team member other than the author conducts the peer review.
- The reviewer has immediate access to CI and security results. If a pipeline failed, the reviewer knows the PR requires fixes before approval.
- If tests pass and quality is validated, the reviewer approves the PR and the change is merged into `main`.

## 5. Promotion to Production (main -> prod)

Once changes are stabilised on `main`, promotion to production is performed via a dedicated PR from `main` to `prod`.

- Create a Pull Request from `main` to `prod` to promote a release candidate.
- This promotion PR receives a formal team review to ensure the feature set is ready for distribution.

## 6. Official Release and Automation (release-please)

The official release is performed when changes are merged into the `prod` branch.

- Merging into `prod` triggers the **Release Please** action.
- `release-please` inspects the integrated Conventional Commits and creates/updates a release PR.
- It manages semantic versioning, updates the `CHANGELOG.md`, and publishes the built `.jar` artifact via the GitHub Releases page for distribution.

This document mirrors the structure used across the project's documentation and can be referenced from the team's Contributing or Release guides.