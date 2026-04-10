-- Per-set workout logging: trainee_exercise_set_logs + missed_sets on trainee_exercise_logs.
-- Run if you use ddl-auto=validate or manage schema manually (Hibernate update usually creates these).
-- psql ... -f scripts/migrate-trainee-exercise-set-logs.sql

BEGIN;

ALTER TABLE trainee_exercise_logs
    ADD COLUMN IF NOT EXISTS missed_sets INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS trainee_exercise_set_logs (
    id                      BIGSERIAL PRIMARY KEY,
    trainee_exercise_log_id BIGINT NOT NULL REFERENCES trainee_exercise_logs(id) ON DELETE CASCADE,
    set_number              INTEGER NOT NULL,
    outcome                 VARCHAR(20) NOT NULL,
    reason                  VARCHAR(500),
    CONSTRAINT uk_tesl_log_set UNIQUE (trainee_exercise_log_id, set_number)
);

CREATE INDEX IF NOT EXISTS idx_tesl_exercise_log ON trainee_exercise_set_logs(trainee_exercise_log_id);

COMMIT;
