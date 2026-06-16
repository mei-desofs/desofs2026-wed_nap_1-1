-- Flyway migration: align `orders` table with the Order JPA entity.
-- Each statement is guarded against the current schema state so the migration
-- is safe to (re)apply on databases where Hibernate `ddl-auto=update` already
-- removed legacy columns or where columns were never present.

-- 1. Drop the auto-generated FK on user_id (if it still exists).
SET @fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA       = DATABASE()
      AND TABLE_NAME         = 'orders'
      AND COLUMN_NAME        = 'user_id'
      AND REFERENCED_TABLE_NAME = 'users'
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE orders DROP FOREIGN KEY ', @fk_name),
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Drop legacy columns only if they exist.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'user_id'
);
SET @sql := IF(@col_exists > 0, 'ALTER TABLE orders DROP COLUMN user_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'total'
);
SET @sql := IF(@col_exists > 0, 'ALTER TABLE orders DROP COLUMN total', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Add new columns only if they are missing.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'auth0_id'
);
SET @sql := IF(@col_exists = 0,
               "ALTER TABLE orders ADD COLUMN auth0_id VARCHAR(255) NOT NULL DEFAULT ''",
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'status'
);
SET @sql := IF(@col_exists = 0,
               "ALTER TABLE orders ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'",
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'receipt_name'
);
SET @sql := IF(@col_exists = 0,
               "ALTER TABLE orders ADD COLUMN receipt_name VARCHAR(255) NOT NULL DEFAULT ''",
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'total_price'
);
SET @sql := IF(@col_exists = 0,
               'ALTER TABLE orders ADD COLUMN total_price DECIMAL(10,2) NOT NULL DEFAULT 0',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Create indexes only if they do not already exist.
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_auth0_id'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_orders_auth0_id ON orders(auth0_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_status'
);
SET @sql := IF(@idx_exists = 0, 'CREATE INDEX idx_orders_status ON orders(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

