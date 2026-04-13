package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngredientResponse {
    private String id;
    private String name; 
    // Add any other specific ingredient fields here later if needed
}