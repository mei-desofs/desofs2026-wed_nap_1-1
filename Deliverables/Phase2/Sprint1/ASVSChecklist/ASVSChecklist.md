# ASVS Traceability - Phase 2, Sprint 1

Phase 1 delivered a full **ASVS 5.0.0 checklist** covering the controls applicable to eMovieShop. Sprint 1 implementation maps directly to those controls:

| ASVS Chapter | Control | Sprint 1 Implementation |
|---|---|---|
| V3-Authentication | JWT validation, audience check | `SecurityConfig`, `AudienceValidator` |
| V4-Access Control | Role enforcement on every endpoint | `RoleGuard`, controller RBAC matrix |
| V5-Validation | Input allow-lists, Bean Validation | `ReceiptFileService`, `@Valid` DTOs |
| V13-API | Stateless JWT, no session | `SecurityConfig` |
| V14-Configuration | Security headers, HSTS | `SecurityHeadersFilter`, `SecurityConfig` |
| V15-Cryptography | No custom crypto; Auth0 RS256 | Auth0 integration |
| V16-Security Logging | Audit trail for admin role changes | `AuditLog`, `AuditLogService` |

> **Full ASVS checklist:** [Phase 1 ASVS Tracker](../../../Phase1/ASVSChecklist/)
