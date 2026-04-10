-- Remove legacy workout_exercise_id from trainee_exercise_logs.
-- The app only maps plan_session_exercise_id (PlanSessionExercise). A leftover NOT NULL column
-- causes inserts to fail with: null value in column "workout_exercise_id" violates not-null constraint
--
-- psql ... -f scripts/migrate-trainee-exercise-logs-drop-workout-exercise-id.sql

BEGIN;

ALTER TABLE trainee_exercise_logs
    DROP COLUMN IF EXISTS workout_exercise_id CASCADE;

COMMIT;
