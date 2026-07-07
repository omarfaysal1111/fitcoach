package com.fitcoach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.domain.entity.Exercise;
import com.fitcoach.domain.entity.Ingredient;
import com.fitcoach.dto.response.CatalogSeedResponse;
import com.fitcoach.repository.ExerciseRepository;
import com.fitcoach.repository.IngredientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DataSeedingService implements CommandLineRunner {

    private static final String WGER_BASE = "https://wger.de/api/v2";
    private static final int PAGE_SIZE = 100;
    private static final String EXERCISEDB_JSON =
            "https://raw.githubusercontent.com/bootstrapping-lab/exercisedb-api/main/src/data/exercises.json";

    // USDA FDC Foundation Foods API
    private static final String USDA_FDC_API_KEY = "mLBKvhwX2xRVLpcswGHJmAxgvTB14Folwkhw9SSs";
    private static final String USDA_FDC_BASE =
            "https://api.nal.usda.gov/fdc/v1/foods/list?dataType=Foundation&pageSize=200&api_key=" + USDA_FDC_API_KEY;

    // Nutrient numbers we care about (USDA standard codes).
    // Energy is reported under one of several numbers depending on the food; try them in order
    // (all kcal). #268 is Energy in kJ and must NOT be used here.
    private static final String[] NUTRIENT_CALORIES = {
            "957", // Energy – Atwater General Factors (kcal)
            "958", // Energy – Atwater Specific Factors (kcal)
            "208"  // Energy (kcal) – classic; many Foundation foods only carry this one
    };
    // Fat is usually #204, but some foods (e.g. oils) only report #298 "Total fat (NLEA)".
    private static final String[] NUTRIENT_FAT = {"204", "298"};
    private static final String NUTRIENT_WATER        = "255"; // Water
    private static final String NUTRIENT_CARBS        = "205"; // Carbohydrate, by difference
    private static final String NUTRIENT_PROTEIN      = "203"; // Protein
    private static final String NUTRIENT_MINERALS     = "207"; // Ash (total minerals)

    private final ExerciseRepository exerciseRepository;
    private final IngredientRepository ingredientRepository;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public DataSeedingService(
            ExerciseRepository exerciseRepository,
            IngredientRepository ingredientRepository,
            PlatformTransactionManager transactionManager) {
        this.exerciseRepository = exerciseRepository;
        this.ingredientRepository = ingredientRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        if (exerciseRepository.count() == 0) {
            int n = fetchAndPersistExercises();
            log.info("Startup: seeded {} exercises from ExerciseDB.", n);
        } else {
            int fixed = migrateWgerVideoLinks();
            if (fixed > 0) {
                log.info("Startup: migrated {} wger video links → ExerciseDB gif URLs.", fixed);
            } else {
                log.info("Startup: no stale video links found — skipping migration.");
            }
        }
        if (ingredientRepository.count() == 0) {
            int n = fetchAndPersistIngredients();
            log.info("Startup: seeded {} ingredients from USDA FDC.", n);
        } else {
            log.info("Ingredients already present — skipping startup seed.");
        }
    }

    /**
     * Fetches from wger and persists rows. If {@code replaceExisting}, clears catalog tables first
     * (fails if exercises/ingredients are still referenced by plans or meals).
     */
    public CatalogSeedResponse seedCatalog(boolean replaceExisting) {
        if (replaceExisting) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    exerciseRepository.deleteAllInBatch();
                    ingredientRepository.deleteAllInBatch();
                });
            } catch (Exception e) {
                log.error("Could not clear catalog: {}", e.getMessage());
                return CatalogSeedResponse.builder()
                        .exercisesSaved(0)
                        .ingredientsSaved(0)
                        .replacedExisting(true)
                        .detail("Clear failed (likely FK from meals/plan lines). Truncate those tables or use replace=false when catalog is empty. "
                                + e.getMessage())
                        .build();
            }
        }

        long exercisesBefore = exerciseRepository.count();
        long ingredientsBefore = ingredientRepository.count();

        int exercisesSaved = 0;
        int ingredientsSaved = 0;

        if (replaceExisting || exercisesBefore == 0) {
            exercisesSaved = fetchAndPersistExercises();
        } else {
            log.info("Exercises not empty — skipped (pass replace=true to re-download).");
        }

        if (replaceExisting || ingredientsBefore == 0) {
            ingredientsSaved = fetchAndPersistIngredients();
        } else {
            log.info("Ingredients not empty — skipped (pass replace=true to re-download).");
        }

        String detail = "OK";
        if ((replaceExisting || exercisesBefore == 0) && exercisesSaved == 0) {
            detail = "Exercise seed produced 0 rows — check logs / network / wger.";
        } else if ((replaceExisting || ingredientsBefore == 0) && ingredientsSaved == 0) {
            detail = "Ingredient seed produced 0 rows — check logs / network / USDA FDC.";
        }

        return CatalogSeedResponse.builder()
                .exercisesSaved(exercisesSaved)
                .ingredientsSaved(ingredientsSaved)
                .replacedExisting(replaceExisting)
                .detail(detail)
                .build();
    }

    /**
     * For exercises already in the DB that still have wger/null videoLinks, fetch the ExerciseDB
     * dataset and assign the correct gifUrl by <em>exact</em> (normalized) name match only.
     *
     * <p>Historically this did fuzzy substring + muscle-group fallback matching, which forced a
     * video onto every exercise even with no real match — attaching the same demonstration GIF to
     * dozens of unrelated movements. That produced the "name and video don't match" corruption.
     * A wrong video is worse than none, so unmatched exercises are now left untouched. To give the
     * whole catalog correct videos, use {@link #reseedExercisesFromExerciseDB()} instead.
     *
     * <p>Updates videoLink in-place — does NOT delete rows, so FK references are safe.
     */
    private int migrateWgerVideoLinks() {
        List<Exercise> stale = exerciseRepository.findAll().stream()
                .filter(e -> e.getVideoLink() == null || e.getVideoLink().contains("wger.de"))
                .toList();
        if (stale.isEmpty()) return 0;

        log.info("Migrating {} exercises with wger/null video links (exact match only)...", stale.size());
        List<Map<String, Object>> raw = fetchExerciseDbDataset();
        if (raw == null || raw.isEmpty()) {
            log.warn("ExerciseDB dataset unavailable — skipping migration.");
            return 0;
        }

        Map<String, String> gifByName = new java.util.HashMap<>();
        for (Map<String, Object> ex : raw) {
            String name = (String) ex.get("name");
            String gif  = (String) ex.get("gifUrl");
            if (name != null && gif != null) gifByName.putIfAbsent(normalizeName(name), gif);
        }

        int updated = 0;
        for (Exercise exercise : stale) {
            String gif = gifByName.get(normalizeName(exercise.getName()));
            if (gif != null) {
                exercise.setVideoLink(gif);
                updated++;
            }
        }
        exerciseRepository.saveAll(stale);
        return updated;
    }

    /**
     * Legacy exercise names that have no entry in the ExerciseDB dataset, mapped to the closest
     * real ExerciseDB movement so referenced (FK-protected) rows can still get a correct video.
     * Keyed by the raw lower-cased legacy name; values are ExerciseDB names (matched normalized).
     */
    private static final Map<String, String> LEGACY_REMAP = Map.ofEntries(
            Map.entry("bear walk 2",                  "bear crawl"),
            Map.entry("cable rear delt fly",          "cable supine reverse fly"),
            Map.entry("axe hold",                     "band horizontal pallof press"),
            Map.entry("tricep pushdown on cable",     "cable triceps pushdown (v-bar)"),
            Map.entry("step-ups",                     "dumbbell step-up"),
            Map.entry("commando pull-ups",            "pull-up"),
            Map.entry("abdominal stabilization",      "weighted front plank"),
            Map.entry("zone 2 running",               "stationary bike run"),
            Map.entry("коробочный присед",            "barbell full squat"),
            Map.entry("elliptical",                   "walk elliptical cross trainer"),
            Map.entry("high knees",                   "high knee against wall"),
            Map.entry("4-count burpees",              "burpee"),
            Map.entry("left levator scapulae stretch","neck side stretch"),
            Map.entry("knee to chest stretch",        "assisted lying glutes stretch")
    );

    /**
     * Rebuilds the exercise catalog from the ExerciseDB dataset so every exercise has a video that
     * genuinely matches its name (each gifUrl is keyed to that exact movement).
     *
     * <p>FK-safe: exercises referenced by plans/workouts are updated in place (IDs preserved) to
     * their matched ExerciseDB entry — by exact name, else via {@link #LEGACY_REMAP}. All other
     * existing exercises are deleted, then the remaining ExerciseDB movements are inserted fresh.
     */
    public CatalogSeedResponse reseedExercisesFromExerciseDB() {
        List<Map<String, Object>> raw = fetchExerciseDbDataset();
        if (raw == null || raw.isEmpty()) {
            return CatalogSeedResponse.builder()
                    .exercisesSaved(0).ingredientsSaved(0).replacedExisting(true)
                    .detail("ExerciseDB dataset unavailable — no changes made.").build();
        }

        Map<String, Map<String, Object>> byName = new java.util.HashMap<>();
        for (Map<String, Object> ex : raw) {
            String name = (String) ex.get("name");
            String gif  = (String) ex.get("gifUrl");
            if (name != null && gif != null) byName.putIfAbsent(normalizeName(name), ex);
        }

        int[] counts = transactionTemplate.execute(status -> {
            java.util.Set<Long> referenced = collectReferencedExerciseIds();
            java.util.Set<String> consumed = new java.util.HashSet<>();

            List<Exercise> keep = new ArrayList<>();
            List<Exercise> remove = new ArrayList<>();
            int remapped = 0;
            for (Exercise e : exerciseRepository.findAll()) {
                if (!referenced.contains(e.getId())) {
                    remove.add(e);
                    continue;
                }
                Map<String, Object> match = resolveDatasetMatch(e.getName(), byName);
                if (match != null) {
                    applyDatasetFields(e, match);
                    consumed.add(normalizeName((String) match.get("name")));
                    remapped++;
                } // else: no match — leave the referenced row's current data as-is
                keep.add(e);
            }

            exerciseRepository.deleteAllInBatch(remove);
            exerciseRepository.saveAll(keep);

            List<Exercise> fresh = new ArrayList<>();
            for (Map<String, Object> ex : raw) {
                String name = (String) ex.get("name");
                if (name == null || name.isBlank()) continue;
                if (consumed.contains(normalizeName(name))) continue; // already folded into a kept row
                fresh.add(toExercise(ex));
            }
            exerciseRepository.saveAll(fresh);

            log.info("Reseed: kept/remapped {} referenced ({} remapped), deleted {}, inserted {} fresh.",
                    keep.size(), remapped, remove.size(), fresh.size());
            return new int[]{keep.size(), remove.size(), fresh.size(), remapped};
        });

        int total = counts[0] + counts[2];
        return CatalogSeedResponse.builder()
                .exercisesSaved(total)
                .ingredientsSaved(0)
                .replacedExisting(true)
                .detail(String.format(
                        "Reseeded from ExerciseDB: %d total exercises (%d referenced rows preserved, %d remapped; %d deleted; %d inserted).",
                        total, counts[0], counts[3], counts[1], counts[2]))
                .build();
    }

    /**
     * IDs of exercises referenced by any FK table, so they can be updated in place rather than
     * deleted. Tolerates schema drift: candidate tables that don't exist in this environment
     * (checked via {@code to_regclass}) are skipped instead of aborting the whole reseed.
     */
    private java.util.Set<Long> collectReferencedExerciseIds() {
        List<String> candidateTables = List.of(
                "plan_session_exercises", "workout_exercises", "workout_exercise_items");
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (String table : candidateTables) {
            Object reg = entityManager
                    .createNativeQuery("SELECT to_regclass('public." + table + "')")
                    .getSingleResult();
            if (reg == null) continue; // table absent in this environment — skip
            @SuppressWarnings("unchecked")
            List<Number> rows = entityManager
                    .createNativeQuery("SELECT DISTINCT exercise_id FROM " + table + " WHERE exercise_id IS NOT NULL")
                    .getResultList();
            for (Number n : rows) {
                if (n != null) ids.add(n.longValue());
            }
        }
        return ids;
    }

    /** Exact (normalized) name match, else the hand-curated legacy remap, else null. */
    private Map<String, Object> resolveDatasetMatch(String oldName, Map<String, Map<String, Object>> byName) {
        if (oldName == null) return null;
        Map<String, Object> exact = byName.get(normalizeName(oldName));
        if (exact != null) return exact;
        String remap = LEGACY_REMAP.get(oldName.trim().toLowerCase());
        return remap == null ? null : byName.get(normalizeName(remap));
    }

    /** Lower-cases, strips punctuation to spaces, and collapses whitespace for tolerant matching. */
    private static String normalizeName(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private int fetchAndPersistExercises() {
        log.info("Fetching exercises from ExerciseDB dataset...");
        List<Map<String, Object>> raw = fetchExerciseDbDataset();
        if (raw == null || raw.isEmpty()) {
            log.warn("ExerciseDB dataset returned empty response.");
            return 0;
        }

        List<Exercise> exercises = new ArrayList<>();
        for (Map<String, Object> ex : raw) {
            String name = (String) ex.get("name");
            if (name == null || name.isBlank()) continue;
            exercises.add(toExercise(ex));
        }

        exerciseRepository.saveAll(exercises);
        log.info("Persisted {} exercises from ExerciseDB dataset.", exercises.size());
        return exercises.size();
    }

    /** Fetches and parses the ExerciseDB dataset, or {@code null} on any failure. */
    private List<Map<String, Object>> fetchExerciseDbDataset() {
        try {
            String json = new RestTemplate().getForObject(EXERCISEDB_JSON, String.class);
            if (json == null || json.isBlank()) return null;
            return new ObjectMapper().readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to fetch ExerciseDB dataset: {}", e.getMessage(), e);
            return null;
        }
    }

    /** Builds a new {@link Exercise} from a raw ExerciseDB record (name and gifUrl always paired). */
    private static Exercise toExercise(Map<String, Object> ex) {
        Exercise e = new Exercise();
        applyDatasetFields(e, ex);
        return e;
    }

    /** Overwrites an exercise's catalog fields from a raw ExerciseDB record. */
    @SuppressWarnings("unchecked")
    private static void applyDatasetFields(Exercise e, Map<String, Object> ex) {
        String name = (String) ex.get("name");
        List<String> targetMuscles = (List<String>) ex.get("targetMuscles");
        List<String> bodyParts     = (List<String>) ex.get("bodyParts");
        String muscle = "General";
        if (targetMuscles != null && !targetMuscles.isEmpty()) {
            muscle = capitalize(targetMuscles.get(0));
        } else if (bodyParts != null && !bodyParts.isEmpty()) {
            muscle = capitalize(bodyParts.get(0));
        }

        e.setName(capitalize(name.trim()));
        e.setDescription(buildDescription((List<String>) ex.get("instructions")));
        e.setTargetedMuscle(muscle);
        e.setVideoLink((String) ex.get("gifUrl"));
    }

    private static String buildDescription(List<String> instructions) {
        if (instructions == null || instructions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<ol>");
        for (String step : instructions) {
            String text = step.replaceAll("(?i)^Step:\\d+\\s*", "").trim();
            sb.append("<li>").append(text).append("</li>");
        }
        sb.append("</ol>");
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private int fetchAndPersistIngredients() {
        log.info("Fetching Foundation food ingredients from USDA FDC...");
        List<Map<String, Object>> items = fetchUsdaFoundationFoods();
        if (items.isEmpty()) {
            log.warn("USDA FDC returned no Foundation foods — nothing seeded.");
            return 0;
        }

        List<Ingredient> ingredients = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Ingredient ing = new Ingredient();
            if (applyIngredientFields(ing, item)) {
                ing.setServingQuantityG(100.0);
                ingredients.add(ing);
            }
        }

        ingredientRepository.saveAll(ingredients);
        log.info("Persisted {} ingredients from USDA FDC.", ingredients.size());
        return ingredients.size();
    }

    /**
     * Recomputes nutrition for existing ingredients that were seeded with the old, buggy nutrient
     * extraction (energy only from #957, fat only from #204) — the source of "0 calories" rows and
     * oils with 0 fat. FK-safe: updates rows in place by name match, never adds/removes rows.
     *
     * <p>Each ingredient matched to its USDA Foundation record (by name) is refreshed with correct
     * energy (#957→#958→#208, else Atwater) and fat (#204→#298). Ingredients with no USDA match
     * still get an Atwater calorie estimate from their stored macros if calories are missing.
     *
     * @return number of ingredients whose nutrition changed
     */
    public int refreshIngredientNutrition() {
        Map<String, Map<String, Object>> byName = new java.util.HashMap<>();
        for (Map<String, Object> item : fetchUsdaFoundationFoods()) {
            String name = (String) item.get("description");
            if (name != null && !name.isBlank()) byName.putIfAbsent(normalizeName(name), item);
        }

        List<Ingredient> all = ingredientRepository.findAll();
        int updated = 0;
        for (Ingredient ing : all) {
            boolean changed = false;

            Map<String, Object> match = byName.get(normalizeName(ing.getName()));
            if (match != null) {
                changed = applyIngredientFields(ing, match);
            }

            // Fallback for rows with no USDA match: derive calories from stored macros.
            Double cal = ing.getCalories();
            if (cal == null || cal == 0) {
                double atwater = 4 * nz(ing.getProtein()) + 4 * nz(ing.getCarbohydrates()) + 9 * nz(ing.getFat());
                if (atwater > 0) {
                    ing.setCalories(atwater);
                    changed = true;
                }
            }

            if (changed) updated++;
        }

        ingredientRepository.saveAll(all);
        log.info("refreshIngredientNutrition: updated {} of {} ingredients.", updated, all.size());
        return updated;
    }

    /** Fetches every USDA FDC Foundation food across all pages; empty list on failure. */
    private List<Map<String, Object>> fetchUsdaFoundationFoods() {
        List<Map<String, Object>> all = new ArrayList<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            int pageNumber = 1;
            while (true) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> page =
                        restTemplate.getForObject(USDA_FDC_BASE + "&pageNumber=" + pageNumber, List.class);
                if (page == null || page.isEmpty()) break;
                all.addAll(page);
                if (page.size() < 200) break;
                pageNumber++;
            }
        } catch (Exception e) {
            log.error("Failed to fetch USDA FDC Foundation foods: {}", e.getMessage(), e);
        }
        return all;
    }

    /**
     * Populates an ingredient's nutrition fields from a raw USDA food record.
     * @return true if the record had a usable name (fields applied), false to skip it
     */
    @SuppressWarnings("unchecked")
    private static boolean applyIngredientFields(Ingredient ing, Map<String, Object> item) {
        String name = (String) item.get("description");
        if (name == null || name.isBlank()) return false;

        List<Map<String, Object>> n = (List<Map<String, Object>>) item.get("foodNutrients");
        Double fat      = extractFirst(n, NUTRIENT_FAT);
        Double water    = extractNutrient(n, NUTRIENT_WATER);
        Double carbs    = extractNutrient(n, NUTRIENT_CARBS);
        Double protein  = extractNutrient(n, NUTRIENT_PROTEIN);
        Double minerals = extractNutrient(n, NUTRIENT_MINERALS);
        Double calories = resolveCalories(n, protein, carbs, fat);

        ing.setName(name.trim());
        ing.setFat(fat);
        ing.setWater(water);
        ing.setCarbohydrates(carbs);
        ing.setProtein(protein);
        ing.setTotalMinerals(minerals);
        ing.setCalories(calories);
        return true;
    }

    private static double nz(Double v) {
        return v == null ? 0 : v;
    }

    /**
     * Resolves energy (kcal): first reported energy number in {@link #NUTRIENT_CALORIES} priority
     * order, else the Atwater estimate from macros (4·protein + 4·carbs + 9·fat). Prevents the
     * "0 calories despite having macros" rows that arose when only one energy number was checked.
     */
    private static Double resolveCalories(List<Map<String, Object>> nutrients,
                                          Double protein, Double carbs, Double fat) {
        Double kcal = extractFirst(nutrients, NUTRIENT_CALORIES);
        if (kcal != null && kcal > 0) return kcal;
        double p = protein == null ? 0 : protein;
        double c = carbs   == null ? 0 : carbs;
        double f = fat     == null ? 0 : fat;
        return 4 * p + 4 * c + 9 * f;
    }

    /** Returns the first present, positive nutrient amount among {@code numbers}, else 0.0. */
    private static Double extractFirst(List<Map<String, Object>> nutrients, String... numbers) {
        for (String number : numbers) {
            Double v = extractNutrient(nutrients, number);
            if (v != null && v > 0) return v;
        }
        return 0.0;
    }

    /**
     * Finds a nutrient by its USDA number string and returns its amount, or 0.0 if absent.
     */
    private static Double extractNutrient(List<Map<String, Object>> nutrients, String number) {
        if (nutrients == null) {
            return 0.0;
        }
        for (Map<String, Object> n : nutrients) {
            if (number.equals(n.get("number"))) {
                return extractDouble(n.get("amount"));
            }
        }
        return 0.0;
    }

    private static Double extractDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                /* fall through */
            }
        }
        return 0.0;
    }

    /**
     * Iterates all exercises whose videoLink still points to the wger API list endpoint
     * (i.e. contains "/api/v2/video/?exercise=") and replaces it with the actual video file URL
     * by calling that endpoint and reading results[0].video.
     *
     * @return number of records updated
     */
    public int refreshVideoLinks() {
        List<Exercise> all = exerciseRepository.findAll();
        RestTemplate restTemplate = new RestTemplate();
        int updated = 0;

        for (Exercise exercise : all) {
            String link = exercise.getVideoLink();
            if (link == null || !link.contains("/video/?exercise=")) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.getForObject(link, Map.class);
                if (response == null) continue;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (results == null || results.isEmpty()) {
                    exercise.setVideoLink(null);
                } else {
                    Object videoUrl = results.get(0).get("video");
                    exercise.setVideoLink(videoUrl instanceof String ? (String) videoUrl : null);
                }
                updated++;
            } catch (Exception e) {
                log.warn("Could not resolve video link for exercise {}: {}", exercise.getId(), e.getMessage());
            }
        }

        if (updated > 0) {
            exerciseRepository.saveAll(all);
        }
        log.info("refreshVideoLinks: updated {} exercise video links.", updated);
        return updated;
    }
}
