-- =============================================================================
-- Fix: trainee_workout_completions still has legacy NOT NULL workout_id while
-- the app now persists plan_session_id (UUID → plan_sessions) only.
-- Hibernate ddl-auto=update often adds plan_session_id but does not drop workout_id.
-- =============================================================================
-- Run:
--   psql ... -f scripts/migrate-trainee-workout-completions-plan-session.sql
-- =============================================================================

BEGIN;

-- Drop FK from workout_id → old workouts table (name may vary)
ALTER TABLE trainee_workout_completions
    DROP CONSTRAINT IF EXISTS trainee_workout_completions_workout_id_fkey;

-- Drop any unique constraint that still references workout_id
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'trainee_workout_completions'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) LIKE '%workout_id%'
    ) LOOP
        EXECUTE format('ALTER TABLE trainee_workout_completions DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;

ALTER TABLE trainee_workout_completions
    DROP COLUMN IF EXISTS workout_id;

-- Ensure plan_session_id exists and references plan_sessions (adjust if already correct)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'trainee_workout_completions'
          AND column_name = 'plan_session_id'
    ) THEN
        ALTER TABLE trainee_workout_completions
            ADD COLUMN plan_session_id UUID REFERENCES plan_sessions(id);
    END IF;
END $$;

-- Optional: clear orphaned rows before NOT NULL (dev)
-- TRUNCATE TABLE trainee_workout_completions;

ALTER TABLE trainee_workout_completions
    ALTER COLUMN plan_session_id SET NOT NULL;

-- Recreate unique constraint expected by JPA (short name — PG truncates long identifiers)
ALTER TABLE trainee_workout_completions
    DROP CONSTRAINT IF EXISTS uk_twc_trainee_session_date;

ALTER TABLE trainee_workout_completions
    ADD CONSTRAINT uk_twc_trainee_session_date
        UNIQUE (trainee_id, plan_session_id, completion_date);

COMMIT;
