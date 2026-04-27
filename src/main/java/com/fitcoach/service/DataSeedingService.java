package com.fitcoach.service;

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
            log.info("Startup: seeded {} exercises from wger.", n);
        } else {
            log.info("Exercises already present — skipping startup seed.");
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

    private int fetchAndPersistExercises() {
        log.info("Fetching English exercises from wger...");
        try {
            RestTemplate restTemplate = new RestTemplate();
            List<Exercise> exercises = new ArrayList<>();
            String url = WGER_BASE + "/exerciseinfo/?language=2&limit=" + PAGE_SIZE + "&offset=0";

            while (url != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response == null) {
                    break;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results =
                        (List<Map<String, Object>>) response.get("results");
                if (results == null || results.isEmpty()) {
                    break;
                }

                for (Map<String, Object> result : results) {
                    String name = null;
                    String description = "";

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> translations =
                            (List<Map<String, Object>>) result.get("translations");

                    if (translations != null) {
                        for (Map<String, Object> t : translations) {
                            if (Integer.valueOf(2).equals(t.get("language"))) {
                                name = (String) t.get("name");
                                description = (String) t.get("description");
                                break;
                            }
                        }
                        if ((name == null || name.isBlank()) && !translations.isEmpty()) {
                            name = (String) translations.get(0).get("name");
                            description = (String) translations.get(0).get("description");
                        }
                    }

                    if (name == null || name.isBlank()) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> category = (Map<String, Object>) result.get("category");
                    String muscle = category != null ? (String) category.get("name") : "General";

                    exercises.add(Exercise.builder()
                            .name(name.trim())
                            .description(description != null ? description.trim() : "")
                            .targetedMuscle(muscle)
                            .videoLink(WGER_BASE + "/video/?exercise=" + result.get("id"))
                            .build());
                }

                url = (String) response.get("next");
                log.info("  → {} exercises parsed...", exercises.size());
            }

            exerciseRepository.saveAll(exercises);
            log.info("Persisted {} exercises.", exercises.size());
            return exercises.size();

        } catch (Exception e) {
            log.error("Failed to seed exercises: {}", e.getMessage(), e);
            return 0;
        }
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
}
