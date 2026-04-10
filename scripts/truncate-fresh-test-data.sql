-- =============================================================================
-- FitCoach — reset all domain data for testing
-- Keeps: users, coaches, trainees (and their coach ↔ user links)
-- Clears: plans, sessions, completions, nutrition, catalog seed rows, invites, etc.
-- =============================================================================
-- Run (adjust URL/user/db to match application.properties):
--   psql "postgresql://admin@localhost:5432/fitcoach_db" -f scripts/truncate-fresh-test-data.sql
-- Or: ./scripts/truncate-fresh-test-data.sh
-- =============================================================================

BEGIN;

-- Join + leaf tables first; CASCADE handles any missed FK edges within this set.
-- Does NOT touch: users, coaches, trainees

TRUNCATE TABLE
    trainee_exercise_logs,
    trainee_workout_completions,
    trainee_meal_completions,
    plan_session_exercises,
    meal_ingredients,
    meals,
    plan_sessions,
    plan_assignments,
    workout_plans,
    nutrition_plan_trainees,
    nutrition_plans,
    invitations,
    measurement_logs,
    progress_pictures,
    exercises,
    ingredients
RESTART IDENTITY CASCADE;

COMMIT;
