-- Fix ingredients showing 0 (or NULL) calories despite having macronutrients.
-- Root cause: seeding read energy only from USDA nutrient #957 (Atwater General); many
-- Foundation foods carry energy under #208/#958 or not at all, so calories fell back to 0.
-- Recompute from stored macros using Atwater factors (4/4/9 kcal per g protein/carb/fat).
-- FK-safe: in-place update, no rows added/removed. Mirrors DataSeedingService.resolveCalories().
BEGIN;

UPDATE ingredients
SET calories = 4 * COALESCE(protein, 0)
             + 4 * COALESCE(carbohydrates, 0)
             + 9 * COALESCE(fat, 0)
WHERE calories IS NULL OR calories = 0;

COMMIT;
