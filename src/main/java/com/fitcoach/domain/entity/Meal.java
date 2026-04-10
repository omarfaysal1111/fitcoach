package com.fitcoach.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "meals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double calories;

    private Double protein;

    private Double carbs;

    private Double fat;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "meal_ingredients",
        joinColumns = @JoinColumn(name = "meal_id"),
        inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    @Builder.Default
    private Set<Ingredient> ingredients = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nutrition_plan_id")
    @JsonIgnore
    private NutritionPlan nutritionPlan;
}
