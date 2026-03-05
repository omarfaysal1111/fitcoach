package com.fitcoach.service;

import com.fitcoach.domain.entity.Exercise;
import com.fitcoach.domain.entity.Ingredient;
import com.fitcoach.repository.ExerciseRepository;
import com.fitcoach.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeedingService implements CommandLineRunner {

    private final ExerciseRepository exerciseRepository;
    private final IngredientRepository ingredientRepository;

    @Override
    public void run(String... args) {
        seedExercises();
        seedIngredients();
    }

    private void seedExercises() {
        if (exerciseRepository.count() > 0) {
            log.info("Exercises already seeded.");
            return;
        }
        log.info("Seeding exercises from open source API...");
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Using wger API for exercises as an example of open source DB
            String url = "https://wger.de/api/v2/exerciseinfo/?language=2&limit=20";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                List<Exercise> exercises = new ArrayList<>();
                for (Map<String, Object> result : results) {
                    String name = "Unknown Exercise";
                    String description = "";
                    if (result.containsKey("translations")) {
                        List<Map<String, Object>> translations = (List<Map<String, Object>>) result.get("translations");
                        for (Map<String, Object> t : translations) {
                            if (Integer.valueOf(2).equals(t.get("language"))) {
                                name = (String) t.get("name");
                                description = (String) t.get("description");
                                break;
                            }
                        }
                        if (name.equals("Unknown Exercise") && !translations.isEmpty()) {
                            name = (String) translations.get(0).get("name");
                            description = (String) translations.get(0).get("description");
                        }
                    }
                    
                    Map<String, Object> category = (Map<String, Object>) result.get("category");
                    String categoryName = category != null ? (String) category.get("name") : "General";

                    Exercise exercise = Exercise.builder()
                            .name(name)
                            .description(description)
                            .videoLink("https://wger.de/api/v2/video/?exercise=" + result.get("id"))
                            .targetedMuscle(categoryName)
                            .build();
                    exercises.add(exercise);
                }
                exerciseRepository.saveAll(exercises);
                log.info("Seeded {} exercises.", exercises.size());
            }
        } catch (Exception e) {
            log.error("Failed to seed exercises: {}", e.getMessage());
        }
    }

    private void seedIngredients() {
        if (ingredientRepository.count() > 0) {
            log.info("Ingredients already seeded.");
            return;
        }
        log.info("Seeding ingredients from open source API...");
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Using a public open food facts search API for basic ingredients
            String url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=basic&search_simple=1&action=process&json=1&page_size=20";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("products")) {
                List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("products");
                List<Ingredient> ingredients = new ArrayList<>();
                for (Map<String, Object> product : products) {
                    String name = (String) product.get("product_name");
                    Map<String, Object> nutriments = (Map<String, Object>) product.get("nutriments");
                    Double calories = 0.0;
                    if (nutriments != null && nutriments.containsKey("energy-kcal_100g")) {
                        Object kcal = nutriments.get("energy-kcal_100g");
                        if (kcal instanceof Number) {
                            calories = ((Number) kcal).doubleValue();
                        } else if (kcal instanceof String) {
                            try {
                                calories = Double.parseDouble((String) kcal);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (name != null && !name.isEmpty()) {
                        Ingredient ingredient = Ingredient.builder()
                                .name(name)
                                .calories(calories)
                                .build();
                        ingredients.add(ingredient);
                    }
                }
                ingredientRepository.saveAll(ingredients);
                log.info("Seeded {} ingredients.", ingredients.size());
            }
        } catch (Exception e) {
            log.error("Failed to seed ingredients: {}", e.getMessage());
        }
    }
}
