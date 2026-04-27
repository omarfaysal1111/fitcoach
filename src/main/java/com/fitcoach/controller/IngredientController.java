package com.fitcoach.controller;

import com.fitcoach.domain.entity.Ingredient;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.dto.response.IngredientResponse;
import com.fitcoach.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    /**
     * GET /ingredients
     * Returns all seeded ingredients with their full nutrition profile per 100 g serving.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getAllIngredients() {
        List<IngredientResponse> ingredients = ingredientRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Ingredients retrieved successfully", ingredients));
    }

    private IngredientResponse toResponse(Ingredient i) {
        return IngredientResponse.builder()
                .id(i.getId())
                .name(i.getName())
                .servingQuantityG(i.getServingQuantityG())
                .calories(i.getCalories())
                .fat(i.getFat())
                .water(i.getWater())
                .carbohydrates(i.getCarbohydrates())
                .protein(i.getProtein())
                .totalMinerals(i.getTotalMinerals())
                .build();
    }
}
