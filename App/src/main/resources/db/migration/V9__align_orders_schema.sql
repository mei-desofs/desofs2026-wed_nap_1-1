-- Flyway migration: align `orders` table with the Order JPA entity.
-- The schema in V1 predated the addition of auth0_id / status / receiptName /
-- totalPrice on the entity; the divergence was previously masked by Hibernate
-- `create-drop`. With Flyway as the source of truth and `validate` mode in
-- production / CI, the columns must match.

-- The user_id FK in V1 was created without an explicit name; resolve it from
-- information_schema so the DROP works regardless of MySQL's implicit naming.
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

ALTER TABLE orders
    DROP COLUMN user_id,
    DROP COLUMN total,
    ADD COLUMN auth0_id     VARCHAR(255)  NOT NULL DEFAULT '',
    ADD COLUMN status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    ADD COLUMN receipt_name VARCHAR(255)  NOT NULL DEFAULT '',
    ADD COLUMN total_price  DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE INDEX idx_orders_auth0_id ON orders(auth0_id);
CREATE INDEX idx_orders_status   ON orders(status);
