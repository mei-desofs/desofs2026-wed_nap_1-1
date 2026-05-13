-- Flyway migration: Create MFA tokens table
CREATE TABLE mfa_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL,
  totp_secret TEXT,
  backup_codes JSON,
  is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  email_otp_code VARCHAR(255),
  email_otp_expiry TIMESTAMP NULL,
  last_used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT idx_mfa_is_enabled UNIQUE (user_id) WHERE is_enabled = TRUE
);

-- Add indices for performance
CREATE INDEX idx_mfa_user_id ON mfa_tokens(user_id);
CREATE INDEX idx_mfa_is_enabled ON mfa_tokens(is_enabled);
CREATE INDEX idx_mfa_email_otp_expiry ON mfa_tokens(email_otp_expiry);
CREATE INDEX idx_mfa_last_used_at ON mfa_tokens(last_used_at);
