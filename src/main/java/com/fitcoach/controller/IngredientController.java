package com.fitcoach.controller;

import com.fitcoach.domain.entity.Ingredient;
import com.fitcoach.dto.response.ApiResponse;
import com.fitcoach.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Ingredient>>> getAllIngredients() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok("Ingredients retrieved successfully", ingredients));
    }
}
