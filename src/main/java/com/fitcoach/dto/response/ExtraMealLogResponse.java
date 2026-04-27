package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ExtraMealLogResponse {
    private Long id;
    private Long traineeId;
    private Long ingredientId;
    private String name;
    private int calories;
    private LocalDate mealDate;
    private LocalDateTime loggedAt;
}
