# eGameShop Documentation — Phase 1

This document serves as the main index for Phase 1 of the project. Here you will find references to all required artefacts, organized by topic, as well as the evaluation criteria.

## Document Structure

- [Analysis](Analysis/analysis.md): System overview, architecture, and domain model.
- [Dataflow](Dataflow/dataflow.md): Documentation of data flows, DFDs (levels 0, 1, and higher if needed), components, trust boundaries, and external entities.
- [Threat Identification and Analysis](ThreatIdentificationAndAnalysis/threatIdentificationAndAnalysis.md): Threat identification and analysis, STRIDE application, attack vectors, and threat agents.
- [Risk Assessment](RiskAssessment/riskAssessment.md): Risk assessment methodology and prioritization.
- [Mitigations](Mitigations/mitigations.md): Proposed mitigations for identified threats.
- [Requirements](Requirements/requirements.md): Justified security requirements, covering authentication, access control, data security, communication, input validation, third-party components, logging, and monitoring.
- [Security Testing](SecurityTesting/securityTesting.md): Security testing methodology, abuse cases, review process, and ASVS assessment.

## Evaluation Criteria

| Criteria                        | Weight | Excellent (100%)                                                                                                                        |
|----------------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Organization and Language        | 5%     | Well-organized document and repository, easy navigation, all components linked to the main document, no major language errors.           |
| Analysis                        | 10%    | Complete and well-documented system overview, architecture, and domain model; all major components described.                            |
| Dataflow                        | 15%    | Data flows documented in detail; components, flows, trust boundaries, and external entities well identified; DFDs included.              |
| Threat Identification and Analysis | 20%   | Identification of relevant threats, proper STRIDE application, detailed attack vectors and threat agents with abuse cases.               |
| Risk Assessment                 | 10%    | Well-defined and justified risk assessment methodology for prioritization.                                                               |
| Mitigations                     | 10%    | Specific, clear, and feasible mitigations for identified threats, focusing on high-priority ones.                                        |
| Requirements                    | 20%    | Justified security requirements, covering all relevant topics and based on best practices, identified threats, and regulations.          |
| Security Testing                | 10%    | Defined testing methodology, reference to abuse cases, review process, ASVS assessment, and traceability.                                |

Each section above is documented in its respective file and directory. Use this README as the starting point to navigate all Phase 1 documentation.