# OWASP ASVS 5.0 Assessment Evolution

This document summarizes the evolution of the project's compliance with **OWASP ASVS 5.0** across three project iterations: **Phase 1, Phase 2 Sprint 1, and Phase 2 Sprint 2**. The purpose of this chapter is to compare the ASVS assessment results obtained during each iteration and analyse how the project's security posture evolved throughout the development lifecycle.

## Table of Contents

1. [Phase 1 Baseline](#phase-1-baseline)
2. [Phase 2 Sprint 1](#phase-2-sprint-1)
3. [Phase 2 Sprint 2](#phase-2-sprint-2)
4. [Comparative Analysis](#comparative-analysis)
5. [Compliance Summary](#compliance-summary)

---

## Phase 1 Baseline

Phase 1 established the initial security baseline of the project. This iteration focused primarily on requirements elicitation, architectural design, threat identification, and the definition of security controls that would guide the implementation phase.

The strongest areas at this stage were:

- V2 - Validation and Business Logic
- V4 - API and Web Service
- V7 - Session Management
- V9 - Self-contained Tokens 
- V13 - Configuration
- V16 - Security Logging and Error Handling

These categories achieved relatively high coverage due to architectural decisions, security requirements, and controls identified during the design phase.

Figure 1 presents the coverage percentage achieved for each ASVS chapter and level during Phase 1.

![Phase 1 - ASVS Coverage by Level](../../../Phase1/ASVSChecklist/ASVSCoverageByLevel.png)

---

## Phase 2 Sprint 1

Phase 2 Sprint 1 marked the transition from design to implementation. During this iteration, the development team focused on implementing the core application features and the security controls identified during Phase 1.

The strongest areas at this stage were:

- V2 - Validation and Business Logic
- V4 - API and Web Service
- V9 - Self-contained Tokens 
- V13 - Configuration
- V16 - Security Logging and Error Handling

Although several security mechanisms were implemented during this sprint, the assessment process became more implementation-driven, evaluating requirements against the functionality actually available in the application rather than planned or architectural controls alone.

Figure 2 presents the coverage percentage achieved for each ASVS chapter and level during Phase 2 Sprint 1

![Phase 2 Sprint 1 - ASVS Coverage by Level](../../Sprint1/ASVSTraceability/ASVSCoverageByLevel.png)

---

## Phase 2 Sprint 2

Phase 2 Sprint 2 represented the final development iteration and the last ASVS assessment performed within the scope of the project.

The strongest areas at this stage were:

- V2 - Validation and Business Logic
- V9 - Self-contained Tokens 
- V13 - Configuration
- V16 - Security Logging and Error Handling

During this sprint, the ASVS checklist was reviewed in greater detail and validated against the final implementation. This reassessment provided a more accurate representation of the security controls effectively implemented, documented, and demonstrable within the project's scope.

Figure 3 presents the coverage percentage achieved for each ASVS chapter and level during Phase 2 Sprint 2.

![Phase 2 Sprint 2 - ASVS Coverage by Level](ASVSCoverageByLevel.png)

---

## Comparative Analysis

Table 1 compares the ASVS coverage percentages obtained during each assessment.

|**Category**|**Phase 1**|**Phase 2 - Sprint 1**|**Phase 2 - Sprint 2**|
|:----------:|:---------:|:--------------------:|:--------------------:|
| V1 – Encoding and Sanitization | 53% | 47% | 47% |
| V2 – Validation and Business Logic | 92% | 85% | 85% |
| V3 – Web Frontend Security | 39% | 32% | 32% |
| V4 – API and Web Service | 56% | 56% | 50% |
| V5 – File Handling | 8% | 8% | 8% |
| V6 – Authentication | 47% | 17% | 17% |
| V7 – Session Management | 63% | 63% | 58%|
| V8 – Authorization | 54% | 54% | 54% |
| V9 – Self-contained Tokens | 100% | 100% | 100% |
| V10 – OAuth and OIDC | 56% | 14% | 14% |
| V11 – Cryptography | 54% | 50% | 50% |
| V12 – Secure Communication | 58% | 58% | 33% |
| V13 – Configuration | 100% | 86% | 76% |
| V14 – Data Protection | 46% | 46% | 38% |
| V15 – Secure Coding and Architecture | 76% | 71% | 62% |
| V16 – Security Logging and Error Handling | 100% | 94% | 71% |
| V17 - WebRTC | 0% | 0% | 0% |

Several categories maintained stable results throughout all assessments, particularly V5 (File Handling), V8 (Authorization), V9 (Self-contained Tokens), and V17 (WebRTC).

Other categories exhibit lower coverage values in later assessments. This behaviour is mainly explained by the progressive refinement of the assessment process. As implementation advanced, requirements were reassessed against concrete evidence, resulting in some controls being reclassified as partially implemented, not implemented, or outside the project's scope.

The most noticeable reductions occurred in Authentication (V6), OAuth and OIDC (V10), Configuration (V13), and Security Logging and Error Handling (V16), reflecting a more rigorous interpretation of ASVS requirements rather than the removal of existing security controls.

---

## Compliance Summary

The three ASVS assessments demonstrate the importance of continuously validating security requirements throughout the software development lifecycle.

Although some categories show lower compliance percentages in later iterations, these results should not be interpreted as a deterioration of the application's security posture. Instead, they reflect the increasing accuracy and maturity of the assessment process as implementation progressed.

Throughout the project, security controls were implemented incrementally and reassessed against the actual state of the application. This resulted in a more evidence-based evaluation, where only controls that could be effectively demonstrated and verified were considered compliant.

Some requirements were intentionally left unimplemented due to project constraints, while others were determined to be outside the scope of the selected architecture and business requirements. Consequently, the final assessment provides the most realistic representation of the application's compliance with OWASP ASVS 5.0.

Overall, the repeated use of ASVS throughout the project enabled systematic security validation, improved traceability between requirements and implementation, and helped identify areas requiring further security investment beyond the project's available timeframe.