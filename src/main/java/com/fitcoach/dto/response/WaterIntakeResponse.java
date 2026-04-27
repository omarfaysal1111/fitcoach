package com.fitcoach.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class WaterIntakeResponse {

    private LocalDate date;
    private double liters;
    private LocalDateTime updatedAt;
}
