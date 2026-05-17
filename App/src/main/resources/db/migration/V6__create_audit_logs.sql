-- Flyway migration: Create audit logs table
CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  actor_id VARCHAR(255) NOT NULL,
  target_user_id VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL,
  operation VARCHAR(100) NOT NULL,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_logs_actor_id (actor_id),
  INDEX idx_audit_logs_target_user_id (target_user_id),
  INDEX idx_audit_logs_role (role),
  INDEX idx_audit_logs_timestamp (timestamp)
);
