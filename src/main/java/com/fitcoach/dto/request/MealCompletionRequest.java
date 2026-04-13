package com.fitcoach.dto.request;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class MealCompletionRequest {
    // If true, the trainee skipped the entire meal. 
    // The service can ignore the lists below if this is true.
    private boolean skipMeal; 

    // IDs of ingredients the trainee decided not to eat
    private List<Long> skippedIngredientIds = new ArrayList<>();

    // Details of ingredients the trainee swapped out
    private List<IngredientSwapRequest> replacedIngredients = new ArrayList<>();
}