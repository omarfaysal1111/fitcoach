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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataSeedingService implements CommandLineRunner {

    private static final String WGER_BASE = "https://wger.de/api/v2";
    private static final int PAGE_SIZE = 100;
    // Gym Visual exercise dataset (hasaneyldrm/exercises-dataset): 1324 exercises, each with a
    // unique animated GIF, target muscle, and multilingual step instructions. Free (MIT), no API
    // key; media served straight from raw.githubusercontent.com.
    private static final String GYMVISUAL_JSON =
            "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/data/exercises.json";
    private static final String GYMVISUAL_MEDIA_BASE =
            "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/";

    // USDA FDC Foundation Foods API
    private static final String USDA_FDC_API_KEY = "mLBKvhwX2xRVLpcswGHJmAxgvTB14Folwkhw9SSs";
    private static final String USDA_FDC_BASE =
            "https://api.nal.usda.gov/fdc/v1/foods/list?dataType=Foundation&pageSize=200&api_key=" + USDA_FDC_API_KEY;

    // Open Food Facts — Egyptian products with completed nutrition, most-scanned first.
    private static final String OFF_EGYPT_SEARCH =
            "https://world.openfoodfacts.org/api/v2/search"
          + "?countries_tags_en=egypt&states_tags_en=nutrition-facts-completed"
          + "&fields=product_name,product_name_ar,nutriments&sort_by=unique_scans_n&page_size=100";
    private static final int OFF_MAX_PAGES = 20;      // cap ingestion (~2000 products)
    private static final double MAX_KCAL_PER_100G = 1000.0; // sanity ceiling (pure fat ≈ 900)
    private static final String USER_AGENT = "FitCoach/1.0 (omarfaysal44@gmail.com)";
    private static final String EGYPTIAN_FOODS_RESOURCE = "data/egyptian-foods.json";

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
            log.info("Startup: seeded {} exercises from Gym Visual dataset.", n);
        } else {
            int fixed = migrateWgerVideoLinks();
            if (fixed > 0) {
                log.info("Startup: migrated {} wger video links → Gym Visual gif URLs.", fixed);
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
     * For exercises already in the DB that still have wger/null videoLinks, fetch the Gym Visual
     * dataset and assign the correct gif URL by <em>exact</em> (normalized) name match only.
     *
     * <p>Historically this did fuzzy substring + muscle-group fallback matching, which forced a
     * video onto every exercise even with no real match — attaching the same demonstration GIF to
     * dozens of unrelated movements. That produced the "name and video don't match" corruption.
     * A wrong video is worse than none, so unmatched exercises are now left untouched. To give the
     * whole catalog correct videos, use {@link #reseedExercisesFromDataset()} instead.
     *
     * <p>Updates videoLink in-place — does NOT delete rows, so FK references are safe.
     */
    private int migrateWgerVideoLinks() {
        List<Exercise> stale = exerciseRepository.findAll().stream()
                .filter(e -> e.getVideoLink() == null || e.getVideoLink().contains("wger.de"))
                .toList();
        if (stale.isEmpty()) return 0;

        log.info("Migrating {} exercises with wger/null video links (exact match only)...", stale.size());
        List<Map<String, Object>> raw = fetchGymVisualDataset();
        if (raw == null || raw.isEmpty()) {
            log.warn("Gym Visual dataset unavailable — skipping migration.");
            return 0;
        }

        Map<String, String> gifByName = new java.util.HashMap<>();
        for (Map<String, Object> ex : raw) {
            String name = (String) ex.get("name");
            String gif  = gifUrlOf(ex);
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
     * Truncate-and-reseed: wipes the exercise catalog and everything that references it, then seeds
     * the full Gym Visual dataset (1324 exercises) fresh.
     *
     * <p><b>Destructive.</b> {@code TRUNCATE ... CASCADE} clears {@code exercises} plus every table
     * that (transitively) references it — plan exercise assignments and trainee workout logs
     * included — atomically, and restarts the identity so IDs begin at 1. CASCADE resolves the FK
     * chain via the catalog itself, so (unlike introspecting {@code information_schema}) it does not
     * depend on the connecting role owning the tables. The dataset is fetched before anything is
     * deleted, so a fetch failure changes nothing, and any DB error is caught and returned in
     * {@code detail} rather than surfacing as a generic 500.
     */
    public CatalogSeedResponse reseedExercisesFromDataset() {
        List<Map<String, Object>> raw = fetchGymVisualDataset();
        if (raw == null || raw.isEmpty()) {
            return CatalogSeedResponse.builder()
                    .exercisesSaved(0).ingredientsSaved(0).replacedExisting(false)
                    .detail("Gym Visual dataset unavailable — no changes made.").build();
        }

        try {
            return transactionTemplate.execute(status -> {
                int before = ((Number) entityManager
                        .createNativeQuery("SELECT count(*) FROM exercises").getSingleResult()).intValue();
                entityManager.createNativeQuery("TRUNCATE TABLE exercises RESTART IDENTITY CASCADE").executeUpdate();

                List<Exercise> fresh = new ArrayList<>();
                for (Map<String, Object> ex : raw) {
                    String name = (String) ex.get("name");
                    if (name == null || name.isBlank()) continue;
                    fresh.add(toExercise(ex));
                }
                exerciseRepository.saveAll(fresh);

                log.info("Reseed (truncate cascade): replaced {} exercises with {} fresh.", before, fresh.size());
                return CatalogSeedResponse.builder()
                        .exercisesSaved(fresh.size()).ingredientsSaved(0).replacedExisting(true)
                        .detail(String.format(
                                "Truncated (cascade) and reseeded from Gym Visual: replaced %d exercises with %d fresh (IDs restarted at 1); dependent plan/log rows were also cleared.",
                                before, fresh.size()))
                        .build();
            });
        } catch (Exception e) {
            log.error("Reseed truncate failed", e);
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            return CatalogSeedResponse.builder()
                    .exercisesSaved(0).ingredientsSaved(0).replacedExisting(false)
                    .detail("Reseed failed: " + root.getClass().getSimpleName() + ": " + root.getMessage())
                    .build();
        }
    }

    /** Lower-cases, strips punctuation to spaces, and collapses whitespace for tolerant matching. */
    private static String normalizeName(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private int fetchAndPersistExercises() {
        log.info("Fetching exercises from Gym Visual dataset...");
        List<Map<String, Object>> raw = fetchGymVisualDataset();
        if (raw == null || raw.isEmpty()) {
            log.warn("Gym Visual dataset returned empty response.");
            return 0;
        }

        List<Exercise> exercises = new ArrayList<>();
        for (Map<String, Object> ex : raw) {
            String name = (String) ex.get("name");
            if (name == null || name.isBlank()) continue;
            exercises.add(toExercise(ex));
        }

        exerciseRepository.saveAll(exercises);
        log.info("Persisted {} exercises from Gym Visual dataset.", exercises.size());
        return exercises.size();
    }

    /** Fetches and parses the Gym Visual dataset, or {@code null} on any failure. */
    private List<Map<String, Object>> fetchGymVisualDataset() {
        try {
            String json = new RestTemplate().getForObject(GYMVISUAL_JSON, String.class);
            if (json == null || json.isBlank()) return null;
            return new ObjectMapper().readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to fetch Gym Visual dataset: {}", e.getMessage(), e);
            return null;
        }
    }

    /** Builds a new {@link Exercise} from a raw Gym Visual record (name and gif_url always paired). */
    private static Exercise toExercise(Map<String, Object> ex) {
        Exercise e = new Exercise();
        applyDatasetFields(e, ex);
        return e;
    }

    /**
     * Overwrites an exercise's catalog fields from a raw Gym Visual record. Target muscle falls back
     * target → muscle_group → body_part; description is built from the English instruction steps.
     */
    private static void applyDatasetFields(Exercise e, Map<String, Object> ex) {
        String name = (String) ex.get("name");
        String muscle = firstNonBlank(
                (String) ex.get("target"),
                (String) ex.get("muscle_group"),
                (String) ex.get("body_part"));

        e.setName(capitalize(name.trim()));
        e.setDescription(buildDescription(englishSteps(ex)));
        e.setTargetedMuscle(muscle == null ? "General" : capitalize(muscle));
        e.setVideoLink(gifUrlOf(ex));
    }

    /** English step list from {@code instruction_steps.en}, else the single {@code instructions.en} paragraph. */
    @SuppressWarnings("unchecked")
    private static List<String> englishSteps(Map<String, Object> ex) {
        Object steps = ex.get("instruction_steps");
        if (steps instanceof Map) {
            Object en = ((Map<String, Object>) steps).get("en");
            if (en instanceof List && !((List<?>) en).isEmpty()) return (List<String>) en;
        }
        Object ins = ex.get("instructions");
        if (ins instanceof Map) {
            Object en = ((Map<String, Object>) ins).get("en");
            if (en instanceof String && !((String) en).isBlank()) return List.of((String) en);
        }
        return List.of();
    }

    /** Absolute GIF URL from the dataset's relative {@code gif_url} path, or {@code null} if absent. */
    private static String gifUrlOf(Map<String, Object> ex) {
        Object g = ex.get("gif_url");
        if (!(g instanceof String) || ((String) g).isBlank()) return null;
        String path = (String) g;
        return path.startsWith("http") ? path : GYMVISUAL_MEDIA_BASE + path;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
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

    /**
     * Adds Egyptian/Arabic foods alongside the existing (USDA) catalog: a bundled set of curated
     * traditional dishes (koshari, ful, molokhia…) plus packaged products from Open Food Facts.
     * Additive and idempotent — skips any food whose name already exists (normalized), so it can be
     * run repeatedly and never touches or removes existing ingredients (FK-safe).
     *
     * @return counts: {@code curatedAdded}, {@code packagedAdded}, {@code totalAdded}
     */
    public Map<String, Integer> seedEgyptianFoods() {
        Set<String> seen = ingredientRepository.findAll().stream()
                .map(i -> normalizeName(i.getName()))
                .collect(Collectors.toCollection(HashSet::new));

        List<Ingredient> toAdd = new ArrayList<>();
        int curated  = addCuratedEgyptianDishes(seen, toAdd);
        int packaged = addOpenFoodFactsEgypt(seen, toAdd);

        ingredientRepository.saveAll(toAdd);
        log.info("seedEgyptianFoods: added {} curated dishes + {} packaged products = {} new ingredients.",
                curated, packaged, toAdd.size());
        return Map.of("curatedAdded", curated, "packagedAdded", packaged, "totalAdded", toAdd.size());
    }

    /** Loads the bundled curated Egyptian dishes and appends any not already present. */
    private int addCuratedEgyptianDishes(Set<String> seen, List<Ingredient> out) {
        List<Map<String, Object>> dishes;
        try {
            dishes = new ObjectMapper().readValue(
                    new ClassPathResource(EGYPTIAN_FOODS_RESOURCE).getInputStream(),
                    new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Could not load curated Egyptian foods resource: {}", e.getMessage());
            return 0;
        }

        int added = 0;
        for (Map<String, Object> d : dishes) {
            String ar = str(d.get("name_ar"));
            String en = str(d.get("name_en"));
            String name = ar.isBlank() ? en : (en.isBlank() ? ar : ar + " (" + en + ")");
            if (name.isBlank() || !seen.add(normalizeName(name))) continue;

            out.add(Ingredient.builder()
                    .name(trim255(name))
                    .servingQuantityG(100.0)
                    .calories(extractDouble(d.get("calories")))
                    .fat(extractDouble(d.get("fat")))
                    .carbohydrates(extractDouble(d.get("carbohydrates")))
                    .protein(extractDouble(d.get("protein")))
                    .build());
            added++;
        }
        return added;
    }

    /** Fetches Egyptian packaged products from Open Food Facts and appends usable, non-duplicate ones. */
    @SuppressWarnings("unchecked")
    private int addOpenFoodFactsEgypt(Set<String> seen, List<Ingredient> out) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT); // Open Food Facts requires a UA
        HttpEntity<Void> request = new HttpEntity<>(headers);

        int added = 0;
        for (int page = 1; page <= OFF_MAX_PAGES; page++) {
            List<Map<String, Object>> products;
            try {
                ResponseEntity<Map> resp = restTemplate.exchange(
                        OFF_EGYPT_SEARCH + "&page=" + page, HttpMethod.GET, request, Map.class);
                Map<String, Object> body = resp.getBody();
                products = body == null ? null : (List<Map<String, Object>>) body.get("products");
            } catch (Exception e) {
                log.warn("Open Food Facts page {} failed: {} — stopping import.", page, e.getMessage());
                break;
            }
            if (products == null || products.isEmpty()) break;

            try {
                Thread.sleep(300); // be polite to Open Food Facts; avoids burst 503s
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }

            for (Map<String, Object> p : products) {
                Map<String, Object> nutr = (Map<String, Object>) p.get("nutriments");
                if (nutr == null) continue;

                Double kcal = extractDouble(nutr.get("energy-kcal_100g"));
                if (kcal == null || kcal <= 0 || kcal > MAX_KCAL_PER_100G) continue;

                String ar = str(p.get("product_name_ar"));
                String en = str(p.get("product_name"));
                String name = (!ar.isBlank() && !ar.equals(".")) ? ar : en;
                if (name.isBlank() || !seen.add(normalizeName(name))) continue;

                out.add(Ingredient.builder()
                        .name(trim255(name.trim()))
                        .servingQuantityG(100.0)
                        .calories(kcal)
                        .fat(extractDouble(nutr.get("fat_100g")))
                        .carbohydrates(extractDouble(nutr.get("carbohydrates_100g")))
                        .protein(extractDouble(nutr.get("proteins_100g")))
                        .build());
                added++;
            }

            if (products.size() < 100) break; // last page
        }
        return added;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String trim255(String s) {
        return s.length() > 255 ? s.substring(0, 255) : s;
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
