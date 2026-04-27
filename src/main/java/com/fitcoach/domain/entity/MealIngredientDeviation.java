package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Records a single ingredient-level deviation for a meal completion.
 * - replacementIngredient == null  → ingredient was skipped entirely
 * - replacementIngredient != null  → ingredient was swapped for another one
 */
@Entity
@Table(name = "meal_ingredient_deviations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealIngredientDeviation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "completion_id")
    private TraineeMealCompletion completion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "original_ingredient_id")
    private Ingredient originalIngredient;

    /** Null when the ingredient was skipped rather than swapped. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_ingredient_id")
    private Ingredient replacementIngredient;

    /** Quantity of the replacement ingredient used (null when skipped). */
    @Column(name = "new_quantity")
    private Double newQuantity;
}
