package com.fitcoach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitcoach.domain.entity.Exercise;
import com.fitcoach.domain.entity.Ingredient;
import com.fitcoach.dto.response.CatalogSeedResponse;
import com.fitcoach.repository.ExerciseRepository;
import com.fitcoach.repository.IngredientRepository;
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

    // Nutrient numbers we care about (USDA standard codes)
    private static final String NUTRIENT_CALORIES     = "957"; // Energy – Atwater General Factors (kcal)
    private static final String NUTRIENT_FAT          = "204"; // Total lipid (fat)
    private static final String NUTRIENT_WATER        = "255"; // Water
    private static final String NUTRIENT_CARBS        = "205"; // Carbohydrate, by difference
    private static final String NUTRIENT_PROTEIN      = "203"; // Protein
    private static final String NUTRIENT_MINERALS     = "207"; // Ash (total minerals)

    private final ExerciseRepository exerciseRepository;
    private final IngredientRepository ingredientRepository;
    private final TransactionTemplate transactionTemplate;

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
     * For exercises already in the DB that still have wger/null videoLinks,
     * fetch the ExerciseDB dataset and assign gifUrls using a three-tier strategy:
     *   1. Exact name match (case-insensitive)
     *   2. Partial name match (one name contains the other)
     *   3. Muscle-group fallback (any exercisedb GIF targeting the same muscle)
     * Updates videoLink in-place — does NOT delete rows, so FK references are safe.
     */
    @SuppressWarnings("unchecked")
    private int migrateWgerVideoLinks() {
        List<Exercise> stale = exerciseRepository.findAll().stream()
                .filter(e -> e.getVideoLink() == null || e.getVideoLink().contains("wger.de"))
                .toList();
        if (stale.isEmpty()) return 0;

        log.info("Migrating {} exercises with wger/null video links...", stale.size());
        try {
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(EXERCISEDB_JSON, String.class);
            if (json == null || json.isBlank()) {
                log.warn("ExerciseDB dataset unavailable — skipping migration.");
                return 0;
            }
            List<Map<String, Object>> raw =
                    new ObjectMapper().readValue(json, new TypeReference<>() {});
            if (raw.isEmpty()) {
                log.warn("ExerciseDB dataset was empty — skipping migration.");
                return 0;
            }

            // Pre-build lookup structures
            Map<String, String> gifByExactName = new java.util.HashMap<>();
            Map<String, List<String>> gifsByMuscle = new java.util.HashMap<>();

            for (Map<String, Object> ex : raw) {
                String name = (String) ex.get("name");
                String gif  = (String) ex.get("gifUrl");
                if (name == null || gif == null) continue;

                gifByExactName.put(name.trim().toLowerCase(), gif);

                List<String> targetMuscles = (List<String>) ex.get("targetMuscles");
                List<String> bodyParts     = (List<String>) ex.get("bodyParts");
                List<String> muscleKeys    = new ArrayList<>();
                if (targetMuscles != null) muscleKeys.addAll(targetMuscles);
                if (bodyParts     != null) muscleKeys.addAll(bodyParts);
                for (String m : muscleKeys) {
                    gifsByMuscle.computeIfAbsent(m.toLowerCase(), k -> new ArrayList<>()).add(gif);
                }
            }

            // Muscle mapping: wger category names → exercisedb muscle/bodypart keywords
            Map<String, List<String>> muscleMap = new java.util.HashMap<>();
            muscleMap.put("legs",      List.of("upper legs", "lower legs", "quads", "hamstrings", "glutes", "calves"));
            muscleMap.put("arms",      List.of("upper arms", "forearms", "biceps", "triceps"));
            muscleMap.put("back",      List.of("back", "lats", "spine", "traps"));
            muscleMap.put("chest",     List.of("chest", "pectorals"));
            muscleMap.put("shoulders", List.of("shoulders", "delts"));
            muscleMap.put("abs",       List.of("waist", "abs"));
            muscleMap.put("cardio",    List.of("cardio", "waist"));
            muscleMap.put("general",   List.of("upper legs", "chest", "back"));

            int updated = 0;
            for (Exercise exercise : stale) {
                String nameLower = exercise.getName() == null ? "" : exercise.getName().trim().toLowerCase();

                // Tier 1: exact name
                String gif = gifByExactName.get(nameLower);

                // Tier 2: partial name match
                if (gif == null) {
                    for (Map.Entry<String, String> entry : gifByExactName.entrySet()) {
                        if (entry.getKey().contains(nameLower) || nameLower.contains(entry.getKey())) {
                            gif = entry.getValue();
                            break;
                        }
                    }
                }

                // Tier 3: muscle-group fallback
                if (gif == null) {
                    String muscle = exercise.getTargetedMuscle() == null
                            ? "general" : exercise.getTargetedMuscle().trim().toLowerCase();
                    List<String> keywords = muscleMap.getOrDefault(muscle, muscleMap.get("general"));
                    outer:
                    for (String keyword : keywords) {
                        List<String> candidates = gifsByMuscle.get(keyword);
                        if (candidates != null && !candidates.isEmpty()) {
                            gif = candidates.get(updated % candidates.size());
                            break outer;
                        }
                    }
                }

                exercise.setVideoLink(gif);
                updated++;
            }

            exerciseRepository.saveAll(stale);
            return updated;

        } catch (Exception e) {
            log.error("Failed to migrate wger video links: {}", e.getMessage(), e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private int fetchAndPersistExercises() {
        log.info("Fetching exercises from ExerciseDB dataset...");
        try {
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(EXERCISEDB_JSON, String.class);
            if (json == null || json.isBlank()) {
                log.warn("ExerciseDB dataset returned empty response.");
                return 0;
            }
            List<Map<String, Object>> raw =
                    new ObjectMapper().readValue(json, new TypeReference<>() {});
            if (raw.isEmpty()) {
                log.warn("ExerciseDB dataset was empty.");
                return 0;
            }

            List<Exercise> exercises = new ArrayList<>();
            for (Map<String, Object> ex : raw) {
                String name = (String) ex.get("name");
                if (name == null || name.isBlank()) continue;

                List<String> instructions = (List<String>) ex.get("instructions");
                String description = buildDescription(instructions);

                String gifUrl = (String) ex.get("gifUrl");

                List<String> targetMuscles = (List<String>) ex.get("targetMuscles");
                List<String> bodyParts    = (List<String>) ex.get("bodyParts");
                String muscle = "General";
                if (targetMuscles != null && !targetMuscles.isEmpty()) {
                    muscle = capitalize(targetMuscles.get(0));
                } else if (bodyParts != null && !bodyParts.isEmpty()) {
                    muscle = capitalize(bodyParts.get(0));
                }

                exercises.add(Exercise.builder()
                        .name(capitalize(name.trim()))
                        .description(description)
                        .targetedMuscle(muscle)
                        .videoLink(gifUrl)
                        .build());
            }

            exerciseRepository.saveAll(exercises);
            log.info("Persisted {} exercises from ExerciseDB dataset.", exercises.size());
            return exercises.size();

        } catch (Exception e) {
            log.error("Failed to seed exercises from ExerciseDB dataset: {}", e.getMessage(), e);
            return 0;
        }
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
        try {
            RestTemplate restTemplate = new RestTemplate();
            List<Ingredient> ingredients = new ArrayList<>();
            int pageNumber = 1;

            while (true) {
                String url = USDA_FDC_BASE + "&pageNumber=" + pageNumber;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> page = restTemplate.getForObject(url, List.class);

                if (page == null || page.isEmpty()) {
                    break;
                }

                for (Map<String, Object> item : page) {
                    String name = (String) item.get("description");
                    if (name == null || name.isBlank()) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> foodNutrients =
                            (List<Map<String, Object>>) item.get("foodNutrients");

                    Double calories     = extractNutrient(foodNutrients, NUTRIENT_CALORIES);
                    Double fat          = extractNutrient(foodNutrients, NUTRIENT_FAT);
                    Double water        = extractNutrient(foodNutrients, NUTRIENT_WATER);
                    Double carbs        = extractNutrient(foodNutrients, NUTRIENT_CARBS);
                    Double protein      = extractNutrient(foodNutrients, NUTRIENT_PROTEIN);
                    Double minerals     = extractNutrient(foodNutrients, NUTRIENT_MINERALS);

                    ingredients.add(Ingredient.builder()
                            .name(name.trim())
                            .servingQuantityG(100.0)
                            .calories(calories)
                            .fat(fat)
                            .water(water)
                            .carbohydrates(carbs)
                            .protein(protein)
                            .totalMinerals(minerals)
                            .build());
                }

                log.info("  → page {}: {} ingredients total so far", pageNumber, ingredients.size());

                if (page.size() < 200) {
                    break;
                }
                pageNumber++;
            }

            ingredientRepository.saveAll(ingredients);
            log.info("Persisted {} ingredients from USDA FDC.", ingredients.size());
            return ingredients.size();

        } catch (Exception e) {
            log.error("Failed to seed ingredients from USDA FDC: {}", e.getMessage(), e);
            return 0;
        }
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
