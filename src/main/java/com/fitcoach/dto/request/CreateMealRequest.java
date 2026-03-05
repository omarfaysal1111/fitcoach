package com.fitcoach.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateMealRequest {
    private String name;
    private List<Long> ingredientIds;
    private Double customCalories;
}
