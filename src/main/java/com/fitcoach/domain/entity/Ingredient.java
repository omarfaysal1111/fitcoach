package com.fitcoach.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Reference quantity in grams for which all nutrition values below are given (always 100 g for USDA Foundation data). */
    @Builder.Default
    private Double servingQuantityG = 100.0;

    /** Energy in kcal per servingQuantityG. */
    private Double calories;

    /** Total lipid (fat) in grams per servingQuantityG. */
    private Double fat;

    /** Water in grams per servingQuantityG. */
    private Double water;

    /** Carbohydrate (by difference) in grams per servingQuantityG. */
    private Double carbohydrates;

    /** Protein in grams per servingQuantityG. */
    private Double protein;

    /** Total minerals (Ash) in grams per servingQuantityG. */
    private Double totalMinerals;
}
