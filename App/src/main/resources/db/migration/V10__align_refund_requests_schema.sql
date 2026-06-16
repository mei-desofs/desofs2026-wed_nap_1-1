-- Flyway migration: align `refund_requests` table with the RefundRequest JPA entity.
-- Same root cause as V9: every statement is guarded against the current schema
-- state so the migration is safe to (re)apply on databases where Hibernate
-- already removed legacy columns.

-- 1. Drop the auto-generated FK on user_id (if it still exists).
SET @fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA           = DATABASE()
      AND TABLE_NAME             = 'refund_requests'
      AND COLUMN_NAME            = 'user_id'
      AND REFERENCED_TABLE_NAME  = 'users'
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE refund_requests DROP FOREIGN KEY ', @fk_name),
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Drop legacy user_id column only if present (its index is dropped with it).
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refund_requests' AND COLUMN_NAME = 'user_id'
);
SET @sql := IF(@col_exists > 0, 'ALTER TABLE refund_requests DROP COLUMN user_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Add auth0_id only if missing.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refund_requests' AND COLUMN_NAME = 'auth0_id'
);
SET @sql := IF(@col_exists = 0,
               "ALTER TABLE refund_requests ADD COLUMN auth0_id VARCHAR(255) NOT NULL DEFAULT '' AFTER order_id",
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Create the auth0_id index only if it does not already exist.
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refund_requests' AND INDEX_NAME = 'idx_refund_auth0_id'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_refund_auth0_id ON refund_requests(auth0_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

