-- Fix cooking oils that had fat=0 (USDA reports oil fat under #298 "Total fat (NLEA)", not #204).
-- Sets fat from USDA #298 and calories = 9 kcal/g fat. FK-safe in-place update.
BEGIN;
UPDATE ingredients SET fat=94.5, calories=850.5 WHERE name='Oil, canola';
UPDATE ingredients SET fat=94.0, calories=846.0 WHERE name='Oil, corn';
UPDATE ingredients SET fat=92.9, calories=836.1 WHERE name='Oil, olive, extra light';
UPDATE ingredients SET fat=93.7, calories=843.3 WHERE name='Oil, olive, extra virgin';
UPDATE ingredients SET fat=93.4, calories=840.6 WHERE name='Oil, peanut';
UPDATE ingredients SET fat=93.2, calories=838.8 WHERE name='Oil, safflower';
UPDATE ingredients SET fat=94.6, calories=851.4 WHERE name='Oil, soybean';
UPDATE ingredients SET fat=93.2, calories=838.8 WHERE name='Oil, sunflower';
COMMIT;
