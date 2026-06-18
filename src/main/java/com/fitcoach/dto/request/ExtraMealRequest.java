package com.fitcoach.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExtraMealRequest {

    /**
     * Optional: resolves the ingredient's name automatically when provided.
     * If omitted, {@link #name} is used as a free-text label.
     */
    private Long ingredientId;

    /**
     * Free-text meal name. Required when {@code ingredientId} is not supplied.
     */
    private String name;

    /**
     * Optional parent meal to nest this item into (SCRUM-62). When provided the
     * item is attached to that meal instead of being logged as a separate entry.
     */
    private Long mealId;

    @NotNull
    @Min(1)
    private Integer calories;

    /**
     * Date this extra meal was consumed. Defaults to today when not supplied.
     */
    private LocalDate date;
}
