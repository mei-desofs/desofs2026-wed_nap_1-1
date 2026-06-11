-- Flyway migration: align `refund_requests` table with the RefundRequest JPA entity.
-- Same root cause as V9: the entity uses `auth0_id` (String, post-Auth0 migration)
-- while V5 created `user_id` (BIGINT FK to users). Drift was masked by
-- Hibernate `ddl-auto=create-drop`; with `validate` the missing column blocks
-- startup.

-- Resolve the auto-generated FK name on user_id and drop it.
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

-- The legacy index on user_id is dropped automatically when the column is dropped.
ALTER TABLE refund_requests
    DROP COLUMN user_id,
    ADD COLUMN auth0_id VARCHAR(255) NOT NULL DEFAULT '' AFTER order_id;

CREATE INDEX idx_refund_auth0_id ON refund_requests(auth0_id);
