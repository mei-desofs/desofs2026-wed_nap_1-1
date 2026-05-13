-- Flyway migration: Add account lockout fields to users table
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_failed_login TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add indices for performance
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_account_locked_until ON users(account_locked_until);
