package com.fitcoach.dto.request;

import lombok.Data;

@Data
public class IngredientSwapRequest {
    private Long originalIngredientId;
    private Long newIngredientId;
    
    // Optional: Include this if you allow trainees to specify how much of the new ingredient they used
    private Double newQuantity; 
}