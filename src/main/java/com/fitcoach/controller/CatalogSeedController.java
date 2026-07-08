package com.fitcoach.controller;

import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.CatalogSeedResponse;
import com.fitcoach.exception.ResourceNotFoundException;
import com.fitcoach.repository.UserRepository;
import com.fitcoach.service.DataSeedingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * On-demand wger catalog seeding (exercises + ingredients). Coach-only.
 */
@RestController
@RequestMapping("/coaches/catalog")
@RequiredArgsConstructor
public class CatalogSeedController {

    private final DataSeedingService dataSeedingService;
    private final UserRepository userRepository;

    /**
     * POST /coaches/catalog/seed?replace=false
     * <p>
     * {@code replace=true} deletes all exercises and ingredients first (requires no FK references).
     */
    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<CatalogSeedResponse>> seedCatalog(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean replace) {

        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can trigger catalog seeding"));
        }

        userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CatalogSeedResponse result = dataSeedingService.seedCatalog(replace);
        return ResponseEntity.ok(ApiResponse.ok("Catalog seed finished", result));
    }

    /**
     * POST /coaches/catalog/reseed-exercises
     * <p>
     * Rebuilds the exercise catalog from the Gym Visual dataset so every exercise's video matches
     * its name (1324 exercises, each with a unique animated GIF). FK-safe: exercises referenced by
     * plans/workouts are updated in place (IDs preserved); all others are replaced.
     */
    @PostMapping("/reseed-exercises")
    public ResponseEntity<ApiResponse<CatalogSeedResponse>> reseedExercises(Authentication authentication) {
        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can trigger catalog reseeding"));
        }

        CatalogSeedResponse result = dataSeedingService.reseedExercisesFromDataset();
        return ResponseEntity.ok(ApiResponse.ok("Exercise catalog reseeded", result));
    }

    /**
     * POST /coaches/catalog/refresh-nutrition
     * <p>
     * Recomputes ingredient nutrition from USDA (correct energy + fat nutrient numbers, Atwater
     * fallback), fixing rows that were seeded with 0 calories or 0 fat. FK-safe: updates in place.
     */
    @PostMapping("/refresh-nutrition")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> refreshNutrition(Authentication authentication) {
        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can trigger nutrition refresh"));
        }

        int updated = dataSeedingService.refreshIngredientNutrition();
        return ResponseEntity.ok(ApiResponse.ok("Ingredient nutrition refreshed", Map.of("updated", updated)));
    }

    /**
     * POST /coaches/catalog/seed-egyptian-foods
     * <p>
     * Adds Egyptian/Arabic foods alongside the existing catalog: bundled curated traditional dishes
     * plus packaged products from Open Food Facts. Additive and idempotent — never removes or edits
     * existing ingredients, so it is safe to run repeatedly.
     */
    @PostMapping("/seed-egyptian-foods")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> seedEgyptianFoods(Authentication authentication) {
        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can seed Egyptian foods"));
        }

        Map<String, Integer> result = dataSeedingService.seedEgyptianFoods();
        return ResponseEntity.ok(ApiResponse.ok("Egyptian foods added", result));
    }

    /**
     * POST /coaches/catalog/refresh-videos
     * <p>
     * Resolves each exercise's videoLink from the wger API list URL to the actual video file URL.
     * Call this once after the initial seed to fix existing records.
     */
    @PostMapping("/refresh-videos")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> refreshVideoLinks(Authentication authentication) {
        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));
        if (!isCoach) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only coaches can trigger video link refresh"));
        }

        int updated = dataSeedingService.refreshVideoLinks();
        return ResponseEntity.ok(ApiResponse.ok("Video links refreshed", Map.of("updated", updated)));
    }
}
