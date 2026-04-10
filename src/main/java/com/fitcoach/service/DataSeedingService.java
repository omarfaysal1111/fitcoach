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
    private static final int MAX_INGREDIENTS = 500;

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
            log.info("Startup: seeded {} ingredients from wger.", n);
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
            detail = "Ingredient seed produced 0 rows — check logs / network / wger.";
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
        log.info("Fetching up to {} English ingredients from wger...", MAX_INGREDIENTS);
        try {
            RestTemplate restTemplate = new RestTemplate();
            List<Ingredient> ingredients = new ArrayList<>();
            String url = WGER_BASE + "/ingredient/?format=json&language=2&limit=" + PAGE_SIZE + "&offset=0";

            while (url != null && ingredients.size() < MAX_INGREDIENTS) {
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

                for (Map<String, Object> item : results) {
                    if (ingredients.size() >= MAX_INGREDIENTS) {
                        break;
                    }

                    String name = (String) item.get("name");
                    if (name == null || name.isBlank()) {
                        continue;
                    }

                    Double calories = extractDouble(item.get("energy"));

                    ingredients.add(Ingredient.builder()
                            .name(name.trim())
                            .calories(calories)
                            .build());
                }

                url = ingredients.size() < MAX_INGREDIENTS ? (String) response.get("next") : null;
                log.info("  → {} ingredients parsed...", ingredients.size());
            }

            ingredientRepository.saveAll(ingredients);
            log.info("Persisted {} ingredients.", ingredients.size());
            return ingredients.size();

        } catch (Exception e) {
            log.error("Failed to seed ingredients: {}", e.getMessage(), e);
            return 0;
        }
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
