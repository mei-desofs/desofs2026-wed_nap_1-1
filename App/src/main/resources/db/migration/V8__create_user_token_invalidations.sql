-- Flyway migration: server-side JWT denylist by user (token freshness check).
-- Used to invalidate all access tokens issued before `invalidated_after`
-- whenever an administrator changes a user's role assignments.
CREATE TABLE user_token_invalidations (
  auth0_user_id VARCHAR(200) PRIMARY KEY,
  invalidated_after TIMESTAMP(3) NOT NULL,
  reason VARCHAR(100) NOT NULL,
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
