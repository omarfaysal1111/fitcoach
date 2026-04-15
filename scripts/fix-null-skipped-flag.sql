-- Fix legacy rows where is_skipped is NULL.
-- Run once against your Postgres DB (fitcoach_db).

UPDATE trainee_meal_completions
SET is_skipped = FALSE
WHERE is_skipped IS NULL;

-- Optional hardening (run after confirming no NULLs remain):
-- ALTER TABLE trainee_meal_completions
--   ALTER COLUMN is_skipped SET DEFAULT FALSE,
--   ALTER COLUMN is_skipped SET NOT NULL;

