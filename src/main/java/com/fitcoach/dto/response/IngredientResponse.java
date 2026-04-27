package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngredientResponse {

    private Long id;
    private String name;

    /**
     * The reference quantity in grams for which all nutrition values below apply.
     * All USDA Foundation data is expressed per 100 g.
     */
    private Double servingQuantityG;

    /** Energy in kcal per servingQuantityG. */
    private Double calories;

    /** Total fat (lipid) in grams per servingQuantityG. */
    private Double fat;

    /** Water content in grams per servingQuantityG. */
    private Double water;

    /** Carbohydrates (by difference) in grams per servingQuantityG. */
    private Double carbohydrates;

    /** Protein in grams per servingQuantityG. */
    private Double protein;

    /**
     * Total minerals (Ash) in grams per servingQuantityG.
     * Ash is a proxy for the total mineral content of the food.
     */
    private Double totalMinerals;
}
